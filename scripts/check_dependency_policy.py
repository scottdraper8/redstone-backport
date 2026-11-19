#!/usr/bin/env python3
"""Enforce reproducible dependencies and security-only automated updates."""

from __future__ import annotations

import re
import sys
import tomllib
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SHA40 = re.compile(r"[0-9a-f]{40}\Z")
ACTION_REFERENCE = re.compile(r"^\s*uses:\s*([^\s#]+)@([^\s#]+)", re.MULTILINE)
PRECOMMIT_REFERENCE = re.compile(
    r"^\s*-\s+repo:\s*(?!local\s*$)\S+.*?^\s+rev:\s*([^\s#]+)",
    re.MULTILINE | re.DOTALL,
)
EXACT_PYTHON = re.compile(r"^[A-Za-z0-9_.-]+(?:\[[A-Za-z0-9_.,-]+\])?==[^<>=!~*]+$")
VERSION_VALUE = re.compile(r"[0-9][0-9A-Za-z.+-]*\Z")


def check_python_manifests(failures: list[str]) -> None:
    for relative in (Path("pyproject.toml"), Path("docs/pyproject.toml")):
        path = ROOT / relative
        with path.open("rb") as stream:
            document = tomllib.load(stream)
        requirements: list[str] = []
        requirements.extend(document.get("build-system", {}).get("requires", []))
        requirements.extend(document.get("project", {}).get("dependencies", []))
        for values in document.get("dependency-groups", {}).values():
            requirements.extend(values)
        for requirement in requirements:
            if requirement == "redstone-backport-docs":
                continue
            if EXACT_PYTHON.fullmatch(requirement) is None:
                failures.append(f"{relative}: dependency {requirement!r} is not exactly pinned")

    lock_path = ROOT / "uv.lock"
    with lock_path.open("rb") as stream:
        lock = tomllib.load(stream)
    for package in lock.get("package", []):
        if package.get("source", {}).get("registry") is None:
            continue
        artifacts = []
        if "sdist" in package:
            artifacts.append(package["sdist"])
        artifacts.extend(package.get("wheels", []))
        if not artifacts:
            failures.append(f"uv.lock: {package['name']} has no hash-verified artifact")
        for artifact in artifacts:
            if not str(artifact.get("hash", "")).startswith("sha256:"):
                failures.append(f"uv.lock: {package['name']} has an unverified artifact")


def check_gradle(failures: list[str]) -> None:
    properties = {}
    for line in (ROOT / "gradle.properties").read_text(encoding="utf-8").splitlines():
        if "=" not in line or line.lstrip().startswith("#"):
            continue
        key, value = line.split("=", 1)
        properties[key.strip()] = value.strip()
    for name, value in properties.items():
        if name.endswith("_version") and VERSION_VALUE.fullmatch(value) is None:
            failures.append(f"gradle.properties: {name} is not an exact version")

    for path in sorted(ROOT.rglob("*.gradle.kts")):
        text = path.read_text(encoding="utf-8")
        if re.search(
            r'(?i)(?:version\s+"(?:latest|\+|\[|\()|:[^"\n:]+:(?:latest(?:\.[^"]*)?|\+|\[[^"]*|\([^"]*)")',
            text,
        ):
            failures.append(f"{path.relative_to(ROOT)}: dynamic dependency version detected")

    for relative in (
        Path("gradle.lockfile"),
        Path("common/gradle.lockfile"),
        Path("fabric/gradle.lockfile"),
        Path("forge/gradle.lockfile"),
        Path("quilt/gradle.lockfile"),
    ):
        path = ROOT / relative
        if not path.is_file() or not path.read_text(encoding="utf-8").strip():
            failures.append(f"{relative}: resolved Gradle dependency lock is required")

    verification = ROOT / "gradle" / "verification-metadata.xml"
    if not verification.is_file() or "<sha256 value=" not in verification.read_text(
        encoding="utf-8"
    ):
        failures.append("gradle/verification-metadata.xml: SHA-256 verification is required")

    wrapper = (ROOT / "gradle" / "wrapper" / "gradle-wrapper.properties").read_text(
        encoding="utf-8"
    )
    if not re.search(r"(?m)^distributionSha256Sum=[0-9a-f]{64}$", wrapper):
        failures.append("Gradle distribution must have an exact SHA-256 checksum")


def check_automation(failures: list[str]) -> None:
    for workflow in sorted((ROOT / ".github" / "workflows").glob("*.y*ml")):
        text = workflow.read_text(encoding="utf-8")
        for action, revision in ACTION_REFERENCE.findall(text):
            if not action.startswith(("./", "docker://")) and SHA40.fullmatch(revision) is None:
                failures.append(
                    f"{workflow.relative_to(ROOT)}: {action} must use a full commit SHA"
                )

    precommit = ROOT / ".pre-commit-config.yaml"
    precommit_text = precommit.read_text(encoding="utf-8")
    for revision in PRECOMMIT_REFERENCE.findall(precommit_text):
        if SHA40.fullmatch(revision) is None:
            failures.append(
                f"{precommit.relative_to(ROOT)}: hook revision {revision!r} is not a full commit SHA"
            )
    for dependency in re.findall(r"additional_dependencies:\s*\[([^]]+)]", precommit_text):
        for item in (value.strip() for value in dependency.split(",")):
            if EXACT_PYTHON.fullmatch(item) is None:
                failures.append(f"{precommit.relative_to(ROOT)}: {item!r} is not exactly pinned")

    dependabot = (ROOT / ".github" / "dependabot.yml").read_text(encoding="utf-8")
    for ecosystem in ("gradle", "uv", "github-actions"):
        if dependabot.count(f"package-ecosystem: {ecosystem}") != 1:
            failures.append(f"Dependabot must monitor {ecosystem} exactly once")
    if dependabot.count("open-pull-requests-limit: 0") != 3:
        failures.append("Dependabot routine version-update PRs must be disabled")
    if dependabot.count("versioning-strategy: increase") != 1:
        failures.append("Package security updates must preserve exact direct pins")

    docs_workflow = (ROOT / ".github" / "workflows" / "deploy-docs.yml").read_text(
        encoding="utf-8"
    )
    if "uv sync --frozen" not in docs_workflow or 'version: "latest"' in docs_workflow:
        failures.append("Documentation CI must use the pinned uv tool and frozen lockfile")


def main() -> int:
    failures: list[str] = []
    check_python_manifests(failures)
    check_gradle(failures)
    check_automation(failures)
    if failures:
        print("\n".join(failures), file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
