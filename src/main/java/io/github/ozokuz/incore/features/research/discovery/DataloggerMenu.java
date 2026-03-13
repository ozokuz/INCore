package io.github.ozokuz.incore.features.research.discovery;

import io.github.ozokuz.incore.Registration;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

public class DataloggerMenu extends AbstractContainerMenu {
    private final DataloggerBlockEntity blockEntity;
    private final BlockPos blockPos;

    public DataloggerMenu(int containerId, Inventory playerInventory, DataloggerBlockEntity blockEntity) {
        super(Registration.DATALOGGER_MENU.get(), containerId);
        this.blockEntity = blockEntity;
        this.blockPos = blockEntity.getBlockPos();

        for (int i = 0; i < blockEntity.data.getCount(); i++) {
            addDataSlot(DataSlot.forContainer(blockEntity.data, i));
        }

        addSlot(new OutputSlot(blockEntity, 0, 80, 34));

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 96 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, 154));
        }
    }

    public BlockPos blockPos() {
        return blockPos;
    }

    public int progressTicks() {
        return blockEntity.data.get(0);
    }

    public int maxProgressTicks() {
        return Math.max(1, blockEntity.data.get(1));
    }

    public int progressScaled(int width) {
        return Math.clamp((progressTicks() * width) / maxProgressTicks(), 0, width);
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
        if (index == 0) {
            if (!moveItemStackTo(stack, 1, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        slot.onTake(player, stack);
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return blockEntity != null && blockEntity.canInteractWith(player);
    }

    private static final class OutputSlot extends SlotItemHandler {
        private OutputSlot(DataloggerBlockEntity blockEntity, int index, int xPosition, int yPosition) {
            super(blockEntity.itemHandler(), index, xPosition, yPosition);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }
}
