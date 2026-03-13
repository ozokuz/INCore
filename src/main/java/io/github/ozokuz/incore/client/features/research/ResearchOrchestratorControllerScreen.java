package io.github.ozokuz.incore.client.features.research;

import io.github.ozokuz.incore.client.features.machines.*;
import io.github.ozokuz.incore.features.machines.multiblock.*;

import io.github.ozokuz.incore.features.research.station.ResearchOrchestratorControllerMenu;
import io.github.ozokuz.incore.features.machines.multiblock.MachinePowerFamily;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class ResearchOrchestratorControllerScreen extends StationStatusScreen<ResearchOrchestratorControllerMenu> {
    public ResearchOrchestratorControllerScreen(ResearchOrchestratorControllerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageHeight = 170;
    }

    @Override
    protected int accentColor() {
        return 0xFF7E6AAE;
    }

    @Override
    protected void renderStatusBody(GuiGraphics guiGraphics, int left, int top, int mouseX, int mouseY) {
        int leftColumn = left + 18;
        int rightColumn = left + 96;
        int y = top + 30;

        drawKeyValue(guiGraphics, leftColumn, y, Component.translatable("screen.incore.research_orchestrator.structure"), state(menu.formed()), menu.formed() ? okColor() : warnColor());
        drawKeyValue(guiGraphics, leftColumn, y + 12, Component.translatable("screen.incore.research_orchestrator.team"), state(menu.teamLinked()), menu.teamLinked() ? okColor() : warnColor());
        drawKeyValue(guiGraphics, leftColumn, y + 24, Component.translatable("screen.incore.research_orchestrator.power_family"), powerFamily(menu.powerFamily()), valueColor());
        drawKeyValue(guiGraphics, leftColumn, y + 36, Component.translatable("screen.incore.research_orchestrator.input_tier"), Component.literal(Integer.toString(menu.powerInputTier())), valueColor());
        drawKeyValue(guiGraphics, leftColumn, y + 48, Component.translatable("screen.incore.research_orchestrator.inputs"), Component.literal(Integer.toString(menu.inputCount())), valueColor());
        drawKeyValue(guiGraphics, leftColumn, y + 60, Component.translatable("screen.incore.research_orchestrator.parts"), Component.literal(Integer.toString(menu.connectedPartCount())), valueColor());
        drawKeyValue(guiGraphics, leftColumn, y + 72, Component.translatable("screen.incore.research_orchestrator.link_ports"), Component.literal(Integer.toString(menu.linkingPortCount())), valueColor());
        drawKeyValue(guiGraphics, leftColumn, y + 84, Component.translatable("screen.incore.research_orchestrator.wireless_link"), state(menu.hasWirelessLink()), menu.hasWirelessLink() ? okColor() : valueColor());
        drawKeyValue(guiGraphics, leftColumn, y + 96, Component.translatable("screen.incore.research_orchestrator.drive"), state(menu.hasOrchestrationDrive()), menu.hasOrchestrationDrive() ? okColor() : warnColor());
        drawKeyValue(guiGraphics, leftColumn, y + 108, Component.translatable("screen.incore.research_orchestrator.augmenter"), state(menu.hasAugmenter()), menu.hasAugmenter() ? okColor() : valueColor());

        drawKeyValue(guiGraphics, rightColumn, y, Component.translatable("screen.incore.research_orchestrator.required"), state(menu.orchestratorRequired()), menu.orchestratorRequired() ? warnColor() : valueColor());
        drawKeyValue(guiGraphics, rightColumn, y + 12, Component.translatable("screen.incore.research_orchestrator.present"), state(menu.orchestratorPresent()), menu.orchestratorPresent() ? okColor() : warnColor());
        drawKeyValue(guiGraphics, rightColumn, y + 24, Component.translatable("screen.incore.research_orchestrator.valid"), state(menu.orchestratorValid()), menu.orchestratorValid() ? okColor() : warnColor());
        drawKeyValue(guiGraphics, rightColumn, y + 36, Component.translatable("screen.incore.research_orchestrator.cable_capacity"), Component.literal(Integer.toString(menu.cableCapacityPerLink())), valueColor());
        drawKeyValue(guiGraphics, rightColumn, y + 48, Component.translatable("screen.incore.research_orchestrator.wireless_capacity"), Component.literal(Integer.toString(menu.wirelessCapacity())), valueColor());
        drawKeyValue(guiGraphics, rightColumn, y + 60, Component.translatable("screen.incore.research_orchestrator.wireless_range"), wirelessRangeValue(), valueColor());
        drawKeyValue(guiGraphics, rightColumn, y + 72, Component.translatable("screen.incore.research_orchestrator.wireless_members"), Component.literal(menu.validWirelessStations() + "/" + menu.invalidWirelessStations()), menu.invalidWirelessStations() > 0 ? warnColor() : valueColor());
        drawKeyValue(guiGraphics, rightColumn, y + 84, Component.translatable("screen.incore.research_orchestrator.networks"), Component.literal(Integer.toString(menu.teamStationNetworkCount())), menu.teamStationNetworkValid() ? valueColor() : warnColor());
        drawKeyValue(guiGraphics, rightColumn, y + 96, Component.translatable("screen.incore.research_orchestrator.network_status"), Component.translatable(menu.teamStationNetworkValid() ? "screen.incore.research_orchestrator.network_status.valid" : "screen.incore.research_orchestrator.network_status.blocked"), menu.teamStationNetworkValid() ? okColor() : warnColor());
        drawKeyValue(guiGraphics, rightColumn, y + 108, Component.translatable("screen.incore.research_orchestrator.mode"), wirelessModeValue(), valueColor());
    }

    private static Component powerFamily(MachinePowerFamily family) {
        if (family == null) {
            return Component.translatable("screen.incore.power_input.family.none");
        }
        return switch (family) {
            case ELECTRIC -> Component.translatable("screen.incore.power_input.family.electric");
            case MECHANICAL -> Component.translatable("screen.incore.power_input.family.mechanical");
            case BURNER -> Component.translatable("screen.incore.power_input.family.none");
        };
    }

    private static Component state(boolean present) {
        return Component.translatable(present ? "screen.incore.common.present" : "screen.incore.common.missing");
    }

    private Component wirelessRangeValue() {
        if (menu.interdimensionalWireless()) {
            return Component.translatable("screen.incore.research_orchestrator.wireless_range.interdimensional");
        }
        if (menu.infiniteWireless()) {
            return Component.translatable("screen.incore.research_orchestrator.wireless_range.infinite");
        }
        return Component.literal(Integer.toString(menu.wirelessRange()));
    }

    private Component wirelessModeValue() {
        if (menu.interdimensionalWireless()) {
            return Component.translatable("screen.incore.research_orchestrator.mode.interdimensional");
        }
        if (menu.infiniteWireless()) {
            return Component.translatable("screen.incore.research_orchestrator.mode.infinite");
        }
        return Component.translatable("screen.incore.research_orchestrator.mode.local");
    }
}
