"""
Project Decom - Minecraft Mod Decompiler & Repository Manager
Entry point.
"""
import sys
from pathlib import Path

# Ensure the project root is on the path
sys.path.insert(0, str(Path(__file__).parent))


def main() -> None:
    try:
        import customtkinter  # noqa: F401
    except ImportError:
        print("Error: customtkinter is not installed.")
        print("Run:   pip install -r requirements.txt")
        sys.exit(1)

    from app.gui.main_window import MainWindow

    app = MainWindow()
    app.run()


if __name__ == "__main__":
    main()
