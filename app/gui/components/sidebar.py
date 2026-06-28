"""
sidebar.py — Left-panel repository browser with search.
"""

import tkinter as tk
from pathlib import Path
from tkinter import ttk
from typing import Any, Callable

import customtkinter as ctk

from ...core.repository import Repository
from ...utils.helpers import open_folder


class RepositorySidebar(ctk.CTkFrame):
    def __init__(
        self,
        parent: Any,
        repository: Repository,
        on_mod_selected: Callable[[dict], None],
    ) -> None:
        super().__init__(parent, width=270)
        self.repository = repository
        self.on_mod_selected = on_mod_selected
        self._item_map: dict[str, dict] = {}  # tree item id → mod info dict

        self.grid_propagate(False)
        self._build()

    # ── Build ─────────────────────────────────────────────────────────────

    def _build(self) -> None:
        self.columnconfigure(0, weight=1)
        self.rowconfigure(2, weight=1)

        # Title row
        title_frame = ctk.CTkFrame(self, fg_color="transparent")
        title_frame.grid(row=0, column=0, sticky="ew", padx=8, pady=(10, 4))
        title_frame.columnconfigure(0, weight=1)

        ctk.CTkLabel(
            title_frame,
            text="Repository",
            font=ctk.CTkFont(size=16, weight="bold"),
            anchor="w",
        ).grid(row=0, column=0, sticky="w")

        ctk.CTkButton(
            title_frame, text="⟳", width=30, height=26,
            command=self.refresh,
            font=ctk.CTkFont(size=14),
        ).grid(row=0, column=1, padx=(4, 0))

        # Search box
        self._search_var = tk.StringVar()
        self._search_var.trace_add("write", lambda *_: self.refresh())
        ctk.CTkEntry(
            self, placeholder_text="Search mods …",
            textvariable=self._search_var,
        ).grid(row=1, column=0, sticky="ew", padx=8, pady=(0, 4))

        # Tree frame
        tree_frame = ctk.CTkFrame(self)
        tree_frame.grid(row=2, column=0, sticky="nsew", padx=6, pady=4)
        tree_frame.columnconfigure(0, weight=1)
        tree_frame.rowconfigure(0, weight=1)

        self._style_treeview()
        self._tree = ttk.Treeview(
            tree_frame,
            style="Decom.Treeview",
            show="tree",
            selectmode="browse",
        )
        self._tree.grid(row=0, column=0, sticky="nsew")

        sb = ttk.Scrollbar(tree_frame, orient="vertical", command=self._tree.yview)
        sb.grid(row=0, column=1, sticky="ns")
        self._tree.configure(yscrollcommand=sb.set)

        self._tree.bind("<<TreeviewSelect>>", self._on_select)
        self._tree.bind("<Double-1>", lambda _e: self._open_selected())

        # Bottom buttons
        btn_bar = ctk.CTkFrame(self, fg_color="transparent")
        btn_bar.grid(row=3, column=0, sticky="ew", padx=6, pady=(0, 8))

        ctk.CTkButton(
            btn_bar, text="Open Folder", height=28,
            command=self._open_selected,
        ).pack(side="left", padx=(0, 4), fill="x", expand=True)

        self.refresh()

    @staticmethod
    def _style_treeview() -> None:
        style = ttk.Style()
        style.theme_use("clam")
        style.configure(
            "Decom.Treeview",
            background="#1e1e2e",
            foreground="#cdd6f4",
            fieldbackground="#1e1e2e",
            borderwidth=0,
            rowheight=26,
            font=("Segoe UI", 10),
        )
        style.configure(
            "Decom.Treeview.Heading",
            background="#181825",
            foreground="#cdd6f4",
        )
        style.map(
            "Decom.Treeview",
            background=[("selected", "#1f6aa5")],
            foreground=[("selected", "white")],
        )

    # ── Refresh ───────────────────────────────────────────────────────────

    def refresh(self) -> None:
        self._tree.delete(*self._tree.get_children())
        self._item_map.clear()

        query = self._search_var.get().strip()

        if query:
            self._populate_flat(self.repository.search(query))
        else:
            self._populate_tree()

    def _populate_tree(self) -> None:
        for version in self.repository.get_versions():
            v_id = self._tree.insert(
                "", "end",
                text=f"  ⬡  MC {version}",
                tags=("version",),
            )
            for loader in self.repository.get_loaders(version):
                l_id = self._tree.insert(
                    v_id, "end",
                    text=f"    {loader}",
                    tags=("loader",),
                )
                for mod in self.repository.get_mods(version, loader):
                    m_id = self._tree.insert(
                        l_id, "end",
                        text=f"      {mod['mod_name']}",
                        tags=("mod",),
                    )
                    self._item_map[m_id] = mod

        self._tree.tag_configure("version", foreground="#89dceb")
        self._tree.tag_configure("loader", foreground="#a6e3a1")
        self._tree.tag_configure("mod", foreground="#cdd6f4")

    def _populate_flat(self, mods: list[dict]) -> None:
        for mod in mods:
            label = (
                f"  {mod['mod_name']}  "
                f"({mod['minecraft_version']} · {mod['loader']})"
            )
            m_id = self._tree.insert("", "end", text=label, tags=("mod",))
            self._item_map[m_id] = mod
        self._tree.tag_configure("mod", foreground="#cdd6f4")

    # ── Handlers ─────────────────────────────────────────────────────────

    def _on_select(self, _event: tk.Event) -> None:  # type: ignore[type-arg]
        sel = self._tree.selection()
        if sel and sel[0] in self._item_map:
            self.on_mod_selected(self._item_map[sel[0]])

    def _open_selected(self) -> None:
        sel = self._tree.selection()
        if not sel or sel[0] not in self._item_map:
            return
        mod = self._item_map[sel[0]]
        path = Path(mod.get("path", ""))
        if path.exists():
            open_folder(path)
