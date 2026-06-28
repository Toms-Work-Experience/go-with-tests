"""
sorter.py — Categorises extracted JAR contents and decompiled Java sources
into the standardised Project Decom folder layout.
"""

import shutil
from pathlib import Path
from typing import Callable, Optional

# ── Asset folder mapping ──────────────────────────────────────────────────
# Key: subfolder name inside assets/<namespace>/   Value: target under Assets/
ASSET_MAP: dict[str, str] = {
    "textures": "Textures",
    "models": "Models",
    "sounds": "Sounds",
    "particles": "Particles",
    "animations": "Animations",
    "lang": "Lang",
    "blockstates": "Models",
    "font": "Textures",
    "shaders": "Textures",
    "optifine": "Textures",
}

# ── Data folder mapping ───────────────────────────────────────────────────
DATA_MAP: dict[str, str] = {
    "loot_tables": "LootTables",
    "recipes": "Recipes",
    "tags": "Tags",
    "advancements": "Advancements",
    "worldgen": "WorldGen",
    "dimension": "WorldGen",
    "dimension_type": "WorldGen",
    "structures": "WorldGen",
    "configured_feature": "WorldGen",
    "placed_feature": "WorldGen",
    "biome": "WorldGen",
    "noise_settings": "WorldGen",
}

# ── Java package path → target folder (ordered, first match wins) ─────────
JAVA_PACKAGE_MAP: list[tuple[list[str], str]] = [
    (["block", "blocks"], "Blocks"),
    (["item", "items"], "Items"),
    (["entity", "entities"], "Entities"),
    (["mob", "mobs", "ai"], "Mobs"),
    (["screen", "screens", "gui", "client/gui", "client/screen"], "Screens"),
    (["network", "networking", "packet", "packets"], "Networking"),
    (["command", "commands"], "Commands"),
    (["event", "events", "handler", "handlers"], "Events"),
    (["util", "utils", "utility", "helper", "helpers", "common"], "Utilities"),
    (["register", "registry", "registries", "init"], "Registries"),
    (["config", "configuration"], "Config"),
]

# ── Java class name suffix / prefix → target folder ──────────────────────
JAVA_CLASS_MAP: list[tuple[list[str], str]] = [
    (["Block", "Blocks"], "Blocks"),
    (["Item", "Items"], "Items"),
    (["Entity", "EntityType"], "Entities"),
    (["Mob", "Monster", "Animal", "Creature", "Boss"], "Mobs"),
    (["Screen", "Gui", "Container", "Menu"], "Screens"),
    (["Packet", "Network", "Channel", "Message"], "Networking"),
    (["Command"], "Commands"),
    (["Event", "Handler", "Listener"], "Events"),
    (["Util", "Utils", "Helper", "Helpers"], "Utilities"),
    (["Registry", "Register", "Init", "Registries"], "Registries"),
]


class FileSorter:
    def __init__(self, log_callback: Optional[Callable[[str], None]] = None) -> None:
        self.log = log_callback or (lambda _: None)
        self.stats: dict[str, int] = {
            "textures": 0,
            "models": 0,
            "sounds": 0,
            "java_classes": 0,
            "recipes": 0,
            "loot_tables": 0,
            "langs": 0,
            "configs": 0,
            "other": 0,
        }

    # ── Public entry point ────────────────────────────────────────────────

    def sort_all(
        self,
        extracted_dir: Path,
        decompiled_dir: Optional[Path],
        mod_dir: Path,
    ) -> None:
        self._create_structure(mod_dir)
        self.sort_assets(extracted_dir, mod_dir / "Assets")
        self.sort_data(extracted_dir, mod_dir / "Data")
        self.sort_config(extracted_dir, mod_dir / "Config")
        if decompiled_dir and decompiled_dir.exists():
            self.sort_java(decompiled_dir, mod_dir / "Java")

    # ── Directory scaffold ────────────────────────────────────────────────

    def _create_structure(self, mod_dir: Path) -> None:
        folders = [
            "Assets/Textures", "Assets/Models", "Assets/Sounds",
            "Assets/Particles", "Assets/Animations", "Assets/Lang",
            "Data/LootTables", "Data/Recipes", "Data/Tags",
            "Data/Advancements", "Data/WorldGen",
            "Java/Blocks", "Java/Items", "Java/Entities",
            "Java/Mobs", "Java/Screens", "Java/Networking",
            "Java/Commands", "Java/Events", "Java/Utilities", "Java/Registries",
            "Config", "Metadata", "Decompiled", "Logs", "Reports",
        ]
        for folder in folders:
            (mod_dir / folder).mkdir(parents=True, exist_ok=True)

    # ── Asset sorting ─────────────────────────────────────────────────────

    def sort_assets(self, extracted_dir: Path, assets_target: Path) -> None:
        assets_root = extracted_dir / "assets"
        if not assets_root.exists():
            return

        for ns_dir in assets_root.iterdir():
            if not ns_dir.is_dir():
                continue
            for folder in ns_dir.iterdir():
                if not folder.is_dir():
                    continue
                key = folder.name.lower()
                dest_name = ASSET_MAP.get(key, folder.name.capitalize())
                dest = assets_target / dest_name / ns_dir.name
                dest.mkdir(parents=True, exist_ok=True)
                self._copy_tree(folder, dest)
                # Tally stats
                count = self._count_files(folder)
                if key == "textures":
                    self.stats["textures"] += count
                elif key in ("models", "blockstates"):
                    self.stats["models"] += count
                elif key == "sounds":
                    self.stats["sounds"] += count
                elif key == "lang":
                    self.stats["langs"] += count

    # ── Data sorting ──────────────────────────────────────────────────────

    def sort_data(self, extracted_dir: Path, data_target: Path) -> None:
        data_root = extracted_dir / "data"
        if not data_root.exists():
            return

        for ns_dir in data_root.iterdir():
            if not ns_dir.is_dir():
                continue
            for folder in ns_dir.iterdir():
                if not folder.is_dir():
                    continue
                key = folder.name.lower()
                dest_name = DATA_MAP.get(key, folder.name.capitalize())
                dest = data_target / dest_name / ns_dir.name
                dest.mkdir(parents=True, exist_ok=True)
                self._copy_tree(folder, dest)
                count = self._count_files(folder)
                if key == "recipes":
                    self.stats["recipes"] += count
                elif key == "loot_tables":
                    self.stats["loot_tables"] += count

    # ── Config sorting ────────────────────────────────────────────────────

    def sort_config(self, extracted_dir: Path, config_target: Path) -> None:
        config_target.mkdir(parents=True, exist_ok=True)

        for dir_name in ("config", "defaultconfigs", "configs"):
            src = extracted_dir / dir_name
            if src.exists():
                self._copy_tree(src, config_target)

        # Loose config files in the JAR root
        for pattern in ("*.toml", "*.cfg", "*.properties", "*.ini"):
            for f in extracted_dir.glob(pattern):
                if f.name == "mods.toml":
                    continue
                dest = config_target / f.name
                shutil.copy2(f, dest)
                self.stats["configs"] += 1

    # ── Java sorting ──────────────────────────────────────────────────────

    def sort_java(self, decompiled_dir: Path, java_target: Path) -> None:
        java_files = list(decompiled_dir.rglob("*.java"))
        self.stats["java_classes"] = len(java_files)

        for java_file in java_files:
            category = self._classify_java(java_file, decompiled_dir)
            dest_dir = java_target / category
            dest_dir.mkdir(parents=True, exist_ok=True)

            # Preserve the package subdirectory structure inside each category
            try:
                rel = java_file.relative_to(decompiled_dir)
            except ValueError:
                rel = Path(java_file.name)

            dest = dest_dir / rel
            dest.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(java_file, dest)

    def _classify_java(self, java_file: Path, base_dir: Path) -> str:
        try:
            parts = [p.lower() for p in java_file.relative_to(base_dir).parts[:-1]]
        except ValueError:
            parts = []

        for patterns, folder in JAVA_PACKAGE_MAP:
            for part in parts:
                for pattern in patterns:
                    if pattern.lower() in part:
                        return folder

        class_name = java_file.stem
        for suffixes, folder in JAVA_CLASS_MAP:
            for s in suffixes:
                if class_name.endswith(s) or class_name.startswith(s):
                    return folder

        return "Utilities"

    # ── Helpers ───────────────────────────────────────────────────────────

    @staticmethod
    def _copy_tree(src: Path, dst: Path) -> None:
        for item in src.rglob("*"):
            if item.is_file():
                rel = item.relative_to(src)
                dest = dst / rel
                dest.parent.mkdir(parents=True, exist_ok=True)
                shutil.copy2(item, dest)

    @staticmethod
    def _count_files(folder: Path) -> int:
        return sum(1 for _ in folder.rglob("*") if _.is_file())
