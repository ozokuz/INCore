package io.github.ozokuz.incore.features.researchv2.discovery;

import io.github.ozokuz.incore.Registration;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

public class TranslatorMenu extends AbstractContainerMenu {
    private final TranslatorBlockEntity blockEntity;
    private final BlockPos blockPos;

    public TranslatorMenu(int containerId, Inventory playerInventory, TranslatorBlockEntity blockEntity) {
        super(Registration.TRANSLATOR_MENU.get(), containerId);
        this.blockEntity = blockEntity;
        this.blockPos = blockEntity.getBlockPos();

        for (int i = 0; i < blockEntity.data.getCount(); i++) {
            addDataSlot(DataSlot.forContainer(blockEntity.data, i));
        }

        addSlot(new InputSlot(blockEntity, 0, 44, 34));
        addSlot(new OutputSlot(blockEntity, 1, 116, 34));

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
        if (index < 2) {
            if (!moveItemStackTo(stack, 2, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.is(Registration.CONTINUUM_DATA_REPORT_ITEM.get())) {
            if (!moveItemStackTo(stack, 0, 1, false)) {
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

    private static final class InputSlot extends SlotItemHandler {
        private InputSlot(TranslatorBlockEntity blockEntity, int index, int xPosition, int yPosition) {
            super(blockEntity.itemHandler(), index, xPosition, yPosition);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.is(Registration.CONTINUUM_DATA_REPORT_ITEM.get());
        }
    }

    private static final class OutputSlot extends SlotItemHandler {
        private OutputSlot(TranslatorBlockEntity blockEntity, int index, int xPosition, int yPosition) {
            super(blockEntity.itemHandler(), index, xPosition, yPosition);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }
}
