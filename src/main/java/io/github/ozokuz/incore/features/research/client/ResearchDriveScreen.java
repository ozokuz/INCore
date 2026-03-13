package io.github.ozokuz.incore.features.research.client;

import io.github.ozokuz.incore.features.research.station.ResearchDriveMenu;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class ResearchDriveScreen extends StationInventoryScreen<ResearchDriveMenu> {
    public ResearchDriveScreen(ResearchDriveMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected int accentColor() {
        return 0xFF52A7C7;
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(Button.builder(
                Component.translatable("screen.incore.research_drive.repair"),
                button -> minecraft.setScreen(new CorruptedDiskScreen(this, menu))
        ).bounds(leftPos + imageWidth - 66, topPos + 6, 58, 18).build());
    }
}
