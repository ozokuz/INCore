package io.github.ozokuz.incore.features.researchv2.station;

import io.github.ozokuz.incore.Registration;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public class MaterialStorageBlockEntity extends AbstractInventoryStationPartBlockEntity {
    public MaterialStorageBlockEntity(BlockPos pos, BlockState state) {
        super(Registration.MATERIAL_STORAGE_BE.get(), pos, state, 36);
    }

    @Override
    public StationPartType stationPartType() {
        return StationPartType.MATERIAL_STORAGE;
    }

    @Override
    public int activeSlotCount() {
        if (getBlockState().getBlock() instanceof MaterialStorageBlock block) {
            return switch (Math.max(1, block.tier())) {
                case 1 -> 9;
                case 2 -> 18;
                case 3 -> 27;
                default -> 36;
            };
        }
        return 9;
    }

    @Override
    public int menuSlotCount() {
        return activeSlotCount();
    }

    @Override
    protected boolean mayPlaceItem(int slot, ItemStack stack) {
        return StationInventoryRules.isResearchMaterial(stack);
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory playerInventory) {
        return new MaterialStorageMenu(containerId, playerInventory, this);
    }
}
