#!/usr/bin/env python3
"""Reject generative-assistant names and attribution in contribution metadata."""

from __future__ import annotations

import argparse
import json
import re
import subprocess
import sys
from pathlib import Path

ASSISTANT_NAME = re.compile(
    r"(?i)(?<![a-z0-9])(?:"
    r"aider|anthropic|chatgpt|claude|codex|cursor|deepseek|devin|gemini|"
    r"github[-_ /]?copilot|gpt(?:[-_ ]?[0-9][a-z0-9.-]*)?|llama|openai|qwen|"
    r"windsurf"
    r")(?![a-z0-9])"
)
ATTRIBUTION = re.compile(
    r"(?im)(?:"
    r"\b(?:ai|llm)[-_ ]?(?:assisted|authored|generated|reviewed|written)\b|"
    r"\b(?:assisted|authored|created|generated|implemented|reviewed|written)\s+"
    r"(?:by|with)\s+(?:an?\s+)?(?:ai|coding assistant|generative assistant|llm)\b|"
    r"^\s*(?:assisted-by|generated-by)\s*:"
    r")"
)


def git(*args: str) -> str:
    return subprocess.run(
        ["git", *args], check=True, capture_output=True, text=True
    ).stdout


def current_branch() -> str:
    result = subprocess.run(
        ["git", "symbolic-ref", "--quiet", "--short", "HEAD"],
        check=False,
        capture_output=True,
        text=True,
    )
    if result.returncode not in (0, 1):
        raise subprocess.CalledProcessError(
            result.returncode, result.args, output=result.stdout, stderr=result.stderr
        )
    return result.stdout.strip()


def metadata_violations(label: str, value: str) -> list[str]:
    violations = []
    if match := ASSISTANT_NAME.search(value):
        violations.append(f"{label}: contains prohibited name {match.group(0)!r}")
    if match := ATTRIBUTION.search(value):
        violations.append(
            f"{label}: contains prohibited attribution {match.group(0)!r}"
        )
    return violations


def branch_violations(label: str, branch: str) -> list[str]:
    if match := ASSISTANT_NAME.search(branch):
        return [
            f"{label}: branch {branch!r} contains prohibited name {match.group(0)!r}"
        ]
    return []


def messages_in_range(revision_range: str) -> list[tuple[str, str]]:
    fields = git("log", "--format=%H%x00%B%x00", revision_range).split("\0")
    return [
        (f"commit {fields[index].strip()[:12]}", fields[index + 1])
        for index in range(0, len(fields) - 1, 2)
        if fields[index].strip()
    ]


def check_event(event_path: Path) -> list[str]:
    event = json.loads(event_path.read_text(encoding="utf-8"))
    violations = []
    pull_request = event.get("pull_request")
    if isinstance(pull_request, dict):
        head = pull_request.get("head", {})
        base = pull_request.get("base", {})
        branch = head.get("ref")
        if isinstance(branch, str):
            violations.extend(branch_violations("pull request", branch))
        for field in ("title", "body"):
            value = pull_request.get(field)
            if isinstance(value, str):
                violations.extend(metadata_violations(f"pull request {field}", value))
        base_sha = base.get("sha")
        head_sha = head.get("sha")
        if isinstance(base_sha, str) and isinstance(head_sha, str):
            for label, message in messages_in_range(f"{base_sha}..{head_sha}"):
                violations.extend(metadata_violations(label, message))
        return violations

    ref = event.get("ref")
    if isinstance(ref, str) and ref.startswith("refs/heads/"):
        violations.extend(branch_violations("push", ref.removeprefix("refs/heads/")))
    for commit in event.get("commits", []):
        if isinstance(commit, dict) and isinstance(commit.get("message"), str):
            violations.extend(
                metadata_violations(
                    f"commit {str(commit.get('id', ''))[:12]}", commit["message"]
                )
            )
    return violations


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    subparsers = parser.add_subparsers(dest="command", required=True)
    subparsers.add_parser("branches")
    message = subparsers.add_parser("message-file")
    message.add_argument("path", type=Path)
    event = subparsers.add_parser("github-event")
    event.add_argument("path", type=Path)
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    if args.command == "branches":
        branch = current_branch()
        violations = branch_violations("repository", branch) if branch else []
    elif args.command == "message-file":
        violations = metadata_violations(
            f"commit message {args.path}", args.path.read_text(encoding="utf-8")
        )
    else:
        violations = check_event(args.path)

    if violations:
        print("Contribution metadata must remain attribution-neutral:", file=sys.stderr)
        for violation in violations:
            print(f"  {violation}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
