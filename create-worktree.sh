#!/bin/bash

set -e

if [ -z "$1" ]; then
    echo "Usage: $0 <branch-name>"
    echo "Example: $0 feature/my-feature"
    exit 1
fi

BRANCH_NAME="$1"
REPO_ROOT="$(git rev-parse --show-toplevel)"
WORKTREE_NAME="INCore-$(basename "$BRANCH_NAME")"
WORKTREE_PATH="$(dirname "$REPO_ROOT")/$WORKTREE_NAME"

if [ -d "$WORKTREE_PATH" ]; then
    echo "Error: Worktree directory already exists: $WORKTREE_PATH"
    exit 1
fi

echo "Creating worktree at: $WORKTREE_PATH"
git worktree add "$WORKTREE_PATH" -b "$BRANCH_NAME" 2>/dev/null || git worktree add "$WORKTREE_PATH" "$BRANCH_NAME"

echo "Copying run directory..."
cp -r "$REPO_ROOT/run" "$WORKTREE_PATH/"

echo "Worktree created successfully at: $WORKTREE_PATH"
