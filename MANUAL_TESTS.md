# MANUAL_TESTS

Manual gameplay verification checklist for testers.

## How To Use
- Run each checklist item in order.
- Mark complete with `[x]` after verification.
- Record observed issues inline under the failed item.

## Template For New Changes
```md
## <Feature or Scenario Name>
- [ ] Given <precondition>, when <action>, then <expected result>.
- [ ] Given <precondition>, when <action>, then <expected result>.
```

## Current Test Cases

## Daily & Weekly Datapack Tasks
- [x] Given at least three valid daily task datapack entries exist, when joining the world and pressing the Daily/Weekly task keybind (`K` by default), then a task overview screen opens and displays exactly three daily task options.
- [x] Given Video Settings `Menu Background Blurriness` is set above 0 before opening the Daily/Weekly screen, when opening the screen with the task keybind and then closing it, then the tasks screen background is not blurred while open and the previous blur value is restored after closing.
- [x] Given the Daily/Weekly screen is opened, when viewing the layout, then the left sidebar starts directly with daily task information (without the `// EVENT CENTER` header or the two summary chips) and the right weekly routine panel remains aligned with readable weekly task titles and no control overlap.
- [x] Given one displayed daily task is an item collection task, when obtaining enough target items to meet its goal and clicking `Claim Daily Reward`, then the configured `daily_completion` reward is granted once and the daily claim button updates to claimed/disabled.
- [x] Given one displayed daily task is a mob kill task, when defeating the target mob type up to the configured goal, then task progress updates and once the daily completion requirement is met all daily tasks are marked complete.
- [x] Given weekly task datapacks include at least four easy, four medium, and two hard weekly entries, when opening the Daily/Weekly screen for a fresh week on multiple players, then each player sees exactly ten weekly tasks with a composition of 4 easy, 4 medium, and 2 hard (player task sets may differ).
- [x] Given weekly tasks include mixed easy/medium/hard definitions in datapacks, when viewing the weekly list, then entries are ordered from hard to medium to easy and each entry shows its matching point value (easy=1, medium=2, hard=5).
- [x] Given weekly tasks exceed the visible list area, when scrolling the mouse wheel over the weekly task list or dragging the weekly scrollbar thumb, then hidden weekly tasks become reachable and the scrollbar updates to match the current list position.
- [x] Given weekly tiers are visible and weekly points are below the max tier requirement, when viewing the tier section, then a `Points: current/max` summary appears near the weekly reward tiers and a progress bar below the tier rewards reflects the same point progress while staying centered and aligned to the first/last tier edges.
- [x] Given weekly point progress reaches the full weekly requirement (10 points), when the requirement is reached by completing enough weekly tasks, then all weekly tasks are marked complete automatically.
- [x] Given a player already has weekly tasks from a previous weekly-selection rule set, when joining and waiting for normal task updates, then weekly tasks automatically refresh to the current configured weekly composition without running sync/reset commands.
- [x] Given weekly tasks are completed to reach at least one unlocked tier and at least one tier remains unclaimed, when clicking `Claim Weekly Rewards (N)`, then all currently unlocked unclaimed tiers grant their configured rewards exactly once and the button updates when no claims remain.
- [ ] Given a weekly tier has become unlocked but `Claim Weekly Rewards` has not been clicked, when checking inventory/chat reward outputs, then no weekly tier reward is granted automatically until the claim button is used.
- [ ] Given daily and weekly reward pools contain item and non-item rewards, when opening the Daily/Weekly screen, then reward item icons are visible with padding in the daily reward section and on each weekly tier slot (without crossing panel/tier borders) and hovering each icon shows a tooltip describing that reward.
- [ ] Given a player has daily/weekly progress and still holds item-collection target items in inventory, when running `/incore tasks daily reset <player>` and `/incore tasks weekly reset <player>`, then daily and weekly progress resets to zero (not auto-restored from held items), claim state resets, and the task screen updates immediately.
- [ ] Given a player has active tasks, when running `/incore tasks daily complete <player>` and `/incore tasks weekly complete <player>`, then active daily/weekly task progress is set to completion and weekly points update based on completed weekly difficulties.
- [ ] Given daily completion or weekly unlocked tiers are available, when running `/incore tasks daily claim <player>` and `/incore tasks weekly claim <player>`, then rewards are granted only for claimable entries and claim state updates in the UI.
- [ ] Given a player has at least one incomplete weekly task in the visible weekly order, when running `/incore tasks weekly complete_slot <player> <slot>`, then only that 1-based weekly slot is marked complete, weekly points recalculate, and other weekly tasks keep their prior progress.
- [ ] Given a player has enough weekly points for multiple reward tiers with at least one unclaimed tier, when running `/incore tasks weekly claim_tier <player> <tier>`, then only that specific tier reward is granted and only that tier's claim state changes.
- [ ] Given any player with task data, when running `/incore tasks status <player>`, then chat output reports daily count/completion/claim state and weekly count/points/claimed tiers/claimable tiers for that player.
- [ ] Given a player has partial daily and weekly task progress, when dying and respawning, then the task overview still shows the same progress values and prior daily/weekly claim states instead of resetting.
- [ ] Given a new UTC day begins, when rejoining or waiting for sync, then daily options/progress reset and a fresh set of up to three daily tasks is selected from datapack definitions.
- [ ] Given a new UTC week begins, when rejoining or waiting for sync, then weekly options/progress/points reset and weekly tiers become unclaimed for the new week.

## Gacha Banner Showcase Glint Isolation
- [x] Given a banner that has at least one enchanted/glint 5★ or 6★ item and at least one non-glint item in the showcase, when opening the banner screen and viewing the right-side showcase, then only the enchanted item icons glow and non-glint icons never show a square/partial borrowed glow.
- [x] Given a banner with many 5★ and 6★ showcase items (for example the updated basic banners), when opening the banner screen, then 6★ and 5★ items render in separate centered multi-row groups without icon overlap artifacts.
- [x] Given the same banner, when switching between different banners in the left sidebar, then the showcase updates to the selected banner and glint behavior remains isolated to enchanted items only.

## Gacha UI Tooltips
- [x] Given the banner selection screen is open, when hovering any 5★ or 6★ item icon in the pull highlights area, then an item tooltip appears for the hovered icon and follows the cursor.
- [x] Given the banner info screen is open, when hovering a reward row, then a tooltip appears showing the reward item name (or item id for invalid entries), rarity stars, and chance percent for that row.
- [x] Given the info screen has multiple reward rows, when moving the cursor across different rows, then the tooltip content updates to match the currently hovered reward.

## Gacha Runtime Banner Permits
- [x] Given a valid banner id, when running `/incore gacha give_banner_permit <player> <banner_id> 1`, then the granted permit has a runtime name matching the banner and a tooltip showing that banner.
- [x] Given a basic banner and inventory containing only matching banner-specific permits for that banner, when pressing `Pull x10`, then purchase succeeds and consumes those specific permits.
- [x] Given a basic banner and inventory containing mixed matching banner-specific permits and basic permits, when pressing `Pull x10`, then the purchase succeeds using both currencies up to 10 total.
- [x] Given an event banner and inventory containing mixed matching banner-specific permits and chartered permits, when pressing `Pull x10`, then the purchase succeeds using both currencies up to 10 total.

## Battle Pass Task Completion and Rotation
- [ ] Given a world date within `season_alpha` week 1 and op permissions, when running `/incore battlepass complete_task <player> alpha_week1_patrol`, then the command succeeds and reports the configured XP gain for the bronze tier task.
- [ ] Given a world date within `season_alpha` week 1 and one weekly task already completed for week 1, when completing another week 1 task, then the command fails with a weekly completion cap reached message (only half of week 1 tasks completable).
- [ ] Given a world date within `season_alpha` and two permanent tasks already completed, when completing a third permanent task, then the command fails with a permanent completion cap reached message (only half of permanent tasks completable).
- [ ] Given the world date advanced to `2026-01-01T00:00:00Z` or later, when running `/incore battlepass status <player>`, then the active battle pass set changes from `incore:season_alpha` to `incore:season_bravo` without server restart.
- [ ] Given enough completed tasks to cross at least one level threshold, when checking inventory and command output, then the player receives the reward(s) defined in the active battle pass datapack level rewards.
- [ ] Given a battle pass level reward configured as `{"type":"sanity_cap_bonus","amount":5}`, when earning that level, then the player's maximum sanity increases by 5 and persists after relog.
