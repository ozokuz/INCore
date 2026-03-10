package io.github.ozokuz.incore.features.researchv2.station;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.core.Direction;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractInventoryStationPartBlockEntity extends AbstractResearchStationPartBlockEntity implements MenuProvider {
    private final ItemStackHandler items;
    private final IItemHandler activeView = new IItemHandler() {
        @Override
        public int getSlots() {
            return items.getSlots();
        }

        @Override
        public @NotNull ItemStack getStackInSlot(int slot) {
            return isSlotActive(slot) ? items.getStackInSlot(slot) : ItemStack.EMPTY;
        }

        @Override
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            return isSlotActive(slot) ? items.insertItem(slot, stack, simulate) : stack;
        }

        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            return isSlotActive(slot) ? items.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return isSlotActive(slot) ? items.getSlotLimit(slot) : 0;
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return isSlotActive(slot) && items.isItemValid(slot, stack);
        }
    };
    private final IItemHandler frontInsertView = new IItemHandler() {
        @Override
        public int getSlots() {
            return items.getSlots();
        }

        @Override
        public @NotNull ItemStack getStackInSlot(int slot) {
            return isSlotActive(slot) ? items.getStackInSlot(slot) : ItemStack.EMPTY;
        }

        @Override
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            return isSlotActive(slot) ? items.insertItem(slot, stack, simulate) : stack;
        }

        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return isSlotActive(slot) ? items.getSlotLimit(slot) : 0;
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return isSlotActive(slot) && items.isItemValid(slot, stack);
        }
    };

    protected AbstractInventoryStationPartBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int slotCount) {
        super(type, pos, state);
        this.items = new ItemStackHandler(slotCount) {
            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
            }

            @Override
            public boolean isItemValid(int slot, @NotNull ItemStack stack) {
                return mayPlaceItem(slot, stack);
            }
        };
    }

    public abstract StationPartType stationPartType();

    public abstract int activeSlotCount();

    protected abstract boolean mayPlaceItem(int slot, ItemStack stack);

    protected abstract AbstractContainerMenu createMenu(int containerId, Inventory playerInventory);

    public IItemHandler itemHandler() {
        return activeView;
    }

    public @Nullable IItemHandler automationView(@Nullable Direction side) {
        if (side == null) {
            return itemHandler();
        }
        return isFrontFace(side) ? itemHandler() : null;
    }

    public IItemHandler frontInsertView() {
        return frontInsertView;
    }

    public ItemStackHandler rawItemHandler() {
        return items;
    }

    public int menuSlotCount() {
        return items.getSlots();
    }

    public boolean isSlotActive(int slot) {
        return slot >= 0 && slot < activeSlotCount();
    }

    protected Direction frontFace() {
        return ResearchStationFacing.frontFace(getBlockState());
    }

    protected boolean isFrontFace(@Nullable Direction side) {
        return ResearchStationFacing.isFrontFace(getBlockState(), side);
    }

    public int stationTier() {
        if (level == null || controllerPos() == null) {
            return 1;
        }
        if (level.getBlockEntity(controllerPos()) instanceof ResearchControllerBlockEntity controller) {
            return controller.stationTier();
        }
        return 1;
    }

    public void dropContents() {
        if (level == null) {
            return;
        }
        for (int slot = 0; slot < items.getSlots(); slot++) {
            ItemStack stack = items.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                net.minecraft.world.Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), stack.copy());
            }
        }
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("items")) {
            items.deserializeNBT(registries, tag.getCompound("items"));
        }
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("items", items.serializeNBT(registries));
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("block.incore." + stationPartType().name().toLowerCase(java.util.Locale.ROOT));
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, @NotNull Inventory inventory, @NotNull Player player) {
        return createMenu(containerId, inventory);
    }
}
