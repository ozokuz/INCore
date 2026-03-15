package ozokuz.incore.client.features.research;

import ozokuz.incore.client.features.machines.MachineInventoryScreen;
import ozokuz.incore.features.research.station.OrchestrationDriveMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class OrchestrationDriveScreen extends MachineInventoryScreen<OrchestrationDriveMenu> {
    public OrchestrationDriveScreen(OrchestrationDriveMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected int accentColor() {
        return 0xFF6791D7;
    }
}
