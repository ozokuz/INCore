package ozokuz.incore.features.research.station.network;

import java.util.Map;
import java.util.Set;

public record TeamStationNetworkSnapshot(
        String teamId,
        int stationNetworkCount,
        boolean stationNetworkValid,
        String stationNetworkStatus,
        String stationNetworkWarning,
        int activeStationCount,
        int linkedStationCount,
        Set<String> activeStationIds,
        Set<String> executableStationIds,
        Set<String> linkedStationIds,
        Set<String> stationsWithLinkPort,
        Map<String, String> stationNetworkIdsByStationId
) {
    public TeamStationNetworkSnapshot {
        teamId = teamId == null ? "" : teamId;
        stationNetworkStatus = stationNetworkStatus == null ? "" : stationNetworkStatus;
        stationNetworkWarning = stationNetworkWarning == null ? "" : stationNetworkWarning;
        stationNetworkCount = Math.max(0, stationNetworkCount);
        activeStationCount = Math.max(0, activeStationCount);
        linkedStationCount = Math.max(0, linkedStationCount);
        activeStationIds = activeStationIds == null ? Set.of() : Set.copyOf(activeStationIds);
        executableStationIds = executableStationIds == null ? Set.of() : Set.copyOf(executableStationIds);
        linkedStationIds = linkedStationIds == null ? Set.of() : Set.copyOf(linkedStationIds);
        stationsWithLinkPort = stationsWithLinkPort == null ? Set.of() : Set.copyOf(stationsWithLinkPort);
        stationNetworkIdsByStationId = stationNetworkIdsByStationId == null ? Map.of() : Map.copyOf(stationNetworkIdsByStationId);
    }

    public static TeamStationNetworkSnapshot empty(String teamId) {
        return new TeamStationNetworkSnapshot(teamId, 0, true, "none", "", 0, 0, Set.of(), Set.of(), Set.of(), Set.of(), Map.of());
    }
}
