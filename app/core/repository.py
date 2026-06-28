"""
repository.py — Reads and queries the on-disk mod repository.
"""

import json
from pathlib import Path
from typing import Any


class Repository:
    def __init__(self, repo_path: Path) -> None:
        self.repo_path = repo_path
        self.versions_path = repo_path / "Versions"

    # ── Scanning ──────────────────────────────────────────────────────────

    def scan(self) -> list[dict[str, Any]]:
        """Return a flat list of every mod in the repository."""
        mods: list[dict] = []
        if not self.versions_path.exists():
            return mods

        for ver_dir in sorted(self.versions_path.iterdir()):
            if not ver_dir.is_dir():
                continue
            for loader_dir in sorted(ver_dir.iterdir()):
                if not loader_dir.is_dir():
                    continue
                for mod_dir in sorted(loader_dir.iterdir()):
                    if mod_dir.is_dir():
                        mods.append(
                            self._read_mod(mod_dir, ver_dir.name, loader_dir.name)
                        )
        return mods

    def search(self, query: str) -> list[dict[str, Any]]:
        """Return mods matching *query* against name, ID, version, or loader."""
        if not query:
            return self.scan()
        q = query.lower()
        return [
            m for m in self.scan()
            if (
                q in m.get("mod_name", "").lower()
                or q in m.get("mod_id", "").lower()
                or q in m.get("minecraft_version", "").lower()
                or q in m.get("loader", "").lower()
                or any(q in a.lower() for a in m.get("authors", []))
            )
        ]

    # ── Tree helpers (used by the sidebar) ───────────────────────────────

    def get_versions(self) -> list[str]:
        if not self.versions_path.exists():
            return []
        return sorted(d.name for d in self.versions_path.iterdir() if d.is_dir())

    def get_loaders(self, version: str) -> list[str]:
        p = self.versions_path / version
        if not p.exists():
            return []
        return sorted(d.name for d in p.iterdir() if d.is_dir())

    def get_mods(self, version: str, loader: str) -> list[dict[str, Any]]:
        p = self.versions_path / version / loader
        if not p.exists():
            return []
        return [
            self._read_mod(d, version, loader)
            for d in sorted(p.iterdir())
            if d.is_dir()
        ]

    # ── Internal ─────────────────────────────────────────────────────────

    def _read_mod(
        self, mod_dir: Path, version: str, loader: str
    ) -> dict[str, Any]:
        info: dict[str, Any] = {
            "mod_name": mod_dir.name,
            "mod_id": "unknown",
            "version": "unknown",
            "minecraft_version": version,
            "loader": loader,
            "authors": [],
            "description": "",
            "dependencies": [],
            "jar_name": "",
            "date_imported": "",
            "decompiler": "",
            "path": str(mod_dir),
        }

        info_json = mod_dir / "Metadata" / "info.json"
        if info_json.exists():
            try:
                data = json.loads(info_json.read_text(encoding="utf-8"))
                info.update(data)
                info["path"] = str(mod_dir)  # always use the live path
            except (json.JSONDecodeError, OSError):
                pass

        return info
