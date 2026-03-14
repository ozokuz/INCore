package io.github.ozokuz.incore.client.features.machines;

import io.github.ozokuz.incore.features.machines.multiblock.OutputPortMenu;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class OutputPortScreen extends MachineInventoryScreen<OutputPortMenu> {
    private Button modeButton;

    public OutputPortScreen(OutputPortMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageHeight = 200;
        this.inventoryLabelY = 106;
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
        return 68;
    }

    @Override
    protected int playerSectionTopOffset() {
        return 104;
    }

    @Override
    protected void renderSubtitle(net.minecraft.client.gui.GuiGraphics guiGraphics) {
        guiGraphics.drawString(font, Component.translatable("screen.incore.output_port.mode"), 12, 46, subtitleColor(), false);
        guiGraphics.drawString(font, Component.translatable(modeHelpKey()), 12, 57, subtitleColor(), false);
    }

    private Component modeButtonLabel() {
        return Component.translatable("screen.incore.output_port.mode_value", Component.translatable(modeLabelKey()));
    }

    private String modeLabelKey() {
        return switch (menu.mode()) {
            case UNBOUND -> "screen.incore.output_port.mode.unbound";
            case LOGIC -> "screen.incore.output_port.mode.logic";
            case DRIVE -> "screen.incore.output_port.mode.drive";
        };
    }

    private String modeHelpKey() {
        return switch (menu.mode()) {
            case UNBOUND -> "screen.incore.output_port.help.unbound";
            case LOGIC -> "screen.incore.output_port.help.logic";
            case DRIVE -> "screen.incore.output_port.help.drive";
        };
    }
}
