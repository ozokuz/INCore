package io.github.ozokuz.incore.features.assembly.content;

import io.github.ozokuz.incore.Registration;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

public class AutoAssemblerMenu extends AbstractContainerMenu {
    private final AutoAssemblerBlockEntity blockEntity;

    public AutoAssemblerMenu(int containerId, Inventory playerInventory, AutoAssemblerBlockEntity blockEntity) {
        super(Registration.AUTO_ASSEMBLER_MENU.get(), containerId);
        this.blockEntity = blockEntity;
        for (int i = 0; i < blockEntity.data().getCount(); i++) {
            addDataSlot(DataSlot.forContainer(blockEntity.data(), i));
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                addSlot(new SlotItemHandler(blockEntity.itemHandler(), col + row * 3, 18 + col * 18, 30 + row * 18));
            }
        }
        for (int col = 0; col < 4; col++) {
            addSlot(new OutputSlot(blockEntity.itemHandler(), 9 + col, 104 + col * 18, 48));
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 108 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, 166));
        }
    }

    public AutoAssemblerBlockEntity assembler() {
        return blockEntity;
    }

    public int progress() {
        return blockEntity.data().get(0);
    }

    public int maxProgress() {
        return Math.max(1, blockEntity.data().get(1));
    }

    public int status() {
        return blockEntity.data().get(2);
    }

    public int tier() {
        return blockEntity.data().get(3);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) {
            return ItemStack.EMPTY;
        }
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index < 13) {
            if (!moveItemStackTo(stack, 13, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 0, 9, false)) {
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
        return blockEntity.canAccess(player);
    }

    private static final class OutputSlot extends SlotItemHandler {
        private OutputSlot(ItemStackHandler handler, int index, int xPosition, int yPosition) {
            super(handler, index, xPosition, yPosition);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }
}
