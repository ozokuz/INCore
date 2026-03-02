package io.github.ozokuz.incore.client.features.tasks;

import com.google.gson.Gson;

import java.util.List;

public final class TaskClientCache {
    private static final Gson GSON = new Gson();
    private static volatile TaskSnapshot snapshot = new TaskSnapshot(List.of(), List.of(), 0, false, false, List.of(), List.of(), List.of(), 0, false, false);

    private TaskClientCache() {
    }

    public static void update(String json) {
        try {
            TaskSnapshot parsed = GSON.fromJson(json, TaskSnapshot.class);
            snapshot = parsed == null ? snapshot : normalize(parsed);
        } catch (Exception ignored) {
        }
    }

    private static TaskSnapshot normalize(TaskSnapshot parsed) {
        List<TaskEntry> daily = parsed.daily() == null ? List.of() : parsed.daily();
        List<TaskEntry> weekly = parsed.weekly() == null ? List.of() : parsed.weekly();
        List<RewardEntry> dailyRewards = parsed.dailyRewards() == null ? List.of() : parsed.dailyRewards();
        List<TierEntry> tiers = parsed.tiers() == null ? List.of() : parsed.tiers().stream()
                .map(tier -> new TierEntry(
                        tier.tier(),
                        tier.requiredPoints(),
                        tier.unlocked(),
                        tier.claimed(),
                        tier.rewards() == null ? List.of() : tier.rewards()
                ))
                .toList();
        List<DailyTaskEntry> fixedDailyTasks = parsed.fixedDailyTasks() == null ? List.of() : parsed.fixedDailyTasks();
        return new TaskSnapshot(
                daily,
                weekly,
                parsed.weeklyPoints(),
                parsed.dailyCompleted(),
                parsed.dailyRewardClaimed(),
                dailyRewards,
                tiers,
                fixedDailyTasks,
                parsed.fixedDailyCompleted(),
                parsed.fixedDailyAllCompleted(),
                parsed.fixedDailyRewardClaimed()
        );
    }

    public static TaskSnapshot snapshot() {
        return snapshot;
    }

    public record TaskSnapshot(
            List<TaskEntry> daily,
            List<TaskEntry> weekly,
            int weeklyPoints,
            boolean dailyCompleted,
            boolean dailyRewardClaimed,
            List<RewardEntry> dailyRewards,
            List<TierEntry> tiers,
            List<DailyTaskEntry> fixedDailyTasks,
            int fixedDailyCompleted,
            boolean fixedDailyAllCompleted,
            boolean fixedDailyRewardClaimed
    ) {
    }

    public record TaskEntry(String title, String description, int goal, int progress, String difficulty, int points) {
    }

    public record DailyTaskEntry(String id, String title, int goal, int progress) {
    }

    public record TierEntry(int tier, int requiredPoints, boolean unlocked, boolean claimed, List<RewardEntry> rewards) {
    }

    public record RewardEntry(String kind, String itemId, int amount, String text) {
    }
}
