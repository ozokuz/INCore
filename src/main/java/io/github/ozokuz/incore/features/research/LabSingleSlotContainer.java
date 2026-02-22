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
        return blockEntity.slotCount();
    }

    @Override
    public boolean isEmpty() {
        for (int i = 0; i < getContainerSize(); i++) {
            if (!blockEntity.getSlotItem(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return blockEntity.getSlotItem(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        return blockEntity.removeSlotItem(slot, amount);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return blockEntity.removeSlotItemNoUpdate(slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        blockEntity.setSlotItem(slot, stack);
    }

    @Override
    public void setChanged() { blockEntity.setChanged(); }

    @Override
    public boolean stillValid(Player player) { return true; }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return blockEntity.isItemValidForSlot(slot, stack);
    }

    @Override
    public void clearContent() {
        for (int slot = 0; slot < blockEntity.slotCount(); slot++) {
            blockEntity.setSlotItem(slot, ItemStack.EMPTY);
        }
    }
}
