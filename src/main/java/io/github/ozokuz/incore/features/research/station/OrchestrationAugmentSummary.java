package io.github.ozokuz.incore.features.research.station;

import io.github.ozokuz.incore.features.machines.multiblock.*;

public record OrchestrationAugmentSummary(
        int cableStationBonusPerLink,
        int wirelessStationBonus,
        int wirelessRangeBonus,
        boolean infiniteWireless,
        boolean interdimensionalWireless,
        double speedMultiplier,
        double powerMultiplier,
        double bonusRunChance,
        double corruptionMultiplier
) {
    public static final OrchestrationAugmentSummary DEFAULT = new OrchestrationAugmentSummary(
            0,
            0,
            0,
            false,
            false,
            1.0D,
            1.0D,
            0.0D,
            1.0D
    );
}
