package io.github.ozokuz.incore.features.research.client;

import io.github.ozokuz.incore.features.research.LabBlockEntity;
import io.github.ozokuz.incore.features.research.LabMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class LabScreen extends AbstractContainerScreen<LabMenu> {
    private static final int PROGRESS_BAR_WIDTH = 82;
    private static final int PROGRESS_BAR_HEIGHT = 8;
    private static final int OUTER_PADDING = 8;
    private static final int TOP_SECTION_Y = 24;
    private static final int TOP_SECTION_HEIGHT = 98;
    private static final int INVENTORY_SECTION_Y = 124;
    private static final int COLUMN_GAP = 4;
    private static final int STATUS_WIDTH = 98;
    private static final int CURRENT_WIDTH = 98;
    private static final int MATERIAL_WIDTH = 98;
    private static final int TEXT_COLOR = 0xCDD3DE;

    public LabScreen(LabMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 320;
        this.imageHeight = 214;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        int statusX = x + OUTER_PADDING;
        int currentX = statusX + STATUS_WIDTH + COLUMN_GAP;
        int materialX = currentX + CURRENT_WIDTH + COLUMN_GAP;
        int topY = y + TOP_SECTION_Y;
        int columnH = TOP_SECTION_HEIGHT;

        drawPanel(guiGraphics, x, y, imageWidth, imageHeight, 0xFF13161A, 0xFF4A4F5A);
        drawPanel(guiGraphics, x + 5, y + 5, imageWidth - 10, 14, 0xFF20252C, 0xFF3D4350);
        drawPanel(guiGraphics, statusX, topY, STATUS_WIDTH, columnH, 0xFF1A1F26, 0xFF363D49);
        drawPanel(guiGraphics, currentX, topY, CURRENT_WIDTH, columnH, 0xFF1A1F26, 0xFF363D49);
        drawPanel(guiGraphics, materialX, topY, MATERIAL_WIDTH, columnH, 0xFF1A1F26, 0xFF363D49);
        drawPanel(guiGraphics, x + 8, y + INVENTORY_SECTION_Y, imageWidth - 16, 85, 0xFF1A1F26, 0xFF363D49);

        for (int row = 0; row < LabMenu.LAB_SLOT_ROWS; row++) {
            for (int col = 0; col < LabMenu.LAB_SLOT_COLUMNS; col++) {
                drawSlotFrame(guiGraphics, x + LabMenu.LAB_SLOT_X + col * 18, y + LabMenu.LAB_SLOT_Y + row * 18);
            }
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                drawSlotFrame(guiGraphics, x + LabMenu.PLAYER_INVENTORY_X + col * 18, y + LabMenu.PLAYER_INVENTORY_Y + row * 18);
            }
        }

        for (int i = 0; i < 9; i++) {
            drawSlotFrame(guiGraphics, x + LabMenu.PLAYER_INVENTORY_X + i * 18, y + LabMenu.HOTBAR_Y);
        }

        int progress = menu.progressScaled(PROGRESS_BAR_WIDTH);
        int barX = statusX + 6;
        int barY = y + 98;
        drawPanel(guiGraphics, barX - 1, barY - 1, PROGRESS_BAR_WIDTH + 2, PROGRESS_BAR_HEIGHT + 2, 0xFF101318, 0xFF2F3540);
        guiGraphics.fill(barX, barY, barX + PROGRESS_BAR_WIDTH, barY + PROGRESS_BAR_HEIGHT, 0xFF242B34);
        if (progress > 0) {
            guiGraphics.fill(barX, barY, barX + progress, barY + PROGRESS_BAR_HEIGHT, 0xFF60DA84);
        }

        int overallProgress = menu.overallProgressScaled(PROGRESS_BAR_WIDTH);
        int overallBarX = currentX + 6;
        int overallBarY = y + 102;
        drawPanel(guiGraphics, overallBarX - 1, overallBarY - 1, PROGRESS_BAR_WIDTH + 2, PROGRESS_BAR_HEIGHT + 2, 0xFF101318, 0xFF2F3540);
        guiGraphics.fill(overallBarX, overallBarY, overallBarX + PROGRESS_BAR_WIDTH, overallBarY + PROGRESS_BAR_HEIGHT, 0xFF242B34);
        if (overallProgress > 0) {
            guiGraphics.fill(overallBarX, overallBarY, overallBarX + overallProgress, overallBarY + PROGRESS_BAR_HEIGHT, 0xFF58C4E0);
        }

        int statusColor = switch (menu.labStatus()) {
            case LabBlockEntity.STATUS_WORKING -> 0xFF5EDB79;
            case LabBlockEntity.STATUS_NOT_ENOUGH_MATERIALS -> 0xFFE2AA4F;
            default -> 0xFFDB4F4F;
        };
        guiGraphics.fill(statusX + 4, topY + 12, statusX + 8, topY + 16, statusColor);
        ItemStack researchIcon = menu.activeResearchIcon();
        if (!researchIcon.isEmpty()) {
            guiGraphics.renderItem(researchIcon, currentX + (CURRENT_WIDTH - 16) / 2, topY + 66);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int statusX = OUTER_PADDING;
        int currentX = statusX + STATUS_WIDTH + COLUMN_GAP;
        int materialX = currentX + CURRENT_WIDTH + COLUMN_GAP;

        guiGraphics.drawString(font, title, 11, 9, 0xE6EBF4, false);

        Component status = switch (menu.labStatus()) {
            case LabBlockEntity.STATUS_WORKING -> Component.translatable("screen.incore.research_lab.status.working");
            case LabBlockEntity.STATUS_NOT_ENOUGH_MATERIALS -> Component.translatable("screen.incore.research_lab.status.not_enough_materials");
            default -> Component.translatable("screen.incore.research_lab.status.no_research_selected");
        };
        guiGraphics.drawString(font, Component.literal("Status"), statusX + 4, 30, TEXT_COLOR, false);
        drawWrappedText(guiGraphics, status, statusX + 12, 42, STATUS_WIDTH - 16, 2, TEXT_COLOR);
        String progressText = Component.translatable("screen.incore.research_lab.progress", menu.progress(), menu.displayMaxProgress()).getString();
        guiGraphics.drawString(font, Component.literal(trimToWidth(progressText, STATUS_WIDTH - 8)), statusX + 4, 84, TEXT_COLOR, false);

        guiGraphics.drawString(font, Component.literal("Current"), currentX + 4, 30, TEXT_COLOR, false);
        List<FormattedCharSequence> wrappedCurrent = font.split(Component.literal(menu.activeResearchTitle()), CURRENT_WIDTH - 8);
        int currentY = 42;
        for (int i = 0; i < wrappedCurrent.size() && i < 3; i++) {
            guiGraphics.drawString(font, wrappedCurrent.get(i), currentX + 4, currentY, TEXT_COLOR);
            currentY += font.lineHeight;
        }

        String overallProgressText = Component.translatable(
                "screen.incore.research_lab.overall_progress",
                menu.overallProgress(),
                menu.overallMaxProgress()
        ).getString();
        guiGraphics.drawString(font, Component.literal(trimToWidth(overallProgressText, CURRENT_WIDTH - 8)), currentX + 4, 88, TEXT_COLOR, false);

        guiGraphics.drawString(font, Component.translatable("screen.incore.research_lab.materials"), materialX + 4, 30, TEXT_COLOR, false);
        guiGraphics.drawString(font, playerInventoryTitle, 16, INVENTORY_SECTION_Y + 3, TEXT_COLOR, false);
    }

    private void drawWrappedText(GuiGraphics guiGraphics, Component text, int x, int y, int width, int maxLines, int color) {
        List<FormattedCharSequence> lines = font.split(text, Math.max(0, width));
        for (int i = 0; i < lines.size() && i < maxLines; i++) {
            guiGraphics.drawString(font, lines.get(i), x, y + i * font.lineHeight, color);
        }
    }

    private String trimToWidth(String text, int maxWidth) {
        if (font.width(text) <= maxWidth) {
            return text;
        }
        String suffix = "...";
        int suffixWidth = font.width(suffix);
        String body = font.plainSubstrByWidth(text, Math.max(0, maxWidth - suffixWidth));
        return body + suffix;
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

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
