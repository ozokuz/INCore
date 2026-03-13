package io.github.ozokuz.incore.features.machines.multiblock;

public interface IMachinePowerInput {
    int availablePower(int maxPower);

    int pullPower(int maxPower);

    MachinePowerFamily family();

    int powerTier();
}
