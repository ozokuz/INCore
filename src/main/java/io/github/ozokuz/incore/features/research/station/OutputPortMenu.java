package io.github.ozokuz.incore.features.research.station;

import io.github.ozokuz.incore.Registration;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.DataSlot;

public class OutputPortMenu extends AbstractStationInventoryMenu {
    private final OutputPortBlockEntity blockEntity;
    private OutputPortMode mode;

    public OutputPortMenu(int containerId, Inventory playerInventory, OutputPortBlockEntity blockEntity) {
        super(Registration.OUTPUT_PORT_MENU.get(), containerId, playerInventory, blockEntity, 9);
        this.blockEntity = blockEntity;
        this.mode = blockEntity.mode();

        addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return blockEntity.mode().ordinal();
            }

            @Override
            public void set(int value) {
                mode = switch (value) {
                    case 1 -> OutputPortMode.DRIVE;
                    default -> OutputPortMode.LOGIC;
                };
            }
        });
    }

    public OutputPortMode mode() {
        return mode;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id != 0) {
            return false;
        }
        if (!stillValid(player)) {
            return false;
        }
        blockEntity.toggleMode();
        broadcastChanges();
        return true;
    }
}
