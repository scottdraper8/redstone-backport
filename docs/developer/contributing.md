# Contributing

## Repository

This mod lives in the **squinchmods** monorepo:

[https://github.com/scottdraper8/squinchmods](https://github.com/scottdraper8/squinchmods)

Use **issues and pull requests** there per project norms.

**Git layout:** Redstone Backport is its **own repository** (often linked from the monorepo as a
**submodule**). Commit and run tooling from **this mod’s clone**, not only from the monorepo root.

## Code style

- **Java:** Google Java Format via **Spotless** — run `./gradlew spotlessApply` before pushing.
- **Static analysis:** **Error Prone** + **NullAway** on Gradle modules; mixin packages are excluded
  from Error Prone where the framework makes flow analysis misleading (`build.gradle.kts`
  `excludedPaths`).
- **Python (docs tooling only):** **[Ruff](https://docs.astral.sh/ruff/)** and
  **[mypy](https://mypy-lang.org/)** in `docs/pyproject.toml` — see {doc}`build` for **uv** workflows.
- **Docs:** Author under `docs/` in **MyST Markdown** (see {doc}`build`).

## Pre-commit (this repository)

From the **mod root** (this git repo):

```bash
pip install pre-commit   # or uv tool install pre-commit
pre-commit install
pre-commit run --all-files   # optional smoke check
```

**`.pre-commit-config.yaml`** here runs **Ruff**, **mypy**, **vulture**, **interrogate**, **codespell**,
**markdownlint-cli2** (with **`.markdownlint-cli2.yaml`**), **validate-pyproject**, **gitleaks**, and
general file hooks on paths under this clone. The **monorepo** may have its own separate pre-commit
config for repository-wide files (for example `games/minecraft/tooling`); it does **not** substitute
for running hooks in this mod’s repo when you commit submodule changes **inside** this repository.

## Scope

Keep changes focused on the mod’s goals; avoid unrelated refactors mixed into feature PRs.
