package io.github.ozokuz.incore.features.research.station.network;

import io.github.ozokuz.incore.Registration;
import io.github.ozokuz.incore.features.research.station.LinkOwnerKind;
import io.github.ozokuz.incore.features.research.station.LinkingPortBlockEntity;
import io.github.ozokuz.incore.features.research.station.ResearchLinkCableBlock;
import io.github.ozokuz.incore.features.research.station.ResearchControllerBlockEntity;
import io.github.ozokuz.incore.features.research.station.ResearchMultiblockStationRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class CableTopologyScanner {
    private CableTopologyScanner() {
    }

    public static Map<String, List<CableTopologyComponent>> scanByTeam(ServerLevel level) {
        Map<String, Map<BlockPos, LinkingPortBlockEntity>> portsByTeam = collectPortsByTeam(level);

        Map<String, List<CableTopologyComponent>> result = new LinkedHashMap<>();
        portsByTeam.forEach((teamId, teamPorts) -> result.put(teamId, scanTeam(level, teamId, teamPorts)));
        return Map.copyOf(result);
    }

    public static List<CableTopologyComponent> scanTeam(ServerLevel level, String teamId) {
        if (level == null || teamId == null || teamId.isBlank()) {
            return List.of();
        }
        Map<BlockPos, LinkingPortBlockEntity> teamPorts = collectPortsByTeam(level).getOrDefault(teamId, Map.of());
        return scanTeam(level, teamId, teamPorts);
    }

    private static List<CableTopologyComponent> scanTeam(ServerLevel level, String teamId, Map<BlockPos, LinkingPortBlockEntity> teamPorts) {
        if (level == null || teamId == null || teamId.isBlank() || teamPorts == null || teamPorts.isEmpty()) {
            return List.of();
        }

        Set<BlockPos> visited = new LinkedHashSet<>();
        List<BlockPos> seeds = teamPorts.keySet().stream().sorted(Comparator.comparingLong(BlockPos::asLong)).toList();
        List<CableTopologyComponent> components = new ArrayList<>();
        for (BlockPos seed : seeds) {
            if (!visited.add(seed)) {
                continue;
            }

            Deque<BlockPos> queue = new ArrayDeque<>();
            queue.add(seed);
            Set<String> stationIds = new LinkedHashSet<>();
            Set<String> orchestratorIds = new LinkedHashSet<>();
            Set<BlockPos> componentPorts = new LinkedHashSet<>();

            while (!queue.isEmpty()) {
                BlockPos current = queue.removeFirst();
                LinkingPortBlockEntity port = teamPorts.get(current);
                if (port != null) {
                    componentPorts.add(current.immutable());
                    if (port.ownerKind() == LinkOwnerKind.STATION && !port.ownerId().isBlank()) {
                        stationIds.add(port.ownerId());
                    } else if (port.ownerKind() == LinkOwnerKind.ORCHESTRATOR && !port.ownerId().isBlank()) {
                        orchestratorIds.add(port.ownerId());
                    }
                }

                for (Direction direction : Direction.values()) {
                    BlockPos neighbor = current.relative(direction);
                    if (!canTraverse(level, current, neighbor, direction, teamPorts)) {
                        continue;
                    }
                    if (visited.add(neighbor)) {
                        queue.addLast(neighbor);
                    }
                }
            }

            if (!stationIds.isEmpty() || !orchestratorIds.isEmpty()) {
                List<BlockPos> ports = componentPorts.stream().sorted(Comparator.comparingLong(BlockPos::asLong)).toList();
                components.add(new CableTopologyComponent(level.dimension().location().toString(), teamId, stationIds, orchestratorIds, ports));
            }
        }
        return List.copyOf(components);
    }

    public static Map<String, Set<BlockPos>> stationPortPositions(ServerLevel level) {
        Map<String, Set<BlockPos>> portsByStationId = new LinkedHashMap<>();
        for (LinkingPortBlockEntity port : LinkingPortRegistry.portsForLevel(level)) {
            if (port.ownerKind() != LinkOwnerKind.STATION || port.ownerId().isBlank()) {
                continue;
            }
            portsByStationId.computeIfAbsent(port.ownerId(), ignored -> new LinkedHashSet<>()).add(port.getBlockPos().immutable());
        }
        for (ResearchControllerBlockEntity controller : ResearchMultiblockStationRegistry.controllersForLevel(level)) {
            if (!controller.isFormed() || controller.stationId().isBlank()) {
                continue;
            }
            for (BlockPos portPos : controller.linkingPortPositions()) {
                if (level.getBlockEntity(portPos) instanceof LinkingPortBlockEntity) {
                    portsByStationId.computeIfAbsent(controller.stationId(), ignored -> new LinkedHashSet<>()).add(portPos.immutable());
                }
            }
        }
        return Map.copyOf(portsByStationId);
    }

    private static Map<String, Map<BlockPos, LinkingPortBlockEntity>> collectPortsByTeam(ServerLevel level) {
        Map<String, Map<BlockPos, LinkingPortBlockEntity>> portsByTeam = new LinkedHashMap<>();
        for (LinkingPortBlockEntity port : LinkingPortRegistry.portsForLevel(level)) {
            String teamId = port.attachedTeamId();
            if (teamId == null || teamId.isBlank()) {
                continue;
            }
            portsByTeam.computeIfAbsent(teamId, ignored -> new LinkedHashMap<>())
                    .put(port.getBlockPos().immutable(), port);
        }
        for (ResearchControllerBlockEntity controller : ResearchMultiblockStationRegistry.controllersForLevel(level)) {
            if (!controller.isFormed() || controller.teamId().isBlank()) {
                continue;
            }
            Map<BlockPos, LinkingPortBlockEntity> teamPorts = portsByTeam.computeIfAbsent(controller.teamId(), ignored -> new LinkedHashMap<>());
            for (BlockPos portPos : controller.linkingPortPositions()) {
                if (level.getBlockEntity(portPos) instanceof LinkingPortBlockEntity port) {
                    teamPorts.putIfAbsent(portPos.immutable(), port);
                }
            }
        }
        return Map.copyOf(portsByTeam);
    }

    private static boolean canTraverse(ServerLevel level, BlockPos current, BlockPos neighbor, Direction direction, Map<BlockPos, LinkingPortBlockEntity> teamPorts) {
        if (!teamPorts.containsKey(neighbor) && !level.getBlockState(neighbor).is(Registration.RESEARCH_LINK_CABLE_BLOCK.get())) {
            return false;
        }

        BlockPos cablePos;
        BlockPos otherPos;
        if (level.getBlockState(current).is(Registration.RESEARCH_LINK_CABLE_BLOCK.get())) {
            cablePos = current;
            otherPos = neighbor;
        } else if (level.getBlockState(neighbor).is(Registration.RESEARCH_LINK_CABLE_BLOCK.get())) {
            cablePos = neighbor;
            otherPos = current;
        } else {
            return true;
        }

        BlockState cableState = level.getBlockState(cablePos);
        Direction fromCable = cablePos.relative(direction).equals(otherPos) ? direction : direction.getOpposite();
        return ResearchLinkCableBlock.hasOpenConnection(cableState, fromCable);
    }
}
