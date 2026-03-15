package ozokuz.incore.integration.ldlib.ui.player;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import ozokuz.incore.client.ui.UIScreenTheme;
import ozokuz.incore.features.playerlevel.network.PlayerLevelClientCache;

final class PlayerLevelRewardsRailElement extends UIElement {
    private final PlayerLevelRewardsUiState state;
    private final ScrollerView scrollerView;
    private int lastPreviewCount = -1;

    PlayerLevelRewardsRailElement(PlayerLevelRewardsUiState state, ScrollerView scrollerView) {
        this.state = state;
        this.scrollerView = scrollerView;
        internalSetup();
        addEventListener(UIEvents.CLICK, event -> {
            if (event.button != 0) {
                return;
            }

            List<PlayerLevelClientCache.RewardPreview> ordered = this.state.orderedPreviews();
            this.state.ensureSelection();
            int row = (int) ((event.y - getPositionY()) / PlayerLevelRewardsUiSupport.LEVEL_CARD_HEIGHT);
            int index = Math.clamp(row, 0, ordered.size() - 1);
            if (index >= 0 && index < ordered.size()) {
                this.state.setSelectedLevel(ordered.get(index).level());
                event.stopPropagation();
            }
        });
        addEventListener(UIEvents.TICK, event -> updateContentMetrics());
    }

    @Override
    public void drawBackgroundAdditional(GUIContext guiContext) {
        int width = Math.round(getSizeWidth());
        int height = Math.round(getSizeHeight());
        if (width <= 0 || height <= 0) {
            return;
        }

        List<PlayerLevelClientCache.RewardPreview> ordered = this.state.orderedPreviews();
        this.state.ensureSelection();

        var font = PlayerLevelRewardsUiSupport.font();
        float pulse = 0.5F + 0.5F * Mth.sin((System.currentTimeMillis() % 4000L) / 220.0F);
        int mouseX = guiContext.mouseX;
        int mouseY = guiContext.mouseY;

        if (ordered.isEmpty()) {
            guiContext.graphics.drawString(
                    font,
                    Component.translatable("screen.incore.player_level_rewards.none"),
                    Math.round(getPositionX()),
                    Math.round(getPositionY()) + 2,
                    PlayerLevelRewardsUiSupport.COLOR_TEXT_MUTED,
                    false
            );
            return;
        }

        int rowX = Math.round(getPositionX());
        int rowRight = rowX + width;
        for (int index = 0; index < ordered.size(); index++) {
            PlayerLevelClientCache.RewardPreview preview = ordered.get(index);
            int rowY = Math.round(getPositionY()) + index * PlayerLevelRewardsUiSupport.LEVEL_CARD_HEIGHT;
            int rowBottom = rowY + PlayerLevelRewardsUiSupport.LEVEL_CARD_HEIGHT - 2;

            boolean hovered = mouseX >= rowX && mouseX < rowRight && mouseY >= rowY && mouseY < rowBottom;
            boolean selected = preview.level() == this.state.selectedLevel();
            boolean reached = preview.level() <= PlayerLevelClientCache.getLevel();

            int rowFill = selected
                    ? (reached ? UIScreenTheme.Info.PLR_ROW_FILL_REACHED_SELECTED : UIScreenTheme.Info.PLR_ROW_FILL_SELECTED)
                    : (reached ? UIScreenTheme.Info.PLR_ROW_FILL_REACHED : UIScreenTheme.Info.PLR_ROW_FILL_IDLE);
            if (hovered && !selected) {
                rowFill = reached ? UIScreenTheme.Info.PLR_ROW_FILL_REACHED_HOVER : UIScreenTheme.Info.PLR_ROW_FILL_CLAIMED;
            }
            guiContext.graphics.fill(rowX, rowY, rowRight, rowBottom, rowFill);

            int borderColor = selected
                    ? PlayerLevelRewardsUiSupport.withAlpha(
                            reached ? UIScreenTheme.Info.PLR_ROW_BORDER_REACHED_SELECTED : UIScreenTheme.Info.PLR_ROW_BORDER_SELECTED_GLOW,
                            140 + Math.round(70 * pulse)
                    )
                    : (hovered
                            ? (reached ? UIScreenTheme.Info.PLR_ROW_BORDER_REACHED_HOVER : UIScreenTheme.Info.PLR_ROW_BORDER_HOVER)
                            : (reached ? UIScreenTheme.Info.PLR_ROW_BORDER_REACHED : UIScreenTheme.Info.PLR_ROW_BORDER_IDLE));
            PlayerLevelRewardsUiSupport.themed(guiContext.graphics).drawBorder(rowX, rowY, rowRight, rowBottom, borderColor);

            int accent = reached
                    ? (selected ? UIScreenTheme.Info.PLR_ROW_ACCENT_REACHED_SELECTED : UIScreenTheme.Info.PLR_ROW_ACCENT_REACHED)
                    : (selected ? UIScreenTheme.Info.PLR_ROW_ACCENT_SELECTED : UIScreenTheme.Info.PLR_ROW_ACCENT_IDLE);
            guiContext.graphics.fill(rowX, rowY, rowX + 3, rowBottom, accent);

            renderSidebarLevelMarker(guiContext, preview, rowX + 8, rowY + 5, reached);

            var levelText = Component.translatable("screen.incore.player_level_rewards.sidebar_level", preview.level());
            int levelTextColor = reached
                    ? (selected ? UIScreenTheme.Info.PLR_ROW_TEXT_REACHED_SELECTED : UIScreenTheme.Info.PLR_ROW_TEXT_REACHED)
                    : (selected ? UIScreenTheme.Info.PLR_ROW_TEXT_SELECTED : UIScreenTheme.Info.PLR_ROW_TEXT_IDLE);
            guiContext.graphics.drawString(font, levelText, rowX + 30, rowY + 6, levelTextColor, false);

            var xpText = Component.translatable("screen.incore.player_level_rewards.sidebar_xp", preview.requiredExperience());
            int xpWidth = font.width(xpText) + 8;
            int xpX = rowRight - xpWidth - 5;
            int xpY = rowY + 5;
            int xpFill = reached
                    ? (selected ? UIScreenTheme.Info.PLR_XP_PILL_FILL_REACHED_SELECTED : UIScreenTheme.Info.PLR_XP_PILL_FILL_REACHED)
                    : (selected ? UIScreenTheme.Info.PLR_XP_PILL_FILL_SELECTED : UIScreenTheme.Info.PLR_XP_PILL_FILL_IDLE);
            guiContext.graphics.fill(xpX, xpY, xpX + xpWidth, xpY + 11, xpFill);
            guiContext.graphics.drawString(
                    font,
                    xpText,
                    xpX + 4,
                    xpY + 2,
                    reached ? UIScreenTheme.Info.PLR_XP_PILL_TEXT_REACHED : UIScreenTheme.Info.PLR_XP_PILL_TEXT,
                    false
            );
        }
    }

    private void updateContentMetrics() {
        List<PlayerLevelClientCache.RewardPreview> ordered = this.state.orderedPreviews();
        int previewCount = ordered.size();
        int targetHeight = ordered.isEmpty()
                ? PlayerLevelRewardsUiSupport.font().lineHeight + 4
                : previewCount * PlayerLevelRewardsUiSupport.LEVEL_CARD_HEIGHT;
        if (previewCount != this.lastPreviewCount || Math.round(getSizeHeight()) != targetHeight) {
            getLayout().height(targetHeight);
            this.lastPreviewCount = previewCount;
        }

        this.state.ensureSelection();
        if (!this.state.pendingInitialFocus() || previewCount <= 0) {
            return;
        }

        int visibleRows = Math.max(1, (int) (this.scrollerView.viewPort.getContentHeight() / PlayerLevelRewardsUiSupport.LEVEL_CARD_HEIGHT));
        if (visibleRows <= 0) {
            return;
        }

        int selectedIndex = this.state.indexForSelectedLevel(ordered);
        if (selectedIndex < 0) {
            return;
        }

        int maxScrollRows = Math.max(0, previewCount - visibleRows);
        int desiredScrollRows = Math.max(0, selectedIndex - visibleRows + 1);
        float normalized = maxScrollRows <= 0 ? 0.0F : (float) desiredScrollRows / (float) maxScrollRows;
        this.scrollerView.verticalScroller.setNormalizedValue(normalized, false);
        this.state.clearPendingInitialFocus();
    }

    private void renderSidebarLevelMarker(
            GUIContext guiContext,
            PlayerLevelClientCache.RewardPreview preview,
            int x,
            int y,
            boolean reached
    ) {
        int boxRight = x + 16;
        int boxBottom = y + 16;
        guiContext.graphics.fill(
                x - 1,
                y - 1,
                boxRight + 1,
                boxBottom + 1,
                reached ? UIScreenTheme.Info.PLR_LEVEL_MARKER_BORDER_REACHED : UIScreenTheme.Info.PLR_LEVEL_MARKER_BORDER
        );
        guiContext.graphics.fill(
                x,
                y,
                boxRight,
                boxBottom,
                reached ? UIScreenTheme.Info.PLR_LEVEL_MARKER_FILL_REACHED : UIScreenTheme.Info.PLR_LEVEL_MARKER_FILL
        );
        if (reached) {
            guiContext.graphics.drawCenteredString(PlayerLevelRewardsUiSupport.font(), Component.literal("\u2713"), x + 8, y + 4, UIScreenTheme.Info.PLR_LEVEL_MARKER_CHECK);
            return;
        }

        PlayerLevelClientCache.RewardEntry majorReward = preview.rewards().isEmpty() ? null : preview.rewards().getFirst();
        if (majorReward != null) {
            guiContext.graphics.renderItem(PlayerLevelRewardsUiSupport.iconStackFor(majorReward), x, y);
        }
    }
}
