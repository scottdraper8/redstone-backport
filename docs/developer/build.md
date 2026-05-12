# Build and documentation

## Requirements

- **JDK 17**
- **uv** and **Python 3.10+** for Sphinx docs — workspace root `pyproject.toml`, member
  `docs/pyproject.toml` (shared `uv.lock` at the mod root)

## Gradle (mod JARs)

From the mod directory:

```bash
cd games/minecraft/mods/redstone-backport   # from monorepo root
./gradlew build
```

From the monorepo, the tooling wrapper is:

```bash
games/minecraft/tooling/build-mod redstone-backport
```

That runs `gradlew build` in the mod folder.

### Formatting

```bash
./gradlew spotlessCheck    # CI-style verify
./gradlew spotlessApply    # auto-fix Java
```

### IDE runs (Forge)

The Legacy Forge Gradle plugin registers **Forge Client** and **Forge Server** run configs (see
`forge/build.gradle.kts`).

## Sphinx documentation

Using **[uv](https://docs.astral.sh/uv/)** (recommended). The mod directory is a **workspace**;
**`uv sync`** works from **`redstone-backport/`** or **`redstone-backport/docs/`**. The virtualenv is
**`redstone-backport/.venv`** by default.

The workspace **root** declares a dependency on **`redstone-backport-docs`** (the `docs/` member), so
a normal **`uv sync`** installs Sphinx and the **`build-docs`** console script into **`.venv/bin`**
(you do not need `uv sync --all-packages` for that).

Install dependencies once:

```bash
cd games/minecraft/mods/redstone-backport   # your clone root
uv sync
```

Build HTML from **any subdirectory** of the mod (the script walks up to the mod root using
`docs/conf.py` as a marker), or from **any working directory** if you pass **`--project`** to uv:

```bash
uv run build-docs
```

```bash
uv run --project /path/to/redstone-backport build-docs
```

Arguments after `--` are forwarded to **`sphinx-build`** (Sphinx 8+), for example:

```bash
uv run build-docs -- -W -a
```

Output: **`docs/_build/html/`** — open `index.html` in a browser, or run `python -m http.server` from
that folder.

### Manual `sphinx-build` (optional)

From the **mod root**:

```bash
uv run --package redstone-backport-docs sphinx-build -b html docs docs/_build/html
```

From **`docs/`**:

```bash
cd docs
uv run sphinx-build -b html . _build/html
```

The `Makefile` still works if Sphinx is on your `PATH` (for example after `uv sync` and
`source .venv/bin/activate` from the mod root), or invoke
`make html SPHINXBUILD="uv run sphinx-build"`.

## Publishing docs (optional)

Continuous deployment (for example Read the Docs or GitHub Pages) is not configured by default; add
a workflow or RTD config if you want a public docs URL.
