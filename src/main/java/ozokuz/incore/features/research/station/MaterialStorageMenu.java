package ozokuz.incore.features.research.station;

import ozokuz.incore.Registration;
import ozokuz.incore.features.machines.multiblock.AbstractMachineInventoryMenu;
import net.minecraft.world.entity.player.Inventory;

public class MaterialStorageMenu extends AbstractMachineInventoryMenu {
    public MaterialStorageMenu(int containerId, Inventory playerInventory, MaterialStorageBlockEntity blockEntity) {
        super(Registration.MATERIAL_STORAGE_MENU.get(), containerId, playerInventory, blockEntity, 9);
    }
}
