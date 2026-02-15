---
name: bug-fixer
description: Fix unchecked manual test cases by reading MANUAL_TESTS.md, implementing code changes that satisfy unchecked scenarios, and updating test documentation. Use when the user asks to "fix test cases", "correct test cases", or "go through tests and correct them".
---

# Bug Fixer

## Overview

Resolve failing or incomplete manual gameplay scenarios from `MANUAL_TESTS.md`.
Use a deterministic workflow: enumerate unchecked items, implement fixes, verify build, and keep manual tests aligned with the new behavior.

## Workflow

1. Read `MANUAL_TESTS.md` and collect every unchecked item (`- [ ]`).
2. Prioritize items that can be verified quickly and that unblock other cases.
3. For each target case:
- Identify feature files tied to the scenario.
- Implement a real behavior fix (do not only edit test text).
- Update `MANUAL_TESTS.md` so the case is accurate for the new behavior.
4. Compile and run relevant checks available in the repo.
5. Report exactly which unchecked cases were addressed and what remains unchecked.

## Execution Notes

- Prefer `rg` for locating affected code and checklist references.
- Keep changes scoped to the selected unchecked cases for each pass.
- Preserve the checklist style in `MANUAL_TESTS.md`:
  `- [ ] Given <precondition>, when <action>, then <expected result>.`
- Add new checklist items when behavior changes introduce new manual verification needs.

## Done Criteria

- Address at least one unchecked case with code changes.
- Keep `MANUAL_TESTS.md` updated for all behavior changes made in the pass.
- Leave the repo in a compiling state before returning results.
