package io.github.ozokuz.incore.features.playerlevel.network;

import java.util.ArrayList;
import java.util.List;

public final class PlayerLevelClientCache {
    private static int level;
    private static int currentExperience;
    private static int experienceToNextLevel = 1;
    private static List<RewardPreview> rewardPreviews = List.of();

    private PlayerLevelClientCache() {
    }

    public static synchronized void update(int nextLevel, int nextCurrentExperience, int nextExperienceToNextLevel, List<RewardPreview> nextRewardPreviews) {
        level = Math.max(0, nextLevel);
        currentExperience = Math.max(0, nextCurrentExperience);
        experienceToNextLevel = Math.max(1, nextExperienceToNextLevel);
        rewardPreviews = nextRewardPreviews.stream()
                .map(preview -> new RewardPreview(
                        preview.level(),
                        Math.max(1, preview.requiredExperience()),
                        List.copyOf(preview.rewards())
                ))
                .toList();
    }

    public static synchronized int getLevel() {
        return level;
    }

    public static synchronized int getCurrentExperience() {
        return Math.min(currentExperience, Math.max(0, experienceToNextLevel - 1));
    }

    public static synchronized int getExperienceToNextLevel() {
        return experienceToNextLevel;
    }

    public static synchronized List<RewardPreview> getRewardPreviews() {
        return new ArrayList<>(rewardPreviews);
    }

    public record RewardPreview(int level, int requiredExperience, List<String> rewards) {
    }
}
