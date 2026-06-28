import os
import subprocess
import sys
from pathlib import Path


def open_folder(path: str | Path) -> None:
    """Open *path* in the system file explorer (cross-platform)."""
    path = str(path)
    if sys.platform == "win32":
        os.startfile(path)
    elif sys.platform == "darwin":
        subprocess.run(["open", path], check=False)
    else:
        subprocess.run(["xdg-open", path], check=False)


def sanitize_folder_name(name: str) -> str:
    """Return a filesystem-safe version of *name*."""
    import re
    name = re.sub(r'[<>:"/\\|?*\x00-\x1f]', "_", name)
    name = name.strip(" .")
    return name or "Unknown"
