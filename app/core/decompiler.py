"""
decompiler.py — Wrapper around external Java decompiler tools.

Supported decompilers (JAR must be placed in the tools/ directory):
  - Vineflower  (vineflower.jar)   https://github.com/Vineflower/vineflower
  - CFR         (cfr.jar)          https://github.com/leibnitz27/cfr
  - FernFlower  (fernflower.jar)   bundled with IntelliJ IDEA
"""

import os
import shutil
import subprocess
from pathlib import Path
from typing import Callable, List, Optional

DECOMPILER_JARS: dict[str, str] = {
    "Vineflower": "vineflower.jar",
    "CFR": "cfr.jar",
    "FernFlower": "fernflower.jar",
}

DECOMPILER_DOWNLOAD_URLS: dict[str, str] = {
    "Vineflower": "https://github.com/Vineflower/vineflower/releases/latest",
    "CFR": "https://github.com/leibnitz27/cfr/releases/latest",
    "FernFlower": "Extract from an IntelliJ IDEA installation",
}


class DecompilerError(Exception):
    pass


class Decompiler:
    def __init__(self, tools_dir: Path) -> None:
        self.tools_dir = tools_dir
        self.tools_dir.mkdir(parents=True, exist_ok=True)

    # ------------------------------------------------------------------
    # Discovery helpers
    # ------------------------------------------------------------------

    def find_java(self) -> Optional[str]:
        """Return the path to the ``java`` executable, or *None*."""
        java_home = os.environ.get("JAVA_HOME")
        if java_home:
            for candidate in ("bin/java.exe", "bin/java"):
                exe = Path(java_home) / candidate
                if exe.exists():
                    return str(exe)
        return shutil.which("java")

    def get_available(self) -> List[str]:
        """Return names of decompilers whose JARs exist in *tools_dir*."""
        return [
            name
            for name, jar in DECOMPILER_JARS.items()
            if (self.tools_dir / jar).exists()
        ]

    def get_jar_path(self, decompiler: str) -> Optional[Path]:
        jar_name = DECOMPILER_JARS.get(decompiler)
        if not jar_name:
            return None
        p = self.tools_dir / jar_name
        return p if p.exists() else None

    # ------------------------------------------------------------------
    # Decompilation
    # ------------------------------------------------------------------

    def decompile(
        self,
        jar_path: Path,
        output_dir: Path,
        decompiler: str = "Vineflower",
        log_callback: Optional[Callable[[str], None]] = None,
    ) -> bool:
        """
        Decompile *jar_path* into *output_dir*.

        Raises :class:`DecompilerError` on failure.
        Returns ``True`` on success.
        """
        java = self.find_java()
        if not java:
            raise DecompilerError(
                "Java not found. Install Java 17+ and ensure it is on PATH "
                "or set the JAVA_HOME environment variable."
            )

        jar = self.get_jar_path(decompiler)
        if not jar:
            url = DECOMPILER_DOWNLOAD_URLS.get(decompiler, "unknown")
            target = self.tools_dir / DECOMPILER_JARS.get(decompiler, "")
            raise DecompilerError(
                f"{decompiler} JAR not found.\n"
                f"Download from: {url}\n"
                f"Place it at:   {target}"
            )

        output_dir.mkdir(parents=True, exist_ok=True)

        if decompiler == "Vineflower":
            cmd = [java, "-jar", str(jar), str(jar_path), str(output_dir)]
        elif decompiler == "CFR":
            cmd = [java, "-jar", str(jar), str(jar_path), "--outputdir", str(output_dir)]
        elif decompiler == "FernFlower":
            cmd = [java, "-jar", str(jar), str(jar_path), str(output_dir)]
        else:
            raise DecompilerError(f"Unknown decompiler: {decompiler!r}")

        if log_callback:
            log_callback(f"Running: {' '.join(cmd)}")

        try:
            process = subprocess.Popen(
                cmd,
                stdout=subprocess.PIPE,
                stderr=subprocess.STDOUT,
                text=True,
                encoding="utf-8",
                errors="replace",
            )
            for line in process.stdout:  # type: ignore[union-attr]
                stripped = line.rstrip()
                if stripped and log_callback:
                    log_callback(stripped)
            process.wait()
        except FileNotFoundError:
            raise DecompilerError(f"Could not start Java: {java}")

        if process.returncode != 0:
            raise DecompilerError(
                f"Decompiler exited with code {process.returncode}"
            )

        return True
