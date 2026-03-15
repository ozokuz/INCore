package ozokuz.incore.integration.ldlib.ui.player;

import java.util.Comparator;
import java.util.List;
import ozokuz.incore.features.playerlevel.network.PlayerLevelClientCache;

final class PlayerLevelRewardsUiState {
    private int selectedLevel = -1;
    private boolean pendingInitialFocus = true;

    List<PlayerLevelClientCache.RewardPreview> orderedPreviews() {
        return PlayerLevelClientCache.getRewardPreviews().stream()
                .sorted(Comparator.comparingInt(PlayerLevelClientCache.RewardPreview::level).reversed())
                .toList();
    }

    void ensureSelection() {
        List<PlayerLevelClientCache.RewardPreview> ordered = orderedPreviews();
        if (ordered.isEmpty()) {
            this.selectedLevel = -1;
            this.pendingInitialFocus = false;
            return;
        }
        if (indexForSelectedLevel(ordered) < 0) {
            focusNextLevel(ordered);
        }
    }

    PlayerLevelClientCache.RewardPreview selectedPreview() {
        ensureSelection();
        List<PlayerLevelClientCache.RewardPreview> ordered = orderedPreviews();
        for (PlayerLevelClientCache.RewardPreview preview : ordered) {
            if (preview.level() == this.selectedLevel) {
                return preview;
            }
        }
        return null;
    }

    int selectedLevel() {
        ensureSelection();
        return this.selectedLevel;
    }

    void setSelectedLevel(int level) {
        this.selectedLevel = level;
    }

    boolean pendingInitialFocus() {
        return this.pendingInitialFocus;
    }

    void clearPendingInitialFocus() {
        this.pendingInitialFocus = false;
    }

    private void focusNextLevel(List<PlayerLevelClientCache.RewardPreview> ordered) {
        int nextLevel = PlayerLevelClientCache.getLevel() + 1;
        int index = -1;
        for (int i = 0; i < ordered.size(); i++) {
            if (ordered.get(i).level() == nextLevel) {
                index = i;
                break;
            }
        }

        if (index < 0) {
            index = ordered.size() - 1;
        }

        this.selectedLevel = ordered.get(index).level();
        this.pendingInitialFocus = true;
    }

    int indexForSelectedLevel(List<PlayerLevelClientCache.RewardPreview> ordered) {
        for (int i = 0; i < ordered.size(); i++) {
            if (ordered.get(i).level() == this.selectedLevel) {
                return i;
            }
        }
        return -1;
    }
}
