"""
metadata_gen.py — Generates the three metadata JSON files for every mod.
"""

import json
from datetime import datetime
from pathlib import Path
from typing import Any

from .detector import ModInfo


def generate_info_json(mod_info: ModInfo, decompiler: str) -> dict[str, Any]:
    return {
        "mod_name": mod_info.mod_name,
        "mod_id": mod_info.mod_id,
        "minecraft_version": mod_info.minecraft_version,
        "loader": mod_info.loader,
        "authors": mod_info.authors,
        "dependencies": mod_info.dependencies,
        "version": mod_info.version,
        "description": mod_info.description,
        "jar_name": mod_info.jar_name,
        "date_imported": datetime.now().isoformat(),
        "decompiler": decompiler,
    }


def generate_mod_json(mod_info: ModInfo) -> dict[str, Any]:
    return {
        "mod_name": mod_info.mod_name,
        "mod_id": mod_info.mod_id,
        "version": mod_info.version,
        "description": mod_info.description,
        "loader": mod_info.loader,
        "minecraft_version": mod_info.minecraft_version,
    }


def generate_dependencies_json(mod_info: ModInfo) -> dict[str, Any]:
    return {
        "mod_id": mod_info.mod_id,
        "dependencies": mod_info.dependencies,
        "loader": mod_info.loader,
        "minecraft_version": mod_info.minecraft_version,
    }


def write_metadata(mod_dir: Path, mod_info: ModInfo, decompiler: str) -> None:
    """Write info.json, mod.json, and dependencies.json to *mod_dir*/Metadata/."""
    meta_dir = mod_dir / "Metadata"
    meta_dir.mkdir(parents=True, exist_ok=True)

    _write_json(meta_dir / "info.json", generate_info_json(mod_info, decompiler))
    _write_json(meta_dir / "mod.json", generate_mod_json(mod_info))
    _write_json(meta_dir / "dependencies.json", generate_dependencies_json(mod_info))


def _write_json(path: Path, data: dict) -> None:
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=4, ensure_ascii=False)
