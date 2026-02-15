package io.github.ozokuz.incore.features.tasks.client;

import com.google.gson.Gson;

import java.util.List;

public final class TaskClientCache {
    private static final Gson GSON = new Gson();
    private static volatile TaskSnapshot snapshot = new TaskSnapshot(List.of(), List.of(), 0, false, List.of());

    private TaskClientCache() {
    }

    public static void update(String json) {
        try {
            TaskSnapshot parsed = GSON.fromJson(json, TaskSnapshot.class);
            snapshot = parsed == null ? snapshot : parsed;
        } catch (Exception ignored) {
        }
    }

    public static TaskSnapshot snapshot() {
        return snapshot;
    }

    public record TaskSnapshot(List<TaskEntry> daily, List<TaskEntry> weekly, int weeklyPoints, boolean dailyCompleted, List<TierEntry> tiers) {
    }

    public record TaskEntry(String title, String description, int goal, int progress, String difficulty, int points) {
    }

    public record TierEntry(int tier, int requiredPoints, boolean unlocked, boolean claimed) {
    }
}
