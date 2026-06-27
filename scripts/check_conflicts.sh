#!/usr/bin/env bash
set -euo pipefail

# Fails if unresolved Git merge-conflict markers are present in tracked source files.
# Excludes generated/build folders and the .git database.
if git grep -n -E '^(<<<<<<<|=======|>>>>>>>)' -- \
  ':!app/build' \
  ':!build' \
  ':!.gradle' \
  ':!.git'; then
  echo "Unresolved merge conflict markers found." >&2
  exit 1
fi

echo "No unresolved merge conflict markers found."
