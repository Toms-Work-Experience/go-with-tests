"""
reporter.py — Generates a human-readable import report for each mod.
"""

from datetime import datetime
from pathlib import Path
from typing import Any

from .detector import ModInfo


def generate_report(
    mod_dir: Path,
    mod_info: ModInfo,
    stats: dict[str, Any],
    errors: list[str],
    warnings: list[str],
    decompiler: str,
) -> str:
    """Write a report file and return its text content."""
    timestamp = datetime.now().strftime("%Y-%m-%d %H:%M:%S")

    lines = [
        "=" * 62,
        "  PROJECT DECOM — IMPORT REPORT",
        "=" * 62,
        f"  Generated : {timestamp}",
        "",
        "  MOD INFORMATION",
        "  " + "-" * 58,
        f"  Mod Name          : {mod_info.mod_name}",
        f"  Mod ID            : {mod_info.mod_id}",
        f"  Version           : {mod_info.version}",
        f"  Minecraft Version : {mod_info.minecraft_version}",
        f"  Loader            : {mod_info.loader}",
        f"  Authors           : {', '.join(mod_info.authors) or 'Unknown'}",
        f"  JAR File          : {mod_info.jar_name}",
        f"  Decompiler        : {decompiler}",
        "",
        "  FILE STATISTICS",
        "  " + "-" * 58,
        f"  Textures Found    : {stats.get('textures', 0)}",
        f"  Models Found      : {stats.get('models', 0)}",
        f"  Sounds Found      : {stats.get('sounds', 0)}",
        f"  Java Classes      : {stats.get('java_classes', 0)}",
        f"  Recipes           : {stats.get('recipes', 0)}",
        f"  Loot Tables       : {stats.get('loot_tables', 0)}",
        f"  Lang Files        : {stats.get('langs', 0)}",
        f"  Config Files      : {stats.get('configs', 0)}",
    ]

    if mod_info.dependencies:
        lines += ["", "  DEPENDENCIES", "  " + "-" * 58]
        for dep in mod_info.dependencies:
            lines.append(f"    • {dep}")

    if errors:
        lines += ["", "  ERRORS", "  " + "-" * 58]
        for err in errors:
            lines.append(f"  [ERROR] {err}")

    if warnings:
        lines += ["", "  WARNINGS", "  " + "-" * 58]
        for warn in warnings:
            lines.append(f"  [WARN]  {warn}")

    if not errors and not warnings:
        lines += ["", "  STATUS: Import completed successfully with no issues."]

    lines.append("=" * 62)

    report_text = "\n".join(lines)

    reports_dir = mod_dir / "Reports"
    reports_dir.mkdir(parents=True, exist_ok=True)
    stamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    report_path = reports_dir / f"Report_{stamp}.txt"
    report_path.write_text(report_text, encoding="utf-8")

    return report_text
