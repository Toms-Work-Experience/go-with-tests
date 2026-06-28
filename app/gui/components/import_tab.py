"""
import_tab.py — The main import panel.

Features:
  • Drag-and-drop zone for .jar files (requires tkinterdnd2)
  • Browse button
  • Decompiler selector
  • Import button with per-file progress
  • Scrollable log / console output
"""

import queue
import re
import threading
from pathlib import Path
from tkinter import filedialog
from typing import Any, Callable, Optional

import customtkinter as ctk

from ...core.importer import ModImporter


class ImportTab(ctk.CTkFrame):
    def __init__(
        self,
        parent: Any,
        importer: ModImporter,
        on_import_complete: Callable[[Path], None],
    ) -> None:
        super().__init__(parent, fg_color="transparent")
        self.importer = importer
        self.on_import_complete = on_import_complete

        self._files: list[Path] = []
        self._import_thread: Optional[threading.Thread] = None
        self._log_queue: queue.Queue = queue.Queue()

        self._build()
        self._setup_dnd()
        self._poll()

    # ── Build ─────────────────────────────────────────────────────────────

    def _build(self) -> None:
        self.columnconfigure(0, weight=1)
        self.rowconfigure(5, weight=1)

        # ── Drop zone ────────────────────────────────────────────────────
        self._drop_zone = ctk.CTkFrame(
            self,
            height=130,
            fg_color=("#d8d8d8", "#252535"),
            border_width=2,
            border_color=("#aaa", "#444"),
            corner_radius=12,
        )
        self._drop_zone.grid(row=0, column=0, sticky="ew", padx=12, pady=(12, 6))
        self._drop_zone.grid_propagate(False)
        self._drop_zone.columnconfigure(0, weight=1)
        self._drop_zone.rowconfigure(0, weight=1)

        self._drop_label = ctk.CTkLabel(
            self._drop_zone,
            text="⬇  Drop .jar files here  or  click Browse",
            font=ctk.CTkFont(size=14),
            text_color=("#666", "#888"),
        )
        self._drop_label.grid(row=0, column=0)
        self._drop_zone.bind("<Button-1>", lambda _e: self._browse())
        self._drop_label.bind("<Button-1>", lambda _e: self._browse())

        # ── Controls row ─────────────────────────────────────────────────
        ctrl = ctk.CTkFrame(self, fg_color="transparent")
        ctrl.grid(row=1, column=0, sticky="ew", padx=12, pady=(0, 4))

        ctk.CTkButton(ctrl, text="Browse Files", width=120, command=self._browse).pack(
            side="left", padx=(0, 8)
        )

        ctk.CTkLabel(ctrl, text="Decompiler:").pack(side="left", padx=(0, 4))
        self._decompiler_var = ctk.StringVar(value="Vineflower")
        ctk.CTkOptionMenu(
            ctrl,
            values=["Vineflower", "CFR", "FernFlower"],
            variable=self._decompiler_var,
            width=140,
        ).pack(side="left", padx=(0, 8))

        self._import_btn = ctk.CTkButton(
            ctrl, text="Import Mods", width=130,
            command=self._start_import,
            fg_color="#1f6aa5",
        )
        self._import_btn.pack(side="left", padx=(0, 6))

        ctk.CTkButton(
            ctrl, text="Clear", width=70,
            command=self._clear_files,
            fg_color=("#666", "#444"),
        ).pack(side="left")

        # ── File list label ──────────────────────────────────────────────
        self._file_label = ctk.CTkLabel(
            self, text="No files selected.", anchor="w",
            text_color=("#666", "#777"),
        )
        self._file_label.grid(row=2, column=0, sticky="ew", padx=14, pady=(0, 4))

        # ── Progress ─────────────────────────────────────────────────────
        prog_frame = ctk.CTkFrame(self, fg_color="transparent")
        prog_frame.grid(row=3, column=0, sticky="ew", padx=12, pady=(0, 4))
        prog_frame.columnconfigure(0, weight=1)

        self._progress_bar = ctk.CTkProgressBar(prog_frame)
        self._progress_bar.grid(row=0, column=0, sticky="ew", pady=(0, 2))
        self._progress_bar.set(0)

        self._progress_label = ctk.CTkLabel(
            prog_frame, text="", anchor="w",
            text_color=("#555", "#888"), font=ctk.CTkFont(size=11),
        )
        self._progress_label.grid(row=1, column=0, sticky="ew")

        # ── Console header ───────────────────────────────────────────────
        console_header = ctk.CTkFrame(self, fg_color="transparent")
        console_header.grid(row=4, column=0, sticky="ew", padx=12, pady=(4, 2))

        ctk.CTkLabel(
            console_header, text="Console Output",
            font=ctk.CTkFont(size=12, weight="bold"), anchor="w",
        ).pack(side="left")

        ctk.CTkButton(
            console_header, text="Clear", width=60, height=24,
            command=self._clear_console,
            fg_color=("#666", "#444"),
        ).pack(side="right")

        # ── Console ──────────────────────────────────────────────────────
        self._console = ctk.CTkTextbox(
            self,
            font=ctk.CTkFont(family="Courier New", size=11),
            fg_color=("#f0f0f0", "#111118"),
            text_color=("#222", "#b0b0c0"),
            wrap="word",
        )
        self._console.grid(row=5, column=0, sticky="nsew", padx=12, pady=(0, 12))
        self._console.configure(state="disabled")

    # ── Drag-and-drop ─────────────────────────────────────────────────────

    def _setup_dnd(self) -> None:
        try:
            from tkinterdnd2 import DND_FILES  # type: ignore

            self._drop_zone.drop_target_register(DND_FILES)
            self._drop_zone.dnd_bind("<<Drop>>", self._on_drop)
            self._drop_label.drop_target_register(DND_FILES)
            self._drop_label.dnd_bind("<<Drop>>", self._on_drop)
        except Exception:
            pass

    def _on_drop(self, event: Any) -> None:
        raw: str = event.data
        # tkinterdnd2 returns paths wrapped in {} when they contain spaces
        paths = [m[0] or m[1] for m in re.findall(r"\{([^}]+)\}|(\S+)", raw)]
        jar_paths = [Path(p) for p in paths if p.lower().endswith(".jar")]
        if jar_paths:
            self._add_files(jar_paths)

    # ── File management ───────────────────────────────────────────────────

    def _browse(self) -> None:
        selected = filedialog.askopenfilenames(
            title="Select Minecraft Mod JAR Files",
            filetypes=[("JAR Files", "*.jar"), ("All Files", "*.*")],
        )
        if selected:
            self._add_files([Path(p) for p in selected])

    def _add_files(self, paths: list[Path]) -> None:
        for p in paths:
            if p not in self._files:
                self._files.append(p)
        self._refresh_file_label()

    def _clear_files(self) -> None:
        self._files.clear()
        self._refresh_file_label()

    def _refresh_file_label(self) -> None:
        n = len(self._files)
        if n == 0:
            self._file_label.configure(text="No files selected.")
        elif n == 1:
            self._file_label.configure(text=f"1 file: {self._files[0].name}")
        else:
            preview = ", ".join(f.name for f in self._files[:3])
            suffix = f" (+{n - 3} more)" if n > 3 else ""
            self._file_label.configure(text=f"{n} files: {preview}{suffix}")

    # ── Import ────────────────────────────────────────────────────────────

    def _start_import(self) -> None:
        if not self._files:
            self._log("No files selected. Add .jar files and try again.")
            return
        if self._import_thread and self._import_thread.is_alive():
            self._log("Import already running — please wait.")
            return

        self._import_btn.configure(state="disabled")
        self._progress_bar.set(0)

        files = list(self._files)
        decompiler = self._decompiler_var.get()

        self._import_thread = threading.Thread(
            target=self._run_import,
            args=(files, decompiler),
            daemon=True,
        )
        self._import_thread.start()

    def _run_import(self, files: list[Path], decompiler: str) -> None:
        total = len(files)
        for idx, jar in enumerate(files):
            self._queue_log(f"\n{'─' * 54}")
            self._queue_log(f"  Importing [{idx + 1}/{total}]: {jar.name}")
            self._queue_log(f"{'─' * 54}")
            try:
                def _progress(pct: int, msg: str, _i: int = idx) -> None:
                    overall = (((_i * 100) + pct) / total) / 100
                    self._queue_progress(overall, f"[{_i + 1}/{total}] {msg}")

                mod_dir = self.importer.import_mod(
                    jar_path=jar,
                    decompiler_name=decompiler,
                    progress_callback=_progress,
                    log_callback=self._queue_log,
                )
                self._queue_log(f"\n✓ Done → {mod_dir}\n")
                self.after(0, lambda d=mod_dir: self.on_import_complete(d))

            except Exception as exc:
                self._queue_log(f"\n✗ Error importing {jar.name}:\n  {exc}\n")

        self._queue_progress(1.0, f"Finished — {total} mod(s) processed.")
        self._queue_log(f"\nAll {total} mod(s) processed.")
        self.after(0, lambda: self._import_btn.configure(state="normal"))

    # ── Thread-safe queue ─────────────────────────────────────────────────

    def _queue_log(self, msg: str) -> None:
        self._log_queue.put(("log", msg))

    def _queue_progress(self, value: float, msg: str) -> None:
        self._log_queue.put(("progress", value, msg))

    def _poll(self) -> None:
        try:
            while True:
                item = self._log_queue.get_nowait()
                if item[0] == "log":
                    self._log(item[1])
                elif item[0] == "progress":
                    self._progress_bar.set(item[1])
                    self._progress_label.configure(text=item[2])
        except queue.Empty:
            pass
        except Exception:
            pass
        self.after(80, self._poll)

    # ── Console helpers ───────────────────────────────────────────────────

    def _log(self, msg: str) -> None:
        self._console.configure(state="normal")
        self._console.insert("end", msg + "\n")
        self._console.see("end")
        self._console.configure(state="disabled")

    def _clear_console(self) -> None:
        self._console.configure(state="normal")
        self._console.delete("1.0", "end")
        self._console.configure(state="disabled")
