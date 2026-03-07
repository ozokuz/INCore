package io.github.ozokuz.incore.features.researchv2.station;

import net.minecraft.core.BlockPos;

import java.util.List;

public record ResearchStationTopology(
        boolean formed,
        List<BlockPos> connectedParts,
        List<BlockPos> inputPositions,
        ResearchPowerFamily powerFamily,
        int powerInputTier
) {
    public ResearchStationTopology {
        connectedParts = connectedParts == null ? List.of() : List.copyOf(connectedParts);
        inputPositions = inputPositions == null ? List.of() : List.copyOf(inputPositions);
        powerInputTier = Math.max(0, powerInputTier);
    }

    public static ResearchStationTopology unformed() {
        return new ResearchStationTopology(false, List.of(), List.of(), null, 0);
    }
}
