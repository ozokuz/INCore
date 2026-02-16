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
- [ ] Given a player has `Lab Basics` first in queue, when inserting that run's required material set in a Research Lab and waiting one full run duration, then the lab progress bar fills once and `Lab Basics` progress increases by 1 run.
- [ ] Given a player has `Lab Basics` first in queue and no matching material in lab slots, when waiting 10 seconds, then lab progress stays at zero and queued research progress does not change.
- [ ] Given `Applied Materials` is first in queue (requires slime balls), when inserting only paper in lab slots and waiting 10 seconds, then lab progress remains at zero until slime balls are inserted.
- [ ] Given a queued technology requires a multi-item material set for each run, when all required items for one run are inserted together and one run completes, then exactly one run worth of progress is added and all required set items are consumed.
- [ ] Given a queued technology reaches full progress from lab material processing, when the last required material cycle completes, then the technology unlocks automatically and the next queue entry becomes active.
- [ ] Given a player has the Research screen open while lab runs complete for the active queued technology, when each run completes in any owned lab, then the current research id and progress update on the screen within the next sync tick.

## Research Lab UI Layout
- [x] Given a player opens a placed Research Lab, when the menu appears, then a 3x3 material input grid is shown in the top panel and the full player inventory (27 slots + hotbar) is shown below it.
- [ ] Given a player opens the Research Lab screen after the wider layout update, when viewing overall panel bounds, then the screen width is larger than before and all three top columns have visible horizontal breathing room.
- [ ] Given a queued active technology exists but required materials are missing, when viewing the left `Status` column in the widened layout, then the full `Not enough materials` state text is visible without clipping.
- [ ] Given a player opens the Research Lab screen, when viewing the divider between top columns and bottom inventory panel, then the `Inventory` label is fully inside the lower panel and does not overlap any border line.
- [ ] Given a player has active queued research with non-zero max progress, when viewing the status column, then the `Progress: <current>/<max>` label is readable without truncating into `...` for typical run values.
- [x] Given no valid process item is in any lab input slot, when the menu is open, then the progress label reads `Progress: 0/0` and the progress bar remains empty.
- [x] Given valid process inputs are placed in any of the 3x3 lab slots, when waiting for processing, then the progress bar fills left-to-right and resets after reward payout.
- [ ] Given a player opens the Research Lab screen, when viewing the top section, then information is split into three columns with lab status on the left, current research in the middle, and material slots on the right.
- [ ] Given a player has an active queued technology (with or without currently matching lab materials), when opening the Research Lab screen, then the status area shows that queued active technology title under the status line.
- [ ] Given a queued active technology exists for the player, when viewing the middle `Current` column, then the icon shown is the current research icon (or its material fallback) rather than the lab block icon.
- [ ] Given a player opens a placed Research Lab with no queued active technology, when viewing the left status column, then status reads `No research selected` with a red indicator.
- [ ] Given a queued active technology exists but required materials are missing from lab slots, when viewing the left status column, then status reads `Not enough materials` with a warning-colored indicator.
- [ ] Given a queued active technology exists and required materials are present in lab slots, when viewing the left status column, then status reads `Working` with a green indicator.
- [ ] Given an active queued technology can run multiple times from available materials, when one run completes and the next run starts, then the Research Lab progress label keeps a non-zero max value and never shows `Progress: <n>/0` while status remains active.
- [ ] Given a lab can execute at least two consecutive runs without closing the UI, when the first run completes and the second run starts, then the on-screen progress value continues updating live during the second run without requiring the screen to be reopened.
- [ ] Given a lab run completes while the UI remains open and another run can continue, when the next run starts, then `Current` remains populated with the active technology and progress text does not show both values rising together as `x/x`.
- [ ] Given a lab run completion consumes one or more material slots while the UI is open, when the slot contents sync to client after payout, then `Status`, `Current`, and progress max values remain populated from the active queue entry instead of clearing until the screen is reopened.
- [ ] Given a queued technology is active and partially completed, when viewing the center `Current` column, then a second progress bar and `Overall: <progress>/<cost>` text are shown and reflect the technology's queue progress rather than per-run lab cycle progress.

## Research Tree and Manual Task Submissions
- [x] Given the player is in-game, when pressing the Research Tree keybind (`K` default), then the Research & Tech Tree screen opens populated from datapack-defined entries and tasks.
- [ ] Given player inventory contains 8 paper, when clicking the "Field Notes Submission" task button, then 8 paper are consumed and the task is marked completed (without granting research points).
- [ ] Given player inventory contains 16 slime balls, when clicking "Specimen Delivery" task multiple times, then each submission consumes 16 slime balls and the task remains valid as a prerequisite gate for required technologies.
- [ ] Given the player has completed all prerequisites and required manual tasks for a technology, when clicking `Start Research`, then that technology is added to the research queue instead of unlocking instantly.
- [ ] Given a queued technology is first in queue and matching research materials are processed in the lab, when one full lab run completes, then active progress increases by exactly 1 run.
- [ ] Given "Lab Basics" is queued first and enough required materials are processed to reach its cost, when progress reaches full, then "Lab Basics" unlocks automatically and the next queued technology becomes active.
- [ ] Given "Lab Basics" is unlocked and "Specimen Delivery" has been submitted at least once, when queuing "Applied Materials" and letting progress reach its full cost, then "Applied Materials" unlocks without additional manual clicks.
- [ ] Given a technology is missing prerequisite techs or required manual tasks, when selecting it in the tree, then `Start Research` is disabled and unmet requirements are shown in the details panel.
- [ ] Given `Lab Basics` is in the queue but not unlocked and required manual tasks are completed, when selecting `Applied Materials`, then `Start Research` is enabled and adds it behind `Lab Basics` in the queue.
- [ ] Given two queued technologies where the first has partial progress, when dragging that first technology behind the second and waiting for a refresh, then the moved technology keeps its previously accumulated progress value.

## Research Tree UI Layout
- [ ] Given the Research screen is open, when viewing the layout, then the left side shows queue/details/list panels and the right side shows a node-based technology tree with connector lines.
- [ ] Given the Research screen is open at default GUI scale, when comparing panel sizes to the previous compact layout, then both the left detail area and right tree viewport provide visibly larger usable space for text/icons.
- [ ] Given default datapack content is loaded, when opening the Research screen, then at least 16 technology entries appear across the tree/list, including disconnected branches such as `Wildlife Survey`, `Acoustic Mapping`, and `Artifact Restoration`.
- [ ] Given technologies are in different states (locked, available, queued, active, unlocked), when rendered on the tree, then node colors change by state and the selected node has a visible white outline.
- [ ] Given technologies in `locked`, `available`, `active`, and `unlocked` states, when rendered in both the tree and left technology list, then colors match: locked=`red`, available=`yellow`, active=`blue`, unlocked=`green`.
- [ ] Given the Research screen is open on the Tech Tree tab with at least one queued technology, when viewing the top-left section, then queue cards render below the tab controls with visible spacing and do not overlap the selected-technology panel.
- [ ] Given the Research screen is open on the Tech Tree tab with one or more queued technologies, when viewing the queue panel title area, then the `Research Queue` text remains fully visible above cards and is not covered by queue entries.
- [ ] Given the Research screen is open on the Tech Tree tab, when viewing the left column, then the technology search box is positioned directly below the `Research Queue` panel and above the selected-technology detail panel.
- [ ] Given the queue contains at least two entries plus separate `available`, `locked`, and `unlocked` entries, when viewing the left side, then queued entries appear only in the top `Research Queue` row while the lower technology list excludes queued entries and orders remaining entries as available, locked, unlocked.
- [ ] Given at least one queued technology is present, when viewing the `Research Queue` row, then queue cards use the same card dimensions/icon layout/selection marker style as cards in the lower technology list.
- [ ] Given at least two queued technologies are visible in the `Research Queue` row, when dragging one queue card onto another, then queue order updates immediately and remains in the new order after the next server sync.
- [ ] Given at least three queued technologies are visible in the `Research Queue` row, when left-dragging the first card horizontally across neighboring card centers, then the dragged card visually follows the cursor and the queue order snaps as each center is crossed.
- [ ] Given at least three queued technologies are visible in the `Research Queue` row, when dragging across card boundaries while keeping the cursor in the queue row, then reorder snapping continues without canceling or resetting the drag state.
- [ ] Given the queue has prerequisite order `Lab Basics -> Applied Materials`, when dragging `Applied Materials` ahead of `Lab Basics`, then the reorder is rejected and queue order stays unchanged.
- [ ] Given the queue has three technologies where the middle and last are independent of each other, when dragging the last onto the middle card, then the reorder is accepted and persists after the next server sync.
- [ ] Given queued technologies are visible in the `Research Queue` row, when hovering the small `x` button on a queue card, then that button background turns red and clicking it removes only that technology from the queue.
- [ ] Given a technology is selected from queue/list/tree, when viewing cards and nodes, then the selected technology renders with a single white outline and non-selected entries have no selection outline.
- [ ] Given multiple technologies have partial stored progress (greater than 0 and less than cost), when viewing queue/list/tree cards, then each partially researched technology shows a compact green progress bar under its card.
- [ ] Given a technology has a configured icon item in datapack, when viewing it in the right-side tree, then that item icon is rendered inside the node card.
- [ ] Given a player has one or more queued researches before opening the Research screen, when the screen opens, then selected technology defaults to the current/queued research instead of the first alphabetical list entry.
- [ ] Given the Research screen is on the Tech Tree tab, when typing in the search box, then technologies are filtered by technology title and by configured `unlocks` text values.
- [ ] Given the Research screen is open and the search box is focused, when waiting at least one server sync cycle (about 1 second), then the search box keeps its focused visual state and typing continues without needing to re-click.
- [ ] Given the Research screen is on the Tech Tree tab, when typing quickly into the search box, then typing remains responsive and filtering applies shortly after a brief pause instead of re-filtering every keystroke.
- [ ] Given the Research screen is on the Tech Tree tab with non-empty search text, when right-clicking inside the search box, then the search text clears immediately and full technology results are restored.
- [ ] Given the Tech Tree tab is open, when comparing node placement before and after recent update, then horizontal and vertical spacing between nodes is visibly increased with less overlap/crowding.
- [ ] Given the Tech Tree tab is open with technologies spanning first/last columns and rows, when viewing the tree viewport, then each node remains inset from the viewport border with visible padding on all sides.
- [ ] Given the Research screen is open on the Tech Tree tab, when left-dragging inside the right technology-tree panel, then the tree nodes/connectors pan with the cursor and remain selectable at their dragged positions.
- [ ] Given the tree is panned in any direction, when viewing the right panel edges, then nodes and connector lines are clipped to the panel bounds and never render outside the tree viewport.
- [ ] Given menu background blur is enabled in client video settings, when opening the Research screen, then the world behind the screen remains sharp (no blur) and blur settings are restored after closing the screen.
- [ ] Given the Research screen is open with a selected technology, when viewing the Tech Tree tab left panel, then the queue row shows large slot cells, the selected technology card shows icon/cost/effects rows with a `Start Research` control, and the bottom technology list renders as an icon grid rather than text rows.
- [ ] Given the Research screen is open, when switching between `Tech Tree` and `Manual Research` tabs, then tech queue/tree controls stay in the Tech Tree tab and manual task submission controls appear only in the Manual Research tab.
- [ ] Given the Research screen is open with a long selected technology description, when viewing the left details card, then title/cost/effects/progress/description text stays within the panel and does not overlap the `Start Research` button.
- [ ] Given a selected technology has required manual tasks, when viewing the left details card `Effects` row, then each task icon remains fully inside that row and does not bleed into the progress bar.
- [ ] Given a selected technology has several required materials and manual tasks, when viewing the left details card requirement row, then additional requirement icon/count content fits inside the row without clipping at the panel edge.
- [ ] Given the Research screen is on the `Manual Research` tab with at least two tasks, when viewing the left column, then the manual title text stays above the task buttons with visible spacing and no overlap.
- [ ] Given the `Manual Research` tab is open with one task selected, when viewing the right panel, then status/item/count/repeatable info appears inside a summary card, description text stays inside a dedicated lower card, and the `Submit tasks` button remains visible at the bottom.

## Research Admin Commands
- [ ] Given operator permission level 2 and a target player, when running `/incore research get <target>`, then chat output shows active progress, queue size, unlocked count, completed task count, and active queue entry id.
- [ ] Given operator permission level 2 and a target player with unmet prerequisites, when running `/incore research enqueue <target> incore:applied_materials`, then the command reports 0 successful queues and the entry is not added to the queue.
- [ ] Given operator permission level 2 and a target player, when running `/incore research force_unlock <target> incore:lab_basics`, then `/incore research get <target>` reflects updated unlocked counts and the forced tech is removed from queue if it was queued.
- [ ] Given operator permission level 2 and a target player with `incore:lab_basics` unlocked, when running `/incore research revoke <target> incore:lab_basics`, then `/incore research get <target>` shows one fewer unlocked research and the revoked entry is not active/queued.
- [ ] Given operator permission level 2 and a target player, when running `/incore research complete_task <target> incore:field_notes` and `/incore research clear_queue <target>`, then required-task gates treat `field_notes` as complete and queue size returns to 0.
- [ ] Given operator permission level 2 and a target player with any research progress, when running `/incore research reset_all <target>`, then `/incore research get <target>` reports queue=0, unlocked=0, completedTasks=0, and active=none.
