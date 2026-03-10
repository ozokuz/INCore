package io.github.ozokuz.incore.features.researchv2.station.network;

import net.minecraft.core.BlockPos;

import java.util.List;
import java.util.Set;

public record StationNetworkComponent(
        String id,
        String dimensionId,
        Set<String> stationIds,
        List<BlockPos> controllerPositions,
        List<BlockPos> portPositions
) {
    public StationNetworkComponent {
        id = id == null ? "" : id;
        dimensionId = dimensionId == null ? "" : dimensionId;
        stationIds = stationIds == null ? Set.of() : Set.copyOf(stationIds);
        controllerPositions = controllerPositions == null ? List.of() : List.copyOf(controllerPositions);
        portPositions = portPositions == null ? List.of() : List.copyOf(portPositions);
    }

    public boolean linked() {
        return stationIds.size() > 1;
    }
}
