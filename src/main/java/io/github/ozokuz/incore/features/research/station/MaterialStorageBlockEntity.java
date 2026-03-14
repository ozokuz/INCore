package io.github.ozokuz.incore.features.research.station;

import io.github.ozokuz.incore.Registration;
import io.github.ozokuz.incore.features.machines.multiblock.AbstractMachineInventoryPartBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class MaterialStorageBlockEntity extends AbstractMachineInventoryPartBlockEntity {
    public MaterialStorageBlockEntity(BlockPos pos, BlockState state) {
        super(Registration.MATERIAL_STORAGE_BE.get(), pos, state, 36);
    }

    @Override
    protected String displayNameKey() {
        return "material_storage";
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

    @Override
    public @NotNull Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }
}
