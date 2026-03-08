package io.github.ozokuz.incore.features.researchv2.client;

import io.github.ozokuz.incore.features.researchv2.station.LogicHousingMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class LogicHousingScreen extends StationInventoryScreen<LogicHousingMenu> {
    public LogicHousingScreen(LogicHousingMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected int accentColor() {
        return 0xFFE0A33A;
    }
}
