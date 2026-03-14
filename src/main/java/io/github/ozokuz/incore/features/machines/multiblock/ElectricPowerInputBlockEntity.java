package io.github.ozokuz.incore.features.machines.multiblock;

import io.github.ozokuz.incore.Config;
import io.github.ozokuz.incore.Registration;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ElectricPowerInputBlockEntity extends BlockEntity implements IMachinePowerInput, MenuProvider {
    private int feRemainder;
    private int energyStored;
    private final IEnergyStorage internalEnergyView = new IEnergyStorage() {
        @Override
        public int receiveEnergy(int toReceive, boolean simulate) {
            int received = Math.min(Math.max(0, toReceive), Math.min(maxReceive(), remainingCapacity()));
            if (received > 0 && !simulate) {
                energyStored += received;
                setChanged();
            }
            return received;
        }

        @Override
        public int extractEnergy(int toExtract, boolean simulate) {
            int extracted = Math.min(Math.max(0, toExtract), Math.max(0, energyStored));
            if (extracted > 0 && !simulate) {
                energyStored -= extracted;
                setChanged();
            }
            return extracted;
        }

        @Override
        public int getEnergyStored() {
            clampEnergyStored();
            return energyStored;
        }

        @Override
        public int getMaxEnergyStored() {
            return capacity();
        }

        @Override
        public boolean canExtract() {
            return true;
        }

        @Override
        public boolean canReceive() {
            return maxReceive() > 0;
        }
    };
    private final IEnergyStorage externalEnergyView = new IEnergyStorage() {
        @Override
        public int receiveEnergy(int toReceive, boolean simulate) {
            return internalEnergyView.receiveEnergy(toReceive, simulate);
        }

        @Override
        public int extractEnergy(int toExtract, boolean simulate) {
            return 0;
        }

        @Override
        public int getEnergyStored() {
            return internalEnergyView.getEnergyStored();
        }

        @Override
        public int getMaxEnergyStored() {
            return internalEnergyView.getMaxEnergyStored();
        }

        @Override
        public boolean canExtract() {
            return false;
        }

        @Override
        public boolean canReceive() {
            return internalEnergyView.canReceive();
        }
    };

    public ElectricPowerInputBlockEntity(BlockPos pos, BlockState state) {
        super(Registration.ELECTRIC_POWER_INPUT_BE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, ElectricPowerInputBlockEntity input) {
        input.clampEnergyStored();
    }

    public @Nullable IEnergyStorage getEnergyStorage(@Nullable Direction side) {
        if (!MultiblockFacing.isFrontFace(getBlockState(), side)) {
            return null;
        }
        return externalEnergyView;
    }

    public int extractForCore(int amount, boolean simulate) {
        return internalEnergyView.extractEnergy(amount, simulate);
    }

    public int energyStored() {
        return internalEnergyView.getEnergyStored();
    }

    public int energyCapacity() {
        return internalEnergyView.getMaxEnergyStored();
    }

    public int maxReceive() {
        return maxReceive(powerTier());
    }

    public int maxFePerTickLimit() {
        return maxFePerTick(powerTier());
    }

    public int maxFePerInputOperationLimit() {
        return maxFePerInputOperation(powerTier());
    }

    @Override
    public int availablePower(int maxPower) {
        return computePower(maxPower, true);
    }

    @Override
    public int pullPower(int maxPower) {
        return computePower(maxPower, false);
    }

    private int computePower(int maxRp, boolean simulate) {
        clampEnergyStored();
        if (maxRp <= 0) {
            return 0;
        }

        int fePerRp = Math.max(1, Config.ELECTRIC_CORE_FE_PER_RP.get());
        int rpLimit = Math.max(0, maxRp);
        int tier = powerTier();
        int totalFe = Math.max(0, feRemainder);
        long targetFe = (long) rpLimit * fePerRp;
        long feNeededLong = Math.max(0L, targetFe - totalFe);
        int feNeeded = (int) Math.min(Integer.MAX_VALUE, feNeededLong);
        int feBudget = Math.min(Math.max(0, maxFePerTick(tier)), feNeeded);

        if (feBudget > 0) {
            totalFe += extractForCore(Math.min(feBudget, Math.max(1, maxFePerInputOperation(tier))), simulate);
        }

        int produced = Math.min(rpLimit, totalFe / fePerRp);
        if (!simulate) {
            int nextRemainder = totalFe - produced * fePerRp;
            if (nextRemainder != feRemainder) {
                feRemainder = nextRemainder;
                setChanged();
            }
        }
        return Math.max(0, produced);
    }

    @Override
    public MachinePowerFamily family() {
        return MachinePowerFamily.ELECTRIC;
    }

    @Override
    public int powerTier() {
        if (getBlockState().getBlock() instanceof MachinePowerInputBlockProvider provider) {
            return provider.powerTier();
        }
        return 1;
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("energyStored")) {
            energyStored = Math.max(0, tag.getInt("energyStored"));
        } else if (tag.contains("energy")) {
            Tag energyTag = tag.get("energy");
            if (energyTag instanceof IntTag intTag) {
                energyStored = Math.max(0, intTag.getAsInt());
            } else {
                energyStored = 0;
            }
        } else {
            energyStored = 0;
        }
        clampEnergyStored();
        feRemainder = Math.max(0, tag.getInt("feRemainder"));
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.saveAdditional(tag, registries);
        clampEnergyStored();
        tag.putInt("energyStored", energyStored);
        tag.putInt("feRemainder", Math.max(0, feRemainder));
    }

    @Override
    public @NotNull Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, @NotNull Inventory playerInventory, @NotNull Player player) {
        return new PowerInputMenu(containerId, playerInventory, worldPosition);
    }

    private int capacity() {
        long scaled = (long) Config.ELECTRIC_INPUT_BUFFER_CAPACITY.get() * tierScalar(powerTier());
        return (int) Math.min(Integer.MAX_VALUE, scaled);
    }

    private static int maxReceive(int tier) {
        long scaled = (long) Config.ELECTRIC_INPUT_MAX_RECEIVE.get() * tierScalar(tier);
        long capacity = (long) Config.ELECTRIC_INPUT_BUFFER_CAPACITY.get() * tierScalar(tier);
        return (int) Math.min(Math.min(Integer.MAX_VALUE, capacity), Math.min(Integer.MAX_VALUE, scaled));
    }

    private int remainingCapacity() {
        clampEnergyStored();
        return Math.max(0, capacity() - energyStored);
    }

    private void clampEnergyStored() {
        int capped = Math.min(Math.max(0, energyStored), capacity());
        if (capped != energyStored) {
            energyStored = capped;
            setChanged();
        }
    }

    private static int tierScalar(int tier) {
        return switch (Math.max(1, tier)) {
            case 1 -> 1;
            case 2 -> 4;
            case 3 -> 16;
            default -> 64;
        };
    }

    private static int maxFePerTick(int tier) {
        return switch (Math.max(1, tier)) {
            case 1 -> Config.ELECTRIC_CORE_T1_MAX_FE_PER_TICK.get();
            case 2 -> Config.ELECTRIC_CORE_T2_MAX_FE_PER_TICK.get();
            case 3 -> Config.ELECTRIC_CORE_T3_MAX_FE_PER_TICK.get();
            default -> Config.ELECTRIC_CORE_T4_MAX_FE_PER_TICK.get();
        };
    }

    private static int maxFePerInputOperation(int tier) {
        return switch (Math.max(1, tier)) {
            case 1 -> Config.ELECTRIC_CORE_T1_MAX_FE_PER_INPUT_OPERATION.get();
            case 2 -> Config.ELECTRIC_CORE_T2_MAX_FE_PER_INPUT_OPERATION.get();
            case 3 -> Config.ELECTRIC_CORE_T3_MAX_FE_PER_INPUT_OPERATION.get();
            default -> Config.ELECTRIC_CORE_T4_MAX_FE_PER_INPUT_OPERATION.get();
        };
    }
}
