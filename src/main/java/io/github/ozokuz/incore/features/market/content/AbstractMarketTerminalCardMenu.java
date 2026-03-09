package io.github.ozokuz.incore.features.market.content;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public abstract class AbstractMarketTerminalCardMenu<T extends AbstractMarketTerminalBlockEntity> extends AbstractContainerMenu {
    public static final int CARD_X = 106;
    public static final int CARD_Y = 52;
    public static final int PLAYER_INVENTORY_X = 34;
    public static final int PLAYER_INVENTORY_Y = 102;
    public static final int HOTBAR_Y = 160;

    protected final T blockEntity;

    protected AbstractMarketTerminalCardMenu(MenuType<?> type, int containerId, Inventory playerInventory, T blockEntity) {
        super(type, containerId);
        this.blockEntity = blockEntity;

        addSlot(new Slot(blockEntity, AbstractMarketTerminalBlockEntity.CARD_SLOT, CARD_X, CARD_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return blockEntity.canPlaceItem(AbstractMarketTerminalBlockEntity.CARD_SLOT, stack);
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
        int machineSlots = AbstractMarketTerminalBlockEntity.SLOT_COUNT;

        if (index < machineSlots) {
            if (!moveItemStackTo(stack, machineSlots, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 0, machineSlots, false)) {
            return ItemStack.EMPTY;
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
        return blockEntity.stillValid(player) && blockEntity.canTrade(player);
    }
}
