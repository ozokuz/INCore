package io.github.ozokuz.incore.features.tasks;

import com.google.gson.Gson;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public final class TaskService {
    private static final Gson GSON = new Gson();
    private static final int DAILY_TASK_COUNT = 3;
    private static final int DAILY_COMPLETION_REQUIRED_TASKS = 1;
    private static final int WEEKLY_EASY_COUNT = 4;
    private static final int WEEKLY_MEDIUM_COUNT = 4;
    private static final int WEEKLY_HARD_COUNT = 2;
    private static final int WEEKLY_TIER_COUNT = 5;
    private static final int WEEKLY_POINTS_PER_TIER = 2;
    private static final String KEY_DAY_INDEX = "incore:tasks_day";
    private static final String KEY_WEEK_INDEX = "incore:tasks_week";
    private static final String KEY_DAILY_IDS = "incore:tasks_daily_ids";
    private static final String KEY_WEEKLY_IDS = "incore:tasks_weekly_ids";
    private static final String KEY_DAILY_PROGRESS = "incore:tasks_daily_progress";
    private static final String KEY_WEEKLY_PROGRESS = "incore:tasks_weekly_progress";
    private static final String KEY_DAILY_ITEM_BASELINES = "incore:tasks_daily_item_baselines";
    private static final String KEY_WEEKLY_ITEM_BASELINES = "incore:tasks_weekly_item_baselines";
    private static final String KEY_DAILY_COMPLETED = "incore:tasks_daily_completed";
    private static final String KEY_DAILY_REWARD_CLAIMED = "incore:tasks_daily_reward_claimed";
    private static final String KEY_WEEKLY_POINTS = "incore:tasks_weekly_points";
    private static final String KEY_WEEKLY_TIER_CLAIMS = "incore:tasks_weekly_tier_claims";
    private static final List<String> PERSISTED_KEYS = List.of(
            KEY_DAY_INDEX,
            KEY_WEEK_INDEX,
            KEY_DAILY_IDS,
            KEY_WEEKLY_IDS,
            KEY_DAILY_PROGRESS,
            KEY_WEEKLY_PROGRESS,
            KEY_DAILY_ITEM_BASELINES,
            KEY_WEEKLY_ITEM_BASELINES,
            KEY_DAILY_COMPLETED,
            KEY_DAILY_REWARD_CLAIMED,
            KEY_WEEKLY_POINTS,
            KEY_WEEKLY_TIER_CLAIMS
    );

    private TaskService() {
    }

    public static void tick(ServerPlayer player) {
        ensurePeriods(player);
        DailyTaskService.tick(player);
        DailyTaskService.onLogin(player);
        refreshItemCollectionProgress(player);
        resolveCompletionsAndRewards(player);
    }

    public static void onMobKill(ServerPlayer player, LivingEntity victim) {
        ensurePeriods(player);
        incrementMobKillProgress(player, victim.getType());
        resolveCompletionsAndRewards(player);
    }

    public static void copyData(ServerPlayer from, ServerPlayer to) {
        CompoundTag fromData = from.getPersistentData();
        CompoundTag toData = to.getPersistentData();

        for (String key : PERSISTED_KEYS) {
            if (fromData.contains(key)) {
                toData.put(key, fromData.get(key).copy());
            } else {
                toData.remove(key);
            }
        }
    }

    public static String buildSyncJson(ServerPlayer player) {
        ensurePeriods(player);

        CompoundTag data = player.getPersistentData();
        List<TaskView> daily = buildViews(player, readIdList(data, KEY_DAILY_IDS), KEY_DAILY_PROGRESS);
        List<TaskView> weekly = buildViews(player, readIdList(data, KEY_WEEKLY_IDS), KEY_WEEKLY_PROGRESS);

        int weeklyPoints = data.getInt(KEY_WEEKLY_POINTS);
        List<RewardView> dailyRewards = buildRewardViews("daily_completion");
        List<TierView> tiers = new ArrayList<>();
        int claimMask = data.getInt(KEY_WEEKLY_TIER_CLAIMS);
        for (int tier = 1; tier <= WEEKLY_TIER_COUNT; tier++) {
            int required = tier * WEEKLY_POINTS_PER_TIER;
            boolean unlocked = weeklyPoints >= required;
            boolean claimed = (claimMask & (1 << (tier - 1))) != 0;
            tiers.add(new TierView(tier, required, unlocked, claimed, buildRewardViews("weekly_tier_" + tier)));
        }

        DailyTaskService.DailySyncView fixedDaily = DailyTaskService.buildSyncJson(player) != null
                ? new Gson().fromJson(DailyTaskService.buildSyncJson(player), DailyTaskService.DailySyncView.class)
                : null;
        List<DailyTaskService.DailyTaskView> fixedDailyTasks = fixedDaily != null ? fixedDaily.tasks() : List.of();
        int fixedDailyCompleted = fixedDaily != null ? fixedDaily.completedCount() : 0;
        boolean fixedDailyAllCompleted = fixedDaily != null && fixedDaily.allCompleted();
        boolean fixedDailyRewardClaimed = DailyTaskService.isRewardClaimed(player);

        return GSON.toJson(new TaskSyncView(
                daily,
                weekly,
                weeklyPoints,
                data.getBoolean(KEY_DAILY_COMPLETED),
                data.getBoolean(KEY_DAILY_REWARD_CLAIMED),
                dailyRewards,
                tiers,
                fixedDailyTasks,
                fixedDailyCompleted,
                fixedDailyAllCompleted,
                fixedDailyRewardClaimed
        ));
    }

    public static void forceResetDaily(ServerPlayer player) {
        resetDaily(player, currentDayIndex());
        refreshItemCollectionProgress(player);
        resolveCompletionsAndRewards(player);
    }

    public static void forceResetWeekly(ServerPlayer player) {
        resetWeekly(player, currentWeekIndex());
        refreshItemCollectionProgress(player);
        resolveCompletionsAndRewards(player);
    }

    public static int completeActiveDailyTasks(ServerPlayer player) {
        ensurePeriods(player);
        CompoundTag data = player.getPersistentData();
        CompoundTag dailyProgress = data.getCompound(KEY_DAILY_PROGRESS);
        int changed = 0;

        for (String id : readIdList(data, KEY_DAILY_IDS)) {
            TaskDefinition definition = findTask(id, TaskDefinition.Period.DAILY);
            if (definition == null) {
                continue;
            }

            int goal = Math.max(1, definition.goal());
            if (dailyProgress.getInt(id) < goal) {
                dailyProgress.putInt(id, goal);
                changed++;
            }
        }

        data.put(KEY_DAILY_PROGRESS, dailyProgress);
        resolveCompletionsAndRewards(player);
        return changed;
    }

    public static int completeActiveWeeklyTasks(ServerPlayer player) {
        ensurePeriods(player);
        CompoundTag data = player.getPersistentData();
        CompoundTag weeklyProgress = data.getCompound(KEY_WEEKLY_PROGRESS);
        int changed = 0;

        for (String id : readIdList(data, KEY_WEEKLY_IDS)) {
            TaskDefinition definition = findTask(id, TaskDefinition.Period.WEEKLY);
            if (definition == null) {
                continue;
            }

            int goal = Math.max(1, definition.goal());
            if (weeklyProgress.getInt(id) < goal) {
                weeklyProgress.putInt(id, goal);
                changed++;
            }
        }

        data.put(KEY_WEEKLY_PROGRESS, weeklyProgress);
        resolveCompletionsAndRewards(player);
        return changed;
    }

    public static boolean completeWeeklyTaskAtSlot(ServerPlayer player, int slot) {
        ensurePeriods(player);
        if (slot < 1) {
            return false;
        }

        CompoundTag data = player.getPersistentData();
        List<String> sortedWeeklyIds = new ArrayList<>(readIdList(data, KEY_WEEKLY_IDS));
        sortedWeeklyIds.sort(TaskService::compareWeeklyTaskOrder);
        if (slot > sortedWeeklyIds.size()) {
            return false;
        }

        String id = sortedWeeklyIds.get(slot - 1);
        TaskDefinition definition = findTask(id, TaskDefinition.Period.WEEKLY);
        if (definition == null) {
            return false;
        }

        CompoundTag weeklyProgress = data.getCompound(KEY_WEEKLY_PROGRESS);
        int goal = Math.max(1, definition.goal());
        if (weeklyProgress.getInt(id) >= goal) {
            return false;
        }

        weeklyProgress.putInt(id, goal);
        data.put(KEY_WEEKLY_PROGRESS, weeklyProgress);
        resolveCompletionsAndRewards(player);
        return true;
    }

    public static TaskAdminStatus adminStatus(ServerPlayer player) {
        ensurePeriods(player);
        refreshItemCollectionProgress(player);
        resolveCompletionsAndRewards(player);

        CompoundTag data = player.getPersistentData();
        int weeklyPoints = data.getInt(KEY_WEEKLY_POINTS);
        int claims = data.getInt(KEY_WEEKLY_TIER_CLAIMS);
        int claimableTiers = 0;
        for (int tier = 1; tier <= WEEKLY_TIER_COUNT; tier++) {
            int required = tier * WEEKLY_POINTS_PER_TIER;
            int bit = 1 << (tier - 1);
            if (weeklyPoints >= required && (claims & bit) == 0) {
                claimableTiers++;
            }
        }

        return new TaskAdminStatus(
                readIdList(data, KEY_DAILY_IDS).size(),
                data.getBoolean(KEY_DAILY_COMPLETED),
                data.getBoolean(KEY_DAILY_REWARD_CLAIMED),
                readIdList(data, KEY_WEEKLY_IDS).size(),
                weeklyPoints,
                Integer.bitCount(claims & 0x1F),
                claimableTiers
        );
    }

    private static void ensurePeriods(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();

        long dayIndex = currentDayIndex();
        if (data.getLong(KEY_DAY_INDEX) != dayIndex) {
            resetDaily(player, dayIndex);
        } else if (needsDailySelectionRefresh(player)) {
            resetDaily(player, dayIndex);
        }

        long weekIndex = currentWeekIndex();
        if (data.getLong(KEY_WEEK_INDEX) != weekIndex) {
            resetWeekly(player, weekIndex);
        } else if (needsWeeklySelectionRefresh(player)) {
            resetWeekly(player, weekIndex);
        }
    }

    private static void resetDaily(ServerPlayer player, long dayIndex) {
        CompoundTag data = player.getPersistentData();
        List<String> selectedIds = pickTasks(TaskDataManager.dailyTasks(), DAILY_TASK_COUNT, dayIndex, 0x11A3);
        data.putLong(KEY_DAY_INDEX, dayIndex);
        writeIdList(data, KEY_DAILY_IDS, selectedIds);
        data.put(KEY_DAILY_PROGRESS, new CompoundTag());
        data.put(KEY_DAILY_ITEM_BASELINES, seedItemCollectionBaselines(player, selectedIds, TaskDefinition.Period.DAILY));
        data.putBoolean(KEY_DAILY_COMPLETED, false);
        data.putBoolean(KEY_DAILY_REWARD_CLAIMED, false);
    }

    private static void resetWeekly(ServerPlayer player, long weekIndex) {
        CompoundTag data = player.getPersistentData();
        List<String> selectedIds = pickWeeklyTasks(TaskDataManager.weeklyTasks(), player.getUUID(), weekIndex);
        data.putLong(KEY_WEEK_INDEX, weekIndex);
        writeIdList(data, KEY_WEEKLY_IDS, selectedIds);
        data.put(KEY_WEEKLY_PROGRESS, new CompoundTag());
        data.put(KEY_WEEKLY_ITEM_BASELINES, seedItemCollectionBaselines(player, selectedIds, TaskDefinition.Period.WEEKLY));
        data.putInt(KEY_WEEKLY_POINTS, 0);
        data.putInt(KEY_WEEKLY_TIER_CLAIMS, 0);
    }

    private static List<String> pickWeeklyTasks(List<TaskDefinition> source, UUID uuid, long index) {
        List<String> ids = new ArrayList<>(WEEKLY_EASY_COUNT + WEEKLY_MEDIUM_COUNT + WEEKLY_HARD_COUNT);
        ids.addAll(pickWeeklyByDifficulty(source, TaskDefinition.WeeklyDifficulty.EASY, WEEKLY_EASY_COUNT, uuid, index, 0x45A5));
        ids.addAll(pickWeeklyByDifficulty(source, TaskDefinition.WeeklyDifficulty.MEDIUM, WEEKLY_MEDIUM_COUNT, uuid, index, 0x8D19));
        ids.addAll(pickWeeklyByDifficulty(source, TaskDefinition.WeeklyDifficulty.HARD, WEEKLY_HARD_COUNT, uuid, index, 0xC703));
        return ids;
    }

    private static List<String> pickWeeklyByDifficulty(
            List<TaskDefinition> source,
            TaskDefinition.WeeklyDifficulty difficulty,
            int requestedCount,
            UUID uuid,
            long index,
            long salt
    ) {
        List<TaskDefinition> matches = new ArrayList<>();
        for (TaskDefinition definition : source) {
            if (definition.period() == TaskDefinition.Period.WEEKLY && definition.difficulty() == difficulty) {
                matches.add(definition);
            }
        }
        if (matches.isEmpty()) {
            return List.of();
        }

        long seed = uuid.getMostSignificantBits() ^ uuid.getLeastSignificantBits() ^ (index * 31L) ^ salt;
        Collections.shuffle(matches, new Random(seed));

        int resultSize = Math.min(requestedCount, matches.size());
        List<String> ids = new ArrayList<>(resultSize);
        for (int i = 0; i < resultSize; i++) {
            ids.add(matches.get(i).id().toString());
        }
        return ids;
    }

    private static List<String> pickTasks(List<TaskDefinition> source, int count, long index, long salt) {
        if (source.isEmpty()) {
            return List.of();
        }

        List<TaskDefinition> shuffled = new ArrayList<>(source);
        long seed = (index * 31L) ^ salt;
        Collections.shuffle(shuffled, new Random(seed));

        int resultSize = Math.min(count, shuffled.size());
        List<String> ids = new ArrayList<>(resultSize);
        for (int i = 0; i < resultSize; i++) {
            ids.add(shuffled.get(i).id().toString());
        }
        return ids;
    }

    private static boolean needsDailySelectionRefresh(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        List<String> selectedIds = readIdList(data, KEY_DAILY_IDS);
        int expectedCount = Math.min(DAILY_TASK_COUNT, TaskDataManager.dailyTasks().size());
        if (selectedIds.size() != expectedCount) {
            return true;
        }

        HashSet<String> seen = new HashSet<>();
        for (String id : selectedIds) {
            if (!seen.add(id)) {
                return true;
            }
            if (findTask(id, TaskDefinition.Period.DAILY) == null) {
                return true;
            }
        }
        return false;
    }

    private static boolean needsWeeklySelectionRefresh(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        List<String> selectedIds = readIdList(data, KEY_WEEKLY_IDS);

        int expectedEasy = Math.min(WEEKLY_EASY_COUNT, countWeeklyTasksByDifficulty(TaskDefinition.WeeklyDifficulty.EASY));
        int expectedMedium = Math.min(WEEKLY_MEDIUM_COUNT, countWeeklyTasksByDifficulty(TaskDefinition.WeeklyDifficulty.MEDIUM));
        int expectedHard = Math.min(WEEKLY_HARD_COUNT, countWeeklyTasksByDifficulty(TaskDefinition.WeeklyDifficulty.HARD));
        int expectedTotal = expectedEasy + expectedMedium + expectedHard;
        if (selectedIds.size() != expectedTotal) {
            return true;
        }

        HashSet<String> seen = new HashSet<>();
        int easy = 0;
        int medium = 0;
        int hard = 0;
        for (String id : selectedIds) {
            if (!seen.add(id)) {
                return true;
            }

            TaskDefinition definition = findTask(id, TaskDefinition.Period.WEEKLY);
            if (definition == null) {
                return true;
            }

            if (definition.difficulty() == TaskDefinition.WeeklyDifficulty.EASY) {
                easy++;
            } else if (definition.difficulty() == TaskDefinition.WeeklyDifficulty.MEDIUM) {
                medium++;
            } else if (definition.difficulty() == TaskDefinition.WeeklyDifficulty.HARD) {
                hard++;
            } else {
                return true;
            }
        }

        return easy != expectedEasy || medium != expectedMedium || hard != expectedHard;
    }

    private static int countWeeklyTasksByDifficulty(TaskDefinition.WeeklyDifficulty difficulty) {
        int count = 0;
        for (TaskDefinition definition : TaskDataManager.weeklyTasks()) {
            if (definition.difficulty() == difficulty) {
                count++;
            }
        }
        return count;
    }

    private static void refreshItemCollectionProgress(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        CompoundTag dailyProgress = data.getCompound(KEY_DAILY_PROGRESS);
        CompoundTag dailyBaselines = data.getCompound(KEY_DAILY_ITEM_BASELINES);
        for (String id : readIdList(data, KEY_DAILY_IDS)) {
            TaskDefinition definition = findTask(id, TaskDefinition.Period.DAILY);
            if (definition != null && definition.type() == TaskDefinition.TaskType.ITEM_COLLECTION) {
                int currentCount = countItem(player, definition.target());
                int baseline = resolveOrInitBaseline(dailyBaselines, id, currentCount, dailyProgress.getInt(id));
                int gainedSinceBaseline = Math.max(0, currentCount - baseline);
                int progress = Math.max(dailyProgress.getInt(id), gainedSinceBaseline);
                dailyProgress.putInt(id, progress);
            }
        }
        data.put(KEY_DAILY_PROGRESS, dailyProgress);
        data.put(KEY_DAILY_ITEM_BASELINES, dailyBaselines);

        CompoundTag weeklyProgress = data.getCompound(KEY_WEEKLY_PROGRESS);
        CompoundTag weeklyBaselines = data.getCompound(KEY_WEEKLY_ITEM_BASELINES);
        for (String id : readIdList(data, KEY_WEEKLY_IDS)) {
            TaskDefinition definition = findTask(id, TaskDefinition.Period.WEEKLY);
            if (definition != null && definition.type() == TaskDefinition.TaskType.ITEM_COLLECTION) {
                int currentCount = countItem(player, definition.target());
                int baseline = resolveOrInitBaseline(weeklyBaselines, id, currentCount, weeklyProgress.getInt(id));
                int gainedSinceBaseline = Math.max(0, currentCount - baseline);
                int progress = Math.max(weeklyProgress.getInt(id), gainedSinceBaseline);
                weeklyProgress.putInt(id, progress);
            }
        }
        data.put(KEY_WEEKLY_PROGRESS, weeklyProgress);
        data.put(KEY_WEEKLY_ITEM_BASELINES, weeklyBaselines);
    }

    private static int countItem(ServerPlayer player, net.minecraft.resources.ResourceLocation itemId) {
        int total = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.getItemHolder().unwrapKey().isPresent() && stack.getItemHolder().unwrapKey().get().location().equals(itemId)) {
                total += stack.getCount();
            }
        }
        for (ItemStack stack : player.getInventory().offhand) {
            if (!stack.isEmpty() && stack.getItemHolder().unwrapKey().isPresent() && stack.getItemHolder().unwrapKey().get().location().equals(itemId)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    private static void incrementMobKillProgress(ServerPlayer player, EntityType<?> victimType) {
        CompoundTag data = player.getPersistentData();

        CompoundTag dailyProgress = data.getCompound(KEY_DAILY_PROGRESS);
        for (String id : readIdList(data, KEY_DAILY_IDS)) {
            TaskDefinition definition = findTask(id, TaskDefinition.Period.DAILY);
            if (definition != null && definition.type() == TaskDefinition.TaskType.MOB_KILL && victimType.builtInRegistryHolder().key().location().equals(definition.target())) {
                dailyProgress.putInt(id, dailyProgress.getInt(id) + 1);
            }
        }
        data.put(KEY_DAILY_PROGRESS, dailyProgress);

        CompoundTag weeklyProgress = data.getCompound(KEY_WEEKLY_PROGRESS);
        for (String id : readIdList(data, KEY_WEEKLY_IDS)) {
            TaskDefinition definition = findTask(id, TaskDefinition.Period.WEEKLY);
            if (definition != null && definition.type() == TaskDefinition.TaskType.MOB_KILL && victimType.builtInRegistryHolder().key().location().equals(definition.target())) {
                weeklyProgress.putInt(id, weeklyProgress.getInt(id) + 1);
            }
        }
        data.put(KEY_WEEKLY_PROGRESS, weeklyProgress);
    }

    private static void resolveCompletionsAndRewards(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        List<String> dailyIds = readIdList(data, KEY_DAILY_IDS);
        List<String> weeklyIds = readIdList(data, KEY_WEEKLY_IDS);
        CompoundTag dailyProgress = data.getCompound(KEY_DAILY_PROGRESS);
        CompoundTag weeklyProgress = data.getCompound(KEY_WEEKLY_PROGRESS);

        int completedDailyTasks = countCompletedTasks(dailyIds, dailyProgress, TaskDefinition.Period.DAILY);
        int dailyRequired = Math.min(DAILY_COMPLETION_REQUIRED_TASKS, dailyIds.size());
        if (dailyRequired > 0 && completedDailyTasks >= dailyRequired) {
            data.putBoolean(KEY_DAILY_COMPLETED, true);
            completeAllSelectedTasks(data, dailyIds, dailyProgress, TaskDefinition.Period.DAILY);
        }

        int points = calculateWeeklyPoints(weeklyIds, weeklyProgress);
        if (points >= maxWeeklyTierPointsRequired()) {
            completeAllSelectedTasks(data, weeklyIds, weeklyProgress, TaskDefinition.Period.WEEKLY);
            points = calculateWeeklyPoints(weeklyIds, weeklyProgress);
        }
        data.putInt(KEY_WEEKLY_POINTS, points);
    }

    public static boolean claimDailyCompletionReward(ServerPlayer player) {
        ensurePeriods(player);
        refreshItemCollectionProgress(player);
        resolveCompletionsAndRewards(player);

        if (!DailyTaskService.claimReward(player)) {
            return false;
        }

        grantRewards(player, "daily_completion");
        return true;
    }

    public static int claimUnlockedWeeklyTierRewards(ServerPlayer player) {
        ensurePeriods(player);
        refreshItemCollectionProgress(player);
        resolveCompletionsAndRewards(player);

        CompoundTag data = player.getPersistentData();
        int points = data.getInt(KEY_WEEKLY_POINTS);
        int claims = data.getInt(KEY_WEEKLY_TIER_CLAIMS);
        int claimedCount = 0;

        for (int tier = 1; tier <= WEEKLY_TIER_COUNT; tier++) {
            int required = tier * WEEKLY_POINTS_PER_TIER;
            int bit = 1 << (tier - 1);
            if (points >= required && (claims & bit) == 0) {
                grantRewards(player, "weekly_tier_" + tier);
                claims |= bit;
                claimedCount++;
            }
        }

        data.putInt(KEY_WEEKLY_TIER_CLAIMS, claims);
        return claimedCount;
    }

    public static boolean claimWeeklyTierReward(ServerPlayer player, int tier) {
        ensurePeriods(player);
        refreshItemCollectionProgress(player);
        resolveCompletionsAndRewards(player);
        if (tier < 1 || tier > WEEKLY_TIER_COUNT) {
            return false;
        }

        CompoundTag data = player.getPersistentData();
        int required = tier * WEEKLY_POINTS_PER_TIER;
        int points = data.getInt(KEY_WEEKLY_POINTS);
        if (points < required) {
            return false;
        }

        int claims = data.getInt(KEY_WEEKLY_TIER_CLAIMS);
        int bit = 1 << (tier - 1);
        if ((claims & bit) != 0) {
            return false;
        }

        grantRewards(player, "weekly_tier_" + tier);
        data.putInt(KEY_WEEKLY_TIER_CLAIMS, claims | bit);
        return true;
    }

    private static void grantRewards(ServerPlayer player, String pool) {
        for (TaskReward reward : TaskDataManager.rewardsForPool(pool)) {
            if (reward instanceof TaskReward.ItemReward itemReward) {
                player.addItem(itemReward.item().getDefaultInstance().copyWithCount(itemReward.count()));
            } else if (reward instanceof TaskReward.CommandReward commandReward) {
                player.server.getCommands().performPrefixedCommand(
                        player.server.createCommandSourceStack().withPermission(2).withEntity(player),
                        commandReward.command().replace("{player}", player.getScoreboardName())
                );
            } else if (reward instanceof TaskReward.SanityReward sanityReward) {
                io.github.ozokuz.incore.features.sanity.SanityManager.addSanity(player, sanityReward.amount());
            }
        }
    }

    private static List<RewardView> buildRewardViews(String pool) {
        List<RewardView> rewards = new ArrayList<>();
        for (TaskReward reward : TaskDataManager.rewardsForPool(pool)) {
            if (reward instanceof TaskReward.ItemReward itemReward) {
                rewards.add(new RewardView(
                        "item",
                        BuiltInRegistries.ITEM.getKey(itemReward.item()).toString(),
                        itemReward.count(),
                        ""
                ));
            } else if (reward instanceof TaskReward.SanityReward sanityReward) {
                rewards.add(new RewardView("sanity", "incore:sanity_vessel", sanityReward.amount(), ""));
            } else if (reward instanceof TaskReward.CommandReward commandReward) {
                rewards.add(new RewardView("command", "minecraft:command_block", 1, commandReward.command()));
            }
        }
        return rewards;
    }

    private static TaskDefinition findTask(String id, TaskDefinition.Period period) {
        List<TaskDefinition> tasks = period == TaskDefinition.Period.DAILY ? TaskDataManager.dailyTasks() : TaskDataManager.weeklyTasks();
        for (TaskDefinition task : tasks) {
            if (task.id().toString().equals(id)) {
                return task;
            }
        }
        return null;
    }

    private static int compareWeeklyTaskOrder(String leftId, String rightId) {
        TaskDefinition left = findTask(leftId, TaskDefinition.Period.WEEKLY);
        TaskDefinition right = findTask(rightId, TaskDefinition.Period.WEEKLY);
        if (left == null && right == null) {
            return leftId.compareTo(rightId);
        }
        if (left == null) {
            return 1;
        }
        if (right == null) {
            return -1;
        }

        int byPoints = Integer.compare(right.difficulty().points(), left.difficulty().points());
        if (byPoints != 0) {
            return byPoints;
        }

        String leftTitle = left.title() == null ? "" : left.title();
        String rightTitle = right.title() == null ? "" : right.title();
        int byTitle = String.CASE_INSENSITIVE_ORDER.compare(leftTitle, rightTitle);
        if (byTitle != 0) {
            return byTitle;
        }

        return left.id().toString().compareTo(right.id().toString());
    }

    private static CompoundTag seedItemCollectionBaselines(ServerPlayer player, List<String> ids, TaskDefinition.Period period) {
        CompoundTag baselines = new CompoundTag();
        for (String id : ids) {
            TaskDefinition definition = findTask(id, period);
            if (definition == null || definition.type() != TaskDefinition.TaskType.ITEM_COLLECTION) {
                continue;
            }
            baselines.putInt(id, countItem(player, definition.target()));
        }
        return baselines;
    }

    private static int resolveOrInitBaseline(CompoundTag baselines, String id, int currentCount, int existingProgress) {
        if (baselines.contains(id, Tag.TAG_INT)) {
            return baselines.getInt(id);
        }
        int baseline = Math.max(0, currentCount - Math.max(0, existingProgress));
        baselines.putInt(id, baseline);
        return baseline;
    }

    private static int countCompletedTasks(List<String> ids, CompoundTag progress, TaskDefinition.Period period) {
        int completed = 0;
        for (String id : ids) {
            TaskDefinition definition = findTask(id, period);
            if (definition != null && progress.getInt(id) >= definition.goal()) {
                completed++;
            }
        }
        return completed;
    }

    private static void completeAllSelectedTasks(CompoundTag data, List<String> ids, CompoundTag progress, TaskDefinition.Period period) {
        for (String id : ids) {
            TaskDefinition definition = findTask(id, period);
            if (definition == null) {
                continue;
            }
            progress.putInt(id, Math.max(progress.getInt(id), Math.max(1, definition.goal())));
        }
        data.put(period == TaskDefinition.Period.DAILY ? KEY_DAILY_PROGRESS : KEY_WEEKLY_PROGRESS, progress);
    }

    private static int calculateWeeklyPoints(List<String> weeklyIds, CompoundTag weeklyProgress) {
        int points = 0;
        for (String id : weeklyIds) {
            TaskDefinition definition = findTask(id, TaskDefinition.Period.WEEKLY);
            if (definition != null && weeklyProgress.getInt(id) >= definition.goal()) {
                points += definition.difficulty().points();
            }
        }
        return points;
    }

    private static int maxWeeklyTierPointsRequired() {
        return WEEKLY_TIER_COUNT * WEEKLY_POINTS_PER_TIER;
    }

    private static List<TaskView> buildViews(ServerPlayer player, List<String> ids, String progressKey) {
        CompoundTag progressTag = player.getPersistentData().getCompound(progressKey);
        List<TaskView> result = new ArrayList<>();
        for (String id : ids) {
            TaskDefinition definition = findTask(id, progressKey.equals(KEY_DAILY_PROGRESS) ? TaskDefinition.Period.DAILY : TaskDefinition.Period.WEEKLY);
            if (definition == null) {
                continue;
            }
            int progress = progressTag.getInt(id);
            result.add(new TaskView(
                    definition.title(),
                    definition.description(),
                    definition.goal(),
                    progress,
                    definition.period() == TaskDefinition.Period.WEEKLY ? definition.difficulty().name().toLowerCase() : "daily",
                    definition.period() == TaskDefinition.Period.WEEKLY ? definition.difficulty().points() : 0
            ));
        }
        return result;
    }

    private static List<String> readIdList(CompoundTag data, String key) {
        ListTag tag = data.getList(key, StringTag.valueOf("").getId());
        List<String> ids = new ArrayList<>(tag.size());
        for (int i = 0; i < tag.size(); i++) {
            ids.add(tag.getString(i));
        }
        return ids;
    }

    private static void writeIdList(CompoundTag data, String key, List<String> ids) {
        ListTag tag = new ListTag();
        ids.forEach(id -> tag.add(StringTag.valueOf(id)));
        data.put(key, tag);
    }

    private static long currentDayIndex() {
        return LocalDate.now(ZoneOffset.UTC).toEpochDay();
    }

    private static long currentWeekIndex() {
        return currentDayIndex() / 7L;
    }

    private record TaskSyncView(
            List<TaskView> daily,
            List<TaskView> weekly,
            int weeklyPoints,
            boolean dailyCompleted,
            boolean dailyRewardClaimed,
            List<RewardView> dailyRewards,
            List<TierView> tiers,
            List<DailyTaskService.DailyTaskView> fixedDailyTasks,
            int fixedDailyCompleted,
            boolean fixedDailyAllCompleted,
            boolean fixedDailyRewardClaimed
    ) {
    }

    public record TaskView(String title, String description, int goal, int progress, String difficulty, int points) {
    }

    public record TierView(int tier, int requiredPoints, boolean unlocked, boolean claimed, List<RewardView> rewards) {
    }

    public record RewardView(String kind, String itemId, int amount, String text) {
    }

    public record TaskAdminStatus(
            int dailyTaskCount,
            boolean dailyCompleted,
            boolean dailyRewardClaimed,
            int weeklyTaskCount,
            int weeklyPoints,
            int weeklyClaimedTiers,
            int weeklyClaimableTiers
    ) {
    }
}
