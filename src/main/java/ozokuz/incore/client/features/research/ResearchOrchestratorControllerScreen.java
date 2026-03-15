package ozokuz.incore.client.features.research;

import ozokuz.incore.client.features.machines.StationStatusScreen;
import ozokuz.incore.features.research.station.ResearchOrchestratorControllerMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class ResearchOrchestratorControllerScreen extends StationStatusScreen<ResearchOrchestratorControllerMenu> {
    private static final int LEFT_VALUE_OFFSET = 78;
    private static final int RIGHT_VALUE_OFFSET = 90;

    public ResearchOrchestratorControllerScreen(ResearchOrchestratorControllerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 350;
        this.imageHeight = 170;
    }

    @Override
    protected int accentColor() {
        return 0xFF7E6AAE;
    }

    @Override
    protected void renderStatusBody(GuiGraphics guiGraphics, int left, int top, int mouseX, int mouseY) {
        int leftColumn = left + 14;
        int rightColumn = left + 176;
        int y = top + 30;

        drawKeyValue(guiGraphics, leftColumn, y, Component.translatable("screen.incore.research_orchestrator.structure"), state(menu.formed()), menu.formed() ? okColor() : warnColor(), LEFT_VALUE_OFFSET);
        drawKeyValue(guiGraphics, leftColumn, y + 12, Component.translatable("screen.incore.research_orchestrator.team"), state(menu.teamLinked()), menu.teamLinked() ? okColor() : warnColor(), LEFT_VALUE_OFFSET);
        drawKeyValue(guiGraphics, leftColumn, y + 24, Component.translatable("screen.incore.research_orchestrator.power_family"), powerFamilyLabel(menu.powerFamily()), valueColor(), LEFT_VALUE_OFFSET);
        drawKeyValue(guiGraphics, leftColumn, y + 36, Component.translatable("screen.incore.research_orchestrator.input_tier"), Component.literal(Integer.toString(menu.powerInputTier())), valueColor(), LEFT_VALUE_OFFSET);
        drawKeyValue(guiGraphics, leftColumn, y + 48, Component.translatable("screen.incore.research_orchestrator.inputs"), Component.literal(Integer.toString(menu.inputCount())), valueColor(), LEFT_VALUE_OFFSET);
        drawKeyValue(guiGraphics, leftColumn, y + 60, Component.translatable("screen.incore.research_orchestrator.parts"), Component.literal(Integer.toString(menu.connectedPartCount())), valueColor(), LEFT_VALUE_OFFSET);
        drawKeyValue(guiGraphics, leftColumn, y + 72, Component.translatable("screen.incore.research_orchestrator.link_ports"), Component.literal(Integer.toString(menu.linkingPortCount())), valueColor(), LEFT_VALUE_OFFSET);
        drawKeyValue(guiGraphics, leftColumn, y + 84, Component.translatable("screen.incore.research_orchestrator.wireless_link"), state(menu.hasWirelessLink()), menu.hasWirelessLink() ? okColor() : valueColor(), LEFT_VALUE_OFFSET);
        drawKeyValue(guiGraphics, leftColumn, y + 96, Component.translatable("screen.incore.research_orchestrator.drive"), state(menu.hasOrchestrationDrive()), menu.hasOrchestrationDrive() ? okColor() : warnColor(), LEFT_VALUE_OFFSET);
        drawKeyValue(guiGraphics, leftColumn, y + 108, Component.translatable("screen.incore.research_orchestrator.augmenter"), state(menu.hasAugmenter()), menu.hasAugmenter() ? okColor() : valueColor(), LEFT_VALUE_OFFSET);

        drawKeyValue(guiGraphics, rightColumn, y, Component.translatable("screen.incore.research_orchestrator.required"), state(menu.orchestratorRequired()), menu.orchestratorRequired() ? warnColor() : valueColor(), RIGHT_VALUE_OFFSET);
        drawKeyValue(guiGraphics, rightColumn, y + 12, Component.translatable("screen.incore.research_orchestrator.present"), state(menu.orchestratorPresent()), menu.orchestratorPresent() ? okColor() : warnColor(), RIGHT_VALUE_OFFSET);
        drawKeyValue(guiGraphics, rightColumn, y + 24, Component.translatable("screen.incore.research_orchestrator.valid"), state(menu.orchestratorValid()), menu.orchestratorValid() ? okColor() : warnColor(), RIGHT_VALUE_OFFSET);
        drawKeyValue(guiGraphics, rightColumn, y + 36, Component.translatable("screen.incore.research_orchestrator.cable_capacity"), Component.literal(Integer.toString(menu.cableCapacityPerLink())), valueColor(), RIGHT_VALUE_OFFSET);
        drawKeyValue(guiGraphics, rightColumn, y + 48, Component.translatable("screen.incore.research_orchestrator.wireless_capacity"), Component.literal(Integer.toString(menu.wirelessCapacity())), valueColor(), RIGHT_VALUE_OFFSET);
        drawKeyValue(guiGraphics, rightColumn, y + 60, Component.translatable("screen.incore.research_orchestrator.wireless_range"), wirelessRangeValue(), valueColor(), RIGHT_VALUE_OFFSET);
        drawKeyValue(guiGraphics, rightColumn, y + 72, Component.translatable("screen.incore.research_orchestrator.wireless_members"), Component.literal(menu.validWirelessStations() + "/" + menu.invalidWirelessStations()), menu.invalidWirelessStations() > 0 ? warnColor() : valueColor(), RIGHT_VALUE_OFFSET);
        drawKeyValue(guiGraphics, rightColumn, y + 84, Component.translatable("screen.incore.research_orchestrator.networks"), Component.literal(Integer.toString(menu.teamStationNetworkCount())), menu.teamStationNetworkValid() ? valueColor() : warnColor(), RIGHT_VALUE_OFFSET);
        drawKeyValue(guiGraphics, rightColumn, y + 96, Component.translatable("screen.incore.research_orchestrator.network_status"), Component.translatable(menu.teamStationNetworkValid() ? "screen.incore.research_orchestrator.network_status.valid" : "screen.incore.research_orchestrator.network_status.blocked"), menu.teamStationNetworkValid() ? okColor() : warnColor(), RIGHT_VALUE_OFFSET);
        drawKeyValue(guiGraphics, rightColumn, y + 108, Component.translatable("screen.incore.research_orchestrator.mode"), wirelessModeValue(), valueColor(), RIGHT_VALUE_OFFSET);
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
