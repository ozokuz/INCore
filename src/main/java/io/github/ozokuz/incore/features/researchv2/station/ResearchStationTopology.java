package io.github.ozokuz.incore.features.researchv2.station;

import net.minecraft.core.BlockPos;

import java.util.List;

public record ResearchStationTopology(
        boolean formed,
        List<BlockPos> connectedParts,
        List<BlockPos> inputPositions,
        BlockPos logicHousingPos,
        BlockPos researchDrivePos,
        BlockPos materialStoragePos,
        List<BlockPos> outputPortPositions,
        BlockPos augmenterPos,
        ResearchPowerFamily powerFamily,
        int powerInputTier
) {
    public ResearchStationTopology {
        connectedParts = connectedParts == null ? List.of() : List.copyOf(connectedParts);
        inputPositions = inputPositions == null ? List.of() : List.copyOf(inputPositions);
        outputPortPositions = outputPortPositions == null ? List.of() : List.copyOf(outputPortPositions);
        powerInputTier = Math.max(0, powerInputTier);
    }

    public static ResearchStationTopology unformed() {
        return new ResearchStationTopology(false, List.of(), List.of(), null, null, null, List.of(), null, null, 0);
    }
}
