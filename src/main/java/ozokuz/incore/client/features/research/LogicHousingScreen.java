package ozokuz.incore.client.features.research;

import ozokuz.incore.client.features.machines.MachineInventoryScreen;
import ozokuz.incore.features.research.station.LogicHousingMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class LogicHousingScreen extends MachineInventoryScreen<LogicHousingMenu> {
    public LogicHousingScreen(LogicHousingMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected int accentColor() {
        return 0xFFE0A33A;
    }
}
