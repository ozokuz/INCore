package io.github.ozokuz.incore.features.researchv2.station;

import io.github.ozokuz.incore.Registration;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public class LogicHousingBlockEntity extends AbstractInventoryStationPartBlockEntity {
    public LogicHousingBlockEntity(BlockPos pos, BlockState state) {
        super(Registration.LOGIC_HOUSING_BE.get(), pos, state, 4);
    }

    @Override
    public StationPartType stationPartType() {
        return StationPartType.LOGIC_HOUSING;
    }

    @Override
    public int activeSlotCount() {
        if (getBlockState().getBlock() instanceof LogicHousingBlock block) {
            return switch (Math.max(1, block.tier())) {
                case 1 -> 1;
                case 2 -> 2;
                case 3 -> 3;
                default -> 4;
            };
        }
        return 1;
    }

    @Override
    public int menuSlotCount() {
        return activeSlotCount();
    }

    @Override
    protected boolean mayPlaceItem(int slot, ItemStack stack) {
        return StationInventoryRules.isLogicModule(stack);
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory playerInventory) {
        return new LogicHousingMenu(containerId, playerInventory, this);
    }
}
