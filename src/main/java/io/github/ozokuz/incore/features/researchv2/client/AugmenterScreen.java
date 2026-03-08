package io.github.ozokuz.incore.features.researchv2.client;

import io.github.ozokuz.incore.features.researchv2.station.AugmenterMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class AugmenterScreen extends StationInventoryScreen<AugmenterMenu> {
    public AugmenterScreen(AugmenterMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected int accentColor() {
        return 0xFF4FAE7C;
    }
}
