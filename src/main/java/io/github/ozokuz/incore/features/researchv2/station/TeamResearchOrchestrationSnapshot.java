package io.github.ozokuz.incore.features.researchv2.station;

import java.util.Set;

public record TeamResearchOrchestrationSnapshot(
        String teamId,
        boolean orchestratorRequired,
        boolean orchestratorPresent,
        boolean orchestratorValid,
        String orchestratorStatus,
        String orchestratorWarning,
        String orchestratorId,
        String wirelessChannelId,
        int cableCapacityPerLink,
        int wirelessCapacity,
        int wirelessRange,
        boolean infiniteWireless,
        boolean interdimensionalWireless,
        Set<String> validWirelessStationIds,
        Set<String> invalidWirelessStationIds
) {
    public TeamResearchOrchestrationSnapshot {
        teamId = teamId == null ? "" : teamId;
        orchestratorStatus = orchestratorStatus == null ? "" : orchestratorStatus;
        orchestratorWarning = orchestratorWarning == null ? "" : orchestratorWarning;
        orchestratorId = orchestratorId == null ? "" : orchestratorId;
        wirelessChannelId = wirelessChannelId == null ? "" : wirelessChannelId;
        cableCapacityPerLink = Math.max(0, cableCapacityPerLink);
        wirelessCapacity = Math.max(0, wirelessCapacity);
        wirelessRange = Math.max(0, wirelessRange);
        validWirelessStationIds = validWirelessStationIds == null ? Set.of() : Set.copyOf(validWirelessStationIds);
        invalidWirelessStationIds = invalidWirelessStationIds == null ? Set.of() : Set.copyOf(invalidWirelessStationIds);
    }

    public static TeamResearchOrchestrationSnapshot notRequired(String teamId) {
        return new TeamResearchOrchestrationSnapshot(teamId, false, false, true, "not_required", "", "", "", 4, 8, 64, false, false, Set.of(), Set.of());
    }
}
