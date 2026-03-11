package io.github.ozokuz.incore.features.researchv2.station.network;

import io.github.ozokuz.incore.features.researchv2.station.ResearchControllerBlockEntity;
import io.github.ozokuz.incore.features.researchv2.station.ResearchMultiblockStationRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

public final class StationNetworkGraphManager {
    private static final Map<ServerLevel, StationNetworkGraphManager> INSTANCES = new WeakHashMap<>();

    private final ServerLevel level;
    private boolean dirty = true;
    private Map<String, StationNetworkComponent> componentsById = Map.of();
    private Map<String, String> stationNetworkIdByStationId = Map.of();
    private Map<String, Set<BlockPos>> portPositionsByStationId = Map.of();

    private StationNetworkGraphManager(ServerLevel level) {
        this.level = level;
    }

    public static synchronized StationNetworkGraphManager get(ServerLevel level) {
        return INSTANCES.computeIfAbsent(level, StationNetworkGraphManager::new);
    }

    public static void invalidate(ServerLevel level) {
        if (level == null) {
            return;
        }
        get(level).markDirty();
    }

    public void markDirty() {
        dirty = true;
    }

    public Map<String, StationNetworkComponent> componentsById() {
        rebuildIfNeeded();
        return componentsById;
    }

    public String stationNetworkId(String stationId) {
        rebuildIfNeeded();
        return stationNetworkIdByStationId.getOrDefault(stationId, "");
    }

    public boolean hasLinkPort(String stationId) {
        rebuildIfNeeded();
        return !portPositionsByStationId.getOrDefault(stationId, Set.of()).isEmpty();
    }

    public StationNetworkComponent componentForStation(String stationId) {
        rebuildIfNeeded();
        String componentId = stationNetworkIdByStationId.getOrDefault(stationId, "");
        return componentId.isBlank() ? null : componentsById.get(componentId);
    }

    private void rebuildIfNeeded() {
        if (!dirty) {
            return;
        }
        dirty = false;

        List<ResearchControllerBlockEntity> controllers = ResearchMultiblockStationRegistry.controllersForLevel(level);
        Map<String, ResearchControllerBlockEntity> controllersByStationId = new LinkedHashMap<>();
        Map<String, Set<String>> stationIdsByTeam = new LinkedHashMap<>();
        for (ResearchControllerBlockEntity controller : controllers) {
            if (controller.stationId() == null || controller.stationId().isBlank()) {
                continue;
            }
            controllersByStationId.put(controller.stationId(), controller);
            if (controller.teamId() != null && !controller.teamId().isBlank()) {
                stationIdsByTeam.computeIfAbsent(controller.teamId(), ignored -> new LinkedHashSet<>()).add(controller.stationId());
            }
        }

        Map<String, List<CableTopologyComponent>> cableComponentsByTeam = CableTopologyScanner.scanByTeam(level);
        Map<String, Set<BlockPos>> attachedPortsByStationId = CableTopologyScanner.stationPortPositions(level);

        Map<String, StationNetworkComponent> nextComponents = new LinkedHashMap<>();
        Map<String, String> nextStationNetworkIds = new LinkedHashMap<>();
        Set<String> assignedStations = new LinkedHashSet<>();

        for (Map.Entry<String, Set<String>> entry : stationIdsByTeam.entrySet()) {
            String teamId = entry.getKey();
            Set<String> teamStationIds = entry.getValue();
            UnionFind unionFind = new UnionFind(teamStationIds);
            for (CableTopologyComponent component : cableComponentsByTeam.getOrDefault(teamId, List.of())) {
                unionFind.unionAll(component.stationIds());
            }

            Map<String, Set<String>> groupedStations = new LinkedHashMap<>();
            Map<String, Set<BlockPos>> groupedPortPositions = new LinkedHashMap<>();
            for (String stationId : teamStationIds) {
                String root = unionFind.find(stationId);
                if (!root.isBlank()) {
                    groupedStations.computeIfAbsent(root, ignored -> new LinkedHashSet<>()).add(stationId);
                }
            }
            for (CableTopologyComponent component : cableComponentsByTeam.getOrDefault(teamId, List.of())) {
                String anchor = component.stationIds().stream()
                        .filter(teamStationIds::contains)
                        .findFirst()
                        .orElse("");
                if (anchor.isBlank()) {
                    continue;
                }
                String root = unionFind.find(anchor);
                if (!root.isBlank()) {
                    groupedPortPositions.computeIfAbsent(root, ignored -> new LinkedHashSet<>()).addAll(component.linkingPortPositions());
                }
            }

            List<Set<String>> grouped = new ArrayList<>(groupedStations.values());
            grouped.sort(Comparator.comparing(component -> component.stream().sorted().findFirst().orElse("")));
            for (Set<String> componentStationIds : grouped) {
                List<BlockPos> controllerPositions = componentStationIds.stream()
                        .map(controllersByStationId::get)
                        .filter(java.util.Objects::nonNull)
                        .map(controller -> controller.getBlockPos().immutable())
                        .sorted(Comparator.comparingLong(BlockPos::asLong))
                        .toList();
                String root = componentStationIds.stream().sorted().findFirst().map(unionFind::find).orElse("");
                List<BlockPos> portPositions = groupedPortPositions.getOrDefault(root, Set.of()).stream()
                        .sorted(Comparator.comparingLong(BlockPos::asLong))
                        .toList();
                String componentId = buildComponentId(controllerPositions, portPositions);
                StationNetworkComponent component = new StationNetworkComponent(
                        componentId,
                        level.dimension().location().toString(),
                        Set.copyOf(componentStationIds),
                        controllerPositions,
                        portPositions
                );
                nextComponents.put(componentId, component);
                for (String stationId : componentStationIds) {
                    nextStationNetworkIds.put(stationId, componentId);
                    assignedStations.add(stationId);
                }
            }
        }

        for (ResearchControllerBlockEntity controller : controllers) {
            String stationId = controller.stationId();
            if (stationId == null || stationId.isBlank() || assignedStations.contains(stationId)) {
                continue;
            }
            String componentId = buildComponentId(List.of(controller.getBlockPos().immutable()), List.of());
            StationNetworkComponent component = new StationNetworkComponent(
                    componentId,
                    level.dimension().location().toString(),
                    Set.of(stationId),
                    List.of(controller.getBlockPos().immutable()),
                    List.of()
            );
            nextComponents.put(componentId, component);
            nextStationNetworkIds.put(stationId, componentId);
        }

        Map<String, Set<BlockPos>> nextPortsByStation = new LinkedHashMap<>();
        attachedPortsByStationId.forEach((stationId, positions) -> {
            if (stationId != null && !stationId.isBlank() && positions != null && !positions.isEmpty()) {
                nextPortsByStation.put(stationId, Set.copyOf(positions));
            }
        });

        componentsById = Map.copyOf(nextComponents);
        stationNetworkIdByStationId = Map.copyOf(nextStationNetworkIds);
        portPositionsByStationId = Map.copyOf(nextPortsByStation);
    }

    private String buildComponentId(List<BlockPos> controllerPositions, List<BlockPos> portPositions) {
        long seed = Long.MAX_VALUE;
        for (BlockPos pos : controllerPositions) {
            seed = Math.min(seed, pos.asLong());
        }
        for (BlockPos pos : portPositions) {
            seed = Math.min(seed, pos.asLong());
        }
        if (seed == Long.MAX_VALUE) {
            seed = 0L;
        }
        return level.dimension().location() + "#station_network#" + seed;
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
