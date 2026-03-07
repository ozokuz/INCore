package io.github.ozokuz.incore.features.cards;

import io.github.ozokuz.incore.Registration;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class DeckStationMenu extends AbstractContainerMenu {
    public static final int GRID_COLUMNS = 8;
    public static final int GRID_ROWS = 3;
    public static final int GRID_START_X = 12;
    public static final int GRID_START_Y = 34;
    public static final int GRID_STEP_X = 20;
    public static final int GRID_STEP_Y = 20;

    public static final int CORE_X = GRID_START_X + 3 * GRID_STEP_X;
    public static final int CORE_Y = GRID_START_Y;
    public static final int BOX_X = GRID_START_X + 4 * GRID_STEP_X;
    public static final int BOX_Y = GRID_START_Y;

    public static final int OUTPUT_X = 188;
    public static final int OUTPUT_Y = 54;

    public static final int PLAYER_INV_X = 115;
    public static final int PLAYER_INV_Y = 152;
    public static final int HOTBAR_Y = PLAYER_INV_Y + 58;

    private final DeckStationBlockEntity blockEntity;
    private final ContainerData data;
    private final ContainerLevelAccess access;

    public DeckStationMenu(int containerId, Inventory playerInventory, DeckStationBlockEntity blockEntity) {
        super(Registration.DECK_STATION_MENU.get(), containerId);
        this.blockEntity = blockEntity;
        this.data = blockEntity.data;
        this.access = ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos());

        for (int i = 0; i < data.getCount(); i++) {
            addDataSlot(DataSlot.forContainer(data, i));
        }

        int moduleSlot = 0;
        for (int row = 0; row < GRID_ROWS; row++) {
            for (int col = 0; col < GRID_COLUMNS; col++) {
                if (row == 0 && col >= 2 && col <= 5) {
                    continue;
                }
                int x = GRID_START_X + col * GRID_STEP_X;
                int y = GRID_START_Y + row * GRID_STEP_Y;
                addSlot(new ModuleSlot(blockEntity, moduleSlot++, x, y));
            }
        }

        addSlot(new CoreSlot(blockEntity, DeckStationBlockEntity.CORE_SLOT, CORE_X, CORE_Y));
        addSlot(new BoxSlot(blockEntity, DeckStationBlockEntity.BOX_SLOT, BOX_X, BOX_Y));
        addSlot(new OutputSlot(blockEntity, DeckStationBlockEntity.OUTPUT_SLOT, OUTPUT_X, OUTPUT_Y));

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
        boolean takingOutput = index == DeckStationBlockEntity.OUTPUT_SLOT;
        int stationSlotCount = DeckStationBlockEntity.SLOT_COUNT;

        if (index < stationSlotCount) {
            if (takingOutput) {
                CardItemData.writeDeckPreview(stack, false);
            }
            if (!moveItemStackTo(stack, stationSlotCount, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.getItem() == Registration.CARD_DECK_CORE_ITEM.get()) {
            if (!moveItemStackTo(stack, DeckStationBlockEntity.CORE_SLOT, DeckStationBlockEntity.CORE_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.getItem() == Registration.CARD_DECK_BOX_ITEM.get()) {
            if (!moveItemStackTo(stack, DeckStationBlockEntity.BOX_SLOT, DeckStationBlockEntity.BOX_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.getItem() == Registration.CARD_MODULE_ITEM.get()) {
            if (!moveItemStackTo(stack, 0, DeckStationBlockEntity.MODULE_SLOT_COUNT, false)) {
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
        return stillValid(access, player, Registration.DECK_STATION_BLOCK.get());
    }

    public int usedPoints() {
        return data.get(0);
    }

    public int capacity() {
        return data.get(1);
    }

    public int maxIntegrity() {
        return data.get(2);
    }

    public int moduleCount() {
        return data.get(3);
    }

    public boolean validPreview() {
        return data.get(4) > 0;
    }

    public int failureCode() {
        return data.get(5);
    }

    public String failureKey() {
        return switch (failureCode()) {
            case DeckStationBlockEntity.FAILURE_MISSING_CORE -> "incore.cards.deck.missing_core";
            case DeckStationBlockEntity.FAILURE_MISSING_BOX -> "incore.cards.deck.missing_box";
            case DeckStationBlockEntity.FAILURE_NO_MODULES -> "incore.cards.deck.missing_modules";
            case DeckStationBlockEntity.FAILURE_OVER_CAPACITY -> "incore.cards.deck.no_capacity";
            default -> "";
        };
    }

    public ItemStack previewDeck() {
        return blockEntity.getItem(DeckStationBlockEntity.OUTPUT_SLOT);
    }

    public List<String> previewModifierLines() {
        return blockEntity.previewModifierLines();
    }

    private static class ModuleSlot extends Slot {
        private ModuleSlot(DeckStationBlockEntity blockEntity, int slot, int x, int y) {
            super(blockEntity, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.getItem() == Registration.CARD_MODULE_ITEM.get();
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }

    private static class CoreSlot extends Slot {
        private CoreSlot(DeckStationBlockEntity blockEntity, int slot, int x, int y) {
            super(blockEntity, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.getItem() == Registration.CARD_DECK_CORE_ITEM.get();
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }

    private static class BoxSlot extends Slot {
        private BoxSlot(DeckStationBlockEntity blockEntity, int slot, int x, int y) {
            super(blockEntity, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.getItem() == Registration.CARD_DECK_BOX_ITEM.get();
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }

    private static class OutputSlot extends Slot {
        private final DeckStationBlockEntity blockEntity;

        private OutputSlot(DeckStationBlockEntity blockEntity, int slot, int x, int y) {
            super(blockEntity, slot, x, y);
            this.blockEntity = blockEntity;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public boolean mayPickup(Player player) {
            return hasItem() && blockEntity.isValidPreview();
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public void onTake(Player player, ItemStack stack) {
            if (!stack.isEmpty()) {
                CardItemData.writeDeckPreview(stack, false);
            }
            super.onTake(player, stack);
            blockEntity.consumeInputsAfterOutputTaken();
        }
    }
}
