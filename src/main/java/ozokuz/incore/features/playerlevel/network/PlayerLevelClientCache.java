package ozokuz.incore.features.playerlevel.network;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PlayerLevelClientCache {
    private static int level;
    private static int currentExperience;
    private static int experienceToNextLevel = 1;
    private static List<RewardPreview> rewardPreviews = List.of();
    private static Map<String, FeatureState> featureStates = Map.of();

    private PlayerLevelClientCache() {
    }

    public static synchronized void update(
            int nextLevel,
            int nextCurrentExperience,
            int nextExperienceToNextLevel,
            List<FeatureState> nextFeatureStates,
            List<RewardPreview> nextRewardPreviews
    ) {
        level = Math.max(0, nextLevel);
        currentExperience = Math.max(0, nextCurrentExperience);
        experienceToNextLevel = Math.max(1, nextExperienceToNextLevel);
        rewardPreviews = nextRewardPreviews.stream()
                .map(preview -> new RewardPreview(
                        preview.level(),
                        Math.max(1, preview.requiredExperience()),
                        preview.rewards().stream()
                                .map(reward -> new RewardEntry(
                                        reward.kind(),
                                        reward.iconItemId(),
                                        reward.amount(),
                                        reward.text()
                                ))
                                .toList()
                ))
                .toList();
        Map<String, FeatureState> nextStates = new LinkedHashMap<>();
        for (FeatureState nextFeatureState : nextFeatureStates) {
            nextStates.put(nextFeatureState.id(), nextFeatureState);
        }
        featureStates = Map.copyOf(nextStates);
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

    public static synchronized boolean isFeatureUnlocked(String id) {
        FeatureState state = featureStates.get(id);
        return state != null && state.unlocked();
    }

    public static synchronized int getFeatureRequiredLevel(String id) {
        FeatureState state = featureStates.get(id);
        return state == null ? 0 : state.requiredLevel();
    }

    public static synchronized String getFeatureDisplayName(String id) {
        FeatureState state = featureStates.get(id);
        return state == null ? id : state.displayName();
    }

    public record RewardPreview(int level, int requiredExperience, List<RewardEntry> rewards) {
    }

    public record RewardEntry(int kind, String iconItemId, int amount, String text) {
    }

    public record FeatureState(String id, int requiredLevel, boolean unlocked, String displayName) {
    }
}
