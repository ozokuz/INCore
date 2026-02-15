# AGENTS.md

## Role
Act as the coding agent for this repository. Make practical, testable changes and keep documentation aligned with gameplay behavior.

## Working Rules
- Preserve existing project conventions and file structure.
- When behavior changes, update related docs in the same task.

## Manual Testing Cases Policy (Required)
For any code change, you must add or update entries in `MANUAL_TESTS.md`.

Use checklist format so a gameplay tester can run the list directly:
- Use Markdown checkboxes: `- [ ]`.
- Write one verifiable outcome per checklist item.
- Group cases by feature or scenario with clear headings.
- Include enough detail for execution: setup/preconditions, action, and expected result.

Example format:

```md
## Movement
- [ ] Given player starts at spawn, when moving forward for 3 seconds, player position changes continuously without stutter.
- [ ] Given player collides with wall, movement stops at collision boundary and player does not clip through.
```

## Definition of Done
- Code changes are implemented.
- The code compiles successfully.
- `MANUAL_TESTS.md` is updated for every code change.
