package io.github.ozokuz.incore.features.assembly.content;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class AutoAssemblerFeBlockEntity extends BlockEntity implements AutoAssemblerBlockEntity, MenuProvider, BaseAutoAssemblerBlock.AutoAssemblerSupport {
    private final AutoAssemblerSharedState state = new AutoAssemblerSharedState();
    private final EnergyStorage energy;

    protected AutoAssemblerFeBlockEntity(net.minecraft.world.level.block.entity.BlockEntityType<?> type, BlockPos pos, BlockState state, int capacity, int maxReceive) {
        super(type, pos, state);
        this.state.setOnDirty(this::setChanged);
        this.energy = new EnergyStorage(capacity, maxReceive, 0) {
            @Override
            public int receiveEnergy(int toReceive, boolean simulate) {
                int received = super.receiveEnergy(toReceive, simulate);
                if (received > 0 && !simulate) {
                    AutoAssemblerFeBlockEntity.this.setChanged();
                }
                return received;
            }
        };
    }

    protected abstract int machineTierInternal();

    protected abstract int fePerTick();

    public void serverTick(Level level) {
        AutoAssemblerMachineLogic.tick(
                level,
                this,
                state,
                machineTierInternal(),
                energy.getEnergyStored() >= fePerTick(),
                () -> energy.extractEnergy(fePerTick(), false)
        );
    }

    @Override
    public ItemStackHandler itemHandler() {
        return state.items();
    }

    @Override
    public @Nullable IItemHandler automationView(@Nullable Direction side) {
        Direction front = getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
        return state.automationView(front, side);
    }

    public @Nullable IEnergyStorage energyStorage(@Nullable Direction side) {
        if (side == null || side == getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING)) {
            return energy;
        }
        return null;
    }

    @Override
    public boolean canAccess(Player player) {
        return level != null
                && level.getBlockEntity(worldPosition) == this
                && player.distanceToSqr(worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D, worldPosition.getZ() + 0.5D) <= 64.0D;
    }

    @Override
    public void setSelectedRecipeId(@Nullable net.minecraft.resources.ResourceLocation recipeId, @Nullable Player player) {
        state.setSelectedRecipeId(recipeId);
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            state.setTeamId(io.github.ozokuz.incore.features.researchv2.team.ResearchTeamResolver.resolveTeamId(serverPlayer));
        }
    }

    @Override
    public @Nullable net.minecraft.resources.ResourceLocation selectedRecipeId() {
        return state.selectedRecipeId();
    }

    @Override
    public int machineTier() {
        return machineTierInternal();
    }

    @Override
    public net.minecraft.world.inventory.ContainerData data() {
        return state.data();
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, @NotNull Inventory inventory, @NotNull Player player) {
        return new AutoAssemblerMenu(containerId, inventory, this);
    }

    @Override
    public void dropContents() {
        if (level != null) {
            state.dropContents(level, worldPosition);
        }
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.saveAdditional(tag, registries);
        state.save(tag, registries);
        tag.put("energy", energy.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.loadAdditional(tag, registries);
        state.load(tag, registries);
        state.setOnDirty(this::setChanged);
        Tag energyTag = tag.get("energy");
        if (energyTag != null) {
            energy.deserializeNBT(registries, energyTag);
        }
    }

    protected Component title(String key) {
        return Component.translatable(key);
    }
}
