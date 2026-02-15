package io.github.ozokuz.incore.features.battlepass;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public final class BattlePassProgressManager {
    private static final String KEY_ROOT = "incore:battlepass";
    private static final String KEY_PROGRESS_BY_SET = "sets";
    private static final String KEY_XP = "xp";
    private static final String KEY_LEVEL = "level";
    private static final String KEY_HIGHEST_REWARDED_LEVEL = "highest_rewarded";
    private static final String KEY_COMPLETED_TASKS = "completed_tasks";

    private BattlePassProgressManager() {
    }

    public static CompletionResult completeTask(ServerPlayer player, String taskId, Instant now) {
        Optional<BattlePassDefinition> activeOptional = BattlePassManager.getActiveSet(now);
        if (activeOptional.isEmpty()) {
            return CompletionResult.failed("No active battle pass set.");
        }

        BattlePassDefinition active = activeOptional.get();
        Optional<BattlePassDefinition.BattlePassTask> taskOptional = active.findTask(taskId);
        if (taskOptional.isEmpty()) {
            return CompletionResult.failed("Task not found in the active battle pass set.");
        }

        BattlePassDefinition.BattlePassTask task = taskOptional.get();
        int activeWeek = getActiveWeek(active, now);
        if (task.type() == BattlePassDefinition.TaskType.WEEKLY && task.week() != activeWeek) {
            return CompletionResult.failed("Task can only be completed during week " + task.week() + ". Current week: " + activeWeek + ".");
        }

        PlayerSetProgress progress = getProgress(player, active);
        if (progress.completedTasks().contains(task.id())) {
            return CompletionResult.failed("Task was already completed.");
        }

        if (task.type() == BattlePassDefinition.TaskType.WEEKLY) {
            int totalWeeklyTasks = (int) active.tasks().stream()
                    .filter(candidate -> candidate.type() == BattlePassDefinition.TaskType.WEEKLY && candidate.week() == task.week())
                    .count();
            int weeklyCap = Math.max(1, totalWeeklyTasks / 2);
            long completedThisWeek = progress.completedTasks().stream()
                    .map(active::findTask)
                    .flatMap(Optional::stream)
                    .filter(candidate -> candidate.type() == BattlePassDefinition.TaskType.WEEKLY && candidate.week() == task.week())
                    .count();

            if (completedThisWeek >= weeklyCap) {
                return CompletionResult.failed("Weekly completion cap reached (" + weeklyCap + ").");
            }
        }

        if (task.type() == BattlePassDefinition.TaskType.PERMANENT) {
            int totalPermanentTasks = (int) active.tasks().stream()
                    .filter(candidate -> candidate.type() == BattlePassDefinition.TaskType.PERMANENT)
                    .count();
            int permanentCap = Math.max(1, totalPermanentTasks / 2);
            long completedPermanent = progress.completedTasks().stream()
                    .map(active::findTask)
                    .flatMap(Optional::stream)
                    .filter(candidate -> candidate.type() == BattlePassDefinition.TaskType.PERMANENT)
                    .count();

            if (completedPermanent >= permanentCap) {
                return CompletionResult.failed("Permanent task completion cap reached (" + permanentCap + ").");
            }
        }

        int xpGained = active.xpForTask(task);
        progress.completedTasks().add(task.id());
        progress.xp += xpGained;

        int previousLevel = progress.level;
        progress.level = Math.max(0, progress.xp / active.xpPerLevel());
        grantRewards(player, active, progress, previousLevel + 1, progress.level);
        saveProgress(player, active, progress);

        return CompletionResult.success(xpGained, progress.level - previousLevel, progress.level);
    }

    public static StatusResult getStatus(ServerPlayer player, Instant now) {
        Optional<BattlePassDefinition> activeOptional = BattlePassManager.getActiveSet(now);
        if (activeOptional.isEmpty()) {
            return new StatusResult("none", 0, 0, 0, 0);
        }

        BattlePassDefinition active = activeOptional.get();
        PlayerSetProgress progress = getProgress(player, active);
        int currentWeek = getActiveWeek(active, now);
        return new StatusResult(active.id().toString(), progress.level, progress.xp, active.xpPerLevel(), currentWeek);
    }

    private static int getActiveWeek(BattlePassDefinition definition, Instant now) {
        if (now.isBefore(definition.startsAt())) {
            return 1;
        }

        long elapsedSeconds = Duration.between(definition.startsAt(), now).getSeconds();
        long week = elapsedSeconds / (7L * 24L * 60L * 60L) + 1L;
        return (int) Math.max(1L, Math.min(definition.durationWeeks(), week));
    }

    private static PlayerSetProgress getProgress(ServerPlayer player, BattlePassDefinition definition) {
        CompoundTag setTag = getOrCreateSetTag(player, definition.id().toString());
        int xp = Math.max(0, setTag.getInt(KEY_XP));
        int level = Math.max(0, setTag.getInt(KEY_LEVEL));
        int highestRewarded = Math.max(0, setTag.getInt(KEY_HIGHEST_REWARDED_LEVEL));

        Set<String> completedTasks = new HashSet<>();
        ListTag completedTag = setTag.getList(KEY_COMPLETED_TASKS, Tag.TAG_STRING);
        for (int i = 0; i < completedTag.size(); i++) {
            completedTasks.add(completedTag.getString(i));
        }

        return new PlayerSetProgress(xp, level, highestRewarded, completedTasks);
    }

    private static void saveProgress(ServerPlayer player, BattlePassDefinition definition, PlayerSetProgress progress) {
        CompoundTag setTag = getOrCreateSetTag(player, definition.id().toString());
        setTag.putInt(KEY_XP, Math.max(0, progress.xp));
        setTag.putInt(KEY_LEVEL, Math.max(0, progress.level));
        setTag.putInt(KEY_HIGHEST_REWARDED_LEVEL, Math.max(0, progress.highestRewardedLevel));

        ListTag completed = new ListTag();
        progress.completedTasks.stream().sorted().map(StringTag::valueOf).forEach(completed::add);
        setTag.put(KEY_COMPLETED_TASKS, completed);
    }

    private static void grantRewards(ServerPlayer player, BattlePassDefinition definition, PlayerSetProgress progress, int fromInclusive, int toInclusive) {
        if (toInclusive < fromInclusive) {
            return;
        }

        int start = Math.max(fromInclusive, progress.highestRewardedLevel + 1);
        for (int level = start; level <= toInclusive; level++) {
            for (BattlePassReward reward : definition.rewardsByLevel().getOrDefault(level, java.util.List.of())) {
                reward.grant(player);
            }
            progress.highestRewardedLevel = level;
        }
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

    private static class PlayerSetProgress {
        private int xp;
        private int level;
        private int highestRewardedLevel;
        private final Set<String> completedTasks;

        private PlayerSetProgress(int xp, int level, int highestRewardedLevel, Set<String> completedTasks) {
            this.xp = xp;
            this.level = level;
            this.highestRewardedLevel = highestRewardedLevel;
            this.completedTasks = completedTasks;
        }

        public Set<String> completedTasks() {
            return completedTasks;
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

    public record StatusResult(String setId, int level, int xp, int xpPerLevel, int currentWeek) {
    }
}
