package io.github.ozokuz.incore.features.research.station;

import io.github.ozokuz.incore.features.machines.multiblock.*;

import io.github.ozokuz.incore.Registration;
import net.minecraft.world.entity.player.Inventory;

public class ResearchDriveMenu extends AbstractMachineInventoryMenu {
    public ResearchDriveMenu(int containerId, Inventory playerInventory, ResearchDriveBlockEntity blockEntity) {
        super(Registration.RESEARCH_DRIVE_MENU.get(), containerId, playerInventory, blockEntity, 1);
    }

    public ResearchDriveBlockEntity drive() {
        return (ResearchDriveBlockEntity) blockEntity;
    }
}
