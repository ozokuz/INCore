package io.github.ozokuz.incore.features.battlepass;

import net.minecraft.resources.ResourceLocation;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public record BattlePassDefinition(
        ResourceLocation id,
        Instant startsAt,
        Instant endsAt,
        int xpPerLevel,
        Map<String, Integer> tierXp,
        List<BattlePassTask> tasks,
        Map<Integer, List<BattlePassReward>> rewardsByLevel
) {
    public boolean isActive(Instant now) {
        return !now.isBefore(startsAt) && now.isBefore(endsAt);
    }

    public Optional<BattlePassTask> findTask(String taskId) {
        return tasks.stream().filter(task -> task.id().equals(taskId)).findFirst();
    }

    public int xpForTask(BattlePassTask task) {
        if (task.xpReward() > 0) {
            return task.xpReward();
        }

        return Math.max(1, tierXp.getOrDefault(task.tier(), 50));
    }

    public long durationWeeks() {
        long seconds = Math.max(0L, endsAt.getEpochSecond() - startsAt.getEpochSecond());
        return Math.max(1L, (long) Math.ceil(seconds / (7d * 24d * 60d * 60d)));
    }

    public record BattlePassTask(String id, TaskType type, int week, String tier, int xpReward, int progressGoal, String description) {
    }

    public enum TaskType {
        WEEKLY,
        PERMANENT
    }
}
