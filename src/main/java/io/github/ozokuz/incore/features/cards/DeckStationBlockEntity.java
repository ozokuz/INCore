package io.github.ozokuz.incore.features.cards;

import io.github.ozokuz.incore.Registration;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class DeckStationBlockEntity extends BlockEntity implements MenuProvider, Container {
    public static final int MODULE_SLOT_COUNT = 20;
    public static final int CORE_SLOT = 20;
    public static final int BOX_SLOT = 21;
    public static final int OUTPUT_SLOT = 22;
    public static final int SLOT_COUNT = 23;

    public static final int FAILURE_NONE = 0;
    public static final int FAILURE_MISSING_CORE = 1;
    public static final int FAILURE_MISSING_BOX = 2;
    public static final int FAILURE_NO_MODULES = 3;
    public static final int FAILURE_OVER_CAPACITY = 4;

    private static final int DATA_USED_POINTS = 0;
    private static final int DATA_CAPACITY = 1;
    private static final int DATA_MAX_INTEGRITY = 2;
    private static final int DATA_MODULE_COUNT = 3;
    private static final int DATA_VALID = 4;
    private static final int DATA_FAILURE = 5;

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private int usedPoints;
    private int capacity;
    private int maxIntegrity;
    private int moduleCount;
    private boolean valid;
    private int failureCode = FAILURE_MISSING_CORE;
    private List<String> previewModifierLines = List.of();
    private boolean updatingOutput;

    public final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_USED_POINTS -> usedPoints;
                case DATA_CAPACITY -> capacity;
                case DATA_MAX_INTEGRITY -> maxIntegrity;
                case DATA_MODULE_COUNT -> moduleCount;
                case DATA_VALID -> valid ? 1 : 0;
                case DATA_FAILURE -> failureCode;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case DATA_USED_POINTS -> usedPoints = Math.max(0, value);
                case DATA_CAPACITY -> capacity = Math.max(0, value);
                case DATA_MAX_INTEGRITY -> maxIntegrity = Math.max(0, value);
                case DATA_MODULE_COUNT -> moduleCount = Math.max(0, value);
                case DATA_VALID -> valid = value > 0;
                case DATA_FAILURE -> failureCode = Math.clamp(value, FAILURE_NONE, FAILURE_OVER_CAPACITY);
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return 6;
        }
    };

    public DeckStationBlockEntity(BlockPos pos, BlockState blockState) {
        super(Registration.DECK_STATION_BE.get(), pos, blockState);
    }

    public void refreshPreview() {
        CardDeckCoreData core = null;
        CardDeckBoxData box = null;

        ItemStack coreStack = items.get(CORE_SLOT);
        if (!coreStack.isEmpty() && coreStack.getItem() == Registration.CARD_DECK_CORE_ITEM.get()) {
            var coreId = CardItemData.readDeckCoreId(coreStack);
            if (coreId == null) {
                coreId = CardDeckCoreManager.getDefaultCoreId();
            }
            core = CardDeckCoreManager.get(coreId);
        }

        ItemStack boxStack = items.get(BOX_SLOT);
        if (!boxStack.isEmpty() && boxStack.getItem() == Registration.CARD_DECK_BOX_ITEM.get()) {
            var boxId = CardItemData.readDeckBoxId(boxStack);
            if (boxId == null) {
                boxId = CardDeckBoxManager.getDefaultBoxId();
            }
            box = CardDeckBoxManager.get(boxId);
        }

        List<CardItemData.CardInstance> modules = new ArrayList<>();
        int nextUsedPoints = 0;
        int nextModuleCount = 0;

        for (int slot = 0; slot < MODULE_SLOT_COUNT; slot++) {
            ItemStack stack = items.get(slot);
            if (stack.isEmpty() || stack.getItem() != Registration.CARD_MODULE_ITEM.get()) {
                continue;
            }

            CardItemData.CardInstance instance = CardItemData.readCardInstance(stack);
            if (instance == null) {
                continue;
            }

            CardModuleData module = CardModuleManager.get(instance.cardId());
            if (module == null) {
                continue;
            }

            modules.add(instance);
            nextModuleCount += 1;
            nextUsedPoints += module.deckPoints();
        }

        int nextCapacity = 0;
        int nextMaxIntegrity = 0;
        if (core != null && box != null) {
            nextCapacity = Math.max(1, core.capacityPoints() + box.capacityBonus());
            nextMaxIntegrity = Math.max(1, core.baseIntegrity() + box.integrityBonus());
        }

        int nextFailure = FAILURE_NONE;
        boolean nextValid = true;
        if (core == null) {
            nextValid = false;
            nextFailure = FAILURE_MISSING_CORE;
        } else if (box == null) {
            nextValid = false;
            nextFailure = FAILURE_MISSING_BOX;
        } else if (nextModuleCount <= 0) {
            nextValid = false;
            nextFailure = FAILURE_NO_MODULES;
        } else if (nextUsedPoints > nextCapacity) {
            nextValid = false;
            nextFailure = FAILURE_OVER_CAPACITY;
        }

        this.usedPoints = nextUsedPoints;
        this.capacity = nextCapacity;
        this.maxIntegrity = nextMaxIntegrity;
        this.moduleCount = nextModuleCount;
        this.valid = nextValid;
        this.failureCode = nextFailure;

        if (nextValid && core != null && box != null) {
            this.previewModifierLines = CardDeckService.previewModifierLines(
                    List.copyOf(modules),
                    nextMaxIntegrity,
                    nextMaxIntegrity,
                    true
            );
            ItemStack output = CardDeckService.createDeckStack(core.id(), box.id(), nextMaxIntegrity, List.copyOf(modules));
            CardItemData.writeDeckPreview(output, true);
            setOutputInternal(output);
        } else {
            this.previewModifierLines = List.of();
            setOutputInternal(ItemStack.EMPTY);
        }
        setChanged();
    }

    public boolean isValidPreview() {
        return valid;
    }

    public int usedPoints() {
        return usedPoints;
    }

    public int capacity() {
        return capacity;
    }

    public int maxIntegrity() {
        return maxIntegrity;
    }

    public int moduleCount() {
        return moduleCount;
    }

    public int failureCode() {
        return failureCode;
    }

    public String failureKey() {
        return switch (failureCode) {
            case FAILURE_MISSING_CORE -> "incore.cards.deck.missing_core";
            case FAILURE_MISSING_BOX -> "incore.cards.deck.missing_box";
            case FAILURE_NO_MODULES -> "incore.cards.deck.missing_modules";
            case FAILURE_OVER_CAPACITY -> "incore.cards.deck.no_capacity";
            default -> "";
        };
    }

    public ItemStack outputStack() {
        return items.get(OUTPUT_SLOT);
    }

    public List<String> previewModifierLines() {
        return previewModifierLines;
    }

    public void consumeInputsAfterOutputTaken() {
        if (!valid) {
            return;
        }

        for (int slot = 0; slot <= BOX_SLOT; slot++) {
            items.set(slot, ItemStack.EMPTY);
        }
        setOutputInternal(ItemStack.EMPTY);
        refreshPreview();
    }

    public List<ItemStack> inputContents() {
        List<ItemStack> drops = new ArrayList<>();
        for (int slot = 0; slot <= BOX_SLOT; slot++) {
            ItemStack stack = items.get(slot);
            if (!stack.isEmpty()) {
                drops.add(stack.copy());
            }
        }
        return List.copyOf(drops);
    }

    private void setOutputInternal(ItemStack stack) {
        updatingOutput = true;
        items.set(OUTPUT_SLOT, stack);
        updatingOutput = false;
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.loadAdditional(tag, registries);
        clearContent();

        if (tag.contains("items", Tag.TAG_LIST)) {
            ListTag list = tag.getList("items", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag row = list.getCompound(i);
                int slot = row.getInt("slot");
                if (slot < 0 || slot > BOX_SLOT) {
                    continue;
                }
                items.set(slot, ItemStack.parseOptional(registries, row.getCompound("stack")));
            }
        }
        refreshPreview();
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.saveAdditional(tag, registries);
        ListTag list = new ListTag();
        for (int slot = 0; slot <= BOX_SLOT; slot++) {
            ItemStack stack = items.get(slot);
            if (stack.isEmpty()) {
                continue;
            }

            CompoundTag row = new CompoundTag();
            row.putInt("slot", slot);
            row.put("stack", stack.save(registries));
            list.add(row);
        }
        tag.put("items", list);
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("block.incore.deck_station");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, @NotNull Inventory inventory, @NotNull Player player) {
        refreshPreview();
        return new DeckStationMenu(containerId, inventory, this);
    }

    @Override
    public int getContainerSize() {
        return SLOT_COUNT;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public @NotNull ItemStack getItem(int slot) {
        if (slot < 0 || slot >= items.size()) {
            return ItemStack.EMPTY;
        }
        return items.get(slot);
    }

    @Override
    public @NotNull ItemStack removeItem(int slot, int amount) {
        if (slot < 0 || slot >= items.size()) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = items.get(slot);
        ItemStack removed = stack.split(amount);
        if (stack.isEmpty()) {
            items.set(slot, ItemStack.EMPTY);
        }
        if (slot != OUTPUT_SLOT && !updatingOutput) {
            refreshPreview();
        }
        return removed;
    }

    @Override
    public @NotNull ItemStack removeItemNoUpdate(int slot) {
        if (slot < 0 || slot >= items.size()) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = items.get(slot);
        items.set(slot, ItemStack.EMPTY);
        if (slot != OUTPUT_SLOT && !updatingOutput) {
            refreshPreview();
        }
        return stack;
    }

    @Override
    public void setItem(int slot, @NotNull ItemStack stack) {
        if (slot < 0 || slot >= items.size()) {
            return;
        }

        ItemStack next = stack.copy();
        int maxStack = getMaxStackSize(next);
        if (!next.isEmpty() && next.getCount() > maxStack) {
            next.setCount(maxStack);
        }
        items.set(slot, next);
        if (slot != OUTPUT_SLOT && !updatingOutput) {
            refreshPreview();
        } else {
            setChanged();
        }
    }

    @Override
    public void setChanged() {
        super.setChanged();
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        if (level == null) {
            return false;
        }
        return player.distanceToSqr(worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D, worldPosition.getZ() + 0.5D) <= 64.0D;
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < items.size(); i++) {
            items.set(i, ItemStack.EMPTY);
        }
    }
}
