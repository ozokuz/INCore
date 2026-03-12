package io.github.ozokuz.incore.features.assembly.content;

import io.github.ozokuz.incore.Registration;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

public class AssemblyStationMenu extends AbstractContainerMenu {
    private final AssemblyStationBlockEntity blockEntity;

    public AssemblyStationMenu(int containerId, Inventory playerInventory, AssemblyStationBlockEntity blockEntity) {
        super(Registration.ASSEMBLY_STATION_MENU.get(), containerId);
        this.blockEntity = blockEntity;

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9 / 3; col++) {
                int slot = AssemblyStationBlockEntity.INPUT_START + col + row * 3;
                addSlot(new SlotItemHandler(blockEntity.itemHandler(), slot, 18 + col * 18, 30 + row * 18));
            }
        }
        addSlot(new OutputSlot(blockEntity, AssemblyStationBlockEntity.OUTPUT_SLOT, 116, 48));

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 108 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, 166));
        }
    }

    public AssemblyStationBlockEntity station() {
        return blockEntity;
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
        if (index < AssemblyStationBlockEntity.SLOT_COUNT) {
            if (!moveItemStackTo(stack, AssemblyStationBlockEntity.SLOT_COUNT, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, AssemblyStationBlockEntity.INPUT_START, AssemblyStationBlockEntity.INPUT_START + AssemblyStationBlockEntity.INPUT_COUNT, false)) {
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
        private OutputSlot(AssemblyStationBlockEntity blockEntity, int index, int xPosition, int yPosition) {
            super(blockEntity.itemHandler(), index, xPosition, yPosition);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }
}
