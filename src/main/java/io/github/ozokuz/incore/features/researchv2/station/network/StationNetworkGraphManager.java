package io.github.ozokuz.incore.features.researchv2.station.network;

import io.github.ozokuz.incore.Registration;
import io.github.ozokuz.incore.features.researchv2.station.ResearchControllerBlockEntity;
import io.github.ozokuz.incore.features.researchv2.station.ResearchMultiblockStationRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
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
        for (ResearchControllerBlockEntity controller : controllers) {
            if (controller.stationId() != null && !controller.stationId().isBlank()) {
                controllersByStationId.put(controller.stationId(), controller);
            }
        }

        Map<BlockPos, PortAttachment> attachmentsByPos = new LinkedHashMap<>();
        Map<String, Set<BlockPos>> attachedPortsByStationId = new HashMap<>();
        for (ResearchControllerBlockEntity controller : controllers) {
            if (controller.stationId() == null || controller.stationId().isBlank()) {
                continue;
            }
            Set<BlockPos> candidatePorts = new LinkedHashSet<>();
            for (BlockPos partPos : controller.connectedParts()) {
                if (isPort(partPos)) {
                    candidatePorts.add(partPos.immutable());
                }
            }
            for (BlockPos portPos : candidatePorts) {
                PortAttachment attachment = new PortAttachment(controller.stationId(), controller.teamId());
                attachmentsByPos.put(portPos.immutable(), attachment);
                attachedPortsByStationId.computeIfAbsent(controller.stationId(), ignored -> new LinkedHashSet<>())
                        .add(portPos.immutable());
                syncPortAttachment(portPos, attachment);
            }
        }

        Map<String, StationNetworkComponent> nextComponents = new LinkedHashMap<>();
        Map<String, String> nextStationNetworkIds = new LinkedHashMap<>();
        Set<String> assignedStations = new LinkedHashSet<>();
        Set<BlockPos> visitedPositions = new HashSet<>();

        List<BlockPos> seedPorts = attachmentsByPos.keySet().stream()
                .sorted(Comparator.comparingLong(BlockPos::asLong))
                .toList();
        for (BlockPos seedPort : seedPorts) {
            if (!visitedPositions.add(seedPort)) {
                continue;
            }

            Set<BlockPos> componentPorts = new LinkedHashSet<>();
            Set<String> componentStationIds = new LinkedHashSet<>();
            Deque<BlockPos> queue = new ArrayDeque<>();
            queue.add(seedPort);

            while (!queue.isEmpty()) {
                BlockPos current = queue.removeFirst();
                PortAttachment currentAttachment = attachmentsByPos.get(current);
                if (currentAttachment != null) {
                    componentPorts.add(current.immutable());
                    if (!currentAttachment.stationId().isBlank()) {
                        componentStationIds.add(currentAttachment.stationId());
                        for (BlockPos siblingPort : attachedPortsByStationId.getOrDefault(currentAttachment.stationId(), Set.of())) {
                            if (visitedPositions.add(siblingPort)) {
                                queue.addLast(siblingPort);
                            }
                        }
                    }
                }

                for (Direction direction : Direction.values()) {
                    BlockPos neighbor = current.relative(direction);
                    if (attachmentsByPos.containsKey(neighbor) || isCable(neighbor)) {
                        if (visitedPositions.add(neighbor)) {
                            queue.addLast(neighbor);
                        }
                    }
                }
            }

            if (componentStationIds.isEmpty()) {
                continue;
            }

            List<BlockPos> controllerPositions = componentStationIds.stream()
                    .map(controllersByStationId::get)
                    .filter(java.util.Objects::nonNull)
                    .map(controller -> controller.getBlockPos().immutable())
                    .sorted(Comparator.comparingLong(BlockPos::asLong))
                    .toList();
            List<BlockPos> portPositions = componentPorts.stream()
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

    private void syncPortAttachment(BlockPos pos, PortAttachment attachment) {
        if (!(level.getBlockEntity(pos) instanceof io.github.ozokuz.incore.features.researchv2.station.LinkingPortBlockEntity port)) {
            return;
        }
        port.setAttachment(attachment.stationId(), attachment.teamId());
    }

    private boolean isCable(BlockPos pos) {
        return level.getBlockState(pos).is(Registration.RESEARCH_LINK_CABLE_BLOCK.get());
    }

    private boolean isPort(BlockPos pos) {
        return level.getBlockState(pos).is(Registration.LINKING_PORT_BLOCK.get());
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

    private record PortAttachment(String stationId, String teamId) {
        private static final PortAttachment EMPTY = new PortAttachment("", "");

        private PortAttachment {
            stationId = stationId == null ? "" : stationId;
            teamId = teamId == null ? "" : teamId;
        }
    }
}
