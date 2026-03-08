package io.github.ozokuz.incore.features.researchv2.client;

import io.github.ozokuz.incore.features.researchv2.station.OutputPortMenu;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class OutputPortScreen extends StationInventoryScreen<OutputPortMenu> {
    private Button modeButton;

    public OutputPortScreen(OutputPortMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void init() {
        super.init();
        modeButton = addRenderableWidget(Button.builder(modeButtonLabel(), button -> {
            if (minecraft != null && minecraft.gameMode != null) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0);
            }
        }).bounds(leftPos + 12, topPos + 22, imageWidth - 24, 20).build());
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (modeButton != null) {
            modeButton.setMessage(modeButtonLabel());
        }
    }

    @Override
    protected int accentColor() {
        return 0xFF8B9C4A;
    }

    @Override
    protected int titleLabelY() {
        return 8;
    }

    @Override
    protected int machineSectionTopOffset() {
        return 24;
    }

    @Override
    protected int playerSectionTopOffset() {
        return 62;
    }

    @Override
    protected void renderSubtitle(net.minecraft.client.gui.GuiGraphics guiGraphics) {
        guiGraphics.drawString(font, Component.translatable("screen.incore.output_port.mode"), 12, 46, subtitleColor(), false);
    }

    private Component modeButtonLabel() {
        return Component.translatable("screen.incore.output_port.mode_value", Component.literal(menu.mode().name()));
    }
}
