package io.github.ozokuz.incore.features.market.content;

import io.github.ozokuz.incore.Registration;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ShipmentTerminalMk2BlockEntity extends ShipmentTerminalBlockEntity {
    private static final int ENERGY_CAPACITY = 65_536;
    private static final int FE_PER_TICK = 256;

    private final EnergyStorage energy = new EnergyStorage(ENERGY_CAPACITY, ENERGY_CAPACITY, 0) {
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

    public ShipmentTerminalMk2BlockEntity(BlockPos pos, BlockState state) {
        super(Registration.SHIPMENT_TERMINAL_MK2_BE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, ShipmentTerminalMk2BlockEntity be) {
        be.tick();
        if (level.isClientSide) {
            return;
        }
        be.serverTick(level);
    }

    @Override
    protected boolean hasOperationalPower() {
        if (energy.getEnergyStored() < FE_PER_TICK) {
            status = STATUS_NO_POWER;
            return false;
        }
        return true;
    }

    @Override
    protected boolean consumePowerForWorkTick() {
        if (energy.extractEnergy(FE_PER_TICK, true) < FE_PER_TICK) {
            status = STATUS_NO_POWER;
            return false;
        }
        energy.extractEnergy(FE_PER_TICK, false);
        return true;
    }

    public @Nullable IEnergyStorage getEnergyStorage(@Nullable Direction side) {
        return energy;
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.put("energy", energy.serializeNBT(registries));
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        if (tag.contains("energy")) {
            Tag energyTag = tag.get("energy");
            if (energyTag != null) {
                energy.deserializeNBT(registries, energyTag);
            }
        }
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("block.incore.shipment_terminal_mk2");
    }
}
