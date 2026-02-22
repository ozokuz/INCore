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

## Gacha 6★ Secondary Pity And Rotation Commands
- [ ] Given op permissions and a target player with `incore:expedition_relic` selected, when running `/incore gacha pity set <player> incore:expedition_relic 0 0 119 0` and then opening one crate on `incore:expedition_relic`, then that pull includes the banner main showcased 6★ (`minecraft:mace`) and featured 6★ pity resets.
- [ ] Given op permissions and a target player on `incore:expedition_relic`, when running `/incore gacha pity set <player> incore:expedition_relic 0 0 20 0` and then `/incore gacha rotate incore:expedition next`, then `/incore gacha pity status <player> incore:expedition_relic` shows featured 6★ pity reset for the new rotation token.
- [ ] Given op permissions and a target player on `incore:basic`, when running `/incore gacha pity set <player> incore:basic 0 0 0 239` and opening one basic crate, then basic selectable 6★ pity reaches at least `240/240` and further basic pulls are blocked.
- [ ] Given a player has reached `240/240` on `incore:basic`, when attempting to buy/pull another basic crate, then the action fails with the guaranteed-6★ required message and no new crate is consumed/opened.
- [ ] Given a player has reached `240/240` on `incore:basic`, when opening the gacha screen, then `Pull x10` remains visible but disabled and `Select Guaranteed 6★` appears to the left of `Info`.
- [ ] Given a player at `240/240` on `incore:basic`, when clicking `Select Guaranteed 6★`, then a dedicated selection screen opens and each pullable 6★ for that banner is shown as a clickable column/card.
- [ ] Given the dedicated 6★ selection screen is open with no column selected, when viewing action buttons, then `Confirm Selection` is disabled until one column/card is clicked.
- [ ] Given a player is at `240/240` on `incore:basic` and selects a valid 6★ column, when clicking `Confirm Selection`, then the selected 6★ item is granted immediately and basic selectable 6★ pity resets to `0`.
- [ ] Given a player is below `240` on a basic banner, when sending an invalid or early guaranteed-claim payload (invalid item id or not ready), then the server rejects it and no free reward is granted.
- [ ] Given op permissions and two target players, when running `/incore gacha pity set <players> incore:basic_tools 5 7 0 120`, then `/incore gacha pity status <player> incore:basic_tools` reports `5*=5`, `6*=7`, and `basic240=120` for each updated player.
- [ ] Given event category `incore:chartered` is loaded, when running `/incore gacha rotate incore:chartered next`, then the active chartered banner changes immediately and the command reports the new active banner id.
- [ ] Given op permissions and an active category already force-rotated once, when waiting until the current category window expires without additional commands, then visible event banners resume normal time-based rotation order automatically.

## Overworld Surface Ore Patches
- [ ] Given the server has unexplored overworld terrain, when traveling into newly generated chunks, then chunk selection for surface ore patches is deterministic and averages to 1 selected chunk in 50 by world seed/chunk hash.
- [ ] Given any generated surface ore patch, when counting all spots in that one patch cluster, then the patch contains 2 to 6 spots.
- [ ] Given any generated patch, when mining each spot once and observing the remaining/max counter message, then each spot in that patch starts with the same max mine count and that max remains between 400 and 1200.
- [ ] Given generated patches near world spawn and generated patches far from world spawn, when comparing their max mine counts across multiple samples, then farther patches trend richer with higher max mine counts.
- [ ] Given newly generated patches in multiple chunks, when identifying ore spot block types, then each patch uses exactly one ore type chosen from: Crimsite, Veridium, Asurine, Ochrum, Cinnabar, Mixed Metals, Gem Clusters.
- [ ] Given a surface ore spot with more than one mine remaining, when breaking it once normally, then the block remains in place and drops one item of that ore type's original ore stone block.
- [ ] Given a surface ore spot with one mine remaining, when breaking it once normally, then the block is removed and drops one item of that ore type's original ore stone block.
- [ ] Given a player in creative mode, when sneaking and breaking a surface ore spot, then the spot is destroyed immediately and the position becomes air.
- [ ] Given a player in survival mode, when sneaking and breaking a surface ore spot once, then the break is canceled and a warning is shown; when sneaking and breaking that same spot again within 4 seconds, then the spot is destroyed.
- [ ] Given any generated patch, when checking distance between each pair of spots in that patch on the XZ plane, then all spots are spaced by at least 3 blocks.
- [ ] Given a generated patch, when inspecting the patch footprint around spots, then the surface area forms an oval/spherical-looking footprint (not a rectangle) and is fully covered with no intentional empty gaps.
- [ ] Given a generated patch footprint, when inspecting covered cells, then light smoothing includes some regular stone slabs and ore-stone slabs mixed among full blocks.
- [ ] Given trees/rocks/structure-like solid blocks in candidate terrain, when generating new patches nearby, then patches do not overwrite those solid feature blocks.
- [ ] Given plants (grass/flowers/etc.) in candidate patch terrain, when a patch generates there, then those plants may be cleared by patch coverage while solid feature blocks remain untouched.
- [ ] Given short/tall grass, ferns, dead bushes, or flowers above patch cells, when a patch generates, then those soft vegetation blocks are cleared up to 2 blocks above the patch surface and above spot blocks.
- [ ] Given a generated patch on uneven terrain (small ledges/slopes), when inspecting exposed patch edges, then patch material extends two blocks below the surface layer so underlying original ground is not visibly bleeding through.
- [ ] Given a generated patch, when checking every ore spot, then each spot remains exposed on top and no generated coverage block is placed above any ore spot.
- [ ] Given a dev environment and a player with the Surface Ore Debug Compass, when right-clicking the compass in the overworld, then it targets the nearest unfound saved ore patch location and points there.
- [ ] Given a Surface Ore Debug Compass already locked to a patch, when holding the compass and rotating around in the same dimension, then the compass needle points toward the locked patch instead of staying static north.
- [ ] Given a Surface Ore Debug Compass lock is set to a patch that has no lodestone block at the target, when waiting at least 10 seconds and moving around, then the compass stays locked to that patch target and keeps pointing correctly.
- [ ] Given repeated right-clicks with the Surface Ore Debug Compass in a dev environment, when multiple saved patches exist, then each lock consumes one unfound patch from that player's persistent found list and progresses to the next nearest unfound patch.
- [ ] Given a player has already marked patches with the Surface Ore Debug Compass, when leaving and rejoining, then right-clicking the compass continues from remaining unfound patches (found patch chunk ids persist in player data).
- [ ] Given a generated patch with soft vegetation above covered cells, when generation completes, then grass/ferns/dead bushes/flowers up to 2 blocks above patch cells are removed while trees/leaves stay intact.
- [ ] Given any surface ore spot variant, when observing its model, then it uses a single baked texture (no overlay geometry), matching the ore stone base with a visible center marking on faces.
- [ ] Given a partially mined surface ore spot, when leaving the area (or restarting server) and returning, then its remaining mine count persists and continues from the previous value.
- [ ] Given fake-player or machine-style block breaking that uses normal break flow, when it mines a surface ore spot, then the spot mine count decreases by one and the spot behavior matches player mining.

## Overworld Surface Stone Patches
- [ ] Given the server has unexplored overworld terrain, when traveling through newly generated chunks, then regular surface stone patches generate deterministically at an average of 1 selected chunk in 75 by world seed/chunk hash.
- [ ] Given any generated regular surface stone patch, when counting spots in that single patch, then it contains 2 to 6 spots.
- [ ] Given multiple generated regular surface stone patches, when checking spot block types across samples, then each patch uses exactly one type from: Stone, Deepslate, Limestone, Basalt, Scoria.
- [ ] Given a regular surface stone spot, when mining it repeatedly without sneaking, then the spot never depletes and each break attempt drops stone-type loot while the block remains in place.
- [ ] Given a deepslate surface stone spot and a non-silk-touch tool, when mining once normally, then the drop is cobbled deepslate.
- [ ] Given a deepslate surface stone spot and a silk-touch tool, when mining once normally, then the drop is deepslate.
- [ ] Given any generated regular surface stone patch, when checking spot spacing on the XZ plane, then all spots are spaced by at least 3 blocks.
- [ ] Given a generated regular surface stone patch, when inspecting the footprint, then it forms an oval/spherical-looking covered area with no intentional empty gaps and includes some slab smoothing.
- [ ] Given a generated regular surface stone patch on uneven terrain, when inspecting edges and top exposure, then patch fill extends two blocks down and no generated block is placed above spot blocks.
- [ ] Given any regular surface stone spot variant, when observing its model, then it uses a baked base-stone texture with the same center mark style used by ore spots (no separate overlay geometry).
- [ ] Given a player in survival mode, when sneaking and breaking a regular surface stone spot once, then the break is canceled with a warning; when sneaking and breaking the same spot again within 4 seconds, then the spot is destroyed.
- [ ] Given a dev environment and a player with the Surface Stone Debug Compass, when right-clicking the compass in the overworld, then it locks to the nearest unfound regular surface stone patch and the needle points there.
- [ ] Given repeated right-clicks with Surface Stone Debug Compass and multiple regular stone patches saved, when using the compass repeatedly, then each use advances to the next nearest unfound stone patch and persists found chunk progress across relogs independently from the ore debug compass.

## Daily & Weekly Datapack Tasks
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

## Battle Pass Task Completion and Rotation
- [ ] Given a task already has partial progress (for example 1/3) and op permissions, when running `/incore battlepass complete_task <player> <task_id>`, then the task always completes (no integer overflow reset), the command reports success with XP gain, and task progress stays at goal.
- [ ] Given op permissions and loaded sets include `incore:season_alpha` and `incore:season_bravo`, when running `/incore battlepass set incore:season_alpha`, then `/incore battlepass status <player>` reports `incore:season_alpha` as the active set regardless of current date.

## Battle Pass Screen and Sync
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
- [ ] Given a player opens a `Modular Lab`, when viewing the Materials panel, then FE text, module-card row, and 3x3 material grid are all visible without overlapping each other or crossing panel borders.
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

## Research Team Sharing (FTB Teams)
- [ ] Given two players are in the same FTB Team and player A queues `incore:lab_basics`, when player B opens the Research screen, then player B sees the same queue entry and active research progress values.
- [ ] Given two players are in the same FTB Team and player A unlocks `incore:lab_basics`, when player B opens the Research screen, then `Lab Basics` is already unlocked for player B without separate progression.
- [ ] Given a player with existing solo research joins an FTB Team that has different research state, when the player reopens the Research screen, then the displayed research state matches the team scope instead of the previous solo scope.
- [ ] Given a player is not in any FTB Team, when progressing research with a lab, then research progress and unlocks remain personal and are not shared to other non-team players.

## Research Recipe Locks And EMI
- [ ] Given `incore:lab_basics_unlocks` lock-set recipes are not yet unlocked, when attempting to craft `minecraft:hopper` or `minecraft:observer`, then crafted outputs are blocked by research lock enforcement and a lock warning message appears.
- [ ] Given `incore:lab_basics` is unlocked, when crafting `minecraft:hopper` and `minecraft:observer` again, then outputs are crafted normally.
- [ ] Given a locked crafting recipe is arranged in a crafting table (or 2x2 player crafting), when attempting to take the result, then the result slot is prevented from crafting and input ingredients are not consumed or voided.
- [ ] Given a locked crafting recipe is arranged in a modded crafting-table style UI (for example backpack crafting or a portable crafting table), when attempting to take the output, then the result is blocked before crafting and the input grid is not consumed.
- [ ] Given a locked recipe is configured in a placed Create blueprint, when right-clicking the blueprint to craft, then crafting is blocked, no ingredients are consumed from inventory, and the lock warning message appears.
- [ ] Given a locked recipe is configured in a powered Create mechanical crafter chain while a player is nearby, when the chain attempts to craft, then no output item is produced and the lock warning message appears.
- [ ] Given a Create mechanical crafter is placed by player A and player A later leaves the area, when the chunk unloads/reloads and no player is near during the next craft attempt, then the crafter still uses player A ownership scope and locked outputs remain blocked.
- [ ] Given a Create mechanical crafter is placed by a member of an FTB team, when no team members are nearby and the recipe is still locked for that team scope, then automated crafting still fails to output the locked recipe.
- [ ] Given player A has unlocked a recipe but player B has not, when player B is the nearest player to the mechanical crafter chain during a craft attempt, then crafting is blocked until player B unlocks the required research.
- [ ] Given a locked recipe is viewed in EMI before unlocking its research, when opening the recipe page, then EMI shows a red `🔒` lock icon centered over the recipe arrow (not beside it) instead of the previous text marker.
- [ ] Given a locked recipe is viewed in EMI, when comparing the lock icon to normal tooltip text glyph thickness, then the `🔒` marker appears visibly bolder/larger for readability.
- [ ] Given the player hovers the red `🔒` icon on a locked EMI recipe, when the tooltip appears, then it shows formatted lines: `Research Locked`, `This recipe is locked behind research.`, and `Required Research: <research name>`.
- [ ] Given a recipe is accidentally mapped to multiple research lock-sets, when a player opens that locked recipe in EMI after joining/reloading the world, then one chat warning appears for that world session and logs contain a conflict error while EMI still shows the first matched research name.
- [ ] Given a recipe has been unlocked by research progression, when reopening EMI recipe view, then the lock marker/tooltip is no longer shown for that recipe.

## Research Materials (Datapack + KubeJS)
- [ ] Given datapack research materials exist under `data/incore/research_materials/`, when reloading datapacks, then server logs show research materials loading and research entries using `material` ids still resolve to correct item costs.
- [ ] Given `Lab Basics` uses `incore:paper` and `incore:glass_bottle` material ids, when inserting required items and completing one run, then the expected item counts are consumed and one run of research progress is granted.
- [ ] Given startup KubeJS script registers a new research material via `INCoreEvents.researchMaterials`, when reloading the world, then research entries referencing that material id resolve without datapack parse errors.
- [ ] Given active research has multiple colored materials, when the lab is actively processing, then colored lab-top particles cycle through those material colors over time.

## Lab Tiers (Burner / Mechanical / Modular)
- [ ] Given a placed `incore:burner_lab`, when opening it, then the screen title shows `Burner Lab` and the block requires burnable fuel in the fuel slot to process.
- [ ] Given a placed `incore:mechanical_lab`, when opening it, then the screen title shows `Mechanical Lab` instead of a generic shared lab name.
- [ ] Given a placed `incore:modular_lab`, when opening it, then the screen title shows `Modular Lab` instead of a generic shared lab name.
- [ ] Given any lab tier UI is open after the second layout pass, when viewing the right panel header area, then helper lines such as `Burner slot active` / `Kinetic input required` are not shown and only core power + materials labels remain.
- [ ] Given a `Modular Lab` UI is open, when viewing the right panel, then `FE`, `Modules`, and `Materials` labels are vertically separated from module slots and material slots with no text-slot overlap.
- [ ] Given any lab UI is open, when viewing the bottom panel, then the `Inventory` label is positioned in the left side of the panel and does not overlap player inventory slot borders.
- [ ] Given any lab UI is open with active progress, when viewing the left panel progress section, then `Progress` and `Overall` text lines render above their bars and do not overlap bar fills.
- [ ] Given player looks at a Burner Lab or Mechanical Lab with Jade overlay enabled, when power-related info is shown, then no Forge Energy storage/capacity line appears for those two tiers.
- [ ] Given a Burner Lab has required materials but no fuel, when waiting 10 seconds, then progress stays at zero and status does not enter continuous working state.
- [ ] Given a Burner Lab has required materials and valid fuel, when waiting one run cycle, then progress advances and consumes both fuel time and research materials.
- [ ] Given a Mechanical Lab has required materials and no adjacent Create kinetic source, when waiting 10 seconds, then processing does not advance and displayed RPM remains zero.
- [ ] Given a Mechanical Lab has required materials and adjacent Create kinetic source, when increasing provided RPM, then processing speed increases and displayed SU demand value increases with RPM.
- [ ] Given a Modular Lab has required materials and five module card slots, when inserting speed/productivity cards and providing FE, then processing speed and bonus progress behavior increase up to configured caps.
- [ ] Given a Modular Lab has required materials but zero FE buffer, when waiting 10 seconds, then progress does not advance until FE is supplied.
- [ ] Given the player hovers `Speed Module Card` in inventory, when reading tooltip text, then it shows `Speed bonus` per card and `Max speed bonus` values that match current config percentages.
- [ ] Given the player hovers `Productivity Module Card` in inventory, when reading tooltip text, then it shows `Productivity chance` per card and `Max productivity chance` values that match current config percentages.
- [ ] Given a `Modular Lab` UI is open with module cards inserted, when viewing the `Modules` section, then `Speed` and `Productivity` modifier lines are visible and update to reflect installed cards (capped by config max values).

## Jade Lab Overlay
- [ ] Given Jade and INCore are both loaded, when joining a world and pressing `F3 + T` to reload resources, then reload completes without `Missing config translation: config.jade.plugin_incore.lab_status*` errors and selected resource packs remain enabled.
- [ ] Given Jade is enabled and player looks at any lab tier block, when tooltip appears, then it includes tier, status, run progress, and overall progress lines.
- [ ] Given a lab is owned by an FTB team member, when Jade tooltip is shown, then it displays `Owner: <team name>` instead of raw scope ids like `team:<uuid>` or `player:<uuid>`.
- [ ] Given player looks at a Burner Lab with active fuel burn, when Jade tooltip is shown, then fuel remaining/total line is visible.
- [ ] Given player looks at a Mechanical Lab while powered by Create rotation, when Jade tooltip is shown, then exactly one RPM/SU line is visible (not duplicated) and no burner fuel or modular FE lines are present.
- [ ] Given player looks at a Modular Lab with FE stored, when Jade tooltip is shown, then exactly one FE stored/capacity line is visible (not duplicated) and no burner fuel or mechanical RPM/SU lines are present.
- [ ] Given player looks at Mechanical or Modular labs while Jade overlay is active, when tooltip is shown, then generic lines (`Tier`, `Owner`, `Status`, `Run`, `Overall`) appear once each (no duplicated sections).

## Dungeon Return Portal Placeholder
- [ ] Given a dungeon structure template contains `incore:dungeon_return_portal`, when a dungeon is generated in the roguelike dimension, then each placeholder block is replaced with an active `incore:roguelike_portal` block.
- [ ] Given the replaced portal block inside the dungeon belongs to the player's active run, when the player right-clicks or walks into it, then the player is teleported back to the original world portal entry position and the run is ended for that player.
- [ ] Given a player without a matching active run in that dungeon uses a replaced portal block, when they interact with it, then they are not teleported and receive the unbound return portal message.
- [ ] Given `incore:dungeon_return_portal` is placed manually in a normal world (outside structure replacement), when a player interacts with it, then it behaves only as a normal placeholder block and does not teleport the player.

## Structure Size And Floor Alignment
- [ ] Given frost starting room, when generating 10 dungeons, then the start-room should get placed in correct position.
- [ ] Given frost starting room is generated with randomized rotation, when generating 10 dungeons, then every start-room return portal placeholder is replaced by an active roguelike portal block.
- [ ] Given frost starting room has one built exit in template space, when generating 10 dungeons, then exactly one hallway connects directly to the starting room in each dungeon (no extra start-room branches).
- [ ] Given frost starting room has one built exit in template space, when generating 10 dungeons, then the start-room exit world direction is not always south.
- [ ] Given hallway templates where one horizontal axis is shorter than the other (for example `16x11x10`), when generating dungeons, then hallways auto-rotate to use the longer axis for travel and touch both connected room openings without gaps.
- [ ] Given a generated dungeon layout, when tracing room connections from the starting room through hallways in each of 10 runs, then every generated room is reachable from the start.
- [ ] Given a dungeon slot is recycled for a new run, when the next dungeon is generated in that slot, then blocks from the previous dungeon are cleared across the full slot build volume before new rooms and hallways are placed.

## Numismatics Bank Keybind
- [ ] Given the player is in-world with Create Numismatics loaded and no other GUI open, when pressing the Numismatics Bank keybind (`N` default), then the Create Numismatics bank terminal screen opens for that player.
- [ ] Given the Numismatics Bank keybind is rebound in Controls to a different key, when pressing the rebound key in-world with no GUI open, then the Create Numismatics bank terminal screen opens and no action is bound to the old default key.

## Arena Combat Catalog
- [ ] Given at least one arena catalog datapack entry exists, when pressing the Combat Catalog keybind (`V` default), then the `Combat Catalog` screen opens and shows reward categories and difficulties.
- [ ] Given the Combat Catalog is open with at least one category selected, when selecting a different category button, then the difficulty list updates to entries from that category only.
- [ ] Given default arena datapack content is loaded, when opening Combat Catalog, then categories include `Gear Materials` and `Banner Permits`, each with difficulties `Level 1` through `Level 5`.
- [ ] Given a difficulty entry is selected in the Combat Catalog, when reading the right-side details, then gateway id and sanity cost match that datapack entry and reward previews render as item icons with count labels.
- [ ] Given a category+difficulty entry is selected, when pressing `Deploy to Arena`, then the player is teleported into the `incore:arena` dimension at their arena slot and receives the run prepared message.
- [ ] Given a player has already received an arena slot from a previous run, when deploying again from the catalog, then the player is teleported to the same slot origin (per-player fixed slot behavior).
- [ ] Given the player is inside a prepared arena, when attempting to move upward through the top boundary (for example with creative flight or vertical movement tools), then an invisible barrier ceiling blocks movement out of the arena.

## Arena Orb And Gateway Flow
- [ ] Given the arena is prepared and no fight is active, when checking the center of the arena, then the `Arena Orb` is present and floating one block higher than its previous center position.
- [ ] Given the player is in their prepared arena run, when right-clicking the center `Arena Orb`, then the configured Gateways gateway starts and the player receives the gateway started message.
- [ ] Given the gateway fight is active, when checking the arena center, then the `Arena Orb` is not present until the run ends.
- [ ] Given a gateway configured by arena catalog is missing from datapacks, when trying to start from the orb, then the orb interaction fails with a missing gateway message and no fight starts.
- [ ] Given an active arena gateway run is completed, when the gateway finishes successfully, then the run state ends as success and the player receives one `Arena Reward Crate`.
- [ ] Given an active arena gateway run fails, when the gateway emits a failure, then the run state ends as failed, no reward crate is granted, and the orb displays return-ready behavior.
- [ ] Given an arena wave entity dies during an arena run, when checking drops at death position, then default mob drops are not present.

## Arena Reward Crate Sanity Spend
- [ ] Given the player receives an `Arena Reward Crate` block item, when hovering it in inventory, then tooltip shows source category+difficulty, sanity cost, and deterministic reward entries with counts.
- [ ] Given the player receives an `Arena Reward Crate` block item, when reading its display name, then the name format is `<Category Name> <Difficulty Name> Sanity Reward Crate` (for example `Gear Materials Level 5 Sanity Reward Crate`) and the label is rendered with normal non-cursive item-name styling.
- [ ] Given the player places an `Arena Reward Crate` block and right-clicks without sneaking, then the crate does not open.
- [ ] Given the player sneak-right-clicks an `Arena Reward Crate` block while either hand is not empty, then the crate does not open and shows the empty-hands required message.
- [ ] Given the player sneak-right-clicks an `Arena Reward Crate` block with empty hands and sanity below configured cost, then the crate does not open, sanity is not consumed, and not-enough-sanity message appears.
- [ ] Given the player sneak-right-clicks an `Arena Reward Crate` block with empty hands and sanity at or above configured cost, then the crate block is consumed, sanity is reduced by exactly configured amount, and deterministic catalog rewards are granted.

## Arena Return Safety
- [ ] Given an arena run has ended (success or fail), when interacting with the arena orb again, then the player is teleported back to the originally stored return dimension and position.
- [ ] Given the player dies during an arena run, when the player respawns, then they are not stranded in the arena and their run state is cleared.
- [ ] Given the player logs out while in the arena with an active run, when logging back in, then pending-return handling restores them from the arena context and prevents being stranded.

## Arena Datapack Extensibility
- [ ] Given a datapack adds a new `data/<namespace>/arena/catalog/*.json` entry referencing a valid `data/<namespace>/gateways/*.json` gateway, when datapacks are reloaded and the catalog is opened, then the new selectable encounter appears without code changes.

## Card System
- [ ] Given op permissions, when running `/incore cards give_booster <player> incore:base_protocol` and right-clicking the `Card Booster`, then one pack opens, a tile-based results screen with flip reveal appears, `Skip` reveals all remaining tiles, and exactly five `Card Module` items are added or dropped.
- [ ] Given valid data-backed stacks from `give_module`, `give_booster`, `give_booster_box`, `give_core`, and `give_deck_box`, when viewing item names in inventory, then displayed names match loaded module/booster/booster-box/core/deck-box data names instead of generic item names.
- [ ] Given op permissions, when running both `/incore cards give_booster <player> incore:base_protocol` and `/incore cards give_set_booster <player> incore:base_protocol`, then both granted boosters open the same set pool (no per-pack distinction within the set).
- [ ] Given op permissions, when typing `/incore cards give_module`, `/incore cards give_core`, `/incore cards give_deck_box`, `/incore cards give_booster`, `/incore cards give_set_booster`, and `/incore cards give_booster_box` arguments, then autocomplete suggestions include loaded module ids, core ids, deck box ids, card set ids, and booster box ids.
- [ ] Given op permissions, when running `give_module`, `give_core`, `give_deck_box`, `give_sleeve`, `give_booster`, `give_set_booster`, `give_booster_box`, and `give_tokens` without a `count` argument, then each command succeeds and grants one item by default.
- [ ] Given op permissions, when running `/incore cards give_booster_box <player> incore:starter_case` and right-clicking the `Card Booster Box`, then the box is consumed and exactly five `Card Booster` items are granted.
- [ ] Given a cryptic card in main hand from `/incore cards give_module <player> incore:base_cryptic_shell 1`, when right-clicking a placed `Decryptor`, then the held card becomes revealed and decrypt success message is shown.
- [ ] Given multiple undecrypted cryptic cards in inventory, when shift-right-clicking a placed `Decryptor`, then all undecrypted cryptic cards are revealed and chat reports decrypted count.
- [ ] Given Deck Station output deck includes undecrypted cryptic modules, when hovering that output deck before taking it, then tooltip/Curios modifier lines hide undecrypted cryptic attribute effects and only show `Undecrypted Cryptics: <count>` for undecrypted cryptics.
- [ ] Given that same deck is taken from Deck Station output into inventory, when hovering it afterward, then Curios-provided deck modifier lines include full cryptic attribute effects and the preview-only undecrypted count line is not shown.
- [ ] Given an ungraded card module in offhand and `Card Sleeve` in main hand from `/incore cards give_sleeve <player>`, when right-clicking with the sleeve, then sleeve count decreases by one and the card gains a grade between 1 and 10.
- [ ] Given a non-cryptic module card in inventory, when hovering it, then tooltip lines include plus/take modifier lines in Curios-style formatting without showing the `curios.modifiers.deck` header key text and whole-number values do not show decimal places.
- [ ] Given a foil module card in inventory, when hovering and visually inspecting the item, then the card has enchantment glint.
- [ ] Given a set with at least five unique modules and a booster for that set, when opening one booster, then each pull is unique within that pack.
- [ ] Given no deck equipped and inventory contains a core, box, and module cards from `/incore cards give_core`, `/incore cards give_deck_box`, and `/incore cards give_module`, when right-clicking a placed `Deck Station` while holding any item, then the slot-based Deck Station menu opens.
- [ ] Given Deck Station menu is open, when inspecting station slots, then module input slots form a 3x8 frame with top-center 4 reserved positions, core and box are fixed in the top-center reserved area, and output slot is separate.
- [ ] Given Deck Station menu is open, when inspecting labels and preview text, then title, `Core`, `Box`, and `Output` labels are visually separated (no merged/overlapping text) and `Points` text is muted instead of green when capacity is `0`.
- [ ] Given module item stacks in inventory, when placing into module slots, then each module slot only accepts one card and only `Card Module` items are accepted there.
- [ ] Given Deck Station has valid core/box/modules in station slots, when reading the right-side preview panel, then it always shows modules count, used points/capacity, max integrity, and modifier lines for the resulting deck.
- [ ] Given Deck Station setup is invalid (missing core, missing box, zero modules, or over capacity), when checking output slot, then output cannot be taken and right-side panel shows invalid status with the correct reason.
- [ ] Given Deck Station setup is valid and output deck is taken, when output transfer completes, then core/box/module input slots are cleared and the produced deck item is received by player inventory/cursor.
- [ ] Given Deck Station has input items placed and menu is closed without taking output, when reopening the same station, then previously inserted input items are still present.
- [ ] Given a newly assembled non-bricked `Deck` item built from module cards (including `/incore cards give_module` cards) with at least one active effect, when hovering that deck in inventory before equipping it, then tooltip uses Curios-provided modifier lines without a visible section header and without duplicate custom `Modifiers:` block.
- [ ] Given module data effects use simplified minecraft attribute ids (for example `minecraft:attack_damage` or `minecraft:movement_speed`), when assembling and hovering a deck, then Curios-provided modifier lines still render with resolved attribute names and values.
- [ ] Given a `Deck Core` and `Deck Box` in inventory, when hovering each item, then stat lines are shown in plus-value style (blue `+...` lines) matching modifier-style presentation.
- [ ] Given a deck containing at least one chaotic module, when triggering a dungeon transition and then checking deck modifiers again, then chaotic module contribution is re-rolled into concrete changed values/attributes (not only an internal multiplier), each rolled amount is capped to at most two digits after the decimal separator, and tooltip/effective modifiers reflect that re-roll.
- [ ] Given Curios deck slot is present and a valid `Deck` item exists, when equipping that deck into the `deck` Curio slot, then player combat/utility attributes change according to included module effects.
- [ ] Given an equipped deck that contains at least one corrupted module and positive integrity, when entering or leaving the roguelike dimension, then deck integrity decreases and corruption status message reports the new integrity value.
- [ ] Given an equipped deck with corrupted modules at low integrity, when crossing integrity thresholds (75%, 50%, 25%), then expected penalty behavior escalates and at 0 integrity the deck becomes bricked with no positive deck buffs.
- [ ] Given a vendor offer JSON is missing required `"category"` or has invalid category id, when datapacks reload, then that offer is skipped and never appears in vendor inventory.
- [ ] Given a placed fresh normal `Vendor`, when opened for the first time, then it rolls exactly one category/theme and only offers from that category are listed.
- [ ] Given two separately placed fresh normal `Vendor` blocks, when opening each once, then each vendor rolls and persists its own independent category/theme and stock.
- [ ] Given a placed fresh `Vendor` that rolled dark market mode, when opened, then offers can include mixed categories and all listed offers show discounted pricing.
- [ ] Given a placed fresh `Vendor` in dark market mode, when reopened after world reload, then dark-market flag, offer stock, and rolled discounts persist.
- [ ] Given a `Vendor` placed before the discount system update (initialized stock but no persisted offer discount entries), when opened after update, then missing per-offer discounts are backfilled and price display/purchase use those values.
- [ ] Given a normal vendor offer roll succeeds for discount chance, when viewing its row, then the UI shows a discount badge and original cost overlay while purchase cost uses the reduced amount.
- [ ] Given a normal vendor offer that did not roll a discount, when viewing its row, then no discount badge is shown and cost equals base amount.
- [ ] Given dark market mode is active, when viewing offer rows, then only dark-market discount is applied per offer (no extra single-offer chance discount stacking).
- [ ] Given player equips `Bargain Sigil` in Curios `charm` slot before opening a fresh normal vendor, when viewing the vendor, then curio bonus discounts can appear and/or grow while the curio remains equipped.
- [ ] Given vendor UI is open with `Bargain Sigil` equipped and showing curio bonus discount on an offer, when unequipping the curio and reopening the same vendor, then the curio-added discount portion is removed and only base persisted discount remains.
- [ ] Given an offer has no base vendor discount but receives a curio-only discount while `Bargain Sigil` is equipped, when viewing its badge, then the discount badge uses the curio-highlight color.
- [ ] Given an offer already has a base vendor discount and `Bargain Sigil` is equipped (increasing amount only), when viewing its badge, then the badge keeps the normal discount color (no curio-highlight color).
- [ ] Given a placed `Vendor` with a `card_token` currency offer and enough card tokens, when buying that offer from any page, then only card tokens are consumed and the configured card booster/booster-box reward is granted.
- [ ] Given a placed `Vendor` with a `card_token` currency offer that defines `"spur_conversion_rate": 8`, when card tokens are short but bank SPUR is sufficient, then the offer remains purchasable and cost strip shows horizontal token+spur icon/count lines.
- [ ] Given that conversion-capable token offer is short on tokens and has sufficient SPUR, when clicking `Buy`, then a conversion confirmation prompt opens in exchange layout (source/target amounts with arrow) with `Cancel` and `Convert & Buy` buttons.
- [ ] Given the conversion confirmation prompt is open for a token offer, when clicking `Convert & Buy`, then purchase succeeds and exactly `missing_tokens * 8` bank SPUR is deducted plus remaining required card tokens are consumed.
- [ ] Given the conversion confirmation prompt is open, when clicking `Cancel`, then no currency is consumed and no reward is granted.
- [ ] Given a token offer shortfall with insufficient bank SPUR for configured `"spur_conversion_rate"`, when opening vendor UI, then that offer's `Buy` button is disabled.
- [ ] Given a `card_booster` and `card_booster_box` product offer are visible, when viewing offer rows, then each row renders its corresponding card item icon and product count text.
- [ ] Given a generic `item` product offer (for example iron ingots) is visible, when buying it with its configured currency, then the exact configured item stack amount is granted.
- [ ] Given an `item` currency offer (for example emerald cost) with `"spur_conversion_rate"` configured, when opening vendor UI, then affordability and cost lines use that item icon/count and SPUR fallback amount.
- [ ] Given an `item` currency offer shortfall and sufficient bank SPUR for its configured `"spur_conversion_rate"`, when confirming conversion purchase, then available item currency is consumed first and only the missing item amount is charged from SPUR by rate.
- [ ] Given a `bank_spur` currency offer, when buying it, then the configured SPUR amount is deducted directly from Numismatics bank balance.
- [ ] Given a vendor offer currency JSON is missing `"spur_conversion_rate"` (or uses `0`), when datapacks reload, then that offer is skipped as invalid and does not appear in vendor inventory rolls.
- [ ] Given a placed `Vendor` is open, when viewing the top balance section, then it renders currency item icons with current counts in cost-strip style without text labels.
- [ ] Given a placed `Vendor` is opened in-world, when viewing the screen background behind the vendor panel, then the world scene remains unblurred while vendor UI text/buttons stay legible.
- [ ] Given vendor offers define `stock_min` and `stock_max`, when placing/opening a fresh `Vendor`, then each listed offer starts with stock inside its configured inclusive min/max range.
- [ ] Given a vendor offer has remaining stock greater than 1, when increasing buy quantity with `+` and purchasing once, then reward amount and currency cost are applied for selected quantity and stock decreases by exactly that quantity.
- [ ] Given a vendor offer has remaining stock `1`, when buying it once and reopening the vendor, then that offer shows `SOLD OUT` and its buy controls are disabled.
- [ ] Given a vendor offer has remaining stock `N`, when pressing `+` repeatedly in that row, then displayed quantity never exceeds `N`.
- [ ] Given a vendor has partially depleted stock, when leaving and rejoining the world/server and reopening that same vendor block, then previously remaining stock values persist.
- [ ] Given op permissions, when running `/incore cards debug_deck <player>` and `/incore cards collection <player>`, then command output reports current deck integrity/bricked/module counts and collection totals without errors.
