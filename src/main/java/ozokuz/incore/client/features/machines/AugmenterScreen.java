package ozokuz.incore.client.features.machines;

import ozokuz.incore.features.machines.multiblock.AugmenterMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class AugmenterScreen extends MachineInventoryScreen<AugmenterMenu> {
    public AugmenterScreen(AugmenterMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected int accentColor() {
        return 0xFF4FAE7C;
    }
}
