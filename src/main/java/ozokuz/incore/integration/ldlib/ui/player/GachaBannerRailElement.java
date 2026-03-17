package ozokuz.incore.integration.ldlib.ui.player;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import ozokuz.incore.client.ui.UIScreenTheme;
import ozokuz.incore.features.gacha.GachaService;

final class GachaBannerRailElement extends UIElement {
    private final GachaAppUiElement owner;
    private int lastBannerCount = -1;

    GachaBannerRailElement(GachaAppUiElement owner) {
        this.owner = owner;
        internalSetup();
        addEventListener(UIEvents.CLICK, event -> {
            if (event.button != 0) {
                return;
            }
            int index = (int) ((event.y - getPositionY()) / GachaAppUiSupport.BANNER_ROW_HEIGHT);
            if (index < 0 || index >= owner.banners().size()) {
                return;
            }
            owner.selectBanner(owner.banners().get(index).id());
            event.stopPropagation();
        });
        addEventListener(UIEvents.TICK, event -> updateContentHeight());
    }

    @Override
    public void drawBackgroundAdditional(GUIContext guiContext) {
        var font = GachaAppUiSupport.font();
        int width = Math.round(getSizeWidth());
        int rowLeft = Math.round(getPositionX());
        int rowRight = rowLeft + width;
        int mouseX = guiContext.mouseX;
        int mouseY = guiContext.mouseY;

        for (int index = 0; index < owner.banners().size(); index++) {
            GachaService.BannerView banner = owner.banners().get(index);
            int y = Math.round(getPositionY()) + index * GachaAppUiSupport.BANNER_ROW_HEIGHT;
            int rowBottom = y + GachaAppUiSupport.BANNER_ROW_HEIGHT - 2;
            boolean selected = banner.id().equals(owner.selectedBannerId());
            boolean hovered = mouseX >= rowLeft && mouseX < rowRight && mouseY >= y && mouseY < rowBottom;

            int border = selected
                    ? GachaAppUiSupport.brightenColor(banner.sidebarColor(), 0.22F)
                    : banner.sidebarColor();
            int fill = selected
                    ? UIScreenTheme.OtherContent.CATALOG_ROW_SELECTED_FILL
                    : UIScreenTheme.OtherContent.CATALOG_ROW_FILL;
            guiContext.graphics.fill(rowLeft + 1, y + 1, rowRight - 1, rowBottom, fill);
            drawRowBorder(guiContext, rowLeft, rowRight, y, rowBottom, border);

            Item mainItem = GachaAppUiSupport.itemFromId(banner.mainItemId());
            if (mainItem != Items.AIR) {
                guiContext.graphics.renderItem(mainItem.getDefaultInstance(), rowLeft + 4, y + 3);
            }

            int textX = rowLeft + 24;
            int textColor = selected
                    ? UIScreenTheme.OtherContent.GACHA_TEXT_SELECTED
                    : UIScreenTheme.OtherContent.CATALOG_TEXT_PRIMARY;
            String clippedName = font.plainSubstrByWidth(banner.name(), width - 30);
            guiContext.graphics.drawString(font, clippedName, textX, y + 3, textColor, false);

            String remainingLabel = GachaAppUiSupport.renderRemainingLabel(banner, owner.syncedAtMs());
            if (!remainingLabel.isEmpty()) {
                guiContext.graphics.drawString(
                        font,
                        remainingLabel,
                        textX,
                        y + 15,
                        hovered ? UIScreenTheme.OtherContent.GACHA_TEXT_PRIMARY : UIScreenTheme.OtherContent.GACHA_TEXT_SECONDARY,
                        false
                );
            } else if (banner.locked()) {
                guiContext.graphics.drawString(
                        font,
                        Component.translatable("screen.incore.gacha_banners.locked", banner.requiredLevel()),
                        textX,
                        y + 15,
                        UIScreenTheme.OtherContent.GACHA_ERROR_TEXT,
                        false
                );
            }
        }
    }

    private void updateContentHeight() {
        int bannerCount = owner.banners().size();
        int targetHeight = Math.max(12, bannerCount * GachaAppUiSupport.BANNER_ROW_HEIGHT);
        if (bannerCount != lastBannerCount || Math.round(getSizeHeight()) != targetHeight) {
            getLayout().height(targetHeight);
            lastBannerCount = bannerCount;
        }
    }

    private static void drawRowBorder(GUIContext guiContext, int left, int right, int top, int bottom, int borderColor) {
        int color = UIScreenTheme.OtherContent.GACHA_ROW_BORDER_MASK | borderColor;
        guiContext.graphics.fill(left, top, right, top + 1, color);
        guiContext.graphics.fill(left, bottom - 1, right, bottom, color);
        guiContext.graphics.fill(left, top, left + 1, bottom, color);
        guiContext.graphics.fill(right - 1, top, right, bottom, color);
    }
}
