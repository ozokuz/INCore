package io.github.ozokuz.incore.features.research.station;

import io.github.ozokuz.incore.features.machines.multiblock.*;

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
        MachinePowerFamily powerFamily,
        int powerInputTier,
        String outputPortModes,
        int mountedDiskTier,
        int mountedDiskSnapshotCount,
        int mountedDiskCorruptedSegmentCount,
        int mountedDiskCorruptedSnapshotCount,
        double activeSpeedMultiplier,
        double activePowerMultiplier,
        double activeBonusRunChance,
        double activeCorruptionMultiplier,
        ResearchStationEndpoints endpoints,
        List<BlockPos> connectedParts,
        String stationNetworkId,
        boolean singletonNetwork,
        boolean linked,
        boolean hasLinkPort
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
        mountedDiskTier = Math.max(0, mountedDiskTier);
        mountedDiskSnapshotCount = Math.max(0, mountedDiskSnapshotCount);
        mountedDiskCorruptedSegmentCount = Math.max(0, mountedDiskCorruptedSegmentCount);
        mountedDiskCorruptedSnapshotCount = Math.max(0, mountedDiskCorruptedSnapshotCount);
        activeSpeedMultiplier = Math.max(0.0D, activeSpeedMultiplier);
        activePowerMultiplier = Math.max(0.0D, activePowerMultiplier);
        activeBonusRunChance = Math.max(0.0D, activeBonusRunChance);
        activeCorruptionMultiplier = Math.max(0.0D, activeCorruptionMultiplier);
        outputPortModes = outputPortModes == null ? "NONE" : outputPortModes;
        endpoints = endpoints == null ? new ResearchStationEndpoints(List.of(), List.of(), List.of(), List.of(), null, null, null, List.of(), null) : endpoints;
        connectedParts = connectedParts == null ? List.of() : List.copyOf(connectedParts);
        stationNetworkId = stationNetworkId == null ? "" : stationNetworkId;
    }

    public ResearchStationDescriptor withStationNetwork(String stationNetworkId, boolean singletonNetwork, boolean linked, boolean hasLinkPort) {
        return new ResearchStationDescriptor(
                stationId,
                teamId,
                dimensionId,
                controllerPos,
                stationTier,
                formed,
                rpBuffer,
                rpCapacity,
                slotCapacity,
                availableResearchPower,
                powerFamily,
                powerInputTier,
                outputPortModes,
                mountedDiskTier,
                mountedDiskSnapshotCount,
                mountedDiskCorruptedSegmentCount,
                mountedDiskCorruptedSnapshotCount,
                activeSpeedMultiplier,
                activePowerMultiplier,
                activeBonusRunChance,
                activeCorruptionMultiplier,
                endpoints,
                connectedParts,
                stationNetworkId,
                singletonNetwork,
                linked,
                hasLinkPort
        );
    }
}
