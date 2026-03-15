package ozokuz.incore.features.battlepass;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class BattlePassProgressManager {
    static final String KEY_ROOT = "incore:battlepass";
    private static final String KEY_PROGRESS_BY_SET = "sets";
    private static final String KEY_XP = "xp";
    private static final String KEY_LEVEL = "level";
    private static final String KEY_COMPLETED_TASKS = "completed_tasks";
    private static final String KEY_TASK_PROGRESS = "task_progress";
    private static final String KEY_UNLOCKED_LANES = "unlocked_lanes";
    private static final String KEY_HIGHEST_REWARDED_BY_LANE = "highest_rewarded_by_lane";
    private static final String KEY_LAST_LOGIN_DAY = "last_login_day";

    private BattlePassProgressManager() {
    }

    public static CompletionResult completeTask(ServerPlayer player, String taskId, Instant now) {
        ProgressResult progressResult = addTaskProgress(player, taskId, Integer.MAX_VALUE, now);
        if (!progressResult.success()) {
            return CompletionResult.failed(progressResult.message());
        }

        return CompletionResult.success(progressResult.xpGained(), progressResult.levelsGained(), progressResult.newLevel());
    }

    public static ClaimResult claimAllRewards(ServerPlayer player, Instant now) {
        Optional<BattlePassDefinition> activeOptional = BattlePassManager.getActiveSet(player.getServer(), now);
        if (activeOptional.isEmpty()) {
            return ClaimResult.failed("No active battle pass set.");
        }

        BattlePassDefinition active = activeOptional.get();
        PlayerSetProgress progress = getProgress(player, active);
        int cappedLevel = Math.max(0, progress.level);

        Set<Integer> claimedLevels = new HashSet<>();
        int rewardCount = 0;

        for (String lane : active.lanes()) {
            if (!progress.unlockedLanes().contains(lane)) {
                continue;
            }

            int highestClaimed = progress.highestRewardedByLane().getOrDefault(lane, -1);
            int fromLevel = Math.max(0, highestClaimed + 1);
            if (cappedLevel < fromLevel) {
                continue;
            }

            for (int level = fromLevel; level <= cappedLevel; level++) {
                List<BattlePassReward> rewards = active.rewardsForLevel(lane, level);
                for (BattlePassReward reward : rewards) {
                    reward.grant(player);
                    rewardCount++;
                }
                progress.highestRewardedByLane().put(lane, level);
                claimedLevels.add(level);
            }
        }

        if (claimedLevels.isEmpty()) {
            return ClaimResult.failed("No unclaimed battle pass rewards.");
        }

        saveProgress(player, active, progress);
        int highestClaimedLevel = claimedLevels.stream().mapToInt(Integer::intValue).max().orElse(0);
        return ClaimResult.success(
                "Claimed " + claimedLevels.size() + " level reward(s).",
                claimedLevels.size(),
                rewardCount,
                highestClaimedLevel
        );
    }

    public static ProgressResult addTaskProgress(ServerPlayer player, String taskId, int amount, Instant now) {
        if (amount <= 0) {
            return ProgressResult.failed("Progress amount must be greater than zero.");
        }

        Optional<BattlePassDefinition> activeOptional = BattlePassManager.getActiveSet(player.getServer(), now);
        if (activeOptional.isEmpty()) {
            return ProgressResult.failed("No active battle pass set.");
        }

        BattlePassDefinition active = activeOptional.get();
        Optional<BattlePassDefinition.BattlePassTask> taskOptional = active.findTask(taskId);
        if (taskOptional.isEmpty()) {
            return ProgressResult.failed("Task not found in the active battle pass set.");
        }

        BattlePassDefinition.BattlePassTask task = taskOptional.get();
        int activeWeek = getActiveWeek(active, now);
        if (!isTaskAvailable(task, activeWeek)) {
            return ProgressResult.failed("Task unlocks in week " + task.week() + ". Current week: " + activeWeek + ".");
        }

        PlayerSetProgress progress = getProgress(player, active);
        int goal = Math.max(1, task.progressGoal());
        if (progress.completedTasks().contains(task.id())) {
            return ProgressResult.failed("Task was already completed.");
        }

        String category = categoryKey(task);
        Map<String, Integer> availableByCategory = availableTaskCountsByCategory(active, activeWeek);
        Map<String, Integer> completedByCategory = completedAvailableTaskCountsByCategory(active, progress.completedTasks(), activeWeek);
        int availableInCategory = availableByCategory.getOrDefault(category, 0);
        int categoryCap = completionCapFromAvailable(availableInCategory);
        int completedInCategory = completedByCategory.getOrDefault(category, 0);
        if (completedInCategory >= categoryCap) {
            return ProgressResult.failed("Category completion cap reached (" + categoryCap + ").");
        }

        int currentProgress = Math.max(0, progress.taskProgress().getOrDefault(task.id(), 0));
        long rawProgress = (long) currentProgress + amount;
        int updatedProgress = (int) Math.min(goal, Math.max(0L, rawProgress));
        progress.taskProgress().put(task.id(), updatedProgress);

        int xpGained = 0;
        int levelsGained = 0;
        int newLevel = progress.level;
        if (updatedProgress >= goal) {
            int previousLevel = progress.level;
            xpGained = active.xpForTask(task);
            progress.completedTasks().add(task.id());
            progress.xp += xpGained;
            progress.level = Math.max(0, progress.xp / active.xpPerLevel());
            levelsGained = progress.level - previousLevel;
            newLevel = progress.level;
        }

        saveProgress(player, active, progress);

        if (updatedProgress >= goal) {
            return ProgressResult.success("Task completed.", updatedProgress, goal, true, xpGained, levelsGained, newLevel);
        }

        return ProgressResult.success("Task progress updated.", updatedProgress, goal, false, 0, 0, newLevel);
    }

    public static void addProgressByTriggerType(ServerPlayer player, BattlePassDefinition.TriggerType triggerType, int amount, Instant now) {
        if (triggerType == BattlePassDefinition.TriggerType.NONE || amount <= 0) {
            return;
        }

        Optional<BattlePassDefinition> activeOptional = BattlePassManager.getActiveSet(player.getServer(), now);
        if (activeOptional.isEmpty()) {
            return;
        }

        BattlePassDefinition active = activeOptional.get();
        int activeWeek = getActiveWeek(active, now);

        for (BattlePassDefinition.BattlePassTask task : active.tasks()) {
            if (task.triggerType() != triggerType) {
                continue;
            }
            if (!isTaskAvailable(task, activeWeek)) {
                continue;
            }

            addTaskProgress(player, task.id(), amount, now);
        }
    }

    public static boolean checkAndProgressLogin(ServerPlayer player, Instant now) {
        Optional<BattlePassDefinition> activeOptional = BattlePassManager.getActiveSet(player.getServer(), now);
        if (activeOptional.isEmpty()) {
            return false;
        }

        BattlePassDefinition active = activeOptional.get();
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        String todayKey = today.toString();

        CompoundTag root = player.getPersistentData().getCompound(KEY_ROOT);
        String lastLoginDay = root.getString(KEY_LAST_LOGIN_DAY);

        if (todayKey.equals(lastLoginDay)) {
            return false;
        }

        root.putString(KEY_LAST_LOGIN_DAY, todayKey);
        player.getPersistentData().put(KEY_ROOT, root);

        addProgressByTriggerType(player, BattlePassDefinition.TriggerType.LOGIN, 1, now);
        return true;
    }

    public static ManagementResult setXp(ServerPlayer player, int xp, Instant now) {
        Optional<BattlePassDefinition> activeOptional = BattlePassManager.getActiveSet(player.getServer(), now);
        if (activeOptional.isEmpty()) {
            return ManagementResult.failed("No active battle pass set.");
        }

        BattlePassDefinition active = activeOptional.get();
        PlayerSetProgress progress = getProgress(player, active);
        int clampedXp = Math.max(0, xp);
        progress.xp = clampedXp;
        progress.level = Math.max(0, clampedXp / active.xpPerLevel());
        clampClaimedRewardsToCurrentLevel(progress);
        saveProgress(player, active, progress);
        return ManagementResult.success("XP set to " + progress.xp + " (level " + progress.level + ").", progress.xp, progress.level);
    }

    public static ManagementResult addXp(ServerPlayer player, int amount, Instant now) {
        Optional<BattlePassDefinition> activeOptional = BattlePassManager.getActiveSet(player.getServer(), now);
        if (activeOptional.isEmpty()) {
            return ManagementResult.failed("No active battle pass set.");
        }

        BattlePassDefinition active = activeOptional.get();
        PlayerSetProgress progress = getProgress(player, active);
        long updated = (long) progress.xp + amount;
        int clampedXp = (int) Math.max(0L, Math.min(Integer.MAX_VALUE, updated));
        progress.xp = clampedXp;
        progress.level = Math.max(0, clampedXp / active.xpPerLevel());
        clampClaimedRewardsToCurrentLevel(progress);
        saveProgress(player, active, progress);
        return ManagementResult.success("XP is now " + progress.xp + " (level " + progress.level + ").", progress.xp, progress.level);
    }

    public static ManagementResult setLevel(ServerPlayer player, int level, Instant now) {
        Optional<BattlePassDefinition> activeOptional = BattlePassManager.getActiveSet(player.getServer(), now);
        if (activeOptional.isEmpty()) {
            return ManagementResult.failed("No active battle pass set.");
        }

        BattlePassDefinition active = activeOptional.get();
        PlayerSetProgress progress = getProgress(player, active);
        int maxRepresentableLevel = Math.max(0, Integer.MAX_VALUE / active.xpPerLevel());
        int clampedLevel = Math.max(0, Math.min(level, maxRepresentableLevel));
        long xpForLevel = (long) clampedLevel * active.xpPerLevel();
        progress.level = clampedLevel;
        progress.xp = (int) xpForLevel;
        clampClaimedRewardsToCurrentLevel(progress);
        saveProgress(player, active, progress);
        return ManagementResult.success("Level set to " + progress.level + " (" + progress.xp + " XP).", progress.xp, progress.level);
    }

    public static ManagementResult addLevel(ServerPlayer player, int amount, Instant now) {
        Optional<BattlePassDefinition> activeOptional = BattlePassManager.getActiveSet(player.getServer(), now);
        if (activeOptional.isEmpty()) {
            return ManagementResult.failed("No active battle pass set.");
        }

        BattlePassDefinition active = activeOptional.get();
        PlayerSetProgress progress = getProgress(player, active);
        int maxRepresentableLevel = Math.max(0, Integer.MAX_VALUE / active.xpPerLevel());
        long updatedLevel = (long) progress.level + amount;
        int clampedLevel = (int) Math.max(0L, Math.min(maxRepresentableLevel, updatedLevel));
        progress.level = clampedLevel;
        long xpForLevel = (long) clampedLevel * active.xpPerLevel();
        progress.xp = (int) xpForLevel;
        clampClaimedRewardsToCurrentLevel(progress);
        saveProgress(player, active, progress);
        return ManagementResult.success("Level is now " + progress.level + " (" + progress.xp + " XP).", progress.xp, progress.level);
    }

    public static ManagementResult resetAllProgress(ServerPlayer player, Instant now) {
        Optional<BattlePassDefinition> activeOptional = BattlePassManager.getActiveSet(player.getServer(), now);
        if (activeOptional.isEmpty()) {
            return ManagementResult.failed("No active battle pass set.");
        }

        BattlePassDefinition active = activeOptional.get();
        PlayerSetProgress progress = getProgress(player, active);
        progress.xp = 0;
        progress.level = 0;
        progress.highestRewardedByLane().replaceAll((lane, ignored) -> -1);
        progress.completedTasks().clear();
        progress.taskProgress().clear();
        saveProgress(player, active, progress);
        return ManagementResult.success("Reset all battle pass progress.", progress.xp, progress.level);
    }

    public static ManagementResult resetAllTasks(ServerPlayer player, Instant now) {
        Optional<BattlePassDefinition> activeOptional = BattlePassManager.getActiveSet(player.getServer(), now);
        if (activeOptional.isEmpty()) {
            return ManagementResult.failed("No active battle pass set.");
        }

        BattlePassDefinition active = activeOptional.get();
        PlayerSetProgress progress = getProgress(player, active);
        progress.completedTasks().clear();
        progress.taskProgress().clear();
        saveProgress(player, active, progress);
        return ManagementResult.success("Reset all task progress for the active battle pass.", progress.xp, progress.level);
    }

    public static ManagementResult resetTask(ServerPlayer player, String taskId, Instant now) {
        Optional<BattlePassDefinition> activeOptional = BattlePassManager.getActiveSet(player.getServer(), now);
        if (activeOptional.isEmpty()) {
            return ManagementResult.failed("No active battle pass set.");
        }

        BattlePassDefinition active = activeOptional.get();
        Optional<BattlePassDefinition.BattlePassTask> taskOptional = active.findTask(taskId);
        if (taskOptional.isEmpty()) {
            return ManagementResult.failed("Task not found in the active battle pass set.");
        }

        PlayerSetProgress progress = getProgress(player, active);
        boolean removedCompleted = progress.completedTasks().remove(taskId);
        Integer removedProgress = progress.taskProgress().remove(taskId);
        if (!removedCompleted && removedProgress == null) {
            return ManagementResult.failed("Task has no stored progress.");
        }

        saveProgress(player, active, progress);
        return ManagementResult.success("Reset task progress for " + taskId + ".", progress.xp, progress.level);
    }

    public static LaneManagementResult unlockLane(ServerPlayer player, String laneId, Instant now) {
        Optional<BattlePassDefinition> activeOptional = BattlePassManager.getActiveSet(player.getServer(), now);
        if (activeOptional.isEmpty()) {
            return LaneManagementResult.failed("No active battle pass set.");
        }

        BattlePassDefinition active = activeOptional.get();
        String lane = BattlePassLane.normalize(laneId);
        if (!BattlePassLane.isValid(lane)) {
            return LaneManagementResult.failed("Unknown lane: " + laneId + ".");
        }
        if (!active.lanes().contains(lane)) {
            return LaneManagementResult.failed("Lane is not configured for the active battle pass: " + lane + ".");
        }
        if (BattlePassLaneManager.getAlwaysAvailableLaneIds().contains(lane)) {
            return LaneManagementResult.failed("This lane is always available.");
        }

        PlayerSetProgress progress = getProgress(player, active);
        if (!progress.unlockedLanes().add(lane)) {
            return LaneManagementResult.failed(BattlePassLane.displayName(lane) + " is already unlocked.");
        }

        saveProgress(player, active, progress);
        return LaneManagementResult.success("Unlocked " + BattlePassLane.displayName(lane) + ".");
    }

    public static LaneManagementResult lockLane(ServerPlayer player, String laneId, Instant now) {
        Optional<BattlePassDefinition> activeOptional = BattlePassManager.getActiveSet(player.getServer(), now);
        if (activeOptional.isEmpty()) {
            return LaneManagementResult.failed("No active battle pass set.");
        }

        BattlePassDefinition active = activeOptional.get();
        String lane = BattlePassLane.normalize(laneId);
        if (!BattlePassLane.isValid(lane)) {
            return LaneManagementResult.failed("Unknown lane: " + laneId + ".");
        }
        if (!active.lanes().contains(lane)) {
            return LaneManagementResult.failed("Lane is not configured for the active battle pass: " + lane + ".");
        }
        if (BattlePassLaneManager.getAlwaysAvailableLaneIds().contains(lane)) {
            return LaneManagementResult.failed("This lane cannot be locked.");
        }

        PlayerSetProgress progress = getProgress(player, active);
        if (!progress.unlockedLanes().remove(lane)) {
            return LaneManagementResult.failed(BattlePassLane.displayName(lane) + " is already locked.");
        }

        saveProgress(player, active, progress);
        return LaneManagementResult.success("Locked " + BattlePassLane.displayName(lane) + ".");
    }

    public static LaneStatusResult laneStatus(ServerPlayer player, Instant now) {
        Optional<BattlePassDefinition> activeOptional = BattlePassManager.getActiveSet(player.getServer(), now);
        if (activeOptional.isEmpty()) {
            return LaneStatusResult.failed("No active battle pass set.");
        }

        BattlePassDefinition active = activeOptional.get();
        PlayerSetProgress progress = getProgress(player, active);
        List<LaneSnapshot> lanes = active.lanes().stream()
                .map(lane -> new LaneSnapshot(
                        lane,
                        BattlePassLane.displayName(lane),
                        progress.unlockedLanes().contains(lane),
                        progress.highestRewardedByLane().getOrDefault(lane, -1)
                ))
                .toList();
        return LaneStatusResult.success(active.id().toString(), lanes);
    }

    public static StatusResult getStatus(ServerPlayer player, Instant now) {
        Optional<BattlePassDefinition> activeOptional = BattlePassManager.getActiveSet(player.getServer(), now);
        if (activeOptional.isEmpty()) {
            return new StatusResult("none", 0, 0, 0, 0);
        }

        BattlePassDefinition active = activeOptional.get();
        PlayerSetProgress progress = getProgress(player, active);
        int currentWeek = getActiveWeek(active, now);
        return new StatusResult(active.id().toString(), progress.level, progress.xp, active.xpPerLevel(), currentWeek);
    }

    public static ScreenSnapshot getScreenSnapshot(ServerPlayer player, Instant now) {
        Optional<BattlePassDefinition> activeOptional = BattlePassManager.getActiveSet(player.getServer(), now);
        if (activeOptional.isEmpty()) {
            return ScreenSnapshot.none();
        }

        BattlePassDefinition active = activeOptional.get();
        PlayerSetProgress progress = getProgress(player, active);
        int currentWeek = getActiveWeek(active, now);

        Map<String, Integer> availableByCategory = availableTaskCountsByCategory(active, currentWeek);
        Map<String, Integer> completedByCategory = completedAvailableTaskCountsByCategory(active, progress.completedTasks(), currentWeek);
        int totalAvailableTaskCount = availableByCategory.values().stream().mapToInt(Integer::intValue).sum();
        int totalTaskCompletionCap = availableByCategory.values().stream().mapToInt(BattlePassProgressManager::completionCapFromAvailable).sum();
        int totalTaskCompletionCount = completedByCategory.values().stream().mapToInt(Integer::intValue).sum();

        Set<Integer> unclaimedLevels = new HashSet<>();
        for (String lane : active.lanes()) {
            if (!progress.unlockedLanes().contains(lane)) {
                continue;
            }

            int highest = progress.highestRewardedByLane().getOrDefault(lane, -1);
            for (int level = Math.max(0, highest + 1); level <= progress.level; level++) {
                unclaimedLevels.add(level);
            }
        }

        List<TaskSnapshot> tasks = new ArrayList<>();
        for (BattlePassDefinition.BattlePassTask task : active.tasks()) {
            boolean completed = progress.completedTasks().contains(task.id());
            int progressGoal = Math.max(1, task.progressGoal());
            int progressCurrent = completed
                    ? progressGoal
                    : Math.min(progressGoal, Math.max(0, progress.taskProgress().getOrDefault(task.id(), 0)));
            int xpReward = active.xpForTask(task);
            String category = categoryKey(task);
            int categoryCap = completionCapFromAvailable(availableByCategory.getOrDefault(category, 0));
            int completedInCategory = completedByCategory.getOrDefault(category, 0);
            boolean completableNow;
            String status;

            if (completed) {
                completableNow = false;
                status = "Completed";
            } else if (!isTaskAvailable(task, currentWeek)) {
                completableNow = false;
                status = "Available in week " + task.week();
            } else if (completedInCategory >= categoryCap) {
                completableNow = false;
                status = "Category cap reached (" + categoryCap + ")";
            } else {
                completableNow = true;
                status = task.type() == BattlePassDefinition.TaskType.WEEKLY && currentWeek > task.week()
                        ? "Completable now (carried from week " + task.week() + ")"
                        : "Completable now";
            }

            tasks.add(new TaskSnapshot(
                    task.id(),
                    task.description(),
                    task.type() == BattlePassDefinition.TaskType.WEEKLY,
                    task.week(),
                    task.tier(),
                    xpReward,
                    progressCurrent,
                    progressGoal,
                    completed,
                    completableNow,
                    status
            ));
        }

        tasks.sort(Comparator
                .comparing(TaskSnapshot::weekly).reversed()
                .thenComparingInt(TaskSnapshot::week)
                .thenComparing(TaskSnapshot::id));

        List<LaneSnapshot> laneSnapshots = active.lanes().stream()
                .map(lane -> new LaneSnapshot(
                        lane,
                        BattlePassLane.displayName(lane),
                        progress.unlockedLanes().contains(lane),
                        progress.highestRewardedByLane().getOrDefault(lane, -1)
                ))
                .toList();

        return new ScreenSnapshot(
                true,
                active.id().toString(),
                active.startsAt().toEpochMilli(),
                active.endsAt().toEpochMilli(),
                currentWeek,
                (int) active.durationWeeks(),
                progress.level,
                progress.xp,
                active.xpPerLevel(),
                totalTaskCompletionCount,
                totalTaskCompletionCap,
                totalAvailableTaskCount,
                availableByCategory.size(),
                unclaimedLevels.size(),
                laneSnapshots,
                tasks
        );
    }

    private static int getActiveWeek(BattlePassDefinition definition, Instant now) {
        return BattlePassManager.resolveCurrentWeek(definition, now);
    }

    private static PlayerSetProgress getProgress(ServerPlayer player, BattlePassDefinition definition) {
        CompoundTag setTag = getOrCreateSetTag(player, progressSetKey(definition));

        int xp = Math.max(0, setTag.getInt(KEY_XP));
        int level = Math.max(0, setTag.getInt(KEY_LEVEL));

        Set<String> completedTasks = new HashSet<>();
        ListTag completedTag = setTag.getList(KEY_COMPLETED_TASKS, Tag.TAG_STRING);
        for (int i = 0; i < completedTag.size(); i++) {
            completedTasks.add(completedTag.getString(i));
        }

        Map<String, Integer> taskProgress = new HashMap<>();
        CompoundTag progressTag = setTag.getCompound(KEY_TASK_PROGRESS);
        for (String key : progressTag.getAllKeys()) {
            taskProgress.put(key, Math.max(0, progressTag.getInt(key)));
        }

        Set<String> unlockedLanes = new HashSet<>();
        ListTag unlockedTag = setTag.getList(KEY_UNLOCKED_LANES, Tag.TAG_STRING);
        for (int i = 0; i < unlockedTag.size(); i++) {
            String lane = BattlePassLane.normalize(unlockedTag.getString(i));
            if (BattlePassLane.isValid(lane)) {
                unlockedLanes.add(lane);
            }
        }
        unlockedLanes.retainAll(definition.lanes());
        for (String alwaysAvailable : BattlePassLaneManager.getAlwaysAvailableLaneIds()) {
            if (definition.lanes().contains(alwaysAvailable)) {
                unlockedLanes.add(alwaysAvailable);
            }
        }

        Map<String, Integer> highestRewardedByLane = new HashMap<>();
        CompoundTag highestByLaneTag = setTag.getCompound(KEY_HIGHEST_REWARDED_BY_LANE);
        for (String lane : definition.lanes()) {
            int highest = highestByLaneTag.contains(lane, Tag.TAG_INT)
                    ? Math.max(-1, highestByLaneTag.getInt(lane))
                    : -1;
            highestRewardedByLane.put(lane, highest);
        }

        return new PlayerSetProgress(xp, level, completedTasks, taskProgress, unlockedLanes, highestRewardedByLane);
    }

    private static void saveProgress(ServerPlayer player, BattlePassDefinition definition, PlayerSetProgress progress) {
        CompoundTag setTag = getOrCreateSetTag(player, progressSetKey(definition));
        setTag.putInt(KEY_XP, Math.max(0, progress.xp));
        setTag.putInt(KEY_LEVEL, Math.max(0, progress.level));

        ListTag completed = new ListTag();
        progress.completedTasks.stream().sorted().map(StringTag::valueOf).forEach(completed::add);
        setTag.put(KEY_COMPLETED_TASKS, completed);

        CompoundTag progressTag = new CompoundTag();
        progress.taskProgress.forEach((taskId, value) -> {
            if (value > 0) {
                progressTag.putInt(taskId, value);
            }
        });
        setTag.put(KEY_TASK_PROGRESS, progressTag);

        ListTag unlockedTag = new ListTag();
        progress.unlockedLanes.stream().sorted().map(StringTag::valueOf).forEach(unlockedTag::add);
        setTag.put(KEY_UNLOCKED_LANES, unlockedTag);

        CompoundTag highestByLaneTag = new CompoundTag();
        progress.highestRewardedByLane.forEach((lane, highestLevel) -> highestByLaneTag.putInt(lane, Math.max(-1, highestLevel)));
        setTag.put(KEY_HIGHEST_REWARDED_BY_LANE, highestByLaneTag);
    }

    private static CompoundTag getOrCreateSetTag(ServerPlayer player, String setId) {
        CompoundTag root = player.getPersistentData().getCompound(KEY_ROOT);
        CompoundTag sets = root.getCompound(KEY_PROGRESS_BY_SET);
        CompoundTag setTag = sets.getCompound(setId);

        sets.put(setId, setTag);
        root.put(KEY_PROGRESS_BY_SET, sets);
        player.getPersistentData().put(KEY_ROOT, root);
        return setTag;
    }

    private static String progressSetKey(BattlePassDefinition definition) {
        ZonedDateTime startsAt = ZonedDateTime.ofInstant(definition.startsAt(), ZoneId.systemDefault());
        long weekKey = BattlePassWeekTime.weekKey(startsAt);
        return definition.id() + "#" + weekKey;
    }

    private static void clampClaimedRewardsToCurrentLevel(PlayerSetProgress progress) {
        progress.highestRewardedByLane.replaceAll((lane, highest) -> Math.min(Math.max(-1, highest), progress.level));
    }

    private static boolean isTaskAvailable(BattlePassDefinition.BattlePassTask task, int activeWeek) {
        return task.type() != BattlePassDefinition.TaskType.WEEKLY || task.week() <= activeWeek;
    }

    private static String categoryKey(BattlePassDefinition.BattlePassTask task) {
        return task.type() == BattlePassDefinition.TaskType.WEEKLY
                ? "week:" + task.week()
                : "permanent";
    }

    private static Map<String, Integer> availableTaskCountsByCategory(BattlePassDefinition definition, int activeWeek) {
        Map<String, Integer> counts = new HashMap<>();
        for (BattlePassDefinition.BattlePassTask task : definition.tasks()) {
            if (!isTaskAvailable(task, activeWeek)) {
                continue;
            }

            counts.merge(categoryKey(task), 1, Integer::sum);
        }
        return counts;
    }

    private static int completionCapFromAvailable(int availableTaskCount) {
        if (availableTaskCount <= 0) {
            return 0;
        }

        return Math.max(1, availableTaskCount / 2);
    }

    private static Map<String, Integer> completedAvailableTaskCountsByCategory(BattlePassDefinition definition, Set<String> completedTasks, int activeWeek) {
        Map<String, Integer> counts = new HashMap<>();
        completedTasks.stream()
                .map(definition::findTask)
                .flatMap(Optional::stream)
                .filter(task -> isTaskAvailable(task, activeWeek))
                .forEach(task -> counts.merge(categoryKey(task), 1, Integer::sum));
        return counts;
    }

    private static class PlayerSetProgress {
        private int xp;
        private int level;
        private final Set<String> completedTasks;
        private final Map<String, Integer> taskProgress;
        private final Set<String> unlockedLanes;
        private final Map<String, Integer> highestRewardedByLane;

        private PlayerSetProgress(
                int xp,
                int level,
                Set<String> completedTasks,
                Map<String, Integer> taskProgress,
                Set<String> unlockedLanes,
                Map<String, Integer> highestRewardedByLane
        ) {
            this.xp = xp;
            this.level = level;
            this.completedTasks = completedTasks;
            this.taskProgress = taskProgress;
            this.unlockedLanes = unlockedLanes;
            this.highestRewardedByLane = highestRewardedByLane;
        }

        public Set<String> completedTasks() {
            return completedTasks;
        }

        public Map<String, Integer> taskProgress() {
            return taskProgress;
        }

        public Set<String> unlockedLanes() {
            return unlockedLanes;
        }

        public Map<String, Integer> highestRewardedByLane() {
            return highestRewardedByLane;
        }
    }

    public record CompletionResult(boolean success, String message, int xpGained, int levelsGained, int newLevel) {
        static CompletionResult failed(String message) {
            return new CompletionResult(false, message, 0, 0, 0);
        }

        static CompletionResult success(int xpGained, int levelsGained, int newLevel) {
            return new CompletionResult(true, "Task completed.", xpGained, levelsGained, newLevel);
        }
    }

    public record ClaimResult(boolean success, String message, int claimedLevels, int rewardCount, int highestClaimedLevel) {
        static ClaimResult failed(String message) {
            return new ClaimResult(false, message, 0, 0, 0);
        }

        static ClaimResult success(String message, int claimedLevels, int rewardCount, int highestClaimedLevel) {
            return new ClaimResult(true, message, claimedLevels, rewardCount, highestClaimedLevel);
        }
    }

    public record ProgressResult(
            boolean success,
            String message,
            int currentProgress,
            int goalProgress,
            boolean completedNow,
            int xpGained,
            int levelsGained,
            int newLevel
    ) {
        static ProgressResult failed(String message) {
            return new ProgressResult(false, message, 0, 0, false, 0, 0, 0);
        }

        static ProgressResult success(String message, int currentProgress, int goalProgress, boolean completedNow, int xpGained, int levelsGained, int newLevel) {
            return new ProgressResult(true, message, currentProgress, goalProgress, completedNow, xpGained, levelsGained, newLevel);
        }
    }

    public record ManagementResult(boolean success, String message, int xp, int level) {
        static ManagementResult failed(String message) {
            return new ManagementResult(false, message, 0, 0);
        }

        static ManagementResult success(String message, int xp, int level) {
            return new ManagementResult(true, message, xp, level);
        }
    }

    public record LaneManagementResult(boolean success, String message) {
        static LaneManagementResult failed(String message) {
            return new LaneManagementResult(false, message);
        }

        static LaneManagementResult success(String message) {
            return new LaneManagementResult(true, message);
        }
    }

    public record LaneStatusResult(boolean success, String message, String setId, List<LaneSnapshot> lanes) {
        static LaneStatusResult failed(String message) {
            return new LaneStatusResult(false, message, "none", List.of());
        }

        static LaneStatusResult success(String setId, List<LaneSnapshot> lanes) {
            return new LaneStatusResult(true, "ok", setId, lanes);
        }
    }

    public record StatusResult(String setId, int level, int xp, int xpPerLevel, int currentWeek) {
    }

    public record LaneSnapshot(String id, String displayName, boolean unlocked, int highestClaimedLevel) {
    }

    public record TaskSnapshot(
            String id,
            String description,
            boolean weekly,
            int week,
            String tier,
            int xpReward,
            int progressCurrent,
            int progressGoal,
            boolean completed,
            boolean completableNow,
            String status
    ) {
    }

    public record ScreenSnapshot(
            boolean hasActiveSet,
            String setId,
            long startsAtMillis,
            long endsAtMillis,
            int currentWeek,
            int totalWeeks,
            int level,
            int xp,
            int xpPerLevel,
            int weeklyCompleted,
            int weeklyCap,
            int permanentCompleted,
            int permanentCap,
            int unclaimedRewardLevels,
            List<LaneSnapshot> lanes,
            List<TaskSnapshot> tasks
    ) {
        static ScreenSnapshot none() {
            return new ScreenSnapshot(
                    false,
                    "none",
                    0L,
                    0L,
                    0,
                    0,
                    0,
                    0,
                    1,
                    0,
                    0,
                    0,
                    0,
                    0,
                    List.of(),
                    List.of()
            );
        }
    }
}
