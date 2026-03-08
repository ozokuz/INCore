package io.github.ozokuz.incore.features.market.content;

import io.github.ozokuz.incore.Registration;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class ShipmentTerminalMenu extends AbstractContainerMenu {
    public static final int INPUT_X = 26;
    public static final int INPUT_Y = 80;
    public static final int CARD_X = 186;
    public static final int CARD_Y = 98;
    public static final int PLAYER_INVENTORY_X = 43;
    public static final int PLAYER_INVENTORY_Y = 164;
    public static final int HOTBAR_Y = PLAYER_INVENTORY_Y + 58;

    private final ShipmentTerminalBlockEntity blockEntity;
    private final ContainerData data;

    public ShipmentTerminalMenu(int containerId, Inventory playerInventory, ShipmentTerminalBlockEntity blockEntity) {
        super(Registration.SHIPMENT_TERMINAL_MENU.get(), containerId);
        this.blockEntity = blockEntity;
        this.data = blockEntity.data;

        addDataSlot(net.minecraft.world.inventory.DataSlot.forContainer(this.data, 0));
        addDataSlot(net.minecraft.world.inventory.DataSlot.forContainer(this.data, 1));
        addDataSlot(net.minecraft.world.inventory.DataSlot.forContainer(this.data, 2));

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int slot = col + row * 3;
                addSlot(new Slot(blockEntity, slot, INPUT_X + col * 18, INPUT_Y + row * 18) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return blockEntity.canPlaceItem(getContainerSlot(), stack);
                    }
                });
            }
        }
        addSlot(new Slot(blockEntity, ShipmentTerminalBlockEntity.CARD_SLOT, CARD_X, CARD_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return blockEntity.canPlaceItem(ShipmentTerminalBlockEntity.CARD_SLOT, stack);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, PLAYER_INVENTORY_X + col * 18, PLAYER_INVENTORY_Y + row * 18));
            }
        }

        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, PLAYER_INVENTORY_X + col * 18, HOTBAR_Y));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        int machineSlots = ShipmentTerminalBlockEntity.SLOT_COUNT;

        if (index < machineSlots) {
            if (!moveItemStackTo(stack, machineSlots, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            if (blockEntity.canPlaceItem(ShipmentTerminalBlockEntity.CARD_SLOT, stack)) {
                if (!moveItemStackTo(stack, ShipmentTerminalBlockEntity.CARD_SLOT, ShipmentTerminalBlockEntity.CARD_SLOT + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(stack, 0, ShipmentTerminalBlockEntity.INPUT_SLOT_COUNT, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return blockEntity.stillValid(player) && blockEntity.canAccess(player);
    }

    public int progress() {
        return data.get(0);
    }

    public int maxProgress() {
        return Math.max(1, data.get(1));
    }

    public int status() {
        return data.get(2);
    }

    public int progressScaled(int width) {
        return Math.clamp((progress() * width) / maxProgress(), 0, width);
    }
}
