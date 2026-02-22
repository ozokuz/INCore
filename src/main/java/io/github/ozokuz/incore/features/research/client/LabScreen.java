package io.github.ozokuz.incore.features.research.client;

import io.github.ozokuz.incore.Config;
import io.github.ozokuz.incore.features.research.LabBlockEntity;
import io.github.ozokuz.incore.features.research.LabMenu;
import io.github.ozokuz.incore.features.research.LabTier;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Locale;

public class LabScreen extends AbstractContainerScreen<LabMenu> {
    private static final int OUTER_PADDING = 8;
    private static final int COLUMN_GAP = 6;
    private static final int TOP_SECTION_Y = 24;
    private static final int TOP_SECTION_HEIGHT = 130;
    private static final int INVENTORY_SECTION_Y = 158;

    private static final int LEFT_PANEL_WIDTH = 168;
    private static final int RIGHT_PANEL_WIDTH = 166;

    private static final int PROGRESS_BAR_WIDTH = LEFT_PANEL_WIDTH - 14;
    private static final int PROGRESS_BAR_HEIGHT = 8;
    private static final int TEXT_COLOR = 0xCDD3DE;

    public LabScreen(LabMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 356;
        this.imageHeight = 248;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;

        int leftPanelX = x + OUTER_PADDING;
        int rightPanelX = leftPanelX + LEFT_PANEL_WIDTH + COLUMN_GAP;
        int topY = y + TOP_SECTION_Y;

        drawPanel(guiGraphics, x, y, imageWidth, imageHeight, 0xFF13161A, 0xFF4A4F5A);
        drawPanel(guiGraphics, x + 5, y + 5, imageWidth - 10, 14, 0xFF20252C, 0xFF3D4350);
        drawPanel(guiGraphics, leftPanelX, topY, LEFT_PANEL_WIDTH, TOP_SECTION_HEIGHT, 0xFF1A1F26, 0xFF363D49);
        drawPanel(guiGraphics, rightPanelX, topY, RIGHT_PANEL_WIDTH, TOP_SECTION_HEIGHT, 0xFF1A1F26, 0xFF363D49);
        drawPanel(guiGraphics, x + OUTER_PADDING, y + INVENTORY_SECTION_Y, imageWidth - (OUTER_PADDING * 2), imageHeight - INVENTORY_SECTION_Y - OUTER_PADDING, 0xFF1A1F26, 0xFF363D49);

        for (Slot slot : menu.slots) {
            drawSlotFrame(guiGraphics, x + slot.x, y + slot.y);
        }

        int runProgress = menu.progressScaled(PROGRESS_BAR_WIDTH);
        int runBarX = leftPanelX + 7;
        int runBarY = topY + 95;
        drawProgressBar(guiGraphics, runBarX, runBarY, runProgress, 0xFF60DA84);

        int overallProgress = menu.overallProgressScaled(PROGRESS_BAR_WIDTH);
        int overallBarY = runBarY + 23;
        drawProgressBar(guiGraphics, runBarX, overallBarY, overallProgress, 0xFF58C4E0);

        int statusColor = switch (menu.labStatus()) {
            case LabBlockEntity.STATUS_WORKING -> 0xFF5EDB79;
            case LabBlockEntity.STATUS_NOT_ENOUGH_MATERIALS -> 0xFFE2AA4F;
            default -> 0xFFDB4F4F;
        };
        guiGraphics.fill(leftPanelX + 6, topY + 18, leftPanelX + 10, topY + 22, statusColor);

        ItemStack researchIcon = menu.activeResearchIcon();
        if (!researchIcon.isEmpty()) {
            guiGraphics.renderItem(researchIcon, leftPanelX + LEFT_PANEL_WIDTH - 26, topY + 70);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int leftX = OUTER_PADDING;
        int rightX = leftX + LEFT_PANEL_WIDTH + COLUMN_GAP;

        guiGraphics.drawString(font, title, 11, 9, 0xE6EBF4, false);

        Component status = switch (menu.labStatus()) {
            case LabBlockEntity.STATUS_WORKING -> Component.translatable("screen.incore.research_lab.status.working");
            case LabBlockEntity.STATUS_NOT_ENOUGH_MATERIALS -> Component.translatable("screen.incore.research_lab.status.not_enough_materials");
            default -> Component.translatable("screen.incore.research_lab.status.no_research_selected");
        };

        Component tierName = switch (menu.labTier()) {
            case BURNER -> Component.translatable("block.incore.burner_lab");
            case MECHANICAL -> Component.translatable("block.incore.mechanical_lab");
            case MODULAR -> Component.translatable("block.incore.modular_lab");
        };

        guiGraphics.drawString(font, Component.literal("Status"), leftX + 6, TOP_SECTION_Y + 6, TEXT_COLOR, false);
        drawWrappedText(guiGraphics, status, leftX + 14, TOP_SECTION_Y + 18, LEFT_PANEL_WIDTH - 20, 2, TEXT_COLOR);
        guiGraphics.drawString(font, Component.translatable("jade.incore.lab.tier", tierName), leftX + 6, TOP_SECTION_Y + 42, TEXT_COLOR, false);

        guiGraphics.drawString(font, Component.literal("Current"), leftX + 6, TOP_SECTION_Y + 58, TEXT_COLOR, false);
        drawWrappedText(guiGraphics, Component.literal(menu.activeResearchTitle()), leftX + 6, TOP_SECTION_Y + 70, LEFT_PANEL_WIDTH - 34, 2, TEXT_COLOR);

        String runProgressText = Component.translatable("screen.incore.research_lab.progress", menu.progress(), menu.displayMaxProgress()).getString();
        guiGraphics.drawString(font, Component.literal(trimToWidth(runProgressText, LEFT_PANEL_WIDTH - 10)), leftX + 6, TOP_SECTION_Y + 84, TEXT_COLOR, false);

        String overallProgressText = Component.translatable(
                "screen.incore.research_lab.overall_progress",
                menu.overallProgress(),
                menu.overallMaxProgress()
        ).getString();
        guiGraphics.drawString(font, Component.literal(trimToWidth(overallProgressText, LEFT_PANEL_WIDTH - 10)), leftX + 6, TOP_SECTION_Y + 107, TEXT_COLOR, false);

        guiGraphics.drawString(font, Component.translatable("screen.incore.research_lab.power"), rightX + 6, TOP_SECTION_Y + 6, TEXT_COLOR, false);
        String powerLine = switch (menu.labTier()) {
            case BURNER -> "Fuel: " + menu.burnTime() + "/" + menu.burnTimeTotal();
            case MECHANICAL -> "RPM: " + menu.mechanicalRpm() + "  SU: " + menu.mechanicalStress();
            case MODULAR -> "FE: " + menu.energyStored() + "/" + menu.energyCapacity();
        };
        guiGraphics.drawString(font, Component.literal(trimToWidth(powerLine, RIGHT_PANEL_WIDTH - 10)), rightX + 6, TOP_SECTION_Y + 18, TEXT_COLOR, false);

        int materialLabelY = TOP_SECTION_Y + 30;
        if (menu.labTier() == LabTier.MODULAR) {
            guiGraphics.drawString(font, Component.translatable("screen.incore.research_lab.modules"), rightX + 6, TOP_SECTION_Y + 30, TEXT_COLOR, false);
            String speedLine = Component.translatable(
                    "screen.incore.research_lab.module_speed",
                    formatPercent(menu.modularSpeedBonus()),
                    formatPercent(Config.MODULAR_LAB_MAX_SPEED_BONUS.get().floatValue())
            ).getString();
            String productivityLine = Component.translatable(
                    "screen.incore.research_lab.module_productivity",
                    formatPercent(menu.modularProductivityBonus()),
                    formatPercent(Config.MODULAR_LAB_MAX_PRODUCTIVITY_BONUS.get().floatValue())
            ).getString();
            guiGraphics.drawString(font, Component.literal(trimToWidth(speedLine, RIGHT_PANEL_WIDTH - 10)), rightX + 6, TOP_SECTION_Y + 42, TEXT_COLOR, false);
            guiGraphics.drawString(font, Component.literal(trimToWidth(productivityLine, RIGHT_PANEL_WIDTH - 10)), rightX + 6, TOP_SECTION_Y + 54, TEXT_COLOR, false);
            materialLabelY += 36;
        }
        guiGraphics.drawString(font, Component.translatable("screen.incore.research_lab.materials"), rightX + 6, materialLabelY, TEXT_COLOR, false);

        guiGraphics.drawString(font, playerInventoryTitle, LabMenu.PLAYER_INVENTORY_X, INVENTORY_SECTION_Y + 4, TEXT_COLOR, false);
    }

    private void drawProgressBar(GuiGraphics guiGraphics, int x, int y, int progress, int fillColor) {
        drawPanel(guiGraphics, x - 1, y - 1, PROGRESS_BAR_WIDTH + 2, PROGRESS_BAR_HEIGHT + 2, 0xFF101318, 0xFF2F3540);
        guiGraphics.fill(x, y, x + PROGRESS_BAR_WIDTH, y + PROGRESS_BAR_HEIGHT, 0xFF242B34);
        if (progress > 0) {
            guiGraphics.fill(x, y, x + progress, y + PROGRESS_BAR_HEIGHT, fillColor);
        }
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

    private static String formatPercent(float ratio) {
        float percent = Math.max(0.0F, ratio) * 100.0F;
        if (Math.abs(percent - Math.round(percent)) < 0.0001F) {
            return Integer.toString(Math.round(percent));
        }
        return String.format(Locale.ROOT, "%.1f", percent);
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
