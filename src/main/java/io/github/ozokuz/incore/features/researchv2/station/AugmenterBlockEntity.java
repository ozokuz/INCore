package io.github.ozokuz.incore.features.researchv2.station;

import io.github.ozokuz.incore.Registration;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public class AugmenterBlockEntity extends AbstractInventoryStationPartBlockEntity {
    public AugmenterBlockEntity(BlockPos pos, BlockState state) {
        super(Registration.AUGMENTER_BE.get(), pos, state, 4);
    }

    @Override
    public StationPartType stationPartType() {
        return StationPartType.AUGMENTER;
    }

    @Override
    public int activeSlotCount() {
        return switch (Math.max(1, stationTier())) {
            case 1 -> 1;
            case 2 -> 2;
            case 3 -> 3;
            default -> 4;
        };
    }

    @Override
    protected boolean mayPlaceItem(int slot, ItemStack stack) {
        return stack.getItem() instanceof ResearchAugmentItem;
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory playerInventory) {
        return new AugmenterMenu(containerId, playerInventory, this);
    }
}
