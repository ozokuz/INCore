package io.github.ozokuz.incore.features.machines.multiblock;

import io.github.ozokuz.incore.Registration;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public class AugmenterBlockEntity extends AbstractMachineInventoryPartBlockEntity {
    public AugmenterBlockEntity(BlockPos pos, BlockState state) {
        super(Registration.AUGMENTER_BE.get(), pos, state, 5);
    }

    @Override
    protected String displayNameKey() {
        return "augmenter";
    }

    @Override
    public int activeSlotCount() {
        if (ownerKind() == MultiblockOwnerKind.ORCHESTRATOR) {
            return 5;
        }
        return switch (Math.max(1, stationTier())) {
            case 1 -> 1;
            case 2 -> 2;
            case 3 -> 3;
            default -> 4;
        };
    }

    @Override
    protected boolean mayPlaceItem(int slot, ItemStack stack) {
        return stack.getItem() instanceof MachineAugmentItem;
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory playerInventory) {
        return new AugmenterMenu(containerId, playerInventory, this);
    }
}
