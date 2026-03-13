package io.github.ozokuz.incore.features.research.station.network;

import io.github.ozokuz.incore.features.research.network.ResearchNetworking;
import io.github.ozokuz.incore.features.research.station.ResearchControllerBlockEntity;
import io.github.ozokuz.incore.features.research.station.ResearchMultiblockStationRegistry;
import io.github.ozokuz.incore.features.research.station.ResearchOrchestrationService;
import io.github.ozokuz.incore.features.research.station.ResearchStationRuntime;
import io.github.ozokuz.incore.features.research.station.TeamResearchOrchestrationSnapshot;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class StationNetworkService {
    private static final String WARNING_MULTIPLE_NETWORKS = "screen.incore.research.network_warning_multiple";

    private StationNetworkService() {
    }

    public static void onTopologyChanged(Level level) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        StationNetworkGraphManager.invalidate(serverLevel);
        MinecraftServer server = serverLevel.getServer();
        if (server == null) {
            return;
        }
        for (String teamId : ResearchMultiblockStationRegistry.teamIds(server)) {
            ResearchNetworking.syncTeam(server, teamId);
        }
    }

    public static TeamStationNetworkSnapshot snapshot(MinecraftServer server, String teamId) {
        if (server == null || teamId == null || teamId.isBlank()) {
            return TeamStationNetworkSnapshot.empty(teamId);
        }

        List<ResearchControllerBlockEntity> controllers = ResearchMultiblockStationRegistry.controllersForTeam(server, teamId);
        if (controllers.isEmpty()) {
            return TeamStationNetworkSnapshot.empty(teamId);
        }

        Set<String> activeStationIds = new LinkedHashSet<>();
        Set<String> stationsWithLinkPort = new LinkedHashSet<>();
        Set<String> teamStationIds = new LinkedHashSet<>();

        for (ResearchControllerBlockEntity controller : controllers) {
            String stationId = controller.stationId();
            if (stationId == null || stationId.isBlank()) {
                continue;
            }
            activeStationIds.add(stationId);
            teamStationIds.add(stationId);
            if (controller.getLevel() instanceof ServerLevel level && StationNetworkGraphManager.get(level).hasLinkPort(stationId)) {
                stationsWithLinkPort.add(stationId);
            }
        }

        UnionFind unionFind = new UnionFind(activeStationIds);
        for (ServerLevel level : server.getAllLevels()) {
            StationNetworkGraphManager graph = StationNetworkGraphManager.get(level);
            for (StationNetworkComponent component : graph.componentsById().values()) {
                List<String> members = component.stationIds().stream()
                        .filter(teamStationIds::contains)
                        .sorted()
                        .toList();
                unionFind.unionAll(members);
            }
        }

        TeamResearchOrchestrationSnapshot orchestrationSnapshot = ResearchOrchestrationService.snapshot(server, teamId);
        if (orchestrationSnapshot.orchestratorValid()) {
            Set<String> orchestratedStationIds = new LinkedHashSet<>(orchestrationSnapshot.validWirelessStationIds());
            for (CableTopologyComponent component : ResearchOrchestrationService.collectCableComponents(server, teamId)) {
                if (component.orchestratorIds().contains(orchestrationSnapshot.orchestratorId())) {
                    orchestratedStationIds.addAll(component.stationIds());
                }
            }
            unionFind.unionAll(orchestratedStationIds);
        }

        Map<String, Set<String>> groupedStations = new LinkedHashMap<>();
        for (String stationId : activeStationIds) {
            groupedStations.computeIfAbsent(unionFind.find(stationId), ignored -> new LinkedHashSet<>()).add(stationId);
        }

        Map<String, String> stationNetworkIds = new LinkedHashMap<>();
        Set<String> linkedStationIds = new LinkedHashSet<>();
        Set<String> componentIds = new LinkedHashSet<>();
        List<Set<String>> components = new ArrayList<>(groupedStations.values());
        components.sort(Comparator.comparing(component -> component.stream().sorted().findFirst().orElse("")));
        for (Set<String> component : components) {
            List<String> members = component.stream().sorted().toList();
            String componentId = buildMergedComponentId(members);
            componentIds.add(componentId);
            if (members.size() > 1) {
                linkedStationIds.addAll(members);
            }
            for (String stationId : members) {
                stationNetworkIds.put(stationId, componentId);
            }
        }

        boolean orchestrationReady = !orchestrationSnapshot.orchestratorRequired() || orchestrationSnapshot.orchestratorValid();
        boolean valid = componentIds.size() <= 1 && orchestrationReady;
        Set<String> executableStationIds = valid && componentIds.size() == 1 ? Set.copyOf(activeStationIds) : Set.of();
        String status;
        if (componentIds.size() > 1) {
            status = "conflict";
        } else if (!orchestrationReady) {
            status = orchestrationSnapshot.orchestratorStatus();
        } else if (linkedStationIds.size() > 1) {
            status = "linked";
        } else if (!activeStationIds.isEmpty()) {
            status = "single";
        } else {
            status = "none";
        }

        return new TeamStationNetworkSnapshot(
                teamId,
                componentIds.size(),
                valid,
                status,
                componentIds.size() > 1 ? WARNING_MULTIPLE_NETWORKS : (valid ? "" : orchestrationSnapshot.orchestratorWarning()),
                activeStationIds.size(),
                linkedStationIds.size(),
                Set.copyOf(activeStationIds),
                executableStationIds,
                Set.copyOf(linkedStationIds),
                Set.copyOf(stationsWithLinkPort),
                Map.copyOf(stationNetworkIds)
        );
    }

    public static List<ResearchControllerBlockEntity> executableControllers(MinecraftServer server, String teamId) {
        TeamStationNetworkSnapshot snapshot = snapshot(server, teamId);
        if (!snapshot.stationNetworkValid() || snapshot.stationNetworkCount() != 1) {
            return List.of();
        }

        List<ResearchControllerBlockEntity> controllers = new ArrayList<>();
        for (ResearchControllerBlockEntity controller : ResearchMultiblockStationRegistry.controllersForTeam(server, teamId)) {
            if (snapshot.executableStationIds().contains(controller.stationId())) {
                controllers.add(controller);
            }
        }
        controllers.sort(Comparator
                .comparing((ResearchControllerBlockEntity controller) -> controller.getLevel().dimension().location().toString())
                .thenComparing(controller -> controller.getBlockPos().asLong()));
        return List.copyOf(controllers);
    }

    public static ResearchControllerBlockEntity resolveAssignedExecutor(
            MinecraftServer server,
            String teamId,
            List<String> assignedStationIds,
            boolean requireWritableDrive
    ) {
        List<ResearchControllerBlockEntity> executable = executableControllers(server, teamId);
        if (executable.isEmpty()) {
            return null;
        }

        if (assignedStationIds != null) {
            for (String stationId : assignedStationIds) {
                if (stationId == null || stationId.isBlank()) {
                    continue;
                }
                for (ResearchControllerBlockEntity controller : executable) {
                    if (!stationId.equals(controller.stationId())) {
                        continue;
                    }
                    if (!requireWritableDrive || ResearchStationRuntime.hasWritableDisk(controller)) {
                        return controller;
                    }
                }
            }
        }

        for (ResearchControllerBlockEntity controller : executable) {
            if (!requireWritableDrive || ResearchStationRuntime.hasWritableDisk(controller)) {
                return controller;
            }
        }
        return executable.get(0);
    }

    public static String stationNetworkId(ResearchControllerBlockEntity controller) {
        if (controller == null || controller.getLevel() == null || controller.getLevel().getServer() == null) {
            return "";
        }
        return snapshot(controller.getLevel().getServer(), controller.teamId()).stationNetworkIdsByStationId().getOrDefault(controller.stationId(), "");
    }

    public static boolean hasLinkPort(ResearchControllerBlockEntity controller) {
        if (controller == null || !(controller.getLevel() instanceof ServerLevel level)) {
            return false;
        }
        return StationNetworkGraphManager.get(level).hasLinkPort(controller.stationId());
    }

    private static String buildMergedComponentId(List<String> stationIds) {
        if (stationIds == null || stationIds.isEmpty()) {
            return "";
        }
        return "merged:" + String.join("|", stationIds);
    }

    private static final class UnionFind {
        private final Map<String, String> parent = new LinkedHashMap<>();

        private UnionFind(Set<String> stationIds) {
            for (String stationId : stationIds) {
                if (stationId != null && !stationId.isBlank()) {
                    parent.put(stationId, stationId);
                }
            }
        }

        private String find(String stationId) {
            String current = parent.get(stationId);
            if (current == null) {
                return "";
            }
            if (current.equals(stationId)) {
                return current;
            }
            String root = find(current);
            parent.put(stationId, root);
            return root;
        }

        private void union(String left, String right) {
            if (left == null || left.isBlank() || right == null || right.isBlank()) {
                return;
            }
            String leftRoot = find(left);
            String rightRoot = find(right);
            if (leftRoot.isBlank() || rightRoot.isBlank() || leftRoot.equals(rightRoot)) {
                return;
            }
            if (leftRoot.compareTo(rightRoot) <= 0) {
                parent.put(rightRoot, leftRoot);
            } else {
                parent.put(leftRoot, rightRoot);
            }
        }

        private void unionAll(Iterable<String> stationIds) {
            if (stationIds == null) {
                return;
            }
            String anchor = null;
            for (String stationId : stationIds) {
                if (stationId == null || stationId.isBlank() || !parent.containsKey(stationId)) {
                    continue;
                }
                if (anchor == null) {
                    anchor = stationId;
                } else {
                    union(anchor, stationId);
                }
            }
        }
    }
}
