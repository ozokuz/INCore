package io.github.ozokuz.incore.client.features.research;

import io.github.ozokuz.incore.client.features.machines.*;
import io.github.ozokuz.incore.features.machines.multiblock.*;

import io.github.ozokuz.incore.features.research.station.MaterialStorageMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class MaterialStorageScreen extends MachineInventoryScreen<MaterialStorageMenu> {
    public MaterialStorageScreen(MaterialStorageMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected int accentColor() {
        return 0xFFC96A2B;
    }
}
