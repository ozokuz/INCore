package ozokuz.incore.integration.ldlib.ui.player;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ProgressBar;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import dev.vfyjxf.taffy.style.TaffyPosition;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import ozokuz.incore.client.ui.UIScreenTheme;
import ozokuz.incore.features.playerlevel.network.PlayerLevelClientCache;
import ozokuz.incore.integration.ldlib.ui.elements.ClippedTextureProgressBar;

final class PlayerLevelRewardsHeroElement extends UIElement {
    private final PlayerLevelRewardsUiState state;
    private final ProgressBar experienceBar;

    PlayerLevelRewardsHeroElement(PlayerLevelRewardsUiState state) {
        this.state = state;
        this.experienceBar = createExperienceBar();
        internalSetup();
        setAllowHitTest(false);
        addChild(this.experienceBar);
    }

    private static ProgressBar createExperienceBar() {
        ProgressBar progressBar = new ClippedTextureProgressBar(
                PlayerLevelRewardsUiSupport.XP_BAR_BACKGROUND_TEXTURE,
                PlayerLevelRewardsUiSupport.XP_BAR_PROGRESS_TEXTURE
        ).setRange(0.0F, 1.0F);
        progressBar.setAllowHitTest(false).layout(layout -> {
            layout.positionType(TaffyPosition.ABSOLUTE);
            layout.left(10);
            layout.right(14);
            layout.top(51);
            layout.height(PlayerLevelRewardsUiSupport.XP_BAR_HEIGHT);
        });
        return progressBar;
    }

    @Override
    public void drawBackgroundAdditional(GUIContext guiContext) {
        int width = Math.round(getSizeWidth());
        int height = Math.round(getSizeHeight());
        if (width <= 0 || height <= 0) {
            return;
        }

        this.state.ensureSelection();
        int left = Math.round(getPositionX());
        int top = Math.round(getPositionY());
        int x = left + 10;
        int y = top + 8;
        int contentWidth = width - 20;

        int currentLevel = PlayerLevelClientCache.getLevel();
        int currentExperience = PlayerLevelClientCache.getCurrentExperience();
        int experienceToNextLevel = PlayerLevelClientCache.getExperienceToNextLevel();

        float pulse = 0.5F + 0.5F * Mth.sin((System.currentTimeMillis() % 4000L) / 220.0F);
        int glowAlpha = 36 + Math.round(26 * pulse);
        guiContext.graphics.fill(
                left + 1,
                top + 1,
                left + width - 1,
                top + 2,
                PlayerLevelRewardsUiSupport.withAlpha(UIScreenTheme.Info.PLR_HERO_GLOW, glowAlpha)
        );

        guiContext.graphics.drawString(
                PlayerLevelRewardsUiSupport.font(),
                Component.translatable("screen.incore.player_level_rewards.current_status"),
                x,
                y,
                PlayerLevelRewardsUiSupport.COLOR_TEXT_PRIMARY,
                false
        );

        Component levelChip = Component.translatable("screen.incore.player_level_rewards.current_level", currentLevel);
        PlayerLevelRewardsUiSupport.themed(guiContext.graphics).drawChipLeft(
                x,
                y + 12,
                levelChip,
                UIScreenTheme.Info.PLR_CHIP_FILL_DEFAULT,
                UIScreenTheme.Info.PLR_CHIP_TEXT_LIGHT
        );

        guiContext.graphics.drawString(
                PlayerLevelRewardsUiSupport.font(),
                Component.translatable("screen.incore.player_level_rewards.current_progress", currentExperience, experienceToNextLevel),
                x,
                y + 30,
                PlayerLevelRewardsUiSupport.COLOR_TEXT_SECONDARY,
                false
        );

        float progress = (float) currentExperience / (float) Math.max(1, experienceToNextLevel);
        this.experienceBar.setProgress(progress);

        Component focusLine = this.state.selectedLevel() > 0
                ? Component.translatable("screen.incore.player_level_rewards.focus_level", this.state.selectedLevel())
                : Component.translatable("screen.incore.player_level_rewards.none");
        int focusWidth = PlayerLevelRewardsUiSupport.font().width(focusLine);
        int focusX = left + width - focusWidth - 12;
        guiContext.graphics.drawString(
                PlayerLevelRewardsUiSupport.font(),
                focusLine,
                focusX,
                y + 14,
                UIScreenTheme.Info.PLR_FOCUS_LINE_TEXT,
                false
        );
    }
}
