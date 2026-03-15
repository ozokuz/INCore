package ozokuz.incore.features.research.station;

import ozokuz.incore.Registration;
import ozokuz.incore.features.machines.multiblock.AbstractMachineInventoryMenu;
import net.minecraft.world.entity.player.Inventory;

public class OrchestrationDriveMenu extends AbstractMachineInventoryMenu {
    public OrchestrationDriveMenu(int containerId, Inventory playerInventory, OrchestrationDriveBlockEntity blockEntity) {
        super(Registration.ORCHESTRATION_DRIVE_MENU.get(), containerId, playerInventory, blockEntity, 1);
    }
}
