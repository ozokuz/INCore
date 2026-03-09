package io.github.ozokuz.incore.client.features.market;

import io.github.ozokuz.incore.client.ui.UIScreenTheme;
import io.github.ozokuz.incore.client.ui.render.ThemedUi;
import io.github.ozokuz.incore.features.market.content.ShipmentTerminalBlockEntity;
import io.github.ozokuz.incore.features.market.content.ShipmentTerminalMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

public class ShipmentTerminalScreen extends AbstractContainerScreen<ShipmentTerminalMenu> {
    private static final UIScreenTheme THEME = UIScreenTheme.MACHINE;
    private static final int TEXT_COLOR = THEME.theme().text().secondary();
    private static final int WORK_PANEL_Y = 66;
    private static final int INVENTORY_PANEL_Y = 146;

    public ShipmentTerminalScreen(ShipmentTerminalMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 248;
        this.imageHeight = 252;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        ThemedUi ui = themed(guiGraphics);

        ui.drawWindow(x, y, imageWidth, imageHeight);
        ui.drawPanel(x + 5, y + 5, imageWidth - 10, 14);
        ui.drawPanel(x + 8, y + 24, imageWidth - 16, 36);
        ui.drawPanel(x + 8, y + WORK_PANEL_Y, imageWidth - 16, 74);
        ui.drawPanel(x + 8, y + INVENTORY_PANEL_Y, imageWidth - 16, 98);

        ui.drawProgressBar(
                x + 12,
                y + 49,
                224,
                6,
                menu.progressScaled(200) / (float) 200,
                UIScreenTheme.Machine.PROGRESS_TRACK_FILL,
                UIScreenTheme.Machine.PROGRESS_FILL_SUCCESS,
                UIScreenTheme.Machine.PROGRESS_FRAME_BORDER
        );

        for (int row = 0; row < ShipmentTerminalMenu.INPUT_ROWS; row++) {
            for (int col = 0; col < ShipmentTerminalMenu.INPUT_COLUMNS; col++) {
                drawSlotFrame(guiGraphics, x + ShipmentTerminalMenu.INPUT_X + col * 18, y + ShipmentTerminalMenu.INPUT_Y + row * 18);
            }
        }
        drawSlotFrame(guiGraphics, x + ShipmentTerminalMenu.CARD_X, y + ShipmentTerminalMenu.CARD_Y);

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                drawSlotFrame(guiGraphics, x + ShipmentTerminalMenu.PLAYER_INVENTORY_X + col * 18, y + ShipmentTerminalMenu.PLAYER_INVENTORY_Y + row * 18);
            }
        }
        for (int col = 0; col < 9; col++) {
            drawSlotFrame(guiGraphics, x + ShipmentTerminalMenu.PLAYER_INVENTORY_X + col * 18, y + ShipmentTerminalMenu.HOTBAR_Y);
        }

        guiGraphics.fill(x + imageWidth - 24, y + 24 + 8, x + imageWidth - 16, y + 24 + 16, statusColor());
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, title, 11, 9, UIScreenTheme.Machine.TITLE_TEXT, false);
        guiGraphics.drawString(font, Component.translatable("screen.incore.common.status"), 12, 28, TEXT_COLOR, false);
        drawWrapped(guiGraphics, statusComponent(), 56, 28, 152, 2, TEXT_COLOR);
        guiGraphics.drawString(font, Component.translatable("screen.incore.market.shipment.input"), 20, 70, TEXT_COLOR, false);
        guiGraphics.drawString(font, Component.translatable("screen.incore.market.shipment.card"), 164, 80, TEXT_COLOR, false);
        guiGraphics.drawString(font, playerInventoryTitle, 12, INVENTORY_PANEL_Y + 4, TEXT_COLOR, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        themed(guiGraphics).drawBackdrop(this.width, this.height);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    private Component statusComponent() {
        return switch (menu.status()) {
            case ShipmentTerminalBlockEntity.STATUS_DISABLED -> Component.translatable("screen.incore.market.shipment.status.disabled");
            case ShipmentTerminalBlockEntity.STATUS_NO_CARD -> Component.translatable("screen.incore.market.shipment.status.no_card");
            case ShipmentTerminalBlockEntity.STATUS_NO_ITEMS -> Component.translatable("screen.incore.market.shipment.status.no_items");
            case ShipmentTerminalBlockEntity.STATUS_INVALID_ITEM -> Component.translatable("screen.incore.market.shipment.status.invalid_item");
            case ShipmentTerminalBlockEntity.STATUS_NEED_FULL_STACK -> Component.translatable("screen.incore.market.shipment.status.need_full_stack");
            case ShipmentTerminalBlockEntity.STATUS_NO_RPM -> Component.translatable("screen.incore.market.shipment.status.no_rpm");
            case ShipmentTerminalBlockEntity.STATUS_NO_STRESS -> Component.translatable("screen.incore.market.shipment.status.no_stress");
            case ShipmentTerminalBlockEntity.STATUS_NO_POWER -> Component.translatable("screen.incore.market.shipment.status.no_power");
            default -> Component.translatable("screen.incore.market.shipment.status.ready");
        };
    }

    private int statusColor() {
        return switch (menu.status()) {
            case ShipmentTerminalBlockEntity.STATUS_DISABLED -> UIScreenTheme.Machine.STATUS_DISABLED_TEXT;
            case ShipmentTerminalBlockEntity.STATUS_NO_CARD, ShipmentTerminalBlockEntity.STATUS_INVALID_ITEM,
                 ShipmentTerminalBlockEntity.STATUS_NO_STRESS, ShipmentTerminalBlockEntity.STATUS_NO_POWER -> UIScreenTheme.Machine.STATUS_ERROR_TEXT;
            case ShipmentTerminalBlockEntity.STATUS_NO_ITEMS, ShipmentTerminalBlockEntity.STATUS_NEED_FULL_STACK,
                 ShipmentTerminalBlockEntity.STATUS_NO_RPM -> UIScreenTheme.Machine.STATUS_WARNING_TEXT;
            default -> UIScreenTheme.Machine.STATUS_READY_TEXT;
        };
    }

    private void drawWrapped(GuiGraphics guiGraphics, Component text, int x, int y, int width, int maxLines, int color) {
        List<FormattedCharSequence> lines = font.split(text, Math.max(0, width));
        for (int i = 0; i < Math.min(lines.size(), maxLines); i++) {
            guiGraphics.drawString(font, lines.get(i), x, y + i * font.lineHeight, color);
        }
    }

    private static void drawSlotFrame(GuiGraphics guiGraphics, int x, int y) {
        themed(guiGraphics).drawSlotFrame(x, y);
    }

    private static ThemedUi themed(GuiGraphics guiGraphics) {
        return new ThemedUi(guiGraphics, THEME.theme());
    }
}
