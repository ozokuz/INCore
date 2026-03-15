package ozokuz.incore.features.machines.multiblock;

import ozokuz.incore.Registration;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class OutputPortBlockEntity extends AbstractMachineInventoryPartBlockEntity {
    private OutputPortMode mode = OutputPortMode.UNBOUND;
    private final IItemHandler frontExtractView = new IItemHandler() {
        @Override
        public int getSlots() {
            return activeSlotCount();
        }

        @Override
        public @NotNull ItemStack getStackInSlot(int slot) {
            return itemHandler().getStackInSlot(slot);
        }

        @Override
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            return stack;
        }

        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            return itemHandler().extractItem(slot, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return itemHandler().getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return false;
        }
    };

    public OutputPortBlockEntity(BlockPos pos, BlockState state) {
        super(Registration.OUTPUT_PORT_BE.get(), pos, state, 9);
    }

    @Override
    protected String displayNameKey() {
        return "output_port";
    }

    @Override
    public int activeSlotCount() {
        return 9;
    }

    @Override
    protected boolean mayPlaceItem(int slot, ItemStack stack) {
        return false;
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory playerInventory) {
        return new OutputPortMenu(containerId, playerInventory, this);
    }

    public OutputPortMode mode() {
        return mode;
    }

    public void setMode(OutputPortMode mode) {
        OutputPortMode nextMode = mode == null ? OutputPortMode.UNBOUND : mode;
        if (this.mode == nextMode) {
            return;
        }
        this.mode = nextMode;
        setChanged();
        MultiblockIntegrationHooks.onOutputModeChanged(this, this.mode);
    }

    public void toggleMode() {
        setMode(mode.next());
    }

    public @Nullable IItemHandler frontExtractView(@Nullable Direction side) {
        if (!MultiblockFacing.isFrontFace(getBlockState(), side)) {
            return null;
        }
        return frontExtractView;
    }

    public ItemStack insertOutput(ItemStack stack) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }

        ItemStack remaining = stack.copy();
        for (int slot = 0; slot < activeSlotCount() && !remaining.isEmpty(); slot++) {
            ItemStack current = rawItemHandler().getStackInSlot(slot);
            if (current.isEmpty()) {
                int move = Math.min(remaining.getCount(), remaining.getMaxStackSize());
                ItemStack moved = remaining.copyWithCount(move);
                rawItemHandler().setStackInSlot(slot, moved);
                remaining.shrink(move);
                continue;
            }

            if (!ItemStack.isSameItemSameComponents(current, remaining)) {
                continue;
            }

            int space = Math.max(0, current.getMaxStackSize() - current.getCount());
            if (space <= 0) {
                continue;
            }
            int move = Math.min(space, remaining.getCount());
            current.grow(move);
            rawItemHandler().setStackInSlot(slot, current);
            remaining.shrink(move);
        }
        return remaining;
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.loadAdditional(tag, registries);
        if (!tag.contains("mode")) {
            mode = OutputPortMode.LOGIC;
            return;
        }
        try {
            mode = OutputPortMode.valueOf(tag.getString("mode"));
        } catch (IllegalArgumentException ignored) {
            mode = OutputPortMode.LOGIC;
        }
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString("mode", mode.name());
    }
}
