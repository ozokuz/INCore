package io.github.ozokuz.incore.features.researchv2.station;

import io.github.ozokuz.incore.Config;
import io.github.ozokuz.incore.Registration;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ElectricPowerInputBlockEntity extends BlockEntity implements IResearchPowerInput {
    private int feRemainder;
    private final EnergyStorage energy = new EnergyStorage(
            Config.ELECTRIC_INPUT_BUFFER_CAPACITY.get(),
            Config.ELECTRIC_INPUT_MAX_RECEIVE.get(),
            Config.ELECTRIC_INPUT_BUFFER_CAPACITY.get()
    ) {
        @Override
        public int receiveEnergy(int toReceive, boolean simulate) {
            int received = super.receiveEnergy(toReceive, simulate);
            if (received > 0 && !simulate) {
                setChanged();
            }
            return received;
        }

        @Override
        public int extractEnergy(int toExtract, boolean simulate) {
            int extracted = super.extractEnergy(toExtract, simulate);
            if (extracted > 0 && !simulate) {
                setChanged();
            }
            return extracted;
        }
    };
    private final IEnergyStorage externalEnergyView = new IEnergyStorage() {
        @Override
        public int receiveEnergy(int toReceive, boolean simulate) {
            return energy.receiveEnergy(toReceive, simulate);
        }

        @Override
        public int extractEnergy(int toExtract, boolean simulate) {
            return 0;
        }

        @Override
        public int getEnergyStored() {
            return energy.getEnergyStored();
        }

        @Override
        public int getMaxEnergyStored() {
            return energy.getMaxEnergyStored();
        }

        @Override
        public boolean canExtract() {
            return false;
        }

        @Override
        public boolean canReceive() {
            return energy.canReceive();
        }
    };

    public ElectricPowerInputBlockEntity(BlockPos pos, BlockState state) {
        super(Registration.ELECTRIC_POWER_INPUT_BE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, ElectricPowerInputBlockEntity input) {
    }

    public @Nullable IEnergyStorage getEnergyStorage(@Nullable Direction side) {
        if (side == null || side != getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING)) {
            return null;
        }
        return externalEnergyView;
    }

    public int extractForCore(int amount, boolean simulate) {
        return energy.extractEnergy(amount, simulate);
    }

    public int energyStored() {
        return energy.getEnergyStored();
    }

    @Override
    public int availableResearchPower(ResearchControllerBlockEntity controller, int maxRp) {
        return computeResearchPower(maxRp, true);
    }

    @Override
    public int pullResearchPower(ResearchControllerBlockEntity controller, int maxRp) {
        return computeResearchPower(maxRp, false);
    }

    private int computeResearchPower(int maxRp, boolean simulate) {
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
    public ResearchPowerFamily family() {
        return ResearchPowerFamily.ELECTRIC;
    }

    @Override
    public int powerTier() {
        if (getBlockState().getBlock() instanceof ResearchPowerInputBlockProvider provider) {
            return provider.powerTier();
        }
        return 1;
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("energy")) {
            Tag energyTag = tag.get("energy");
            if (energyTag != null) {
                energy.deserializeNBT(registries, energyTag);
            }
        }
        feRemainder = Math.max(0, tag.getInt("feRemainder"));
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("energy", energy.serializeNBT(registries));
        tag.putInt("feRemainder", Math.max(0, feRemainder));
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
