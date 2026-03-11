package io.github.ozokuz.incore.features.researchv2.station;

import io.github.ozokuz.incore.Registration;
import net.minecraft.world.entity.player.Inventory;

public class OrchestrationDriveMenu extends AbstractStationInventoryMenu {
    public OrchestrationDriveMenu(int containerId, Inventory playerInventory, OrchestrationDriveBlockEntity blockEntity) {
        super(Registration.ORCHESTRATION_DRIVE_MENU.get(), containerId, playerInventory, blockEntity, 1);
    }
}
