package io.github.ozokuz.incore.client.features.research;

import io.github.ozokuz.incore.features.research.station.CrudeResearchStationMenu;
import io.github.ozokuz.incore.features.research.state.ResearchQueueStatus;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class CrudeResearchStationScreen extends AbstractContainerScreen<CrudeResearchStationMenu> {
    public CrudeResearchStationScreen(CrudeResearchStationMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;

        drawPanel(guiGraphics, x, y, imageWidth, imageHeight, 0xCC111724, 0xFF4B596F);
        drawPanel(guiGraphics, x + 8, y + 18, imageWidth - 16, 54, 0x99202A3A, 0xFF4B596F);
        drawPanel(guiGraphics, x + 8, y + 80, imageWidth - 16, 78, 0x99202A3A, 0xFF4B596F);

        drawSlot(guiGraphics, x + CrudeResearchStationMenu.FUEL_X, y + CrudeResearchStationMenu.FUEL_Y);
        drawSlot(guiGraphics, x + CrudeResearchStationMenu.LOGIC_X, y + CrudeResearchStationMenu.LOGIC_Y);
        drawSlot(guiGraphics, x + CrudeResearchStationMenu.DRIVE_X, y + CrudeResearchStationMenu.DRIVE_Y);

        int burnProgress = menu.burnProgressScaled(18);
        int burnX = x + CrudeResearchStationMenu.FUEL_X;
        int burnY = y + CrudeResearchStationMenu.FUEL_Y + 20;
        guiGraphics.fill(burnX - 1, burnY - 1, burnX + 17, burnY + 4, 0xFF3D4A5F);
        guiGraphics.fill(burnX, burnY, burnX + 16, burnY + 3, 0xFF1B2330);
        if (burnProgress > 0) {
            guiGraphics.fill(burnX, burnY, burnX + Math.min(16, burnProgress), burnY + 3, 0xFFEE9B34);
        }

        int runBarX = x + 10;
        int runBarY = y + 72;
        int runBarW = imageWidth - 20;
        int runFill = menu.runProgressScaled(runBarW);
        guiGraphics.fill(runBarX, runBarY, runBarX + runBarW, runBarY + 5, 0xFF243143);
        guiGraphics.fill(runBarX + 1, runBarY + 1, runBarX + runBarW - 1, runBarY + 4, 0xFF101722);
        if (runFill > 0) {
            guiGraphics.fill(runBarX + 1, runBarY + 1, runBarX + Math.min(runBarW - 1, runFill), runBarY + 4, 0xFF55A9E6);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, title, 8, 6, 0xE8EEF8, false);
        guiGraphics.drawString(font, Component.translatable("screen.incore.crude_research_station.fuel"), 35, 20, 0xD6E0EF, false);
        guiGraphics.drawString(font, Component.translatable("screen.incore.crude_research_station.logic"), 73, 20, 0xD6E0EF, false);
        guiGraphics.drawString(font, Component.translatable("screen.incore.crude_research_station.drive"), 109, 20, 0xD6E0EF, false);

        guiGraphics.drawString(
                font,
                Component.translatable("screen.incore.crude_research_station.rp", menu.researchPowerBuffer()),
                10,
                52,
                0x8EF7A0,
                false
        );

        String linkedKey = menu.hasTeamBinding()
                ? "screen.incore.crude_research_station.team.linked"
                : "screen.incore.crude_research_station.team.unlinked";
        int linkedColor = menu.hasTeamBinding() ? 0x8CC5F3 : 0xFF7A7A;
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
                0xBFE2FF,
                false
        );

        Component runStatus = runStatusText(queueStatusOrdinal);
        guiGraphics.drawString(font, runStatus, 96, 79, 0xC8D8EA, false);

        guiGraphics.drawString(font, playerInventoryTitle, 8, 86, 0xD6E0EF, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    private static void drawPanel(GuiGraphics guiGraphics, int x, int y, int width, int height, int fillColor, int borderColor) {
        guiGraphics.fill(x, y, x + width, y + height, fillColor);
        guiGraphics.fill(x, y, x + width, y + 1, borderColor);
        guiGraphics.fill(x, y + height - 1, x + width, y + height, borderColor);
        guiGraphics.fill(x, y, x + 1, y + height, borderColor);
        guiGraphics.fill(x + width - 1, y, x + width, y + height, borderColor);
    }

    private static void drawSlot(GuiGraphics guiGraphics, int x, int y) {
        drawPanel(guiGraphics, x - 1, y - 1, 18, 18, 0xFF181F2A, 0xFF46566F);
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
}
