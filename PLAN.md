# PLAN.md — Battlepass System Changes (Minecraft Mod)

## Goals
Implement the following behavior changes and features:
- Weeks start on **Monday** (ignore locale).
- A week starts at **12:00 (midday)** using **server local time**.
- Fix battlepass admin commands (currently “invalid argument” / non-executable).
- Battlepass GUI: selecting a **level** shows **XP required** for that level.
- Changing battlepass set via commands sets the battlepass start to the **start of the ongoing week**.
- Each battlepass defines its **length** in its datapack files.
- Rename `tier` command to `level` command (and keep compatibility).
- Add **separate reward lanes** (Basic Supply, Originium Supply, Protocol Customized) unlockable via **items and commands**.

---

## Required context (answer these or I’ll implement reasonable defaults)
1. **Time source**: Are you currently using `Instant/UTC`, world time (`Level.getDayTime()`), or `LocalDateTime`? Any existing “week” utility?
2. **Battlepass data**: Where does battlepass config live (datapack JSON, custom registry, NBT, config file)? Provide an example file if possible.
3. **GUI code**: Which GUI framework is used (vanilla screens, Forge menu/screen, Fabric ScreenHandler, custom)? Where is “select level” handled?
4. **Reward lanes**:
   - What rewards currently exist (single lane per level, multiple rewards per level, etc.)?
   - Should lanes have **independent** reward lists per level, or share levels but different rewards?
   - Unlock items: what are the item IDs (or should we add new items)? Are they consumed on use?
5. **Command framework**: Brigadier commands? What is the root command and subcommand structure now?

If the above isn’t provided, implement with defaults noted under “Defaults”.

---

## Defaults (used if context isn’t provided)
- Time calculations done with `java.time.ZonedDateTime` using server JVM default zone (`ZoneId.systemDefault()`), derived from server tick time only for “now”.
- Datapack format is JSON per battlepass with fields like:
  - `id`, `display`, `rewards`, `xp_curve`, `length_weeks`
- Reward lanes stored per battlepass as 3 named lanes with per-level reward lists.
- Unlocks stored per-player (capability/component) as booleans: `basic`, `originium`, `protocol`.
- Unlock items:
  - `modid:originium_supply_unlock`
  - `modid:protocol_customized_unlock`
  - Basic lane is default unlocked.

---

## Step 1 — Week definition: Monday @ 12:00 local
### Deliverable
A single authoritative utility that returns:
- start of current week (Monday 12:00)
- start of next week (Monday 12:00 next week)
- week index / stable key (for storage and comparison)

### Tasks
- Add `WeekTime` utility (name as appropriate):
  - `ZonedDateTime now(Server)`
  - `ZonedDateTime weekStart(ZonedDateTime t)`:
    - Convert to local zone.
    - Find the Monday of the week containing `t` (ISO week, Monday-based).
    - Set time to `12:00:00.000`.
    - If `t` is before Monday 12:00, weekStart should be the previous Monday 12:00.
  - `ZonedDateTime nextWeekStart(ZonedDateTime t)` = `weekStart(t).plusWeeks(1)`
  - `long weekKey(ZonedDateTime t)` (e.g., `YYYY-WW` or epoch millis of weekStart) for persistence.
- Replace any existing “week start” logic to use this utility.

### Tests / checks
- Sunday 11:00 -> weekStart is previous Monday 12:00.
- Monday 11:59 -> weekStart is previous Monday 12:00 (i.e., still last week).
- Monday 12:00 -> weekStart is that moment.
- Monday 12:01 -> weekStart is that Monday 12:00.
- DST transition week: weekStart should still be a valid local time; if 12:00 is skipped (rare), shift using `withLaterOffsetAtOverlap` / `ofLocal` fallback.

---

## Step 2 — Battlepass start alignment when switching sets
### Deliverable
When admin uses “change battlepass set”, the battlepass start time becomes the **current ongoing weekStart** (Monday 12:00).

### Tasks
- Identify commands that switch battlepass sets (e.g., `/battlepass set <id>`).
- Update handler:
  - `newStart = weekStart(now(server))`
  - Persist `battlepassStart = newStart` and `battlepassId = selected`
  - If there’s a per-player week cache, invalidate/refresh.
- Ensure this does NOT retroactively erase progress unless intended:
  - If progress is tied to `battlepassId + weekKey`, switching battlepass should either:
    - (Preferred) start a fresh weekly cycle for the new pass, or
    - preserve XP but remap? (document chosen behavior)

---

## Step 3 — Fix “commands for setting the current battlepass” invalid argument
### Deliverable
Commands are executable and validate inputs correctly.

### Tasks
- Locate command registration:
  - Brigadier `literal("battlepass")` tree.
  - Find subcommand for “set current battlepass”.
- Common failure causes:
  - argument type mismatch (e.g., expecting resource location but parsing string)
  - missing suggestion provider / registry lookup throwing
  - server/client command registration mismatch
- Implement:
  - Use `ResourceLocationArgument` (or equivalent) for battlepass IDs if IDs are namespaced.
  - Validate battlepass exists in loaded datapack registry before applying.
  - Add suggestions from available battlepasses.
  - Provide clear error messages: “Unknown battlepass: <id>”.
- Add a minimal integration test path (or log-verified manual steps) to confirm execution.

---

## Step 4 — Battlepass length is defined in datapack
### Deliverable
Each battlepass declares length; runtime uses it to compute end time and validity.

### Tasks
- Extend datapack schema with a required field, e.g.:
  - `length_weeks: int` (or `length_days: int`; choose one and standardize)
  - Optionally: `length_type: "WEEKS" | "DAYS"` if you want flexibility.
- Update loader/parser:
  - Validate positive integer.
  - Backward compatibility: if missing, default to 1 week (or previous hardcoded behavior).
- Update battlepass runtime:
  - `battlepassEnd = battlepassStart + length`
  - Any “is active” checks use `[start, end)`.
- Update any UI text showing duration (optional but useful).

---

## Step 5 — GUI: selecting a level shows XP required for that level
### Deliverable
In battlepass GUI, when hovering/clicking a level, show the XP required.

### Tasks
- Define what “XP required” means:
  - Total XP required to reach that level from level 1 (cumulative), or
  - XP required for that level alone (delta).
- Implement both values if easy:
  - “XP to reach: X” and “XP for level: Y”
- Find the selection handler:
  - Add a text widget/tooltip region that updates on selection.
  - Pull XP requirement from the battlepass progression curve:
    - `requiredTotalXp(level)`
- Ensure server/client sync:
  - If curve is in datapack and already synced to client, compute client-side.
  - If not, send a small packet with curve or per-level required XP list for the active pass.

---

## Step 6 — Rename `tier` command to `level` command
### Deliverable
Users use `/incore battlepass level ...` instead of `/incore battlepass tier ...`.

### Tasks
- Add new `level` subcommand tree matching old `tier` behavior.
- Update:
  - help text
  - permissions
  - docs/README
  - any command autocompletion/suggestions

---

## Step 7 — Reward lanes system (Basic / Originium / Protocol)
### Deliverable
Battlepass rewards are split into multiple lanes; lanes can be locked/unlocked per player via items and commands.

### Data model changes
- Battlepass datapack adds:
  - `lanes: ["basic", "originium", "protocol"]` (optional; default all)
  - `rewards_by_lane`:
    - `basic`: per-level reward list
    - `originium`: per-level reward list
    - `protocol`: per-level reward list
- Player unlock state:
  - `unlocked_lanes: set<string>` or bitmask
  - Defaults: `basic` unlocked, others locked.

### Unlock mechanisms
- Items:
  - On right-click/use (server-side), unlock lane if not already unlocked.
  - Decide consume:
    - Default: consume 1 item on successful unlock.
  - Feedback:
    - chat message + sound + optional toast.
- Commands:
  - `/battlepass lane unlock <player> <lane>`
  - `/battlepass lane lock <player> <lane>`
  - `/battlepass lane list <player>`
  - Suggestions for `<lane>` from enum.

### Claiming logic
- If your system already supports “claim per level”, update it:
  - When claiming at level N:
    - grant rewards for each lane where `laneUnlocked(player)`.
    - track claim state per lane, per level:
      - `claimed[level][lane] = true`
- If you have auto-claim:
  - apply same per-lane checks.

### GUI changes
- Show lanes visually:
  - tabs or columns per lane.
  - locked lanes appear greyed with “Locked” overlay and hint on how to unlock.
- When a level is selected:
  - show XP requirement (from Step 5)
  - show rewards for each lane, with lock status.

### Networking
- Ensure client knows:
  - active battlepass lanes and reward data
  - player unlocked lanes
  - per-lane claim states (or compute from server on demand)
- Add packets:
  - `S2C_BattlepassData` includes lanes + rewards + xp curve
  - `S2C_PlayerBattlepassState` includes unlocked lanes + claims + current XP/level

### Backward compatibility
- Do not worry about backward compatibility

---

## Step 8 — Validation and edge cases
### Edge cases
- Server timezone changes:
  - Week boundaries shift; document as “server-local-time dependent”.
- Switching battlepass set mid-week:
  - Start snaps to current weekStart; end = start + length; users may see shortened/extended cycle depending on length.
- Datapack reload:
  - If active pass definition changes, ensure runtime refresh does not corrupt claims.

---

## Step 9 — Quick manual test checklist
- WeekStart:
  - Verify with printed debug command `/battlepass debug week` showing now/weekStart/nextWeekStart.
- Command fixes:
  - `/battlepass set <validId>` works; invalid shows clear error.
- Switching sets:
  - After switching, stored start equals Monday 12:00 of current week.
- Length:
  - Battlepass ends after configured length; active checks behave.
- GUI:
  - Selecting a level shows required XP consistently.
- Rename:
  - `level` works; `tier` works with warning.
- Lanes:
  - Basic rewards always claimable.
  - Unlock items unlock lanes and update GUI.
  - Commands unlock/lock lanes correctly.
  - Claims tracked per lane.

---

## Implementation order (recommended)
1. WeekTime utility (Step 1)
2. Command fixes + rename alias (Steps 3 + 6)
3. Switch-set alignment (Step 2)
4. Datapack length support (Step 4)
5. GUI XP requirement (Step 5)
6. Lanes: data model + unlocks + claiming + GUI (Step 7)
7. Edge-case hardening (Step 8)
8. Manual tests (Step 9)

