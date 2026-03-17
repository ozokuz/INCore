package ozokuz.incore.integration.ldlib.ui.player;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import ozokuz.incore.client.ui.UIScreenTheme;
import ozokuz.incore.features.gacha.GachaService;

final class GachaOddsTableElement extends UIElement {
    private final GachaAppUiElement owner;

    GachaOddsTableElement(GachaAppUiElement owner) {
        this.owner = owner;
        internalSetup();
    }

    @Override
    public void drawBackgroundAdditional(GUIContext guiContext) {
        Font font = GachaAppUiSupport.font();
        GachaService.BannerView banner = owner.selectedBanner();
        if (banner == null) {
            return;
        }

        int left = Math.round(getPositionX());
        int top = Math.round(getPositionY());
        int right = left + Math.round(getSizeWidth());
        int rowHeight = 16;
        List<GachaService.RewardView> rewards = GachaAppUiSupport.rewardsForPage(banner, owner.infoPage());
        List<Component> hoveredTooltip = null;

        for (int index = 0; index < rewards.size(); index++) {
            GachaService.RewardView reward = rewards.get(index);
            int y = top + index * rowHeight;
            guiContext.graphics.fill(
                    left,
                    y - 1,
                    right,
                    y + 13,
                    index % 2 == 0 ? UIScreenTheme.OtherContent.INFO_ROW_FILL_A : UIScreenTheme.OtherContent.INFO_ROW_FILL_B
            );

            ItemStack displayStack = GachaAppUiSupport.stackForId(reward.itemId());
            if (!displayStack.isEmpty()) {
                guiContext.graphics.renderItem(displayStack, left + 2, y - 2);
                guiContext.graphics.drawString(
                        font,
                        displayStack.getHoverName(),
                        left + 22,
                        y + 2,
                        UIScreenTheme.OtherContent.INFO_ITEM_TEXT,
                        false
                );
            } else {
                guiContext.graphics.drawString(font, reward.itemId(), left + 22, y + 2, UIScreenTheme.OtherContent.INFO_ITEM_MISSING_TEXT, false);
            }

            guiContext.graphics.drawString(
                    font,
                    Component.literal(reward.rarity() + "★"),
                    right - 72,
                    y + 2,
                    GachaAppUiSupport.rarityColor(reward.rarity()),
                    false
            );
            guiContext.graphics.drawString(
                    font,
                    Component.literal(String.format(Locale.ROOT, "%.2f%%", reward.chancePercent())),
                    right - 36,
                    y + 2,
                    UIScreenTheme.OtherContent.INFO_CHANCE_TEXT,
                    false
            );

            if (guiContext.mouseX >= left && guiContext.mouseX < right && guiContext.mouseY >= y - 1 && guiContext.mouseY < y + 13) {
                List<Component> tooltip = new ArrayList<>();
                if (!displayStack.isEmpty()) {
                    tooltip.addAll(Screen.getTooltipFromItem(Minecraft.getInstance(), displayStack));
                } else {
                    tooltip.add(Component.literal(reward.itemId()));
                }
                tooltip.add(Component.literal(reward.rarity() + "★").withColor(GachaAppUiSupport.rarityColor(reward.rarity())));
                tooltip.add(Component.literal(String.format(Locale.ROOT, "%.2f%%", reward.chancePercent())).withColor(UIScreenTheme.OtherContent.INFO_TOOLTIP_TEXT));
                hoveredTooltip = tooltip;
            }
        }

        if (hoveredTooltip != null) {
            guiContext.graphics.renderComponentTooltip(font, hoveredTooltip, guiContext.mouseX, guiContext.mouseY);
        }
    }
}
