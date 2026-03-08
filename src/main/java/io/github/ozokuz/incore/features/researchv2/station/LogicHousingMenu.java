package io.github.ozokuz.incore.features.researchv2.station;

import io.github.ozokuz.incore.Registration;
import net.minecraft.world.entity.player.Inventory;

public class LogicHousingMenu extends AbstractStationInventoryMenu {
    public LogicHousingMenu(int containerId, Inventory playerInventory, LogicHousingBlockEntity blockEntity) {
        super(Registration.LOGIC_HOUSING_MENU.get(), containerId, playerInventory, blockEntity, 4);
    }
}
