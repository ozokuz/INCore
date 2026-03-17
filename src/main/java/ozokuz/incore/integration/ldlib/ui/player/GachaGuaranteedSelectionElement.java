package ozokuz.incore.integration.ldlib.ui.player;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;
import ozokuz.incore.client.ui.UIScreenTheme;

final class GachaGuaranteedSelectionElement extends UIElement {
    private final GachaAppUiElement owner;
    private List<CardLayout> cardLayouts = List.of();
    private int lastWidth = -1;
    private int lastItemCount = -1;

    GachaGuaranteedSelectionElement(GachaAppUiElement owner) {
        this.owner = owner;
        internalSetup();
        addEventListener(UIEvents.CLICK, event -> {
            if (event.button != 0) {
                return;
            }
            updateLayouts();
            for (CardLayout layout : cardLayouts) {
                if (layout.contains(event.x, event.y)) {
                    owner.selectGuaranteedItem(owner.selectableSixItems().get(layout.index()));
                    event.stopPropagation();
                    return;
                }
            }
        });
        addEventListener(UIEvents.TICK, event -> updateLayouts());
    }

    @Override
    public void drawBackgroundAdditional(GUIContext guiContext) {
        Font font = GachaAppUiSupport.font();
        List<ResourceLocation> items = owner.selectableSixItems();
        if (items.isEmpty()) {
            guiContext.graphics.drawCenteredString(
                    font,
                    Component.translatable("screen.incore.gacha_guaranteed_six.none_available"),
                    Math.round(getPositionX() + getSizeWidth() / 2.0F),
                    Math.round(getPositionY() + getSizeHeight() / 2.0F),
                    UIScreenTheme.OtherContent.GUARANTEE_ERROR_TEXT
            );
            return;
        }

        @Nullable CardLayout hovered = null;
        for (CardLayout layout : cardLayouts) {
            boolean selected = layout.itemId().equals(owner.selectedGuaranteedItem());
            boolean isHovered = layout.contains(guiContext.mouseX, guiContext.mouseY);
            if (isHovered) {
                hovered = layout;
            }

            int border = selected
                    ? UIScreenTheme.OtherContent.GUARANTEE_ROW_BORDER_SELECTED
                    : (isHovered ? UIScreenTheme.OtherContent.GUARANTEE_ROW_BORDER_HOVER : UIScreenTheme.OtherContent.GUARANTEE_ROW_BORDER);
            int fill = selected
                    ? UIScreenTheme.OtherContent.GUARANTEE_ROW_FILL_SELECTED
                    : UIScreenTheme.OtherContent.GUARANTEE_ROW_FILL;
            guiContext.graphics.fill(layout.left(), layout.top(), layout.right(), layout.bottom(), fill);
            guiContext.graphics.fill(layout.left(), layout.top(), layout.right(), layout.top() + 1, border);
            guiContext.graphics.fill(layout.left(), layout.bottom() - 1, layout.right(), layout.bottom(), border);
            guiContext.graphics.fill(layout.left(), layout.top(), layout.left() + 1, layout.bottom(), border);
            guiContext.graphics.fill(layout.right() - 1, layout.top(), layout.right(), layout.bottom(), border);

            Item item = GachaAppUiSupport.itemFromId(layout.itemId().toString());
            ItemStack stack = item == Items.AIR ? ItemStack.EMPTY : item.getDefaultInstance();
            if (!stack.isEmpty()) {
                int iconX = layout.left() + (layout.right() - layout.left() - 16) / 2;
                int iconY = layout.top() + 10;
                guiContext.graphics.renderItem(stack, iconX, iconY);
            }

            Component name = stack.isEmpty() ? Component.literal(layout.itemId().toString()) : stack.getHoverName();
            String clippedName = font.plainSubstrByWidth(name.getString(), layout.right() - layout.left() - 12);
            int nameX = layout.left() + (layout.right() - layout.left() - font.width(clippedName)) / 2;
            guiContext.graphics.drawString(font, clippedName, nameX, layout.top() + 34, UIScreenTheme.OtherContent.CATALOG_TEXT_HEADING, false);

            String rawId = layout.itemId().toString();
            String clippedId = font.plainSubstrByWidth(rawId, layout.right() - layout.left() - 12);
            int idX = layout.left() + (layout.right() - layout.left() - font.width(clippedId)) / 2;
            guiContext.graphics.drawString(font, clippedId, idX, layout.top() + 50, UIScreenTheme.OtherContent.GUARANTEE_ID_TEXT, false);

            guiContext.graphics.drawCenteredString(
                    font,
                    Component.literal("6★"),
                    (layout.left() + layout.right()) / 2,
                    layout.bottom() - 16,
                    UIScreenTheme.OtherContent.GACHA_SHOWCASE_SIX_TEXT
            );
        }

        if (hovered != null) {
            ItemStack stack = GachaAppUiSupport.stackForId(hovered.itemId());
            List<Component> tooltip = new ArrayList<>();
            if (!stack.isEmpty()) {
                tooltip.addAll(Screen.getTooltipFromItem(Minecraft.getInstance(), stack));
            } else {
                tooltip.add(Component.literal(hovered.itemId().toString()));
            }
            tooltip.add(Component.literal("6★").withColor(UIScreenTheme.OtherContent.GACHA_SHOWCASE_SIX_TEXT));
            guiContext.graphics.renderComponentTooltip(font, tooltip, guiContext.mouseX, guiContext.mouseY);
        }
    }

    private void updateLayouts() {
        List<ResourceLocation> items = owner.selectableSixItems();
        int width = Math.round(getSizeWidth());
        if (width == lastWidth && items.size() == lastItemCount) {
            return;
        }

        if (items.isEmpty()) {
            cardLayouts = List.of();
            lastWidth = width;
            lastItemCount = 0;
            return;
        }

        int availableWidth = Math.max(0, width - 32);
        int maxColumns = Math.max(1, Math.min(items.size(), 6));
        int columns = maxColumns;
        while (columns > 1 && (columns * GachaAppUiSupport.GUARANTEE_CARD_WIDTH + (columns - 1) * GachaAppUiSupport.GUARANTEE_CARD_GAP) > availableWidth) {
            columns--;
        }

        int totalWidth = columns * GachaAppUiSupport.GUARANTEE_CARD_WIDTH + (columns - 1) * GachaAppUiSupport.GUARANTEE_CARD_GAP;
        int startX = Math.round(getPositionX()) + (width - totalWidth) / 2;
        int startY = Math.round(getPositionY()) + 8;
        List<CardLayout> layouts = new ArrayList<>();
        for (int index = 0; index < items.size(); index++) {
            int row = index / columns;
            int col = index % columns;
            int left = startX + col * (GachaAppUiSupport.GUARANTEE_CARD_WIDTH + GachaAppUiSupport.GUARANTEE_CARD_GAP);
            int top = startY + row * (GachaAppUiSupport.GUARANTEE_CARD_HEIGHT + GachaAppUiSupport.GUARANTEE_CARD_GAP);
            layouts.add(new CardLayout(index, items.get(index), left, top, left + GachaAppUiSupport.GUARANTEE_CARD_WIDTH, top + GachaAppUiSupport.GUARANTEE_CARD_HEIGHT));
        }
        cardLayouts = List.copyOf(layouts);
        lastWidth = width;
        lastItemCount = items.size();
    }

    private record CardLayout(int index, ResourceLocation itemId, int left, int top, int right, int bottom) {
        private boolean contains(double x, double y) {
            return x >= left && x < right && y >= top && y < bottom;
        }
    }
}
