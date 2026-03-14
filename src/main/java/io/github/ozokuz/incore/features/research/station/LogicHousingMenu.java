package io.github.ozokuz.incore.features.research.station;

import io.github.ozokuz.incore.Registration;
import io.github.ozokuz.incore.features.machines.multiblock.AbstractMachineInventoryMenu;
import net.minecraft.world.entity.player.Inventory;

public class LogicHousingMenu extends AbstractMachineInventoryMenu {
    public LogicHousingMenu(int containerId, Inventory playerInventory, LogicHousingBlockEntity blockEntity) {
        super(Registration.LOGIC_HOUSING_MENU.get(), containerId, playerInventory, blockEntity, 4);
    }
}
