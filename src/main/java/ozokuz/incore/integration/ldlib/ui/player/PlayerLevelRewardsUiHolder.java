package ozokuz.incore.integration.ldlib.ui.player;

import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.DataBindingBuilder;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import ozokuz.incore.features.playerlevel.PlayerFeatureUnlockDefinition;
import ozokuz.incore.features.playerlevel.PlayerFeatureUnlockManager;
import ozokuz.incore.features.playerlevel.PlayerLevelManager;
import ozokuz.incore.features.playerlevel.PlayerLevelReward;
import ozokuz.incore.features.playerlevel.PlayerLevelRewardManager;
import ozokuz.incore.integration.ldlib.ui.INCoreLdLibUiScaffold;
import ozokuz.incore.integration.ldlib.ui.INCorePlayerUiNavigator;

public final class PlayerLevelRewardsUiHolder implements com.lowdragmc.lowdraglib2.gui.factory.PlayerUIMenuType.PlayerUIHolder {
    private static final int PREVIEW_ROWS = 8;

    @Override
    public ModularUI createUI(Player player) {
        var window = INCoreLdLibUiScaffold.createWindow(Component.translatable("screen.incore.player_level_rewards.title"), 430, 290);

        Label currentLevel = INCoreLdLibUiScaffold.wrappedLabel(Component.literal("..."));
        currentLevel.bind(DataBindingBuilder.componentS2C(() -> currentLevelLine(player)).build());

        Label currentProgress = INCoreLdLibUiScaffold.wrappedLabel(Component.literal("..."));
        currentProgress.bind(DataBindingBuilder.componentS2C(() -> currentProgressLine(player)).build());

        window.body().addChildren(currentLevel, currentProgress);

        var previewSection = INCoreLdLibUiScaffold.createSection(
                Component.translatable("screen.incore.player_level_rewards.sidebar_title")
        );
        for (int index = 0; index < PREVIEW_ROWS; index++) {
            Label previewLabel = INCoreLdLibUiScaffold.wrappedLabel(Component.literal("..."));
            int previewIndex = index;
            previewLabel.bind(DataBindingBuilder.componentS2C(() -> previewLine(player, previewIndex)).build());
            previewSection.body().addChild(previewLabel);
        }

        Button backButton = INCoreLdLibUiScaffold.actionButton(Component.translatable("gui.back"));
        backButton.setOnServerClick(event -> {
            if (player instanceof ServerPlayer serverPlayer) {
                INCorePlayerUiNavigator.goBack(serverPlayer);
            }
        });

        window.body().addChildren(previewSection.root(), backButton);
        return INCoreLdLibUiScaffold.build(player, window.root());
    }

    private static Component currentLevelLine(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return Component.literal("...");
        }
        return Component.translatable("screen.incore.player_level_rewards.current_level", PlayerLevelManager.getLevel(serverPlayer));
    }

    private static Component currentProgressLine(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return Component.literal("...");
        }
        return Component.translatable(
                "screen.incore.player_level_rewards.current_progress",
                PlayerLevelManager.getCurrentExperience(serverPlayer),
                PlayerLevelManager.getExperienceToNextLevel(serverPlayer)
        );
    }

    private static Component previewLine(Player player, int previewIndex) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return Component.literal("...");
        }

        int previewLevel = PlayerLevelManager.getLevel(serverPlayer) + previewIndex + 1;
        int requiredExperience = PlayerLevelManager.getExperienceToNextLevel(previewLevel - 1);
        List<String> entries = new ArrayList<>();
        for (PlayerLevelReward reward : PlayerLevelRewardManager.getRewardsForLevel(previewLevel)) {
            entries.add(reward.previewText());
        }
        for (PlayerFeatureUnlockDefinition unlock : PlayerFeatureUnlockManager.unlocksForLevel(previewLevel)) {
            entries.add(unlock.displayName());
        }

        Component prefix = Component.translatable(
                "screen.incore.player_level_rewards.level_row",
                previewLevel,
                requiredExperience
        );
        if (entries.isEmpty()) {
            return prefix.copy().append(Component.literal(" - "))
                    .append(Component.translatable("screen.incore.player_level_rewards.level_empty"));
        }
        return prefix.copy().append(Component.literal(" - " + String.join(", ", entries)));
    }
}
