#!/usr/bin/env sh
# Points git at the versioned hooks in scripts/git-hooks.
set -e
cd "$(git rev-parse --show-toplevel)"
chmod +x scripts/git-hooks/*
git config core.hooksPath scripts/git-hooks
echo "Git hooks installed (core.hooksPath=scripts/git-hooks)."
