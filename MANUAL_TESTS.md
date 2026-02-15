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
- [x] Given a world date within `season_alpha` week 1 and op permissions, when running `/incore battlepass complete_task <player> alpha_week1_patrol`, then the command succeeds and reports the configured XP gain for the bronze tier task.
- [x] Given an active battle pass set exists and the command input cursor is at `/incore battlepass complete_task <player> ` or `/incore battlepass progress_task <player> `, when requesting autocomplete for `task_id`, then suggestions include only task ids from the currently active battle pass set.
- [x] Given battle pass sets are loaded, when requesting autocomplete for `/incore battlepass set <set_id>`, then suggestions include available set ids such as `incore:season_alpha` and `incore:season_bravo`.
- [x] Given a player completes a battle pass task that grants XP and may grant a level, when running `/incore battlepass complete_task <player> <task_id>`, then success chat output includes `<player>: <result message> +<xp> xp` and appends `+<level(s)> level(s) (now <new level>)` only when at least one level is gained.
- [x] Given a task has goal > 1 (for example `alpha_week1_patrol` goal 2), when running `/incore battlepass progress_task <player> alpha_week1_patrol 1`, then command output reports updated progress `(1/2)` and task remains incomplete with no XP granted yet.
- [x] Given the same task is at `(1/2)`, when running `/incore battlepass progress_task <player> alpha_week1_patrol 1` again, then progress reaches `(2/2)`, the task completes, and XP/level gain messaging is shown.
- [x] Given a task has goal > 1, when running `/incore battlepass complete_task <player> <task_id>`, then the task progress is forced to goal and immediately completes in one command.
- [ ] Given a task already has partial progress (for example 1/3) and op permissions, when running `/incore battlepass complete_task <player> <task_id>`, then the task always completes (no integer overflow reset), the command reports success with XP gain, and task progress stays at goal.
- [x] Given world date is within `season_alpha` week 2 and op permissions, when running `/incore battlepass complete_task <player> alpha_week2_overdrive`, then the command grants `+550 xp` from the new `diamond` tier value configured in `season_alpha` tier XP.
- [x] Given world date is within `season_bravo` week 2 and op permissions, when running `/incore battlepass complete_task <player> bravo_week2_supreme`, then the command grants `+600 xp` from the new `diamond` tier value configured in `season_bravo` tier XP.
- [x] Given world date is within `season_bravo` week 2, the `week:1` category completion cap has not been reached, and `bravo_week1_supply` is unfinished, when running `/incore battlepass complete_task <player> bravo_week1_supply`, then the task still succeeds (weekly tasks do not expire after their week passes).
- [ ] Given op permissions and loaded sets include `incore:season_alpha` and `incore:season_bravo`, when running `/incore battlepass set incore:season_alpha`, then `/incore battlepass status <player>` reports `incore:season_alpha` as the active set regardless of current date.
- [x] Given the forced active set is `incore:season_alpha`, when running `/incore battlepass next`, then `/incore battlepass status <player>` reports `incore:season_bravo`.
- [x] Given the forced active set is `incore:season_bravo`, when running `/incore battlepass previous`, then `/incore battlepass status <player>` reports `incore:season_alpha`.
- [x] Given an active battle pass set with 2 total weeks, when running `/incore battlepass week set 2`, then `/incore battlepass status <player>` reports current week `2`.
- [x] Given forced active week is `1`, when running `/incore battlepass week next`, then `/incore battlepass status <player>` reports current week `2`.
- [x] Given forced active week is `2`, when running `/incore battlepass week previous`, then `/incore battlepass status <player>` reports current week `1`.
- [x] Given op permissions and an active battle pass set, when running `/incore battlepass xp set <player> 1400`, then `/incore battlepass status <player>` reports XP `1400` and level `2` when xp-per-level is `700`.
- [x] Given the same player currently has `1400` battle pass XP, when running `/incore battlepass xp add <player> -700`, then `/incore battlepass status <player>` reports XP `700` and level `1`.
- [x] Given op permissions and an active battle pass set, when running `/incore battlepass tier set <player> 3`, then `/incore battlepass status <player>` reports level `3` and XP equals `3 * xp_per_level` for the active set.
- [x] Given the same player currently has tier/level `3`, when running `/incore battlepass tier add <player> -1`, then `/incore battlepass status <player>` reports tier/level `2` and XP equals `2 * xp_per_level`.
- [x] Given a player has partial progress on `alpha_week1_patrol`, when running `/incore battlepass reset task <player> alpha_week1_patrol`, then the task no longer shows stored progress/completion while other task progress remains unchanged.
- [x] Given a player has progress on multiple tasks in the active set, when running `/incore battlepass reset tasks <player>`, then all task progress/completions are cleared while battle pass XP/level remains unchanged.
- [x] Given a player has non-zero battle pass XP/level and completed tasks in the active set, when running `/incore battlepass reset all <player>`, then XP becomes `0`, level becomes `0`, and all task progress/completions are cleared.
- [x] Given a task category (for example `week:1` or `permanent`) has `N` currently available tasks and `floor(N/2)` tasks in that same category are already completed, when completing another task from that category, then the command fails with a category completion cap reached message.
- [x] Given week `1` category is at its completion cap and week `2` is now active with room under its own cap, when completing a week `2` task, then the completion succeeds (category caps are tracked independently).
- [x] Given the world date advanced to `2026-02-15T00:00:00Z` or later, when running `/incore battlepass status <player>`, then the active battle pass set changes from `incore:season_alpha` to `incore:season_bravo` without server restart.
- [x] Given enough completed tasks to cross at least one level threshold, when pressing `Claim All Rewards`, then the player receives the reward(s) defined in the active battle pass datapack level rewards.
- [x] Given enough completed tasks to cross at least one level threshold, when checking inventory before pressing `Claim All Rewards`, then level rewards are not granted automatically.
- [x] Given a fresh player on an active set with a configured level `0` reward, when opening Battle Pass and pressing `Claim All Rewards` at level `0`, then the level `0` reward grants once and unclaimed count decreases.
- [x] Given unclaimed battle pass levels exist, when pressing the `Claim All Rewards` button in the bottom-right of Battle Pass screen, then all currently unclaimed level rewards are granted and unclaimed count returns to zero.
- [x] Given a battle pass level reward configured as `{"type":"sanity_cap_bonus","amount":5}`, when earning that level, then the player's maximum sanity increases by 5 and persists after relog.
- [x] Given `season_alpha` rewards now include levels `0` through `50`, when gaining levels to at least `50` and claiming rewards, then each configured level reward grants exactly once per level.
- [x] Given `season_bravo` rewards now include levels `0` through `50`, when gaining levels to at least `50` and claiming rewards, then each configured level reward grants its configured reward(s) with no missing levels.

## Battle Pass Screen and Sync
- [x] Given the player opens Player Status with the `Open Player Status` keybind (`O` by default), when pressing `View Battle Pass`, then a Battle Pass screen opens showing active set id, current week, level, XP progress, and completion caps.
- [x] Given the player is in-game with no menu open, when pressing the dedicated `Open Battle Pass` keybind (`B` by default), then the Battle Pass screen opens directly.
- [x] Given the Battle Pass screen is open, when viewing the top area, then the UI uses two tabs (`Pass Rewards` and `Pass Missions`) plus a prominent hero progress bar similar to a battle-pass storefront layout (set card on left, level/progress/time on right).
- [x] Given the Battle Pass screen is open at default GUI scale, when viewing the top XP progress bar at different window widths, then bar borders/details remain crisp and not visibly stretched (sliced texture rendering).
- [x] Given a battle pass set has weekly and permanent tasks with tier XP, when viewing the Battle Pass screen task list, then each row shows task description plus week/permanent label, tier, and XP reward computed from datapack task tier/xp config.
- [x] Given the `Pass Missions` tab is selected, when viewing a mission row, then the row shows a per-task progress bar and numeric `current/goal` progress value that updates as `progress_task` commands are executed.
- [x] Given the `Pass Missions` tab is selected and a mission category is clicked, when selecting another category, then the mission list updates to show only tasks from the selected category.
- [x] Given the `Pass Missions` tab is selected, when viewing mission categories, then each category shows its own completion progress in `completed/cap` format (for example `1/2 completions`) based only on available tasks in that category.
- [x] Given the `Pass Missions` tab is selected, when viewing a mission row, then the row shows a per-task progress bar and numeric `current/goal` progress value that updates as `progress_task` commands are executed.
- [x] Given the `Pass Rewards` tab is selected, when viewing reward rows, then reward cards are grouped into three labeled tracks (`Basic Supply`, `Originium Supply`, `Protocol Customized`) with level columns and per-card reward icons/amounts.
- [x] Given the `Pass Rewards` tab is selected with the reward list scrolled to the beginning, when viewing level headers, then a `Lv 0` column is present before `Lv 1`.
- [x] Given the player is at battle pass level `N` and the pass has a highest configured level `L` (for example `50`), when manually scrolling the rewards track, then `Lv L` stays pinned on the far-right column, and `Lv N` pins on the far-left only after no smaller levels than `N` are visible.
- [x] Given a reward card has stack amount greater than 1, when viewing `Pass Rewards`, then quantity text appears in a dedicated badge area inside the card and does not overlap or render underneath the item icon sprite.
- [x] Given the Battle Pass screen is open on `Pass Rewards`, when viewing the header status line, then `Current Week Completions ...` and `Unclaimed Levels ...` are readable without overlapping each other at default GUI scale.
- [x] Given a reward level is still locked, when viewing its card in `Pass Rewards`, then the card appears visibly dimmed with no stray `L` glyph or extra text rendered over the item area.
- [?] Given a player completes a task via `/incore battlepass complete_task <player> <task_id>` while the Battle Pass screen is open, when waiting up to one second, then the screen updates task state/progress and reflects new XP and level without relogging.
- [ ] Given level rewards exist in the active battle pass datapack, when viewing the Battle Pass screen rewards list, then each configured reward level appears with required XP and reward preview text.
- [ ] Given a lane has levels already claimed and later levels still unclaimed, when viewing rewards in that lane, then claimed levels use the claimed border color while claimable-unclaimed levels use a distinct unclaimed border color.
- [ ] Given rewards include a maximum configured level (for example `Lv 50`), when viewing that level card in rewards tab, then it is marked with a dedicated highest-level border color distinct from normal cards.
- [ ] Given a lane is locked, when viewing rewards cards and the lane title row, then cards are darkened with a red tint and the lane title is prefixed with `🔒` without per-slot locked text.
- [ ] Given `battlepass_set.incore.season_alpha` and `battlepass_set.incore.season_bravo` locale keys exist, when opening Battle Pass header for those sets, then the set label shows localized names (for example `Season Alpha`) instead of raw ids.
- [ ] Given a reward level is selected while the `Claim All Rewards` button is visible, when viewing the footer in rewards tab, then selected-level text (`Selected level`, `XP to reach`, `XP for level`) does not overlap the claim button.
- [?] Given server time crosses a battle pass boundary (for example from before `2026-02-15T00:00:00Z` to after), when keeping the Battle Pass screen open, then set id/week/tasks/rewards update to the next active battle pass set without server restart.

## Battle Pass Week Alignment and Lane Unlocks
- [ ] Given battlepass datapacks with `length_weeks` are loaded, when starting server and joining a world, then player spawn succeeds without `UnsupportedTemporalTypeException: Unsupported unit: Weeks` and battle pass UI opens normally.
- [ ] Given server local time is Monday at 11:59, when running `/incore battlepass set incore:season_alpha` and then `/incore battlepass status <player>`, then the set switches successfully and current week is still reported from the previous Monday 12:00 weekly boundary.
- [ ] Given server local time is Monday at 12:00 or later, when running `/incore battlepass set incore:season_bravo`, then the active pass starts from that ongoing Monday 12:00 week start and week progression matches the Monday-noon week boundary.
- [ ] Given op permissions, when typing `/incore battlepass set ` and autocomplete is requested, then set ids are suggested as namespaced resource ids and executing `/incore battlepass set incore:season_alpha` no longer throws invalid argument.
- [ ] Given op permissions, when running `/incore battlepass level set <player> 3` and `/incore battlepass level add <player> -1`, then level and XP change exactly as the old `tier` command behavior.
- [ ] Given op permissions, when running `/incore battlepass tier set <player> 3` and `/incore battlepass tier add <player> 1`, then legacy `tier` command still works as an alias of `level`.
- [ ] Given a player has `originium_supply_unlock` in hand and an active battle pass, when right-clicking the item, then the Originium lane unlocks, one item is consumed (unless creative), and `/incore battlepass lane list <player>` shows `originium=unlocked`.
- [ ] Given a player has `protocol_customized_unlock` in hand and an active battle pass, when right-clicking the item, then the Protocol lane unlocks, one item is consumed (unless creative), and `/incore battlepass lane list <player>` shows `protocol=unlocked`.
- [ ] Given op permissions and a target player, when running `/incore battlepass lane unlock <player> originium` and `/incore battlepass lane lock <player> originium`, then each command succeeds with clear feedback and the lane state flips accordingly.
- [ ] Given Battle Pass rewards tab is open and a reward level card is clicked, when selecting any level, then the footer shows `XP to reach` and `XP for level` values for the selected level.
- [ ] Given a lane is locked for the player, when viewing the rewards tab, then that lane row appears as locked (greyed/locked label) and unlocked lanes remain claimable.
- [ ] Given `season_alpha` and `season_bravo` datapacks define rewards only under `rewards_by_lane` (with no `level_rewards` key), when the server reloads datapacks and players open Battle Pass rewards, then all configured reward levels and lane previews still load correctly without parse errors.

## Research Lab Automatic Processing
- [ ] Given a player places a Research Lab and opens it, when inserting 2 iron ingots and waiting 10 seconds, then the lab progress bar fills to completion and the owner gains 5 research points.
- [ ] Given a non-owner opens a placed Research Lab with valid input, when processing completes, then research points are granted to the player who originally placed the lab.
- [ ] Given a Research Lab has an input stack below any configured process threshold, when waiting 10 seconds, then progress remains at zero and no research points are granted.

## Research Tree and Manual Task Submissions
- [ ] Given the player is in-game, when pressing the Research Tree keybind (`K` default), then the Research & Tech Tree screen opens populated from datapack-defined entries and tasks.
- [ ] Given player inventory contains 8 paper, when clicking the "Field Notes Submission" task button, then 8 paper are consumed and research points increase by 10.
- [ ] Given player inventory contains 16 slime balls, when clicking "Specimen Delivery" task multiple times, then each submission consumes 16 slime balls and grants repeatable research points.
- [ ] Given the player has enough research points and completed prerequisites, when clicking an unlockable tech entry button, then points are spent and the entry is marked unlocked.

## Research Lab Build Compatibility
- [ ] Given a clean checkout on CI or local dev machine with required network access, when running `./gradlew compileJava`, then `LabBlock` codec registration compiles without constructor-reference errors.
