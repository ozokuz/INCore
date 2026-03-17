package ozokuz.incore.integration.ldlib.ui.player;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import ozokuz.incore.client.ui.UIScreenTheme;
import ozokuz.incore.features.gacha.GachaService;

final class GachaBannerDetailsElement extends UIElement {
    private final GachaAppUiElement owner;

    GachaBannerDetailsElement(GachaAppUiElement owner) {
        this.owner = owner;
        internalSetup();
    }

    @Override
    public void drawBackgroundAdditional(GUIContext guiContext) {
        Font font = GachaAppUiSupport.font();
        int left = Math.round(getPositionX());
        int top = Math.round(getPositionY());
        int right = left + Math.round(getSizeWidth());
        int bottom = top + Math.round(getSizeHeight());
        GachaService.BannerView selected = owner.selectedBanner();
        if (selected == null) {
            guiContext.graphics.drawCenteredString(
                    font,
                    Component.translatable("incore.gacha.banner.none_configured"),
                    (left + right) / 2,
                    top + 40,
                    UIScreenTheme.OtherContent.GACHA_ERROR_TEXT
            );
            return;
        }

        guiContext.graphics.drawString(font, selected.name(), left + 10, top + 8, UIScreenTheme.OtherContent.GACHA_TEXT_PRIMARY, false);
        int infoY = top + 20;
        if (selected.locked()) {
            guiContext.graphics.drawString(
                    font,
                    Component.translatable("screen.incore.gacha_banners.locked", selected.requiredLevel()),
                    left + 10,
                    infoY,
                    UIScreenTheme.OtherContent.GACHA_ERROR_TEXT,
                    false
            );
            infoY += 12;
        }

        guiContext.graphics.drawString(
                font,
                Component.translatable("screen.incore.gacha_banners.type." + selected.type()),
                left + 10,
                infoY,
                "basic".equals(selected.type())
                        ? UIScreenTheme.OtherContent.GACHA_BANNER_TYPE_BASIC_TEXT
                        : UIScreenTheme.OtherContent.GACHA_BANNER_TYPE_LIMITED_TEXT,
                false
        );
        infoY += 12;

        guiContext.graphics.drawString(
                font,
                Component.translatable("screen.incore.gacha_banners.pity", selected.pityFive(), 40, selected.pitySix(), 80),
                left + 10,
                infoY,
                UIScreenTheme.OtherContent.GACHA_SIDEBAR_LABEL_TEXT,
                false
        );
        infoY += 12;

        if ("event".equals(selected.type())) {
            Component featuredLine = selected.eventFeaturedPityEnabled()
                    ? Component.translatable(
                            "screen.incore.gacha_banners.event_featured_pity",
                            selected.eventFeaturedPity(),
                            GachaService.EVENT_FEATURED_SIX_PITY_THRESHOLD
                    )
                    : Component.translatable("screen.incore.gacha_banners.event_featured_pity.unavailable");
            guiContext.graphics.drawString(
                    font,
                    featuredLine,
                    left + 10,
                    infoY,
                    UIScreenTheme.OtherContent.GACHA_FEATURED_TEXT,
                    false
            );
            infoY += 12;
        } else {
            guiContext.graphics.drawString(
                    font,
                    Component.translatable(
                            "screen.incore.gacha_banners.basic_guaranteed_pity",
                            selected.basicSelectedSixPity(),
                            GachaService.BASIC_SELECTED_SIX_THRESHOLD
                    ),
                    left + 10,
                    infoY,
                    UIScreenTheme.OtherContent.GACHA_DROP_RATE_TEXT,
                    false
            );
            infoY += 12;
            if (selected.basicGuaranteeBlocked()) {
                guiContext.graphics.drawString(
                        font,
                        Component.translatable("screen.incore.gacha_banners.basic_guaranteed_locked"),
                        left + 10,
                        infoY,
                        UIScreenTheme.OtherContent.GACHA_PITY_VALUE_TEXT,
                        false
                );
                infoY += 12;
            }
        }

        if (!selected.basicGuaranteeBlocked()) {
            guiContext.graphics.drawString(
                    font,
                    Component.translatable("screen.incore.gacha_banners.cost", GachaService.PULLS_PER_CRATE),
                    left + 10,
                    infoY,
                    UIScreenTheme.OtherContent.GACHA_TEXT_MUTED,
                    false
            );
            infoY += 12;
        }

        String remainingLabel = GachaAppUiSupport.renderRemainingLabel(selected, owner.syncedAtMs());
        if (!remainingLabel.isEmpty()) {
            guiContext.graphics.drawString(
                    font,
                    Component.translatable("screen.incore.gacha_banners.time_left", remainingLabel),
                    left + 10,
                    infoY,
                    UIScreenTheme.OtherContent.GACHA_SIDEBAR_LABEL_TEXT,
                    false
            );
            infoY += 12;
        }

        int showcaseTitleY = Math.max(top + 74, infoY + 6);
        guiContext.graphics.drawCenteredString(
                font,
                Component.translatable("screen.incore.gacha_banners.high_rarity_showcase"),
                (left + right) / 2,
                showcaseTitleY,
                UIScreenTheme.OtherContent.GACHA_PITY_LABEL_TEXT
        );

        if (!selected.basicGuaranteeBlocked()) {
            renderPermitUsage(guiContext, selected, right - 78, bottom - 36);
        }

        ItemStack hoveredHighlight = renderShowcase(guiContext, selected, left, showcaseTitleY + 12, right, bottom, guiContext.mouseX, guiContext.mouseY);
        if (!hoveredHighlight.isEmpty()) {
            guiContext.graphics.renderTooltip(font, hoveredHighlight, guiContext.mouseX, guiContext.mouseY);
        }
    }

    private void renderPermitUsage(GUIContext guiContext, GachaService.BannerView banner, int left, int bottom) {
        if (banner.permitUsage().isEmpty()) {
            return;
        }

        int maxScaledWidth = 0;
        for (GachaService.PermitUsageLineView line : banner.permitUsage()) {
            maxScaledWidth = Math.max(maxScaledWidth, scaledCostLineWidth(line.count()));
        }
        int lineHeight = 11;
        int totalHeight = banner.permitUsage().size() * lineHeight + Math.max(0, banner.permitUsage().size() - 1);
        int panelRight = left + 78;
        int panelTop = bottom - totalHeight - 2;
        guiContext.graphics.fill(left, panelTop, panelRight, bottom, UIScreenTheme.OtherContent.GACHA_BALANCE_PANEL_FILL);

        int rowY = panelTop + 2;
        for (GachaService.PermitUsageLineView line : banner.permitUsage()) {
            int lineX = left + (78 - Math.max(1, Math.min(78, maxScaledWidth))) / 2;
            renderCostLine(guiContext, lineX, rowY, GachaAppUiSupport.stackForId(line.itemId()), "x" + line.count(), line.missing());
            rowY += lineHeight;
        }
    }

    private void renderCostLine(GUIContext guiContext, int x, int y, ItemStack stack, String count, boolean missing) {
        if (stack.isEmpty()) {
            return;
        }
        guiContext.graphics.pose().pushPose();
        guiContext.graphics.pose().translate(x, y, 0.0F);
        guiContext.graphics.pose().scale(0.75F, 0.75F, 1.0F);
        guiContext.graphics.renderItem(stack, 0, 0);
        guiContext.graphics.drawString(
                GachaAppUiSupport.font(),
                count,
                20,
                4,
                missing ? UIScreenTheme.OtherContent.GACHA_COST_MISSING_TEXT : UIScreenTheme.OtherContent.GACHA_COST_OK_TEXT,
                false
        );
        guiContext.graphics.pose().popPose();
    }

    private int scaledCostLineWidth(int count) {
        return (int) Math.ceil((20 + GachaAppUiSupport.font().width("x" + count)) * 0.75F);
    }

    private ItemStack renderShowcase(
            GUIContext guiContext,
            GachaService.BannerView banner,
            int left,
            int top,
            int right,
            int bottom,
            int mouseX,
            int mouseY
    ) {
        List<Item> sixStars = GachaAppUiSupport.uniqueRewardsByRarity(banner, 6);
        List<Item> fiveStars = GachaAppUiSupport.uniqueRewardsByRarity(banner, 5);
        if (sixStars.isEmpty() && fiveStars.isEmpty()) {
            guiContext.graphics.drawCenteredString(
                    GachaAppUiSupport.font(),
                    Component.translatable("screen.incore.gacha_banners.high_rarity_none"),
                    (left + right) / 2,
                    top + 24,
                    UIScreenTheme.OtherContent.GACHA_SHOWCASE_CHANCE_TEXT
            );
            return ItemStack.EMPTY;
        }

        int centerX = (left + right) / 2;
        int maxWidth = Math.max(80, (right - left) - 28);
        int y = top;
        ItemStack hovered = ItemStack.EMPTY;

        if (!sixStars.isEmpty()) {
            guiContext.graphics.drawCenteredString(
                    GachaAppUiSupport.font(),
                    Component.translatable("screen.incore.gacha_banners.showcase.six"),
                    centerX,
                    y,
                    UIScreenTheme.OtherContent.GACHA_SHOWCASE_SIX_TEXT
            );
            y += 10;
            GridRenderResult sixRender = renderItemGrid(guiContext, sixStars, centerX, y, maxWidth, 1.55F, 5, 3, mouseX, mouseY);
            y = sixRender.nextY();
            hovered = sixRender.hoveredStack();
        }

        if (!fiveStars.isEmpty()) {
            y += 6;
            guiContext.graphics.drawCenteredString(
                    GachaAppUiSupport.font(),
                    Component.translatable("screen.incore.gacha_banners.showcase.five"),
                    centerX,
                    y,
                    UIScreenTheme.OtherContent.GACHA_SHOWCASE_FIVE_TEXT
            );
            y += 10;
            GridRenderResult fiveRender = renderItemGrid(guiContext, fiveStars, centerX, y, maxWidth, 1.15F, 6, 2, mouseX, mouseY);
            if (hovered.isEmpty()) {
                hovered = fiveRender.hoveredStack();
            }
        }
        return hovered;
    }

    private GridRenderResult renderItemGrid(
            GUIContext guiContext,
            List<Item> items,
            int centerX,
            int topY,
            int maxWidth,
            float scale,
            int preferredMaxColumns,
            int gap,
            int mouseX,
            int mouseY
    ) {
        if (items.isEmpty()) {
            return new GridRenderResult(topY, ItemStack.EMPTY);
        }

        int iconSize = Math.max(1, Math.round(16.0F * scale));
        int cell = iconSize + gap;
        int maxColumns = Math.max(1, Math.min(preferredMaxColumns, maxWidth / Math.max(1, cell)));
        int columns = Math.max(1, Math.min(items.size(), maxColumns));
        int rows = (items.size() + columns - 1) / columns;
        int rowWidth = columns * cell - gap;
        int startX = centerX - rowWidth / 2;
        ItemStack hovered = ItemStack.EMPTY;

        for (int index = 0; index < items.size(); index++) {
            ItemStack displayStack = items.get(index).getDefaultInstance();
            int row = index / columns;
            int col = index % columns;
            int x = startX + col * cell;
            int y = topY + row * cell;
            guiContext.graphics.pose().pushPose();
            guiContext.graphics.pose().translate(x, y, 0.0F);
            guiContext.graphics.pose().scale(scale, scale, 1.0F);
            guiContext.graphics.renderItem(displayStack, 0, 0);
            guiContext.graphics.pose().popPose();
            if (hovered.isEmpty() && mouseX >= x && mouseX < x + iconSize && mouseY >= y && mouseY < y + iconSize) {
                hovered = displayStack;
            }
        }
        return new GridRenderResult(topY + rows * cell, hovered);
    }

    private record GridRenderResult(int nextY, ItemStack hoveredStack) {
    }
}
