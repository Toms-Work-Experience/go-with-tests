"""
detector.py — Reads a Minecraft mod JAR and extracts metadata.

Supports:
  Fabric   (fabric.mod.json)
  Quilt    (quilt.mod.json)
  NeoForge (META-INF/neoforge.mods.toml)
  Forge    (META-INF/mods.toml  or  mcmod.info)
"""

import json
import re
import zipfile
from dataclasses import dataclass, field
from pathlib import Path
from typing import List

# Python 3.11+ ships tomllib; older versions need the tomli back-port.
try:
    import tomllib  # type: ignore
except ImportError:
    try:
        import tomli as tomllib  # type: ignore
    except ImportError:
        tomllib = None  # type: ignore


# ---------------------------------------------------------------------------
# Data model
# ---------------------------------------------------------------------------

@dataclass
class ModInfo:
    mod_name: str = "Unknown"
    mod_id: str = "unknown"
    minecraft_version: str = "Unknown"
    loader: str = "Unknown"
    authors: List[str] = field(default_factory=list)
    dependencies: List[str] = field(default_factory=list)
    version: str = "0.0.0"
    description: str = ""
    jar_name: str = ""


# ---------------------------------------------------------------------------
# Public entry point
# ---------------------------------------------------------------------------

def detect_mod_info(jar_path: Path) -> ModInfo:
    """Return a :class:`ModInfo` populated from the JAR's metadata files."""
    info = ModInfo(jar_name=jar_path.name)

    try:
        with zipfile.ZipFile(jar_path, "r") as zf:
            names = set(zf.namelist())

            if "quilt.mod.json" in names:
                _parse_quilt(zf, info)
            elif "fabric.mod.json" in names:
                _parse_fabric(zf, info)
            elif "META-INF/neoforge.mods.toml" in names:
                _parse_neoforge_toml(zf, info)
            elif "META-INF/mods.toml" in names:
                _parse_forge_toml(zf, info, names)
            elif "mcmod.info" in names:
                _parse_mcmod_info(zf, info)
            # else: unknown format — leave defaults

    except zipfile.BadZipFile:
        pass  # corrupt jar; defaults remain

    # Ensure a usable display name
    if not info.mod_name or info.mod_name == "Unknown":
        info.mod_name = jar_path.stem

    return info


# ---------------------------------------------------------------------------
# Per-loader parsers
# ---------------------------------------------------------------------------

def _parse_quilt(zf: zipfile.ZipFile, info: ModInfo) -> None:
    try:
        data = json.loads(zf.read("quilt.mod.json"))
        loader = data.get("quilt_loader", {})
        meta = loader.get("metadata", {})

        info.loader = "Quilt"
        info.mod_id = loader.get("id", "unknown")
        info.version = loader.get("version", "0.0.0")
        info.mod_name = meta.get("name", info.mod_id)
        info.description = meta.get("description", "")

        contributors = meta.get("contributors", {})
        info.authors = list(contributors.keys())

        depends = loader.get("depends", [])
        skip = {"quilt_loader", "minecraft", "java", "quilted_fabric_api"}
        for dep in depends:
            if isinstance(dep, dict):
                dep_id = dep.get("id", "")
                if dep_id == "minecraft":
                    info.minecraft_version = _extract_version(
                        str(dep.get("versions", ""))
                    )
                elif dep_id and dep_id not in skip:
                    info.dependencies.append(dep_id)
    except Exception:
        info.loader = "Quilt"


def _parse_fabric(zf: zipfile.ZipFile, info: ModInfo) -> None:
    try:
        data = json.loads(zf.read("fabric.mod.json"))

        info.loader = "Fabric"
        info.mod_id = data.get("id", "unknown")
        info.version = data.get("version", "0.0.0")
        info.mod_name = data.get("name", info.mod_id)
        info.description = data.get("description", "")

        authors = data.get("authors", [])
        info.authors = [
            a if isinstance(a, str) else a.get("name", str(a)) for a in authors
        ]

        depends: dict = data.get("depends", {})
        info.minecraft_version = _extract_version(
            str(depends.get("minecraft", "Unknown"))
        )

        skip = {"fabricloader", "minecraft", "java", "fabric-api"}
        info.dependencies = [k for k in depends if k not in skip]
    except Exception:
        info.loader = "Fabric"


def _parse_neoforge_toml(zf: zipfile.ZipFile, info: ModInfo) -> None:
    info.loader = "NeoForge"
    try:
        content = zf.read("META-INF/neoforge.mods.toml").decode("utf-8", errors="replace")
        _apply_forge_style_toml(content, info, skip_ids={"neoforge", "java"})
    except Exception:
        pass


def _parse_forge_toml(
    zf: zipfile.ZipFile, info: ModInfo, all_names: set
) -> None:
    try:
        content = zf.read("META-INF/mods.toml").decode("utf-8", errors="replace")

        # Detect NeoForge: 1.20.4+ uses mods.toml with neoforge dependency
        if re.search(r'modId\s*=\s*["\']neoforge["\']', content, re.IGNORECASE):
            info.loader = "NeoForge"
            skip = {"neoforge", "java"}
        else:
            info.loader = "Forge"
            skip = {"forge", "java"}

        _apply_forge_style_toml(content, info, skip_ids=skip)
    except Exception:
        info.loader = "Forge"


def _apply_forge_style_toml(
    content: str, info: ModInfo, skip_ids: set
) -> None:
    """Parse a Forge/NeoForge style mods.toml and populate *info*."""
    if tomllib:
        try:
            data = tomllib.loads(content)
            mods = data.get("mods", [{}])
            if mods:
                mod = mods[0]
                info.mod_id = mod.get("modId", "unknown")
                raw_ver = mod.get("version", "0.0.0")
                # Version can be a Maven substitution like ${file.jarVersion}
                if not raw_ver.startswith("$"):
                    info.version = raw_ver
                info.mod_name = mod.get("displayName", info.mod_id)
                info.description = str(mod.get("description", "")).strip()
                authors_str = mod.get("authors", "")
                if authors_str:
                    info.authors = [a.strip() for a in str(authors_str).split(",")]

            deps: dict = data.get("dependencies", {})
            mod_deps = deps.get(info.mod_id, [])
            for dep in mod_deps:
                dep_id = dep.get("modId", "")
                if dep_id == "minecraft":
                    info.minecraft_version = _extract_version_from_range(
                        dep.get("versionRange", "")
                    )
                elif dep_id and dep_id not in skip_ids:
                    info.dependencies.append(dep_id)
            return
        except Exception:
            pass
    # Fallback: regex
    _parse_forge_toml_regex(content, info)


def _parse_forge_toml_regex(content: str, info: ModInfo) -> None:
    m = re.search(r'modId\s*=\s*["\']([^"\']+)["\']', content)
    if m:
        info.mod_id = m.group(1)

    m = re.search(r'displayName\s*=\s*["\']([^"\']+)["\']', content)
    if m:
        info.mod_name = m.group(1)

    m = re.search(r'\bversion\s*=\s*["\']([^"\'$][^"\']*)["\']', content)
    if m:
        info.version = m.group(1)

    m = re.search(r'\bauthors\s*=\s*["\']([^"\']+)["\']', content)
    if m:
        info.authors = [a.strip() for a in m.group(1).split(",")]

    # Find the Minecraft dependency version range
    m = re.search(
        r'modId\s*=\s*["\']minecraft["\'].*?versionRange\s*=\s*["\']([^"\']+)["\']',
        content,
        re.DOTALL,
    )
    if m:
        info.minecraft_version = _extract_version_from_range(m.group(1))


def _parse_mcmod_info(zf: zipfile.ZipFile, info: ModInfo) -> None:
    try:
        raw = zf.read("mcmod.info").decode("utf-8", errors="replace").strip()
        if raw.startswith("["):
            mods = json.loads(raw)
            mod = mods[0] if mods else {}
        else:
            wrapper = json.loads(raw)
            mods = wrapper.get("modList", wrapper.get("mods", [wrapper]))
            mod = mods[0] if mods else {}

        info.loader = "Forge"
        info.mod_id = mod.get("modid", "unknown")
        info.mod_name = mod.get("name", info.mod_id)
        info.version = mod.get("version", "0.0.0")
        info.minecraft_version = mod.get("mcversion", "Unknown")
        info.description = mod.get("description", "")

        authors = mod.get("authorList", mod.get("authors", []))
        info.authors = authors if isinstance(authors, list) else [str(authors)]

        raw_deps = mod.get("dependencies", "")
        if raw_deps:
            info.dependencies = [d.strip() for d in str(raw_deps).split(",") if d.strip()]
    except Exception:
        info.loader = "Forge"


# ---------------------------------------------------------------------------
# Version string helpers
# ---------------------------------------------------------------------------

def _extract_version(version_str: str) -> str:
    """Strip comparison operators and return the first X.Y[.Z] found."""
    if not version_str or version_str in ("Unknown", "*"):
        return "Unknown"
    version_str = re.sub(r'^[~^>=<* ]+', "", version_str.strip())
    m = re.search(r"\d+\.\d+(?:\.\d+)?", version_str)
    return m.group(0) if m else (version_str or "Unknown")


def _extract_version_from_range(range_str: str) -> str:
    """Extract a version from Maven range notation like ``[1.20.1,1.21)``."""
    if not range_str:
        return "Unknown"
    m = re.search(r"[\[\(](\d+\.\d+(?:\.\d+)?)", range_str)
    if m:
        return m.group(1)
    m = re.search(r"\d+\.\d+(?:\.\d+)?", range_str)
    return m.group(0) if m else "Unknown"
