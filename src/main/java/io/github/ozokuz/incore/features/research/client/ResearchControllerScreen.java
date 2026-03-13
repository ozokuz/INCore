package io.github.ozokuz.incore.features.research.client;

import io.github.ozokuz.incore.features.research.station.ResearchControllerMenu;
import io.github.ozokuz.incore.features.research.station.ResearchPowerFamily;
import io.github.ozokuz.incore.features.research.state.ResearchQueueStatus;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class ResearchControllerScreen extends StationStatusScreen<ResearchControllerMenu> {
    public ResearchControllerScreen(ResearchControllerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageHeight = 178;
    }

    @Override
    protected int accentColor() {
        return 0xFF5C8F6E;
    }

    @Override
    protected void renderStatusBody(GuiGraphics guiGraphics, int left, int top, int mouseX, int mouseY) {
        int leftColumn = left + 18;
        int rightColumn = left + 96;
        int y = top + 30;

        drawKeyValue(guiGraphics, leftColumn, y, Component.translatable("screen.incore.research_controller.structure"), Component.translatable(menu.formed() ? "screen.incore.research_controller.structure.formed" : "screen.incore.research_controller.structure.incomplete"), menu.formed() ? okColor() : warnColor());
        drawKeyValue(guiGraphics, leftColumn, y + 12, Component.translatable("screen.incore.research_controller.team"), Component.translatable(menu.teamLinked() ? "screen.incore.common.linked" : "screen.incore.common.missing"), menu.teamLinked() ? okColor() : warnColor());
        drawKeyValue(guiGraphics, leftColumn, y + 24, Component.translatable("screen.incore.research_controller.tier"), Component.literal(Integer.toString(menu.stationTier())), valueColor());
        drawKeyValue(guiGraphics, leftColumn, y + 36, Component.translatable("screen.incore.research_controller.power_family"), powerFamily(menu.powerFamily()), valueColor());
        drawKeyValue(guiGraphics, leftColumn, y + 48, Component.translatable("screen.incore.research_controller.input_tier"), Component.literal(Integer.toString(menu.powerInputTier())), valueColor());
        drawKeyValue(guiGraphics, leftColumn, y + 60, Component.translatable("screen.incore.research_controller.inputs"), Component.literal(Integer.toString(menu.inputCount())), valueColor());
        drawKeyValue(guiGraphics, leftColumn, y + 72, Component.translatable("screen.incore.research_controller.parts"), Component.literal(Integer.toString(menu.connectedPartCount())), valueColor());

        drawKeyValue(guiGraphics, rightColumn, y, Component.translatable("screen.incore.research_controller.available_power"), Component.literal(Integer.toString(menu.availablePower())), okColor());
        drawKeyValue(guiGraphics, rightColumn, y + 12, Component.translatable("screen.incore.research_controller.outputs"), Component.literal(Integer.toString(menu.outputPortCount())), valueColor());
        drawKeyValue(guiGraphics, rightColumn, y + 24, Component.translatable("screen.incore.research_controller.logic"), presence(menu.hasLogicHousing()), menu.hasLogicHousing() ? okColor() : warnColor());
        drawKeyValue(guiGraphics, rightColumn, y + 36, Component.translatable("screen.incore.research_controller.drive"), presence(menu.hasResearchDrive()), menu.hasResearchDrive() ? okColor() : warnColor());
        drawKeyValue(guiGraphics, rightColumn, y + 48, Component.translatable("screen.incore.research_controller.storage"), presence(menu.hasMaterialStorage()), menu.hasMaterialStorage() ? okColor() : warnColor());
        drawKeyValue(guiGraphics, rightColumn, y + 60, Component.translatable("screen.incore.research_controller.augmenter"), presence(menu.hasAugmenter()), menu.hasAugmenter() ? okColor() : warnColor());
        drawKeyValue(guiGraphics, rightColumn, y + 72, Component.translatable("screen.incore.research_controller.link_port"), presence(menu.hasLinkPort()), menu.hasLinkPort() ? okColor() : warnColor());
        drawKeyValue(guiGraphics, leftColumn, y + 84, Component.translatable("screen.incore.research_controller.network_mode"), Component.translatable(menu.stationNetworkLinked() ? "screen.incore.research_controller.network_mode.linked" : "screen.incore.research_controller.network_mode.singleton"), menu.stationNetworkLinked() ? okColor() : valueColor());
        drawKeyValue(guiGraphics, rightColumn, y + 84, Component.translatable("screen.incore.research_controller.team_networks"), Component.literal(Integer.toString(menu.teamStationNetworkCount())), menu.teamStationNetworkValid() ? valueColor() : warnColor());
        drawKeyValue(guiGraphics, leftColumn, y + 96, Component.translatable("screen.incore.research_controller.network_status"), Component.translatable(menu.teamStationNetworkValid() ? "screen.incore.research_controller.network_status.valid" : "screen.incore.research_controller.network_status.blocked"), menu.teamStationNetworkValid() ? okColor() : warnColor());
        drawKeyValue(guiGraphics, rightColumn, y + 96, Component.translatable("screen.incore.research_controller.run"), runValue(), menu.hasActiveRun() ? okColor() : valueColor());
        drawKeyValue(guiGraphics, leftColumn, y + 108, Component.translatable("screen.incore.research_controller.run_progress"), progressValue(), menu.hasActiveRun() ? valueColor() : warnColor());
        drawKeyValue(guiGraphics, rightColumn, y + 108, Component.translatable("screen.incore.research_controller.run_status"), runStatus(menu.queueStatus()), runStatusColor(menu.queueStatus()));
        drawRunBar(guiGraphics, left + 18, y + 124, imageWidth - 36, menu.runProgressScaled(imageWidth - 36), menu.hasActiveRun());
    }

    private static Component powerFamily(ResearchPowerFamily family) {
        if (family == null) {
            return Component.translatable("screen.incore.power_input.family.none");
        }
        return switch (family) {
            case ELECTRIC -> Component.translatable("screen.incore.power_input.family.electric");
            case MECHANICAL -> Component.translatable("screen.incore.power_input.family.mechanical");
            case BURNER -> Component.translatable("screen.incore.power_input.family.none");
        };
    }

    private static Component presence(boolean present) {
        return Component.translatable(present ? "screen.incore.common.present" : "screen.incore.common.missing");
    }

    private Component runValue() {
        if (!menu.hasActiveRun()) {
            return Component.translatable("screen.incore.research_controller.run.none");
        }
        return Component.literal(Math.min(menu.requiredRuns(), menu.completedRuns() + 1) + "/" + menu.requiredRuns());
    }

    private Component progressValue() {
        if (!menu.hasActiveRun()) {
            return Component.translatable("screen.incore.research_controller.run.none");
        }
        return Component.literal(menu.runTickProgress() + "/" + menu.runTickRequired());
    }

    private Component runStatus(ResearchQueueStatus status) {
        if (status == null) {
            return Component.translatable("screen.incore.research_controller.run_status.idle");
        }
        return switch (status) {
            case QUEUED -> Component.translatable("screen.incore.research_controller.run_status.queued");
            case RUNNING -> Component.translatable("screen.incore.research_controller.run_status.running");
            case PAUSED_MISSING_INPUTS -> Component.translatable("screen.incore.research_controller.run_status.missing_inputs");
            case PAUSED_NO_POWER -> Component.translatable("screen.incore.research_controller.run_status.no_power");
            case PAUSED_NETWORK_CONFLICT -> Component.translatable("screen.incore.research_controller.run_status.network_conflict");
        };
    }

    private int runStatusColor(ResearchQueueStatus status) {
        return status == ResearchQueueStatus.RUNNING ? okColor() : (status == null ? valueColor() : warnColor());
    }

    private void drawRunBar(GuiGraphics guiGraphics, int x, int y, int width, int fill, boolean active) {
        float ratio = active && width > 0 ? fill / (float) width : 0.0F;
        ResearchScreenRenderer.drawProgressBar(guiGraphics, x, y, width, 6, ratio);
    }
}
