package io.github.ozokuz.incore.features.researchv2.station;

import net.minecraft.core.BlockPos;

import java.util.List;
import java.util.Set;

public record TeamCableComponent(
        String dimensionId,
        Set<String> stationIds,
        Set<String> orchestratorIds,
        List<BlockPos> linkingPortPositions
) {
    public TeamCableComponent {
        dimensionId = dimensionId == null ? "" : dimensionId;
        stationIds = stationIds == null ? Set.of() : Set.copyOf(stationIds);
        orchestratorIds = orchestratorIds == null ? Set.of() : Set.copyOf(orchestratorIds);
        linkingPortPositions = linkingPortPositions == null ? List.of() : List.copyOf(linkingPortPositions);
    }
}
