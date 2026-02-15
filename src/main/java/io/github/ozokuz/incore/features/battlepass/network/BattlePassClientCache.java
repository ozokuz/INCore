package io.github.ozokuz.incore.features.battlepass.network;

import java.util.ArrayList;
import java.util.List;

public final class BattlePassClientCache {
    private static boolean hasActiveSet;
    private static String setId = "none";
    private static long startsAtMillis;
    private static long endsAtMillis;
    private static int currentWeek;
    private static int totalWeeks;
    private static int level;
    private static int xp;
    private static int xpPerLevel = 1;
    private static int weeklyCompleted;
    private static int weeklyCap;
    private static int permanentCompleted;
    private static int permanentCap;
    private static int unclaimedRewardLevels;
    private static List<LaneEntry> lanes = List.of();
    private static List<TaskEntry> tasks = List.of();
    private static List<RewardLevelEntry> rewardLevels = List.of();

    private BattlePassClientCache() {
    }

    public static synchronized void update(
            boolean nextHasActiveSet,
            String nextSetId,
            long nextStartsAtMillis,
            long nextEndsAtMillis,
            int nextCurrentWeek,
            int nextTotalWeeks,
            int nextLevel,
            int nextXp,
            int nextXpPerLevel,
            int nextWeeklyCompleted,
            int nextWeeklyCap,
            int nextPermanentCompleted,
            int nextPermanentCap,
            int nextUnclaimedRewardLevels,
            List<LaneEntry> nextLanes,
            List<TaskEntry> nextTasks,
            List<RewardLevelEntry> nextRewardLevels
    ) {
        hasActiveSet = nextHasActiveSet;
        setId = nextSetId;
        startsAtMillis = nextStartsAtMillis;
        endsAtMillis = nextEndsAtMillis;
        currentWeek = Math.max(0, nextCurrentWeek);
        totalWeeks = Math.max(0, nextTotalWeeks);
        level = Math.max(0, nextLevel);
        xp = Math.max(0, nextXp);
        xpPerLevel = Math.max(1, nextXpPerLevel);
        weeklyCompleted = Math.max(0, nextWeeklyCompleted);
        weeklyCap = Math.max(0, nextWeeklyCap);
        permanentCompleted = Math.max(0, nextPermanentCompleted);
        permanentCap = Math.max(0, nextPermanentCap);
        unclaimedRewardLevels = Math.max(0, nextUnclaimedRewardLevels);

        lanes = nextLanes.stream()
                .map(lane -> new LaneEntry(
                        lane.id(),
                        lane.unlocked(),
                        lane.highestClaimedLevel()
                ))
                .toList();

        tasks = nextTasks.stream()
                .map(task -> new TaskEntry(
                        task.id(),
                        task.description(),
                        task.weekly(),
                        Math.max(1, task.week()),
                        task.tier(),
                        Math.max(0, task.xpReward()),
                        Math.max(0, task.progressCurrent()),
                        Math.max(1, task.progressGoal()),
                        task.completed(),
                        task.completableNow(),
                        task.status()
                ))
                .toList();

        rewardLevels = nextRewardLevels.stream()
                .map(levelEntry -> new RewardLevelEntry(
                        Math.max(0, levelEntry.level()),
                        Math.max(0, levelEntry.requiredXp()),
                        Math.max(0, levelEntry.xpForLevel()),
                        levelEntry.rewards().stream()
                                .map(reward -> new RewardEntry(
                                        reward.kind(),
                                        reward.iconItemId(),
                                        Math.max(0, reward.amount()),
                                        reward.text()
                                ))
                                .toList()
                ))
                .toList();
    }

    public static synchronized boolean hasActiveSet() {
        return hasActiveSet;
    }

    public static synchronized String getSetId() {
        return setId;
    }

    public static synchronized long getStartsAtMillis() {
        return startsAtMillis;
    }

    public static synchronized long getEndsAtMillis() {
        return endsAtMillis;
    }

    public static synchronized int getCurrentWeek() {
        return currentWeek;
    }

    public static synchronized int getTotalWeeks() {
        return totalWeeks;
    }

    public static synchronized int getLevel() {
        return level;
    }

    public static synchronized int getXp() {
        return xp;
    }

    public static synchronized int getXpPerLevel() {
        return xpPerLevel;
    }

    public static synchronized int getXpIntoCurrentLevel() {
        return xp % Math.max(1, xpPerLevel);
    }

    public static synchronized int getWeeklyCompleted() {
        return weeklyCompleted;
    }

    public static synchronized int getWeeklyCap() {
        return weeklyCap;
    }

    public static synchronized int getPermanentCompleted() {
        return permanentCompleted;
    }

    public static synchronized int getPermanentCap() {
        return permanentCap;
    }

    public static synchronized int getUnclaimedRewardLevels() {
        return unclaimedRewardLevels;
    }

    public static synchronized List<LaneEntry> getLanes() {
        return new ArrayList<>(lanes);
    }

    public static synchronized List<TaskEntry> getTasks() {
        return new ArrayList<>(tasks);
    }

    public static synchronized List<RewardLevelEntry> getRewardLevels() {
        return new ArrayList<>(rewardLevels);
    }

    public record LaneEntry(String id, boolean unlocked, int highestClaimedLevel) {
    }

    public record TaskEntry(
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

    public record RewardLevelEntry(int level, int requiredXp, int xpForLevel, List<RewardEntry> rewards) {
    }

    public record RewardEntry(int kind, String iconItemId, int amount, String text) {
    }
}
