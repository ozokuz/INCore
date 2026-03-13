package io.github.ozokuz.incore.client.features.research;

import io.github.ozokuz.incore.features.research.station.CrudeResearchStationMenu;
import io.github.ozokuz.incore.features.research.station.CrudeResearchStationBlockEntity;
import io.github.ozokuz.incore.features.research.state.ResearchQueueStatus;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

public class CrudeResearchStationScreen extends AbstractContainerScreen<CrudeResearchStationMenu> {
    private static final int ACCENT_COLOR = 0xFF7CB9FF;
    private static final int BURN_FILL = 0xFFEE9B34;
    private static final int PLAYER_SLOT_OUTER = 0xFF3A312B;
    private static final int PLAYER_SLOT_INNER = 0xFF1B1714;
    private static final int PLAYER_SLOT_HIGHLIGHT = 0xFF7F6A5C;

    public CrudeResearchStationScreen(CrudeResearchStationMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;

        ResearchScreenRenderer.drawAccentedWindow(guiGraphics, x, y, imageWidth, imageHeight, ACCENT_COLOR);
        ResearchScreenRenderer.drawAccentedPanel(guiGraphics, x + 8, y + 18, imageWidth - 16, 54, ACCENT_COLOR);
        ResearchScreenRenderer.drawAccentedPanel(guiGraphics, x + 8, y + 80, imageWidth - 16, 78, ACCENT_COLOR);

        ResearchScreenRenderer.drawMachineSlotFrame(guiGraphics, x + CrudeResearchStationMenu.FUEL_X, y + CrudeResearchStationMenu.FUEL_Y, ACCENT_COLOR);
        ResearchScreenRenderer.drawMachineSlotFrame(guiGraphics, x + CrudeResearchStationMenu.LOGIC_X, y + CrudeResearchStationMenu.LOGIC_Y, ACCENT_COLOR);
        ResearchScreenRenderer.drawMachineSlotFrame(guiGraphics, x + CrudeResearchStationMenu.DRIVE_X, y + CrudeResearchStationMenu.DRIVE_Y, ACCENT_COLOR);
        drawPlayerInventorySlots(guiGraphics, x, y);

        int burnProgress = menu.burnProgressScaled(18);
        int burnX = x + CrudeResearchStationMenu.FUEL_X;
        int burnY = y + CrudeResearchStationMenu.FUEL_Y + 20;
        ResearchScreenRenderer.drawProgressBar(guiGraphics, burnX - 1, burnY - 1, 18, 5, burnProgress / 18.0F, BURN_FILL);

        int runBarX = x + 10;
        int runBarY = y + 72;
        int runBarW = imageWidth - 20;
        int runFill = menu.runProgressScaled(runBarW);
        ResearchScreenRenderer.drawCompactProgressBar(guiGraphics, runBarX, runBarY, runBarW, runFill / (float) runBarW, ResearchScreenRenderer.theme().progress().fill());
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, title, 8, 6, ResearchScreenRenderer.titleText(), false);
        guiGraphics.drawString(font, Component.translatable("screen.incore.crude_research_station.fuel"), 35, 20, ResearchScreenRenderer.secondaryText(), false);
        guiGraphics.drawString(font, Component.translatable("screen.incore.crude_research_station.logic"), 73, 20, ResearchScreenRenderer.secondaryText(), false);
        guiGraphics.drawString(font, Component.translatable("screen.incore.crude_research_station.drive"), 109, 20, ResearchScreenRenderer.secondaryText(), false);

        guiGraphics.drawString(
                font,
                Component.translatable("screen.incore.crude_research_station.rp", menu.researchPowerBuffer()),
                10,
                52,
                ResearchScreenRenderer.successText(),
                false
        );

        String linkedKey = menu.hasTeamBinding()
                ? "screen.incore.crude_research_station.team.linked"
                : "screen.incore.crude_research_station.team.unlinked";
        int linkedColor = menu.hasTeamBinding() ? ResearchScreenRenderer.accentText() : ResearchScreenRenderer.dangerText();
        guiGraphics.drawString(font, Component.translatable(linkedKey), 10, 64, linkedColor, false);

        int queueStatusOrdinal = menu.queueStatusOrdinal();
        int completedRuns = menu.completedRuns();
        int requiredRuns = menu.requiredRuns();
        Component runLabel = queueStatusOrdinal < 0
                ? Component.translatable("screen.incore.crude_research_station.run_idle")
                : Component.translatable("screen.incore.crude_research_station.run", Math.min(requiredRuns, completedRuns + 1), requiredRuns);
        guiGraphics.drawString(
                font,
                runLabel,
                10,
                79,
                ResearchScreenRenderer.accentText(),
                false
        );

        Component runStatus = runStatusText(queueStatusOrdinal);
        guiGraphics.drawString(font, runStatus, 96, 79, ResearchScreenRenderer.secondaryText(), false);

        guiGraphics.drawString(font, playerInventoryTitle, 8, 86, ResearchScreenRenderer.secondaryText(), false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        ResearchScreenRenderer.drawBackdrop(guiGraphics, width, height);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    private static Component runStatusText(int statusOrdinal) {
        if (statusOrdinal < 0 || statusOrdinal >= ResearchQueueStatus.values().length) {
            return Component.translatable("screen.incore.crude_research_station.status.idle");
        }

        ResearchQueueStatus status = ResearchQueueStatus.values()[statusOrdinal];
        return switch (status) {
            case RUNNING -> Component.translatable("screen.incore.crude_research_station.status.running");
            case PAUSED_MISSING_INPUTS -> Component.translatable("screen.incore.crude_research_station.status.missing_inputs");
            case PAUSED_NO_POWER -> Component.translatable("screen.incore.crude_research_station.status.no_power");
            default -> Component.translatable("screen.incore.crude_research_station.status.queued");
        };
    }

    private void drawPlayerInventorySlots(GuiGraphics guiGraphics, int left, int top) {
        for (int slotIndex = CrudeResearchStationBlockEntity.SLOT_COUNT; slotIndex < menu.slots.size(); slotIndex++) {
            Slot slot = menu.slots.get(slotIndex);
            if (!slot.isActive()) {
                continue;
            }
            ResearchScreenRenderer.drawSlotFrame(
                    guiGraphics,
                    left + slot.x,
                    top + slot.y,
                    PLAYER_SLOT_OUTER,
                    PLAYER_SLOT_INNER,
                    PLAYER_SLOT_HIGHLIGHT
            );
        }
    }
}
