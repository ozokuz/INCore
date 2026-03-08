package io.github.ozokuz.incore.features.researchv2.station;

import net.minecraft.core.BlockPos;

import java.util.List;

public record ResearchStationDescriptor(
        String stationId,
        String teamId,
        String dimensionId,
        BlockPos controllerPos,
        int stationTier,
        boolean formed,
        int rpBuffer,
        int rpCapacity,
        int slotCapacity,
        int availableResearchPower,
        ResearchPowerFamily powerFamily,
        int powerInputTier,
        ResearchStationEndpoints endpoints,
        List<BlockPos> connectedParts
) {
    public ResearchStationDescriptor {
        stationId = stationId == null ? "" : stationId;
        teamId = teamId == null ? "" : teamId;
        dimensionId = dimensionId == null ? "" : dimensionId;
        stationTier = Math.max(0, stationTier);
        rpBuffer = Math.max(0, rpBuffer);
        rpCapacity = Math.max(0, rpCapacity);
        slotCapacity = Math.max(0, slotCapacity);
        availableResearchPower = Math.max(0, availableResearchPower);
        powerInputTier = Math.max(0, powerInputTier);
        endpoints = endpoints == null ? new ResearchStationEndpoints(List.of(), List.of()) : endpoints;
        connectedParts = connectedParts == null ? List.of() : List.copyOf(connectedParts);
    }
}
