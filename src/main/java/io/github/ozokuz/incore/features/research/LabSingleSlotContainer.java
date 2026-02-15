package io.github.ozokuz.incore.features.research;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class LabSingleSlotContainer implements Container {
    private final LabBlockEntity blockEntity;

    public LabSingleSlotContainer(LabBlockEntity blockEntity) {
        this.blockEntity = blockEntity;
    }

    @Override
    public int getContainerSize() { return 1; }

    @Override
    public boolean isEmpty() { return blockEntity.getInput().isEmpty(); }

    @Override
    public ItemStack getItem(int slot) { return slot == 0 ? blockEntity.getInput() : ItemStack.EMPTY; }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (slot != 0) return ItemStack.EMPTY;
        ItemStack current = blockEntity.getInput();
        ItemStack removed = current.split(amount);
        blockEntity.setInput(current);
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if (slot != 0) return ItemStack.EMPTY;
        ItemStack stack = blockEntity.getInput();
        blockEntity.setInput(ItemStack.EMPTY);
        return stack;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot == 0) {
            blockEntity.setInput(stack);
        }
    }

    @Override
    public void setChanged() { blockEntity.setChanged(); }

    @Override
    public boolean stillValid(Player player) { return true; }

    @Override
    public void clearContent() { blockEntity.setInput(ItemStack.EMPTY); }
}
