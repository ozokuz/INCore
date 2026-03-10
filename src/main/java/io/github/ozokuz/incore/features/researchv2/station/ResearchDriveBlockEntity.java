package io.github.ozokuz.incore.features.researchv2.station;

import io.github.ozokuz.incore.Registration;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;

public class ResearchDriveBlockEntity extends AbstractInventoryStationPartBlockEntity {
    private final IItemHandler exposedHandler = new IItemHandler() {
        @Override
        public int getSlots() {
            return rawItemHandler().getSlots();
        }

        @Override
        public @NotNull ItemStack getStackInSlot(int slot) {
            return rawItemHandler().getStackInSlot(slot);
        }

        @Override
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            return rawItemHandler().insertItem(slot, stack, simulate);
        }

        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            return rawItemHandler().extractItem(slot, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return rawItemHandler().getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return rawItemHandler().isItemValid(slot, stack);
        }
    };

    public ResearchDriveBlockEntity(BlockPos pos, BlockState state) {
        super(Registration.RESEARCH_DRIVE_BE.get(), pos, state, 1);
    }

    @Override
    public StationPartType stationPartType() {
        return StationPartType.RESEARCH_DRIVE;
    }

    @Override
    public int activeSlotCount() {
        return 1;
    }

    @Override
    protected boolean mayPlaceItem(int slot, ItemStack stack) {
        return StationInventoryRules.isResearchDisk(stack);
    }

    @Override
    public IItemHandler itemHandler() {
        return exposedHandler;
    }

    public ItemStack mountedDisk() {
        return rawItemHandler().getStackInSlot(0);
    }
    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory playerInventory) {
        return new ResearchDriveMenu(containerId, playerInventory, this);
    }
}
