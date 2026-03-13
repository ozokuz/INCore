package io.github.ozokuz.incore.features.machines.multiblock;

public interface MachinePowerInputBlockProvider {
    MachinePowerFamily family();

    int powerTier();
}
