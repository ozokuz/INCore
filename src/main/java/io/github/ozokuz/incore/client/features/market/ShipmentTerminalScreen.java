package io.github.ozokuz.incore.client.features.market;

import io.github.ozokuz.incore.features.market.content.ShipmentTerminalBlockEntity;
import io.github.ozokuz.incore.features.market.content.ShipmentTerminalMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

public class ShipmentTerminalScreen extends AbstractContainerScreen<ShipmentTerminalMenu> {
    private static final int TEXT_COLOR = 0xCDD3DE;

    public ShipmentTerminalScreen(ShipmentTerminalMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 230;
        this.imageHeight = 214;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;

        drawPanel(guiGraphics, x, y, imageWidth, imageHeight, 0xFF13161A, 0xFF4A4F5A);
        drawPanel(guiGraphics, x + 5, y + 5, imageWidth - 10, 14, 0xFF20252C, 0xFF3D4350);
        drawPanel(guiGraphics, x + 8, y + 24, imageWidth - 16, 36, 0xFF1A1F26, 0xFF363D49);
        drawPanel(guiGraphics, x + 8, y + 62, imageWidth - 16, 62, 0xFF1A1F26, 0xFF363D49);
        drawPanel(guiGraphics, x + 8, y + 124, imageWidth - 16, 82, 0xFF1A1F26, 0xFF363D49);

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                drawSlotFrame(
                        guiGraphics,
                        x + ShipmentTerminalMenu.INPUT_X + col * 18,
                        y + ShipmentTerminalMenu.INPUT_Y + row * 18
                );
            }
        }
        drawSlotFrame(guiGraphics, x + ShipmentTerminalMenu.CARD_X, y + ShipmentTerminalMenu.CARD_Y);

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                drawSlotFrame(
                        guiGraphics,
                        x + ShipmentTerminalMenu.PLAYER_INVENTORY_X + col * 18,
                        y + ShipmentTerminalMenu.PLAYER_INVENTORY_Y + row * 18
                );
            }
        }
        for (int col = 0; col < 9; col++) {
            drawSlotFrame(
                    guiGraphics,
                    x + ShipmentTerminalMenu.PLAYER_INVENTORY_X + col * 18,
                    y + ShipmentTerminalMenu.HOTBAR_Y
            );
        }

        int progressX = x + 12;
        int progressY = y + 49;
        int progressWidth = 180;
        drawPanel(guiGraphics, progressX - 1, progressY - 1, progressWidth + 2, 8, 0xFF101318, 0xFF2F3540);
        guiGraphics.fill(progressX, progressY, progressX + progressWidth, progressY + 6, 0xFF242B34);
        guiGraphics.fill(progressX, progressY, progressX + menu.progressScaled(progressWidth), progressY + 6, 0xFF5ED084);

        int statusColor = switch (menu.status()) {
            case ShipmentTerminalBlockEntity.STATUS_NO_CARD, ShipmentTerminalBlockEntity.STATUS_INVALID_ITEM,
                 ShipmentTerminalBlockEntity.STATUS_NO_STRESS, ShipmentTerminalBlockEntity.STATUS_NO_POWER -> 0xFFD17C7C;
            case ShipmentTerminalBlockEntity.STATUS_NO_ITEMS, ShipmentTerminalBlockEntity.STATUS_NEED_FULL_STACK,
                 ShipmentTerminalBlockEntity.STATUS_NO_RPM -> 0xFFE2C777;
            default -> 0xFF7DD6A7;
        };
        guiGraphics.fill(x + 198, y + 31, x + 206, y + 39, statusColor);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, 11, 9, 0xE6EBF4, false);
        guiGraphics.drawString(this.font, Component.literal("Status"), 12, 29, TEXT_COLOR, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 12, 128, TEXT_COLOR, false);

        Component status = switch (menu.status()) {
            case ShipmentTerminalBlockEntity.STATUS_NO_CARD -> Component.translatable("screen.incore.market.shipment.status.no_card");
            case ShipmentTerminalBlockEntity.STATUS_NO_ITEMS -> Component.translatable("screen.incore.market.shipment.status.no_items");
            case ShipmentTerminalBlockEntity.STATUS_INVALID_ITEM -> Component.translatable("screen.incore.market.shipment.status.invalid_item");
            case ShipmentTerminalBlockEntity.STATUS_NEED_FULL_STACK -> Component.translatable("screen.incore.market.shipment.status.need_full_stack");
            case ShipmentTerminalBlockEntity.STATUS_NO_RPM -> Component.translatable("screen.incore.market.shipment.status.no_rpm");
            case ShipmentTerminalBlockEntity.STATUS_NO_STRESS -> Component.translatable("screen.incore.market.shipment.status.no_stress");
            case ShipmentTerminalBlockEntity.STATUS_NO_POWER -> Component.translatable("screen.incore.market.shipment.status.no_power");
            default -> Component.translatable("screen.incore.market.shipment.status.ready");
        };

        List<FormattedCharSequence> lines = font.split(status, 180);
        for (int i = 0; i < lines.size() && i < 2; i++) {
            guiGraphics.drawString(this.font, lines.get(i), 56, 29 + i * this.font.lineHeight, TEXT_COLOR);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    private static void drawPanel(GuiGraphics guiGraphics, int x, int y, int width, int height, int fillColor, int borderColor) {
        guiGraphics.fill(x, y, x + width, y + height, fillColor);
        guiGraphics.fill(x, y, x + width, y + 1, borderColor);
        guiGraphics.fill(x, y + height - 1, x + width, y + height, borderColor);
        guiGraphics.fill(x, y, x + 1, y + height, borderColor);
        guiGraphics.fill(x + width - 1, y, x + width, y + height, borderColor);
    }

    private static void drawSlotFrame(GuiGraphics guiGraphics, int x, int y) {
        drawPanel(guiGraphics, x - 1, y - 1, 18, 18, 0xFF252A32, 0xFF4A5261);
        guiGraphics.fill(x, y, x + 16, y + 16, 0xFF181D24);
    }
}
