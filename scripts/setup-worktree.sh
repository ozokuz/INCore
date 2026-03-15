#!/bin/bash

set -euo pipefail

REPO_ROOT="$(git rev-parse --show-toplevel)"

MAIN_WORKTREE=""
PENDING_WORKTREE=""

while IFS= read -r line; do
    if [[ -z "$line" ]]; then
        PENDING_WORKTREE=""
        continue
    fi

    case "$line" in
        worktree\ *)
            PENDING_WORKTREE="${line#worktree }"
            ;;
        branch\ refs/heads/main)
            MAIN_WORKTREE="$PENDING_WORKTREE"
            ;;
    esac
done < <(git worktree list --porcelain)

if [[ -z "$MAIN_WORKTREE" ]]; then
    echo "Error: Could not find a checked out worktree for branch 'main'."
    exit 1
fi

SOURCE_RUN_DIR="$MAIN_WORKTREE/run"
TARGET_RUN_DIR="$REPO_ROOT/run"

if [[ ! -d "$SOURCE_RUN_DIR" ]]; then
    echo "Error: Main worktree run directory not found at: $SOURCE_RUN_DIR"
    exit 1
fi

if [[ "$MAIN_WORKTREE" != "$REPO_ROOT" ]]; then
    echo "Copying run directory from $MAIN_WORKTREE to $REPO_ROOT"
    rm -rf "$TARGET_RUN_DIR"
    cp -a "$SOURCE_RUN_DIR" "$TARGET_RUN_DIR"
else
    echo "Current worktree is main; skipping run directory copy."
fi

echo "Compiling code with Gradle..."
cd "$REPO_ROOT"
./gradlew compileJava
