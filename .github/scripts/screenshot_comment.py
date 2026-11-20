"""Posts (or updates) one PR comment that shows the screenshot baselines the PR changed.

Why: reviewers should see UI changes on the PR itself, without checking out the branch.
Images are raw.githubusercontent.com URLs pinned to a commit SHA, so a later push never
changes what an old comment shows. The repo is public, so the images load without a token.

Standard library only. The pure functions build the text; `main` talks to `gh`.
"""

import json
import os
import re
import subprocess
import sys

MARKER = "<!-- screenshots -->"
REPO = "ddiling/mydayboard"
MAX_IMAGES = 12
SCREENSHOT_DIR = "/src/test/screenshots/"
_PREVIEW_NAME = re.compile(r"^(?P<class>.+Kt)\.(?P<preview>.+?)(?:\.(?P<variant>Light|Dark_NIGHT|[^.]+))?\.png$")


def raw_url(sha: str, path: str) -> str:
    return f"https://raw.githubusercontent.com/{REPO}/{sha}/{path.replace(' ', '%20')}"


def module_of(path: str) -> str:
    return path.split(SCREENSHOT_DIR)[0].replace("/", ":")


def preview_label(path: str) -> str:
    """The preview name without the package, e.g. `PrimaryButtonPreview (Light)`."""
    file_name = path.rsplit("/", 1)[-1]
    match = _PREVIEW_NAME.match(file_name)
    if match is None:
        return file_name
    variant = match.group("variant")
    return f"{match.group('preview')} ({variant})" if variant else match.group("preview")


def _is_light_first(path: str) -> tuple[bool, str]:
    return ("Light" not in path.rsplit("/", 1)[-1], path)


def parse_name_status(output: str) -> list[tuple[str, str]]:
    """Parses `git diff --name-status` lines, keeping only screenshot PNGs."""
    entries = []
    for line in output.splitlines():
        status, _, path = line.partition("\t")
        if status and path and SCREENSHOT_DIR in path and path.endswith(".png"):
            entries.append((status[0], path))
    return entries


def build_body(changes: list[tuple[str, str]], base_sha: str, head_sha: str) -> str:
    if not changes:
        return f"{MARKER}\n## Screenshots\n\nNo screenshot changes."
    ordered = sorted(changes, key=lambda change: _is_light_first(change[1]))
    shown, hidden = ordered[:MAX_IMAGES], ordered[MAX_IMAGES:]
    lines = [MARKER, "## Screenshots", "", f"Changed baselines at `{head_sha[:7]}`.", ""]
    lines += ["| Module | Preview | Before | After |", "| --- | --- | --- | --- |"]
    for status, path in shown:
        before = f"![before]({raw_url(base_sha, path)})" if status == "M" else "new"
        after = f"![after]({raw_url(head_sha, path)})"
        lines.append(f"| `{module_of(path)}` | {preview_label(path)} | {before} | {after} |")
    if hidden:
        lines += ["", f"and {len(hidden)} more:", ""]
        lines += [f"- `{module_of(path)}` {preview_label(path)}: [{status}]({raw_url(head_sha, path)})" for status, path in hidden]
    return "\n".join(lines)


def find_comment_id(comments: list[dict]) -> int | None:
    for comment in comments:
        if MARKER in comment.get("body", ""):
            return comment["id"]
    return None


def _gh(*args: str, stdin: str | None = None) -> str:
    result = subprocess.run(["gh", *args], input=stdin, capture_output=True, text=True, check=True)
    return result.stdout


def main() -> int:
    base_sha, head_sha = os.environ["BASE_SHA"], os.environ["HEAD_SHA"]
    pr, repo = os.environ["PR_NUMBER"], os.environ["GITHUB_REPOSITORY"]
    diff = subprocess.run(
        ["git", "diff", "--name-status", "--diff-filter=AM", f"{base_sha}...{head_sha}"],
        capture_output=True, text=True, check=True,
    ).stdout
    changes = parse_name_status(diff)
    comments = json.loads(_gh("api", "--paginate", "--slurp", f"repos/{repo}/issues/{pr}/comments"))
    existing = find_comment_id([c for page in comments for c in page])
    if not changes and existing is None:
        print("No screenshot changes and no comment to update.")
        return 0
    payload = json.dumps({"body": build_body(changes, base_sha, head_sha)})
    if existing is None:
        _gh("api", f"repos/{repo}/issues/{pr}/comments", "--input", "-", stdin=payload)
    else:
        _gh("api", "--method", "PATCH", f"repos/{repo}/issues/comments/{existing}", "--input", "-", stdin=payload)
    return 0


if __name__ == "__main__":
    sys.exit(main())
