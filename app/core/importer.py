"""
importer.py — Orchestrates the full mod-import workflow.

Steps:
  1. Detect metadata
  2. Build target directory  (Repository/Versions/<mc>/<loader>/<mod>/)
  3. Extract JAR
  4. Copy original JAR
  5. Decompile  (optional — skipped if decompiler / Java is missing)
  6. Copy raw decompiled output → Decompiled/
  7. Sort assets, data, config, Java sources
  8. Write metadata JSONs
  9. Generate report
 10. Write import log
"""

import shutil
import tempfile
import zipfile
from datetime import datetime
from pathlib import Path
from typing import Callable, Optional

from .decompiler import Decompiler, DecompilerError
from .detector import ModInfo, detect_mod_info
from .metadata_gen import write_metadata
from .reporter import generate_report
from .sorter import FileSorter
from ..utils.helpers import sanitize_folder_name


class ImportError(Exception):
    pass


class ModImporter:
    def __init__(self, repository_path: Path, tools_dir: Path) -> None:
        self.repository_path = repository_path
        self.tools_dir = tools_dir
        self.decompiler = Decompiler(tools_dir)

    def import_mod(
        self,
        jar_path: Path,
        decompiler_name: str = "Vineflower",
        progress_callback: Optional[Callable[[int, str], None]] = None,
        log_callback: Optional[Callable[[str], None]] = None,
    ) -> Path:
        """
        Import *jar_path* into the repository.

        Parameters
        ----------
        jar_path:
            Path to the ``.jar`` file to import.
        decompiler_name:
            One of ``"Vineflower"``, ``"CFR"``, ``"FernFlower"``.
        progress_callback:
            Called with ``(percent: int, message: str)`` throughout the process.
        log_callback:
            Called with individual log lines.

        Returns
        -------
        Path
            The mod directory that was created or updated.
        """
        errors: list[str] = []
        warnings: list[str] = []

        def progress(pct: int, msg: str) -> None:
            if progress_callback:
                progress_callback(pct, msg)
            if log_callback:
                log_callback(f"[{pct:3d}%] {msg}")

        def log(msg: str) -> None:
            if log_callback:
                log_callback(msg)

        # ── 1. Detect metadata ──────────────────────────────────────────
        progress(5, f"Detecting metadata from {jar_path.name} …")
        mod_info: ModInfo = detect_mod_info(jar_path)
        log(
            f"Detected: {mod_info.mod_name}  |  "
            f"Loader: {mod_info.loader}  |  "
            f"MC: {mod_info.minecraft_version}"
        )

        # ── 2. Resolve target directory ─────────────────────────────────
        progress(10, "Preparing repository structure …")
        folder_name = sanitize_folder_name(mod_info.mod_name)
        mc_ver = sanitize_folder_name(mod_info.minecraft_version or "Unknown")
        loader = sanitize_folder_name(mod_info.loader or "Unknown")

        mod_dir = self.repository_path / "Versions" / mc_ver / loader / folder_name

        if mod_dir.exists():
            warnings.append(
                "Mod directory already exists — files will be merged/overwritten."
            )
            log("[WARN] Directory exists; merging …")

        mod_dir.mkdir(parents=True, exist_ok=True)

        # ── 3 & 4. Extract JAR + copy original ─────────────────────────
        with tempfile.TemporaryDirectory(prefix="projectdecom_") as tmp_str:
            tmp = Path(tmp_str)
            extract_dir = tmp / "extracted"
            decompiled_dir = tmp / "decompiled"
            extract_dir.mkdir()
            decompiled_dir.mkdir()

            progress(20, "Extracting JAR …")
            try:
                with zipfile.ZipFile(jar_path, "r") as zf:
                    zf.extractall(extract_dir)
            except zipfile.BadZipFile as exc:
                raise ImportError(f"Invalid JAR file: {exc}") from exc

            progress(25, "Copying original JAR …")
            shutil.copy2(jar_path, mod_dir / jar_path.name)

            # ── 5. Decompile ────────────────────────────────────────────
            progress(30, f"Decompiling with {decompiler_name} …")
            decompile_ok = False

            if not self.decompiler.get_jar_path(decompiler_name):
                msg = (
                    f"{decompiler_name} JAR not found in tools/. "
                    "Decompilation skipped."
                )
                warnings.append(msg)
                log(f"[WARN] {msg}")
            elif not self.decompiler.find_java():
                msg = "Java not found — decompilation skipped."
                warnings.append(msg)
                log(f"[WARN] {msg}")
            else:
                try:
                    self.decompiler.decompile(
                        jar_path=jar_path,
                        output_dir=decompiled_dir,
                        decompiler=decompiler_name,
                        log_callback=log,
                    )
                    decompile_ok = True
                    log("Decompilation complete.")
                except DecompilerError as exc:
                    errors.append(str(exc))
                    log(f"[ERROR] Decompilation failed: {exc}")

            # ── 6. Copy raw decompiled source ───────────────────────────
            progress(60, "Copying decompiled source …")
            if decompile_ok and decompiled_dir.exists():
                target_decompiled = mod_dir / "Decompiled"
                target_decompiled.mkdir(exist_ok=True)
                shutil.copytree(
                    decompiled_dir, target_decompiled, dirs_exist_ok=True
                )

            # ── 7. Sort files ───────────────────────────────────────────
            progress(70, "Sorting files …")
            sorter = FileSorter(log_callback=log)
            sorter.sort_all(
                extracted_dir=extract_dir,
                decompiled_dir=decompiled_dir if decompile_ok else None,
                mod_dir=mod_dir,
            )
            log(
                f"Sorted: {sorter.stats['textures']} textures, "
                f"{sorter.stats['models']} models, "
                f"{sorter.stats['java_classes']} Java classes, "
                f"{sorter.stats['recipes']} recipes."
            )

            # ── 8. Write metadata ───────────────────────────────────────
            progress(85, "Writing metadata …")
            write_metadata(mod_dir, mod_info, decompiler_name)

            # ── 9. Generate report ──────────────────────────────────────
            progress(93, "Generating report …")
            generate_report(
                mod_dir=mod_dir,
                mod_info=mod_info,
                stats=sorter.stats,
                errors=errors,
                warnings=warnings,
                decompiler=decompiler_name,
            )

            # ── 10. Write log ───────────────────────────────────────────
            logs_dir = mod_dir / "Logs"
            logs_dir.mkdir(exist_ok=True)
            stamp = datetime.now().strftime("%Y%m%d_%H%M%S")
            log_path = logs_dir / f"import_{stamp}.log"
            log_path.write_text(
                "\n".join(
                    (warnings and ["WARNINGS:"] + warnings or [])
                    + (errors and ["ERRORS:"] + errors or [])
                ),
                encoding="utf-8",
            )

        progress(100, f"Done — {mod_info.mod_name}")
        return mod_dir
