# Project Decom

A Python desktop application that decompiles Minecraft mod `.jar` files and organises every mod into a clean, standardised repository structure.

---

## Requirements

- **Python 3.10+**
- **Java 17+** — must be on `PATH` or `JAVA_HOME` must be set
- pip packages (see `requirements.txt`)

---

## Installation

```bash
pip install -r requirements.txt
python main.py
```

---

## Decompiler Setup

Place one or more decompiler JARs inside the `tools/` folder:

| File name        | Download                                                   |
|------------------|------------------------------------------------------------|
| `vineflower.jar` | https://github.com/Vineflower/vineflower/releases          |
| `cfr.jar`        | https://github.com/leibnitz27/cfr/releases                 |
| `fernflower.jar` | Extract from an IntelliJ IDEA installation                 |

Vineflower is recommended — it produces the cleanest output for Minecraft mods.

---

## Repository Layout

```
Repository/
└── Versions/
    └── <MC version>/
        └── <Loader>/
            └── <ModName>/
                ├── Assets/
                │   ├── Textures/
                │   ├── Models/
                │   ├── Sounds/
                │   ├── Particles/
                │   ├── Animations/
                │   └── Lang/
                ├── Data/
                │   ├── LootTables/
                │   ├── Recipes/
                │   ├── Tags/
                │   ├── Advancements/
                │   └── WorldGen/
                ├── Java/
                │   ├── Blocks/
                │   ├── Items/
                │   ├── Entities/
                │   ├── Mobs/
                │   ├── Screens/
                │   ├── Networking/
                │   ├── Commands/
                │   ├── Events/
                │   ├── Utilities/
                │   └── Registries/
                ├── Config/
                ├── Metadata/
                │   ├── info.json
                │   ├── mod.json
                │   └── dependencies.json
                ├── Decompiled/
                ├── Logs/
                └── Reports/
```

---

## Usage

1. Launch with `python main.py`.
2. Drag `.jar` files onto the drop zone (or click **Browse Files**).
3. Select a decompiler from the dropdown.
4. Click **Import Mods** and watch the console.
5. Browse the imported mods in the left sidebar.
