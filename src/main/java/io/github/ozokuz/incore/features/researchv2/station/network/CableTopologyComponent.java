package io.github.ozokuz.incore.features.researchv2.station.network;

import net.minecraft.core.BlockPos;

import java.util.List;
import java.util.Set;

public record CableTopologyComponent(
        String dimensionId,
        String teamId,
        Set<String> stationIds,
        Set<String> orchestratorIds,
        List<BlockPos> linkingPortPositions
) {
    public CableTopologyComponent {
        dimensionId = dimensionId == null ? "" : dimensionId;
        teamId = teamId == null ? "" : teamId;
        stationIds = stationIds == null ? Set.of() : Set.copyOf(stationIds);
        orchestratorIds = orchestratorIds == null ? Set.of() : Set.copyOf(orchestratorIds);
        linkingPortPositions = linkingPortPositions == null ? List.of() : List.copyOf(linkingPortPositions);
    }
}
