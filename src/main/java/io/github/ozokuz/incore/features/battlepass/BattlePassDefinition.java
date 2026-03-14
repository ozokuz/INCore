package io.github.ozokuz.incore.features.battlepass;

import net.minecraft.resources.ResourceLocation;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;

public record BattlePassDefinition(
        ResourceLocation id,
        int order,
        Instant startsAt,
        int lengthWeeks,
        int xpPerLevel,
        Map<String, Integer> tierXp,
        List<BattlePassTask> tasks,
        List<String> lanes,
        Map<String, Map<Integer, List<BattlePassReward>>> rewardsByLane
) {
    public BattlePassDefinition {
        order = Math.max(0, order);
        lengthWeeks = Math.max(1, lengthWeeks);
        xpPerLevel = Math.max(1, xpPerLevel);

        Map<String, Integer> sourceTierXp = tierXp == null ? Map.of() : tierXp;
        Map<String, Integer> normalizedTierXp = new HashMap<>();
        sourceTierXp.forEach((tier, xp) -> normalizedTierXp.put(tier, Math.max(1, xp)));
        tierXp = Map.copyOf(normalizedTierXp);

        List<String> sourceLanes = lanes == null ? List.of() : lanes;
        List<String> normalizedLanes = sourceLanes.isEmpty()
                ? BattlePassLaneManager.getAllLaneIds()
                : sourceLanes.stream().map(BattlePassLane::normalize).distinct().toList();
        lanes = List.copyOf(normalizedLanes);

        Map<String, Map<Integer, List<BattlePassReward>>> sourceRewards = rewardsByLane == null ? Map.of() : rewardsByLane;
        Map<String, Map<Integer, List<BattlePassReward>>> normalizedRewards = new HashMap<>();
        for (String lane : lanes) {
            Map<Integer, List<BattlePassReward>> laneRewards = sourceRewards.getOrDefault(lane, Map.of());
            Map<Integer, List<BattlePassReward>> immutableLaneRewards = new HashMap<>();
            laneRewards.forEach((level, rewards) -> immutableLaneRewards.put(Math.max(0, level), List.copyOf(rewards)));
            normalizedRewards.put(lane, Map.copyOf(immutableLaneRewards));
        }
        rewardsByLane = Map.copyOf(normalizedRewards);

        tasks = tasks == null ? List.of() : List.copyOf(tasks);
    }

    public Instant endsAt() {
        long totalDays = Math.max(1L, (long) lengthWeeks * 7L);
        return startsAt.plus(Duration.ofDays(totalDays));
    }

    public boolean isActive(Instant now) {
        Instant end = endsAt();
        return !now.isBefore(startsAt) && now.isBefore(end);
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
        return lengthWeeks;
    }

    public BattlePassDefinition withStart(Instant newStart) {
        return new BattlePassDefinition(id, order, newStart, lengthWeeks, xpPerLevel, tierXp, tasks, lanes, rewardsByLane);
    }

    public List<BattlePassReward> rewardsForLevel(String laneId, int level) {
        String lane = BattlePassLane.normalize(laneId);
        return rewardsByLane.getOrDefault(lane, Map.of()).getOrDefault(level, List.of());
    }

    public Map<Integer, List<BattlePassReward>> rewardsByLevel(String laneId) {
        return rewardsByLane.getOrDefault(BattlePassLane.normalize(laneId), Map.of());
    }

    public List<Integer> allConfiguredRewardLevels() {
        TreeSet<Integer> levels = new TreeSet<>();
        rewardsByLane.values().forEach(laneRewards -> levels.addAll(laneRewards.keySet()));
        return List.copyOf(levels);
    }

    public record BattlePassTask(String id, TaskType type, int week, String tier, int xpReward, int progressGoal, String description, TriggerType triggerType) {
        public BattlePassTask(String id, TaskType type, int week, String tier, int xpReward, int progressGoal, String description) {
            this(id, type, week, tier, xpReward, progressGoal, description, TriggerType.NONE);
        }
    }

    public enum TaskType {
        WEEKLY,
        PERMANENT
    }

    public enum TriggerType {
        NONE,
        LOGIN,
        ENTROPY_RECOVER,
        GACHA_CRATE_OPEN,
        BANNER_PERMIT_USE,
        ARENA_COMPLETE,
        DUNGEON_COMPLETE,
        SURFACE_ORE_MINE,
        VENDING_MACHINE_PURCHASE,
        MARKET_BUY,
        MARKET_SELL,
        CARD_BOOSTER_OPEN,
        RESEARCH_COMPLETE
    }
}
