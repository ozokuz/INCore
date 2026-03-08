package io.github.ozokuz.incore.features.roguelike.content;

import io.github.ozokuz.incore.Registration;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class CustomDungeonCrystalForgeMenu extends AbstractContainerMenu {
    public static final int INPUT_X = 22;
    public static final int INPUT_Y = 40;
    public static final int THEME_X = 62;
    public static final int THEME_Y = 40;
    public static final int OBJECTIVE_X = 110;
    public static final int OBJECTIVE_Y = 40;
    public static final int MODIFIER_ONE_X = 22;
    public static final int MODIFIER_ONE_Y = 76;
    public static final int MODIFIER_TWO_X = 44;
    public static final int MODIFIER_TWO_Y = 76;
    public static final int MODIFIER_THREE_X = 66;
    public static final int MODIFIER_THREE_Y = 76;
    public static final int OUTPUT_X = 188;
    public static final int OUTPUT_Y = 40;

    public static final int PLAYER_INV_X = 43;
    public static final int PLAYER_INV_Y = 122;
    public static final int HOTBAR_Y = PLAYER_INV_Y + 58;

    private final CustomDungeonCrystalForgeBlockEntity blockEntity;
    private final ContainerLevelAccess access;

    public CustomDungeonCrystalForgeMenu(int containerId, Inventory playerInventory, CustomDungeonCrystalForgeBlockEntity blockEntity) {
        super(Registration.CUSTOM_DUNGEON_CRYSTAL_FORGE_MENU.get(), containerId);
        this.blockEntity = blockEntity;
        this.access = ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos());

        addSlot(new InputCrystalSlot(blockEntity, CustomDungeonCrystalForgeBlockEntity.INPUT_SLOT, INPUT_X, INPUT_Y));
        addSlot(new SelectorSlot(blockEntity, CustomDungeonCrystalForgeBlockEntity.THEME_SLOT, THEME_X, THEME_Y));
        addSlot(new SelectorSlot(blockEntity, CustomDungeonCrystalForgeBlockEntity.OBJECTIVE_SLOT, OBJECTIVE_X, OBJECTIVE_Y));
        addSlot(new SelectorSlot(blockEntity, CustomDungeonCrystalForgeBlockEntity.MODIFIER_START, MODIFIER_ONE_X, MODIFIER_ONE_Y));
        addSlot(new SelectorSlot(blockEntity, CustomDungeonCrystalForgeBlockEntity.MODIFIER_START + 1, MODIFIER_TWO_X, MODIFIER_TWO_Y));
        addSlot(new SelectorSlot(blockEntity, CustomDungeonCrystalForgeBlockEntity.MODIFIER_START + 2, MODIFIER_THREE_X, MODIFIER_THREE_Y));
        addSlot(new OutputSlot(blockEntity, CustomDungeonCrystalForgeBlockEntity.OUTPUT_SLOT, OUTPUT_X, OUTPUT_Y));

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, PLAYER_INV_X + col * 18, PLAYER_INV_Y + row * 18));
            }
        }

        for (int i = 0; i < 9; i++) {
            addSlot(new Slot(playerInventory, i, PLAYER_INV_X + i * 18, HOTBAR_Y));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        int forgeSlotCount = CustomDungeonCrystalForgeBlockEntity.SLOT_COUNT;

        if (index < forgeSlotCount) {
            if (!moveItemStackTo(stack, forgeSlotCount, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.is(Registration.EMPTY_DUNGEON_CRYSTAL_ITEM.get())) {
            if (!moveItemStackTo(stack, CustomDungeonCrystalForgeBlockEntity.INPUT_SLOT, CustomDungeonCrystalForgeBlockEntity.INPUT_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, CustomDungeonCrystalForgeBlockEntity.THEME_SLOT, CustomDungeonCrystalForgeBlockEntity.OUTPUT_SLOT, false)) {
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
        return stillValid(access, player, Registration.CUSTOM_DUNGEON_CRYSTAL_FORGE_BLOCK.get());
    }

    public boolean validPreview() {
        return blockEntity.validPreview();
    }

    private static class InputCrystalSlot extends Slot {
        private InputCrystalSlot(CustomDungeonCrystalForgeBlockEntity blockEntity, int slot, int x, int y) {
            super(blockEntity, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.is(Registration.EMPTY_DUNGEON_CRYSTAL_ITEM.get());
        }
    }

    private static class SelectorSlot extends Slot {
        private SelectorSlot(CustomDungeonCrystalForgeBlockEntity blockEntity, int slot, int x, int y) {
            super(blockEntity, slot, x, y);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }

    private static class OutputSlot extends Slot {
        private final CustomDungeonCrystalForgeBlockEntity blockEntity;

        private OutputSlot(CustomDungeonCrystalForgeBlockEntity blockEntity, int slot, int x, int y) {
            super(blockEntity, slot, x, y);
            this.blockEntity = blockEntity;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public boolean mayPickup(Player player) {
            return hasItem() && blockEntity.validPreview();
        }

        @Override
        public void onTake(Player player, ItemStack stack) {
            super.onTake(player, stack);
            blockEntity.consumeInputAfterOutputTaken();
        }
    }
}
