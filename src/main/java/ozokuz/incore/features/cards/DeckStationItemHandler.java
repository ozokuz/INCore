package ozokuz.incore.features.cards;

import ozokuz.incore.Registration;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

public final class DeckStationItemHandler implements IItemHandlerModifiable {
    private final DeckStationBlockEntity blockEntity;

    public DeckStationItemHandler(DeckStationBlockEntity blockEntity) {
        this.blockEntity = blockEntity;
    }

    @Override
    public int getSlots() {
        return DeckStationBlockEntity.SLOT_COUNT;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return isValidSlot(slot) ? blockEntity.getItem(slot) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (!isValidSlot(slot) || stack.isEmpty() || !isItemValid(slot, stack)) {
            return stack;
        }

        ItemStack existing = blockEntity.getItem(slot);
        int limit = Math.min(getSlotLimit(slot), stack.getMaxStackSize());
        if (!existing.isEmpty()) {
            if (!ItemStack.isSameItemSameComponents(existing, stack) || existing.getCount() >= limit) {
                return stack;
            }
            limit -= existing.getCount();
        }

        if (limit <= 0) {
            return stack;
        }

        int toInsert = Math.min(limit, stack.getCount());
        if (!simulate) {
            ItemStack next = stack.copyWithCount(toInsert);
            if (!existing.isEmpty()) {
                next.grow(existing.getCount());
            }
            blockEntity.setItem(slot, next);
        }

        return stack.copyWithCount(stack.getCount() - toInsert);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (!isValidSlot(slot) || amount <= 0) {
            return ItemStack.EMPTY;
        }

        ItemStack existing = blockEntity.getItem(slot);
        if (existing.isEmpty()) {
            return ItemStack.EMPTY;
        }

        if (slot == DeckStationBlockEntity.OUTPUT_SLOT) {
            if (!blockEntity.isValidPreview()) {
                return ItemStack.EMPTY;
            }
            ItemStack extracted = existing.copyWithCount(Math.min(amount, existing.getCount()));
            if (!simulate) {
                blockEntity.consumeInputsAfterOutputTaken();
            }
            return extracted;
        }

        ItemStack extracted = existing.copyWithCount(Math.min(amount, existing.getCount()));
        if (!simulate) {
            blockEntity.removeItem(slot, extracted.getCount());
        }
        return extracted;
    }

    @Override
    public void setStackInSlot(int slot, ItemStack stack) {
        if (!isValidSlot(slot)) {
            return;
        }
        if (slot == DeckStationBlockEntity.OUTPUT_SLOT) {
            blockEntity.setItem(slot, ItemStack.EMPTY);
            return;
        }
        if (!stack.isEmpty() && !isItemValid(slot, stack)) {
            return;
        }
        ItemStack toSet = stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(Math.min(1, stack.getCount()));
        blockEntity.setItem(slot, toSet);
    }

    @Override
    public int getSlotLimit(int slot) {
        return isValidSlot(slot) ? 1 : 0;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        if (!isValidSlot(slot) || stack.isEmpty()) {
            return false;
        }
        if (slot < DeckStationBlockEntity.MODULE_SLOT_COUNT) {
            return stack.getItem() == Registration.CARD_MODULE_ITEM.get();
        }
        if (slot == DeckStationBlockEntity.CORE_SLOT) {
            return stack.getItem() == Registration.CARD_DECK_CORE_ITEM.get();
        }
        if (slot == DeckStationBlockEntity.BOX_SLOT) {
            return stack.getItem() == Registration.CARD_DECK_BOX_ITEM.get();
        }
        return false;
    }

    private static boolean isValidSlot(int slot) {
        return slot >= 0 && slot < DeckStationBlockEntity.SLOT_COUNT;
    }
}