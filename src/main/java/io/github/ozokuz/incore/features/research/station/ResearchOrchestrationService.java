package io.github.ozokuz.incore.features.research.station;

import io.github.ozokuz.incore.features.machines.multiblock.*;

import io.github.ozokuz.incore.features.research.station.network.CableTopologyComponent;
import io.github.ozokuz.incore.features.research.station.network.CableTopologyScanner;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ResearchOrchestrationService {
    private static final int BASE_CABLE_CAPACITY = 4;
    private static final int BASE_WIRELESS_CAPACITY = 8;
    private static final int BASE_WIRELESS_RANGE = 64;

    private ResearchOrchestrationService() {
    }

    public static TeamResearchOrchestrationSnapshot snapshot(MinecraftServer server, String teamId) {
        if (server == null || teamId == null || teamId.isBlank()) {
            return TeamResearchOrchestrationSnapshot.notRequired(teamId);
        }

        List<ResearchControllerBlockEntity> stations = ResearchMultiblockStationRegistry.controllersForTeam(server, teamId);
        List<CableTopologyComponent> cableComponents = collectCableComponents(server, teamId);
        Map<String, Set<String>> stationsByOrchestratorId = stationsByOrchestratorId(cableComponents);
        boolean anyWirelessInstalled = false;
        for (ResearchControllerBlockEntity station : stations) {
            for (BlockPos wirelessPos : station.wirelessLinkPositions()) {
                if (station.getLevel().getBlockEntity(wirelessPos) instanceof WirelessLinkBlockEntity wireless && wireless.hasInstalledTransmitter()) {
                    anyWirelessInstalled = true;
                }
            }
        }

        boolean requiresOrchestrator = anyWirelessInstalled
                || cableComponents.stream().anyMatch(component -> component.stationIds().size() > 4)
                || stationsByOrchestratorId.values().stream().anyMatch(stationIds -> stationIds.size() > 4);
        List<ResearchOrchestratorControllerBlockEntity> orchestrators = ResearchOrchestratorRegistry.orchestratorsForTeam(server, teamId);
        List<ResearchOrchestratorControllerBlockEntity> formedOrchestrators = orchestrators.stream()
                .filter(ResearchOrchestratorControllerBlockEntity::isFormed)
                .toList();
        if (!requiresOrchestrator) {
            if (formedOrchestrators.isEmpty()) {
                return TeamResearchOrchestrationSnapshot.notRequired(teamId);
            }
            if (formedOrchestrators.size() != 1) {
                return new TeamResearchOrchestrationSnapshot(
                        teamId,
                        false,
                        true,
                        false,
                        "multiple_orchestrators",
                        "screen.incore.research.orchestrator_multiple",
                        "",
                        "",
                        BASE_CABLE_CAPACITY,
                        BASE_WIRELESS_CAPACITY,
                        BASE_WIRELESS_RANGE,
                        false,
                        false,
                        Set.of(),
                        Set.of()
                );
            }
            return snapshotForOrchestrator(teamId, false, stations, cableComponents, stationsByOrchestratorId, anyWirelessInstalled, formedOrchestrators.get(0));
        }
        if (formedOrchestrators.isEmpty()) {
            return new TeamResearchOrchestrationSnapshot(
                    teamId,
                    true,
                    !orchestrators.isEmpty(),
                    false,
                    "missing_orchestrator",
                    "screen.incore.research.orchestrator_missing",
                    "",
                    "",
                    BASE_CABLE_CAPACITY,
                    BASE_WIRELESS_CAPACITY,
                    BASE_WIRELESS_RANGE,
                    false,
                    false,
                    Set.of(),
                    Set.of()
            );
        }
        if (formedOrchestrators.size() != 1) {
            return new TeamResearchOrchestrationSnapshot(
                    teamId,
                    requiresOrchestrator,
                    true,
                    false,
                    "multiple_or_missing",
                    "screen.incore.research.orchestrator_multiple",
                    "",
                    "",
                    BASE_CABLE_CAPACITY,
                    BASE_WIRELESS_CAPACITY,
                    BASE_WIRELESS_RANGE,
                    false,
                    false,
                    Set.of(),
                    Set.of()
            );
        }

        return snapshotForOrchestrator(teamId, true, stations, cableComponents, stationsByOrchestratorId, anyWirelessInstalled, formedOrchestrators.get(0));
    }

    private static TeamResearchOrchestrationSnapshot snapshotForOrchestrator(
            String teamId,
            boolean requiresOrchestrator,
            List<ResearchControllerBlockEntity> stations,
            List<CableTopologyComponent> cableComponents,
            Map<String, Set<String>> stationsByOrchestratorId,
            boolean anyWirelessInstalled,
            ResearchOrchestratorControllerBlockEntity orchestrator
    ) {
        OrchestrationAugmentSummary augmentSummary = ResearchStationServices.computeOrchestrationSummary(orchestrator.getLevel(), orchestrator);
        int cableCapacity = BASE_CABLE_CAPACITY + augmentSummary.cableStationBonusPerLink();
        int wirelessCapacity = BASE_WIRELESS_CAPACITY + augmentSummary.wirelessStationBonus();
        int wirelessRange = BASE_WIRELESS_RANGE + augmentSummary.wirelessRangeBonus();

        boolean valid = orchestrator.isFormed();
        String warning = "";
        if (!orchestrator.hasMountedOrchestrationDisk()) {
            valid = false;
            warning = "screen.incore.research.orchestrator_missing_disk";
        } else if (orchestrator.availableResearchPower(Integer.MAX_VALUE) <= 0) {
            valid = false;
            warning = "screen.incore.research.orchestrator_no_power";
        }
        Set<String> cableManagedStationIds = new LinkedHashSet<>();
        for (CableTopologyComponent component : cableComponents) {
            if (!component.orchestratorIds().contains(orchestrator.orchestratorId())) {
                continue;
            }
            cableManagedStationIds.addAll(component.stationIds());
            if (component.stationIds().size() > cableCapacity) {
                valid = false;
                warning = "screen.incore.research.orchestrator_cable_capacity";
                break;
            }
        }

        Set<String> validWirelessStationIds = new LinkedHashSet<>();
        Set<String> invalidWirelessStationIds = new LinkedHashSet<>();
        String channelId = "";
        if (orchestrator.wirelessLinkPos() != null && orchestrator.getLevel().getBlockEntity(orchestrator.wirelessLinkPos()) instanceof WirelessLinkBlockEntity wirelessHub) {
            channelId = wirelessHub.channelId();
            if (anyWirelessInstalled && channelId.isBlank()) {
                valid = false;
                warning = "screen.incore.research.orchestrator_wireless_unbound";
            } else {
                for (ResearchControllerBlockEntity station : stations) {
                    boolean stationWirelessSeen = false;
                    boolean stationWirelessValid = false;
                    for (BlockPos wirelessPos : station.wirelessLinkPositions()) {
                        if (!(station.getLevel().getBlockEntity(wirelessPos) instanceof WirelessLinkBlockEntity stationWireless) || !stationWireless.hasInstalledTransmitter()) {
                            continue;
                        }
                        stationWirelessSeen = true;
                        if (!SignalTransmitterData.matches(stationWireless.transmitter(), wirelessHub.channelId(), wirelessHub.ownerTeamId())) {
                            continue;
                        }
                        if (!wirelessConnectionAllowed(orchestrator, wirelessHub, station, stationWireless, augmentSummary, wirelessRange)) {
                            continue;
                        }
                        stationWirelessValid = true;
                    }
                    if (stationWirelessSeen) {
                        if (stationWirelessValid) {
                            validWirelessStationIds.add(station.stationId());
                        } else {
                            invalidWirelessStationIds.add(station.stationId());
                        }
                    }
                }
                if (validWirelessStationIds.size() > wirelessCapacity) {
                    valid = false;
                    warning = "screen.incore.research.orchestrator_wireless_capacity";
                } else if (!invalidWirelessStationIds.isEmpty()) {
                    valid = false;
                    warning = "screen.incore.research.orchestrator_wireless_invalid";
                }
            }
        } else if (anyWirelessInstalled) {
            valid = false;
            warning = "screen.incore.research.orchestrator_wireless_missing";
        }

        Set<String> requiredManagedStationIds = new LinkedHashSet<>();
        for (CableTopologyComponent component : cableComponents) {
            if (component.stationIds().size() > 4) {
                requiredManagedStationIds.addAll(component.stationIds());
            }
        }
        Set<String> aggregateManagedStations = stationsByOrchestratorId.get(orchestrator.orchestratorId());
        if (aggregateManagedStations != null && aggregateManagedStations.size() > 4) {
            requiredManagedStationIds.addAll(aggregateManagedStations);
        }
        for (ResearchControllerBlockEntity station : stations) {
            for (BlockPos wirelessPos : station.wirelessLinkPositions()) {
                if (station.getLevel().getBlockEntity(wirelessPos) instanceof WirelessLinkBlockEntity wireless && wireless.hasInstalledTransmitter()) {
                    requiredManagedStationIds.add(station.stationId());
                    break;
                }
            }
        }

        Set<String> managedStationIds = new LinkedHashSet<>(cableManagedStationIds);
        managedStationIds.addAll(validWirelessStationIds);
        if (!requiredManagedStationIds.isEmpty() && !managedStationIds.containsAll(requiredManagedStationIds)) {
            valid = false;
            if (warning.isBlank()) {
                warning = "screen.incore.research.orchestrator_not_connected";
            }
        }

        return new TeamResearchOrchestrationSnapshot(
                teamId,
                requiresOrchestrator,
                true,
                valid,
                valid ? "ready" : "invalid",
                warning,
                orchestrator.orchestratorId(),
                channelId,
                cableCapacity,
                wirelessCapacity,
                wirelessRange,
                augmentSummary.infiniteWireless(),
                augmentSummary.interdimensionalWireless(),
                validWirelessStationIds,
                invalidWirelessStationIds
        );
    }

    private static Map<String, Set<String>> stationsByOrchestratorId(List<CableTopologyComponent> cableComponents) {
        Map<String, Set<String>> result = new LinkedHashMap<>();
        for (CableTopologyComponent component : cableComponents) {
            for (String orchestratorId : component.orchestratorIds()) {
                if (orchestratorId == null || orchestratorId.isBlank()) {
                    continue;
                }
                result.computeIfAbsent(orchestratorId, ignored -> new LinkedHashSet<>()).addAll(component.stationIds());
            }
        }
        return result;
    }

    public static List<CableTopologyComponent> collectCableComponents(MinecraftServer server, String teamId) {
        if (server == null || teamId == null || teamId.isBlank()) {
            return List.of();
        }
        List<CableTopologyComponent> components = new ArrayList<>();
        for (ServerLevel level : server.getAllLevels()) {
            components.addAll(CableTopologyScanner.scanTeam(level, teamId));
        }
        components.sort(Comparator
                .comparing(CableTopologyComponent::dimensionId)
                .thenComparing(component -> component.linkingPortPositions().stream().mapToLong(BlockPos::asLong).min().orElse(0L)));
        return List.copyOf(components);
    }

    private static boolean wirelessConnectionAllowed(
            ResearchOrchestratorControllerBlockEntity orchestrator,
            WirelessLinkBlockEntity hub,
            ResearchControllerBlockEntity station,
            WirelessLinkBlockEntity stationWireless,
            OrchestrationAugmentSummary augmentSummary,
            int wirelessRange
    ) {
        if (augmentSummary.interdimensionalWireless()) {
            return true;
        }
        if (orchestrator.getLevel() != station.getLevel()) {
            return false;
        }
        if (augmentSummary.infiniteWireless()) {
            return true;
        }
        if (orchestrator.wirelessLinkPos() == null) {
            return false;
        }
        double maxDistanceSqr = (double) wirelessRange * (double) wirelessRange;
        return orchestrator.wirelessLinkPos().distSqr(stationWireless.getBlockPos()) <= maxDistanceSqr;
    }
}
