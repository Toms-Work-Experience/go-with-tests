"""
main_window.py — Application root window (CustomTkinter).
"""

import json
import os
from pathlib import Path

import customtkinter as ctk

from ..core.importer import ModImporter
from ..core.repository import Repository
from .components.import_tab import ImportTab
from .components.mod_details_tab import ModDetailsTab
from .components.sidebar import RepositorySidebar

# ── Defaults ──────────────────────────────────────────────────────────────
APP_DIR = Path(__file__).parent.parent.parent
DEFAULT_REPO_PATH = APP_DIR / "Repository"
TOOLS_DIR = APP_DIR / "tools"
CONFIG_FILE = APP_DIR / "config.json"

ctk.set_appearance_mode("dark")
ctk.set_default_color_theme("blue")


class MainWindow:
    def __init__(self) -> None:
        self.root = ctk.CTk()
        self.root.title("Project Decom")
        self.root.geometry("1300x820")
        self.root.minsize(960, 640)

        # ── Try to enable drag-and-drop ───────────────────────────────
        self._dnd_enabled = False
        try:
            from tkinterdnd2 import TkinterDnD  # type: ignore

            self.root.TkdndVersion = TkinterDnD._require(self.root)
            self._dnd_enabled = True
        except Exception:
            pass

        # ── Config / paths ────────────────────────────────────────────
        self._config = self._load_config()
        self.repo_path = Path(
            self._config.get("repository_path", str(DEFAULT_REPO_PATH))
        )
        self.repo_path.mkdir(parents=True, exist_ok=True)
        TOOLS_DIR.mkdir(exist_ok=True)

        self.repository = Repository(self.repo_path)
        self.importer = ModImporter(self.repo_path, TOOLS_DIR)

        self._build_ui()
        self.root.protocol("WM_DELETE_WINDOW", self._on_close)

        # Centre on screen
        self.root.update_idletasks()
        w, h = 1300, 820
        sw = self.root.winfo_screenwidth()
        sh = self.root.winfo_screenheight()
        x = (sw - w) // 2
        y = (sh - h) // 2
        self.root.geometry(f"{w}x{h}+{x}+{y}")

        # Force to foreground using Windows API (reliable across all scenarios)
        self.root.after(100, self._force_foreground)

    def _force_foreground(self) -> None:
        """Bring the window to the front using the Windows API."""
        try:
            import ctypes
            hwnd = ctypes.windll.user32.FindWindowW(None, "Project Decom")
            if hwnd:
                ctypes.windll.user32.ShowWindow(hwnd, 9)   # SW_RESTORE
                ctypes.windll.user32.SetForegroundWindow(hwnd)
        except Exception:
            pass
        self.root.lift()
        self.root.focus_force()

    # ── Config helpers ────────────────────────────────────────────────────

    def _load_config(self) -> dict:
        if CONFIG_FILE.exists():
            try:
                return json.loads(CONFIG_FILE.read_text(encoding="utf-8"))
            except Exception:
                pass
        return {}

    def _save_config(self) -> None:
        try:
            CONFIG_FILE.write_text(
                json.dumps(self._config, indent=4), encoding="utf-8"
            )
        except OSError:
            pass

    # ── UI construction ───────────────────────────────────────────────────

    def _build_ui(self) -> None:
        self.root.grid_columnconfigure(1, weight=1)
        self.root.grid_rowconfigure(0, weight=1)

        # Left sidebar
        self.sidebar = RepositorySidebar(
            self.root,
            repository=self.repository,
            on_mod_selected=self._on_mod_selected,
        )
        self.sidebar.grid(row=0, column=0, sticky="nsew", padx=(10, 4), pady=10)

        # Right area
        right = ctk.CTkFrame(self.root)
        right.grid(row=0, column=1, sticky="nsew", padx=(4, 10), pady=10)
        right.grid_columnconfigure(0, weight=1)
        right.grid_rowconfigure(0, weight=1)

        self.tabview = ctk.CTkTabview(right)
        self.tabview.grid(row=0, column=0, sticky="nsew", padx=4, pady=4)

        self.tabview.add("Import")
        self.tabview.add("Mod Details")
        self.tabview.add("Settings")

        # Import tab
        self.import_tab = ImportTab(
            self.tabview.tab("Import"),
            importer=self.importer,
            on_import_complete=self._on_import_complete,
        )
        self.import_tab.pack(fill="both", expand=True)

        # Mod Details tab
        self.mod_details_tab = ModDetailsTab(self.tabview.tab("Mod Details"))
        self.mod_details_tab.pack(fill="both", expand=True)

        # Settings tab
        self._build_settings_tab(self.tabview.tab("Settings"))

        # Status bar
        self.status_label = ctk.CTkLabel(
            self.root, text="Ready", anchor="w", height=24,
            text_color=("#555", "#888"),
        )
        self.status_label.grid(
            row=1, column=0, columnspan=2, sticky="ew", padx=14, pady=(0, 6)
        )

    def _build_settings_tab(self, parent: ctk.CTkFrame) -> None:
        frame = ctk.CTkScrollableFrame(parent)
        frame.pack(fill="both", expand=True, padx=10, pady=10)
        frame.columnconfigure(1, weight=1)

        # Title
        ctk.CTkLabel(
            frame, text="Settings",
            font=ctk.CTkFont(size=18, weight="bold"),
        ).grid(row=0, column=0, columnspan=3, sticky="w", pady=(6, 14))

        # Repository path row
        ctk.CTkLabel(
            frame, text="Repository Path:", font=ctk.CTkFont(weight="bold")
        ).grid(row=1, column=0, sticky="w", padx=(4, 8), pady=4)

        self._repo_path_entry = ctk.CTkEntry(frame, width=380)
        self._repo_path_entry.insert(0, str(self.repo_path))
        self._repo_path_entry.grid(row=1, column=1, sticky="ew", pady=4)

        ctk.CTkButton(
            frame, text="Browse", width=80,
            command=self._browse_repo_path,
        ).grid(row=1, column=2, padx=(6, 4), pady=4)

        ctk.CTkButton(
            frame, text="Save Settings", width=140,
            command=self._save_settings,
        ).grid(row=2, column=0, columnspan=3, sticky="w", padx=4, pady=(8, 20))

        # Decompiler info
        ctk.CTkLabel(
            frame, text="Decompiler Setup",
            font=ctk.CTkFont(size=15, weight="bold"),
        ).grid(row=3, column=0, columnspan=3, sticky="w", padx=4, pady=(0, 6))

        info = (
            "Place decompiler JARs in the  tools/  folder next to main.py:\n\n"
            "  vineflower.jar  →  github.com/Vineflower/vineflower/releases\n"
            "  cfr.jar         →  github.com/leibnitz27/cfr/releases\n"
            "  fernflower.jar  →  extract from IntelliJ IDEA\n\n"
            "Java 17+ must be installed and on PATH (or JAVA_HOME must be set)."
        )
        ctk.CTkLabel(
            frame, text=info, justify="left",
            font=ctk.CTkFont(family="Courier New", size=12),
            wraplength=540,
        ).grid(row=4, column=0, columnspan=3, sticky="w", padx=4, pady=4)

        ctk.CTkButton(
            frame, text="Open tools/ Folder", width=160,
            command=lambda: self._open_path(TOOLS_DIR),
        ).grid(row=5, column=0, columnspan=3, sticky="w", padx=4, pady=(6, 4))

    # ── Settings actions ──────────────────────────────────────────────────

    def _browse_repo_path(self) -> None:
        from tkinter import filedialog

        path = filedialog.askdirectory(
            title="Select Repository Folder",
            initialdir=str(self.repo_path),
        )
        if path:
            self._repo_path_entry.delete(0, "end")
            self._repo_path_entry.insert(0, path)

    def _save_settings(self) -> None:
        raw = self._repo_path_entry.get().strip()
        if not raw:
            return
        new_path = Path(raw)
        new_path.mkdir(parents=True, exist_ok=True)
        self.repo_path = new_path
        self._config["repository_path"] = str(new_path)
        self._save_config()

        self.repository = Repository(new_path)
        self.importer = ModImporter(new_path, TOOLS_DIR)
        self.import_tab.importer = self.importer
        self.sidebar.repository = self.repository
        self.sidebar.refresh()
        self._set_status("Settings saved.")

    # ── Event handlers ────────────────────────────────────────────────────

    def _on_mod_selected(self, mod_info: dict) -> None:
        self.mod_details_tab.show_mod(mod_info)
        self.tabview.set("Mod Details")

    def _on_import_complete(self, mod_dir: Path) -> None:
        self.sidebar.refresh()
        self._set_status(f"Import complete → {mod_dir}")

    def _on_close(self) -> None:
        self._save_config()
        self.root.destroy()

    # ── Helpers ───────────────────────────────────────────────────────────

    def _set_status(self, msg: str) -> None:
        self.status_label.configure(text=msg)

    @staticmethod
    def _open_path(path: Path) -> None:
        from ..utils.helpers import open_folder

        open_folder(path)

    # ── Launch ────────────────────────────────────────────────────────────

    def run(self) -> None:
        self.root.mainloop()
