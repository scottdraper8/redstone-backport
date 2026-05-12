"""Console entry point for building Redstone Backport Sphinx docs."""

from __future__ import annotations

import sys
from pathlib import Path
from typing import cast

__all__ = ["find_mod_root", "main"]


def find_mod_root(start: Path | None = None) -> Path:
    """Resolve and return the mod root that contains ``docs/conf.py`` for this project.

    Search parents of *start* (default: the current working directory), then fall back
    to the editable-install layout where this package lives under ``docs/``.
    """
    cur = (start or Path.cwd()).resolve()
    for d in [cur, *cur.parents]:
        conf = d / "docs" / "conf.py"
        marker = d / "docs" / "pyproject.toml"
        if conf.is_file() and marker.is_file():
            return d
    # Editable install: this file is ``<mod>/docs/redstone_backport_docs/cli.py``.
    pkg_dir = Path(__file__).resolve().parent
    docs_dir = pkg_dir.parent
    if docs_dir.name == "docs":
        candidate = docs_dir.parent
        if (candidate / "docs" / "conf.py").is_file():
            return candidate
    print(
        "Could not find Redstone Backport mod root. Run from a directory under "
        "games/minecraft/mods/redstone-backport, or use an editable install of "
        "redstone-backport-docs so the package path can locate docs/conf.py.",
        file=sys.stderr,
    )
    raise SystemExit(2)


def main() -> int:
    """Build HTML documentation via ``sphinx-build`` into ``docs/_build/html``."""
    mod_root = find_mod_root()
    docs_dir = mod_root / "docs"
    out_dir = docs_dir / "_build" / "html"
    out_dir.parent.mkdir(parents=True, exist_ok=True)
    argv: list[str] = [
        "-b",
        "html",
        str(docs_dir),
        str(out_dir),
        *sys.argv[1:],
    ]
    from sphinx.cmd.build import build_main

    return cast(int, build_main(argv))
