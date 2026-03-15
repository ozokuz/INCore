package ozokuz.incore.integration.ldlib.ui.player;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.event.HoverTooltips;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import ozokuz.incore.client.ui.UIScreenTheme;
import ozokuz.incore.features.playerlevel.network.PlayerLevelClientCache;
import ozokuz.incore.features.playerlevel.network.PlayerLevelSyncPayload;

final class PlayerLevelRewardsDetailsElement extends UIElement {
    private final PlayerLevelRewardsUiState state;
    private PlayerLevelClientCache.RewardEntry hoveredReward;
    private ItemStack hoveredStack = ItemStack.EMPTY;

    PlayerLevelRewardsDetailsElement(PlayerLevelRewardsUiState state) {
        this.state = state;
        internalSetup();
        addEventListener(UIEvents.HOVER_TOOLTIPS, event -> {
            if (this.hoveredReward == null) {
                return;
            }
            event.hoverTooltips = new HoverTooltips(
                    PlayerLevelRewardsUiSupport.tooltipForReward(this.hoveredReward, this.hoveredStack),
                    null,
                    PlayerLevelRewardsUiSupport.font(),
                    this.hoveredStack
            );
        });
    }

    @Override
    public void drawBackgroundAdditional(GUIContext guiContext) {
        this.hoveredReward = null;
        this.hoveredStack = ItemStack.EMPTY;

        int width = Math.round(getSizeWidth());
        int height = Math.round(getSizeHeight());
        if (width <= 0 || height <= 0) {
            return;
        }

        this.state.ensureSelection();
        PlayerLevelClientCache.RewardPreview selectedPreview = this.state.selectedPreview();

        int left = Math.round(getPositionX());
        int top = Math.round(getPositionY());
        int x = left + 10;
        int y = top + 8;

        guiContext.graphics.drawString(
                PlayerLevelRewardsUiSupport.font(),
                Component.translatable("screen.incore.player_level_rewards.details_title"),
                x,
                y,
                PlayerLevelRewardsUiSupport.COLOR_TEXT_PRIMARY,
                false
        );

        if (selectedPreview == null) {
            guiContext.graphics.drawString(
                    PlayerLevelRewardsUiSupport.font(),
                    Component.translatable("screen.incore.player_level_rewards.none"),
                    x,
                    y + 18,
                    PlayerLevelRewardsUiSupport.COLOR_TEXT_MUTED,
                    false
            );
            return;
        }

        Component levelChip = Component.translatable("screen.incore.player_level_rewards.details_level", selectedPreview.level());
        Component xpChip = Component.translatable("screen.incore.player_level_rewards.details_required_xp", selectedPreview.requiredExperience());
        PlayerLevelRewardsUiSupport.themed(guiContext.graphics).drawChipLeft(
                x,
                y + 12,
                levelChip,
                UIScreenTheme.Info.PLR_CHIP_FILL_LEVEL,
                UIScreenTheme.Info.PLR_CHIP_TEXT_LIGHT
        );

        int xpChipWidth = PlayerLevelRewardsUiSupport.font().width(xpChip) + 10;
        int xpX = left + width - xpChipWidth - 10;
        PlayerLevelRewardsUiSupport.themed(guiContext.graphics).drawChipLeft(
                xpX,
                y + 12,
                xpChip,
                UIScreenTheme.Info.PLR_CHIP_FILL_XP,
                UIScreenTheme.Info.PLR_CHIP_TEXT_XP
        );

        guiContext.graphics.drawString(
                PlayerLevelRewardsUiSupport.font(),
                Component.translatable("screen.incore.player_level_rewards.details_rewards"),
                x,
                y + 30,
                PlayerLevelRewardsUiSupport.COLOR_TEXT_SECONDARY,
                false
        );

        if (selectedPreview.rewards().isEmpty()) {
            guiContext.graphics.drawString(
                    PlayerLevelRewardsUiSupport.font(),
                    Component.translatable("screen.incore.player_level_rewards.level_empty"),
                    x,
                    y + 46,
                    PlayerLevelRewardsUiSupport.COLOR_TEXT_MUTED,
                    false
            );
            return;
        }

        int cardsX = x;
        int cardsY = y + 46;
        int availableWidth = Math.max(32, width - 20);
        int availableHeight = Math.max(32, height - 60);
        int columns = Math.max(1, (availableWidth + PlayerLevelRewardsUiSupport.REWARD_CARD_GAP)
                / (PlayerLevelRewardsUiSupport.REWARD_CARD_SIZE + PlayerLevelRewardsUiSupport.REWARD_CARD_GAP));
        int rows = Math.max(1, (availableHeight + PlayerLevelRewardsUiSupport.REWARD_CARD_GAP)
                / (PlayerLevelRewardsUiSupport.REWARD_CARD_SIZE + PlayerLevelRewardsUiSupport.REWARD_CARD_GAP));
        int maxCards = columns * rows;
        int rewardCount = Math.min(maxCards, selectedPreview.rewards().size());

        int mouseX = guiContext.mouseX;
        int mouseY = guiContext.mouseY;
        long now = System.currentTimeMillis();
        float pulse = 0.5F + 0.5F * Mth.sin((now % 4000L) / 220.0F);

        for (int i = 0; i < rewardCount; i++) {
            PlayerLevelClientCache.RewardEntry reward = selectedPreview.rewards().get(i);
            int col = i % columns;
            int row = i / columns;
            int cardX = cardsX + col * (PlayerLevelRewardsUiSupport.REWARD_CARD_SIZE + PlayerLevelRewardsUiSupport.REWARD_CARD_GAP);
            int cardY = cardsY + row * (PlayerLevelRewardsUiSupport.REWARD_CARD_SIZE + PlayerLevelRewardsUiSupport.REWARD_CARD_GAP);
            int cardRight = cardX + PlayerLevelRewardsUiSupport.REWARD_CARD_SIZE;
            int cardBottom = cardY + PlayerLevelRewardsUiSupport.REWARD_CARD_SIZE;

            boolean hovered = mouseX >= cardX && mouseX < cardRight && mouseY >= cardY && mouseY < cardBottom;
            int fill = PlayerLevelRewardsUiSupport.rewardCardFill(reward.kind());
            if (hovered) {
                fill = PlayerLevelRewardsUiSupport.brighten(fill, 18);
            }
            guiContext.graphics.fill(cardX, cardY, cardRight, cardBottom, fill);

            int borderAlpha = hovered ? 200 : (120 + Math.round(40 * pulse));
            PlayerLevelRewardsUiSupport.themed(guiContext.graphics).drawBorder(
                    cardX,
                    cardY,
                    cardRight,
                    cardBottom,
                    PlayerLevelRewardsUiSupport.withAlpha(UIScreenTheme.Info.PLR_CARD_OUTLINE_GLOW, borderAlpha)
            );

            ItemStack iconStack = PlayerLevelRewardsUiSupport.iconStackFor(reward);
            int iconX = cardX + (PlayerLevelRewardsUiSupport.REWARD_CARD_SIZE - 16) / 2;
            int iconY = cardY + (PlayerLevelRewardsUiSupport.REWARD_CARD_SIZE - 16) / 2 + (hovered ? -1 : 0);
            guiContext.graphics.renderItem(iconStack, iconX, iconY);

            if (reward.kind() == PlayerLevelSyncPayload.REWARD_KIND_ITEM && reward.amount() > 1) {
                String qty = "x" + reward.amount();
                int qtyWidth = PlayerLevelRewardsUiSupport.font().width(qty);
                int qtyX = cardRight - qtyWidth - 3;
                int qtyY = cardBottom - PlayerLevelRewardsUiSupport.font().lineHeight - 2;
                guiContext.graphics.fill(qtyX - 2, qtyY - 1, qtyX + qtyWidth + 2, qtyY + PlayerLevelRewardsUiSupport.font().lineHeight, UIScreenTheme.Info.PLR_QTY_CHIP_FILL);
                guiContext.graphics.drawString(PlayerLevelRewardsUiSupport.font(), qty, qtyX, qtyY, UIScreenTheme.Info.PLR_QTY_CHIP_TEXT, false);
            }

            if (hovered) {
                this.hoveredReward = reward;
                this.hoveredStack = iconStack;
            }
        }

        if (rewardCount < selectedPreview.rewards().size()) {
            Component overflow = Component.translatable(
                    "screen.incore.player_level_rewards.more_rewards",
                    selectedPreview.rewards().size() - rewardCount
            );
            int overflowX = left + width - PlayerLevelRewardsUiSupport.font().width(overflow) - 10;
            int overflowY = top + height - 12;
            guiContext.graphics.drawString(
                    PlayerLevelRewardsUiSupport.font(),
                    overflow,
                    overflowX,
                    overflowY,
                    PlayerLevelRewardsUiSupport.COLOR_TEXT_MUTED,
                    false
            );
        }
    }
}
