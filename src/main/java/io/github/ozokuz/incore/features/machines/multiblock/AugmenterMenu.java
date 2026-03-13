package io.github.ozokuz.incore.features.machines.multiblock;

import io.github.ozokuz.incore.Registration;
import net.minecraft.world.entity.player.Inventory;

public class AugmenterMenu extends AbstractMachineInventoryMenu {
    public AugmenterMenu(int containerId, Inventory playerInventory, AugmenterBlockEntity blockEntity) {
        super(Registration.AUGMENTER_MENU.get(), containerId, playerInventory, blockEntity, 4);
    }
}
