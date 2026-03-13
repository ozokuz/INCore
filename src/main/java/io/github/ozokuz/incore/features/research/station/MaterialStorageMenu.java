package io.github.ozokuz.incore.features.research.station;

import io.github.ozokuz.incore.features.machines.multiblock.*;

import io.github.ozokuz.incore.Registration;
import net.minecraft.world.entity.player.Inventory;

public class MaterialStorageMenu extends AbstractMachineInventoryMenu {
    public MaterialStorageMenu(int containerId, Inventory playerInventory, MaterialStorageBlockEntity blockEntity) {
        super(Registration.MATERIAL_STORAGE_MENU.get(), containerId, playerInventory, blockEntity, 9);
    }
}
