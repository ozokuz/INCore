package ozokuz.incore.features.research.station;

import ozokuz.incore.Registration;
import ozokuz.incore.features.machines.multiblock.AbstractMachineInventoryMenu;
import net.minecraft.world.entity.player.Inventory;

public class LogicHousingMenu extends AbstractMachineInventoryMenu {
    public LogicHousingMenu(int containerId, Inventory playerInventory, LogicHousingBlockEntity blockEntity) {
        super(Registration.LOGIC_HOUSING_MENU.get(), containerId, playerInventory, blockEntity, 4);
    }
}
