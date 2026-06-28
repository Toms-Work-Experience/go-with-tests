"""
mod_details_tab.py — Read-only detail view for a selected repository mod.
"""

from pathlib import Path
from typing import Any, Optional

import customtkinter as ctk

from ...utils.helpers import open_folder


class ModDetailsTab(ctk.CTkFrame):
    def __init__(self, parent: Any) -> None:
        super().__init__(parent, fg_color="transparent")
        self._current: Optional[dict] = None
        self._build()

    # ── Build ─────────────────────────────────────────────────────────────

    def _build(self) -> None:
        self.columnconfigure(0, weight=1)
        self.rowconfigure(1, weight=1)

        # Header
        self._name_label = ctk.CTkLabel(
            self,
            text="Select a mod from the repository",
            font=ctk.CTkFont(size=20, weight="bold"),
            anchor="w",
        )
        self._name_label.grid(row=0, column=0, sticky="ew", padx=16, pady=(14, 6))

        # Scrollable body
        scroll = ctk.CTkScrollableFrame(self)
        scroll.grid(row=1, column=0, sticky="nsew", padx=10, pady=(0, 10))
        scroll.columnconfigure(1, weight=1)

        self._value_labels: dict[str, ctk.CTkLabel] = {}

        fields = [
            ("Mod ID",            "mod_id"),
            ("Version",           "version"),
            ("Minecraft Version", "minecraft_version"),
            ("Loader",            "loader"),
            ("Authors",           "authors"),
            ("Description",       "description"),
            ("JAR File",          "jar_name"),
            ("Date Imported",     "date_imported"),
            ("Decompiler",        "decompiler"),
        ]
        row = 0
        for label_text, key in fields:
            ctk.CTkLabel(
                scroll,
                text=label_text + ":",
                font=ctk.CTkFont(weight="bold"),
                anchor="nw",
                width=170,
            ).grid(row=row, column=0, sticky="nw", padx=(10, 6), pady=4)

            val = ctk.CTkLabel(
                scroll, text="—", anchor="w", justify="left", wraplength=460,
            )
            val.grid(row=row, column=1, sticky="ew", padx=(0, 10), pady=4)
            self._value_labels[key] = val
            row += 1

        # Dependencies
        ctk.CTkLabel(
            scroll,
            text="Dependencies:",
            font=ctk.CTkFont(weight="bold"),
            anchor="nw",
            width=170,
        ).grid(row=row, column=0, sticky="nw", padx=(10, 6), pady=4)
        self._deps_label = ctk.CTkLabel(
            scroll, text="—", anchor="w", justify="left", wraplength=460,
        )
        self._deps_label.grid(row=row, column=1, sticky="ew", padx=(0, 10), pady=4)
        row += 1

        # ── Statistics section ────────────────────────────────────────────
        ctk.CTkLabel(
            scroll,
            text="\nFile Statistics",
            font=ctk.CTkFont(size=14, weight="bold"),
            anchor="w",
        ).grid(row=row, column=0, columnspan=2, sticky="ew", padx=10, pady=(8, 2))
        row += 1

        stat_fields = [
            ("Textures",    "Assets/Textures"),
            ("Models",      "Assets/Models"),
            ("Sounds",      "Assets/Sounds"),
            ("Lang Files",  "Assets/Lang"),
            ("Java Classes","Java"),
            ("Recipes",     "Data/Recipes"),
            ("Loot Tables", "Data/LootTables"),
            ("Advancements","Data/Advancements"),
        ]

        for label_text, sub_path in stat_fields:
            ctk.CTkLabel(
                scroll,
                text=label_text + ":",
                font=ctk.CTkFont(weight="bold"),
                anchor="w",
                width=170,
            ).grid(row=row, column=0, sticky="w", padx=(10, 6), pady=2)
            val = ctk.CTkLabel(scroll, text="—", anchor="w")
            val.grid(row=row, column=1, sticky="ew", padx=(0, 10), pady=2)
            self._value_labels[f"stat::{sub_path}"] = val
            row += 1

        # ── Action buttons ────────────────────────────────────────────────
        btn_frame = ctk.CTkFrame(scroll, fg_color="transparent")
        btn_frame.grid(row=row, column=0, columnspan=2, sticky="ew", padx=10, pady=(16, 8))

        self._open_btn = ctk.CTkButton(
            btn_frame, text="Open Folder", width=130,
            command=self._open_folder,
            state="disabled",
        )
        self._open_btn.pack(side="left", padx=(0, 8))

        self._report_btn = ctk.CTkButton(
            btn_frame, text="View Report", width=130,
            command=self._view_report,
            fg_color=("#555", "#333"),
            state="disabled",
        )
        self._report_btn.pack(side="left")

    # ── Public API ────────────────────────────────────────────────────────

    def show_mod(self, mod_info: dict) -> None:
        self._current = mod_info
        name = mod_info.get("mod_name", "Unknown Mod")
        self._name_label.configure(text=name)

        simple = {
            "mod_id":            mod_info.get("mod_id", "—"),
            "version":           mod_info.get("version", "—"),
            "minecraft_version": mod_info.get("minecraft_version", "—"),
            "loader":            mod_info.get("loader", "—"),
            "authors":           ", ".join(mod_info.get("authors", [])) or "—",
            "description":       mod_info.get("description") or "—",
            "jar_name":          mod_info.get("jar_name", "—"),
            "date_imported":     mod_info.get("date_imported", "—"),
            "decompiler":        mod_info.get("decompiler", "—"),
        }
        for key, val in simple.items():
            if key in self._value_labels:
                self._value_labels[key].configure(text=str(val) or "—")

        deps = mod_info.get("dependencies", [])
        self._deps_label.configure(text=", ".join(deps) if deps else "None")

        # Count files per category
        mod_path = Path(mod_info.get("path", ""))
        for key, lbl in self._value_labels.items():
            if key.startswith("stat::"):
                sub = key[len("stat::"):]
                target = mod_path / sub
                if target.exists():
                    count = sum(1 for _ in target.rglob("*") if _.is_file())
                    lbl.configure(text=str(count))
                else:
                    lbl.configure(text="0")

        self._open_btn.configure(state="normal")
        self._report_btn.configure(state="normal")

    # ── Actions ───────────────────────────────────────────────────────────

    def _open_folder(self) -> None:
        if not self._current:
            return
        path = Path(self._current.get("path", ""))
        if path.exists():
            open_folder(path)

    def _view_report(self) -> None:
        if not self._current:
            return
        reports_dir = Path(self._current.get("path", "")) / "Reports"
        if not reports_dir.exists():
            return
        reports = sorted(reports_dir.glob("Report_*.txt"), reverse=True)
        if not reports:
            return

        win = ctk.CTkToplevel(self)
        win.title(f"Report — {self._current.get('mod_name', '')}")
        win.geometry("720x520")
        win.lift()

        text = ctk.CTkTextbox(
            win,
            font=ctk.CTkFont(family="Courier New", size=11),
            wrap="none",
        )
        text.pack(fill="both", expand=True, padx=10, pady=10)
        text.insert("1.0", reports[0].read_text(encoding="utf-8", errors="replace"))
        text.configure(state="disabled")
