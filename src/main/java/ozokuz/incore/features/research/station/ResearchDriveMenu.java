package ozokuz.incore.features.research.station;

import ozokuz.incore.Registration;
import ozokuz.incore.features.machines.multiblock.AbstractMachineInventoryMenu;
import net.minecraft.world.entity.player.Inventory;

public class ResearchDriveMenu extends AbstractMachineInventoryMenu {
    public ResearchDriveMenu(int containerId, Inventory playerInventory, ResearchDriveBlockEntity blockEntity) {
        super(Registration.RESEARCH_DRIVE_MENU.get(), containerId, playerInventory, blockEntity, 1);
    }

    public ResearchDriveBlockEntity drive() {
        return (ResearchDriveBlockEntity) blockEntity;
    }
}
