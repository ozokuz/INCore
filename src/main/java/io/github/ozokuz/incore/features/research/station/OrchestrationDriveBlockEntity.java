package io.github.ozokuz.incore.features.research.station;

import io.github.ozokuz.incore.Registration;
import io.github.ozokuz.incore.features.machines.multiblock.AbstractMachineInventoryPartBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public class OrchestrationDriveBlockEntity extends AbstractMachineInventoryPartBlockEntity {
    public OrchestrationDriveBlockEntity(BlockPos pos, BlockState state) {
        super(Registration.ORCHESTRATION_DRIVE_BE.get(), pos, state, 1);
    }

    @Override
    protected String displayNameKey() {
        return "orchestration_drive";
    }

    @Override
    public int activeSlotCount() {
        return 1;
    }

    @Override
    protected boolean mayPlaceItem(int slot, ItemStack stack) {
        return StationInventoryRules.isOrchestrationDisk(stack);
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory playerInventory) {
        return new OrchestrationDriveMenu(containerId, playerInventory, this);
    }

    public ItemStack mountedDisk() {
        return rawItemHandler().getStackInSlot(0);
    }
}
