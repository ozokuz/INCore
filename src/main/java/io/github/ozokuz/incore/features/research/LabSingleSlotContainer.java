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
    public int getContainerSize() {
        return blockEntity.inputSlotCount();
    }

    @Override
    public boolean isEmpty() {
        return blockEntity.isInputEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return blockEntity.getInput(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        return blockEntity.removeInput(slot, amount);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return blockEntity.removeInputNoUpdate(slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        blockEntity.setInput(slot, stack);
    }

    @Override
    public void setChanged() { blockEntity.setChanged(); }

    @Override
    public boolean stillValid(Player player) { return true; }

    @Override
    public void clearContent() {
        for (int slot = 0; slot < blockEntity.inputSlotCount(); slot++) {
            blockEntity.setInput(slot, ItemStack.EMPTY);
        }
    }
}
