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

## Daily & Weekly Datapack Tasks
- [ ] Given at least three valid daily task datapack entries exist, when joining the world and pressing the Daily/Weekly task keybind (`K` by default), then a task overview screen opens and displays exactly three daily task options.
- [ ] Given one displayed daily task is an item collection task, when obtaining enough target items to meet its goal, then the daily status changes to complete and the configured `daily_completion` reward is granted once.
- [ ] Given one displayed daily task is a mob kill task, when defeating the target mob type up to the configured goal, then task progress updates and completing any one daily task completes the daily track.
- [ ] Given weekly tasks include mixed easy/medium/hard definitions in datapacks, when viewing the weekly list, then each weekly entry shows its difficulty and matching point value (easy=1, medium=2, hard=5).
- [ ] Given weekly tasks are completed to reach 2, 4, 6, 8, and 10 points, when each threshold is reached, then weekly tier pools `weekly_tier_1` through `weekly_tier_5` are each rewarded exactly once.
- [ ] Given a new UTC day begins, when rejoining or waiting for sync, then daily options/progress reset and a fresh set of up to three daily tasks is selected from datapack definitions.
- [ ] Given a new UTC week begins, when rejoining or waiting for sync, then weekly options/progress/points reset and weekly tiers become unclaimed for the new week.
