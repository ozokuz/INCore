package io.github.ozokuz.incore.features.researchv2.station.network;

import io.github.ozokuz.incore.features.researchv2.network.ResearchV2Networking;
import io.github.ozokuz.incore.features.researchv2.station.ResearchControllerBlockEntity;
import io.github.ozokuz.incore.features.researchv2.station.ResearchMultiblockStationRegistry;
import io.github.ozokuz.incore.features.researchv2.station.ResearchStationRuntime;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class StationNetworkService {
    private static final String WARNING_MULTIPLE_NETWORKS = "screen.incore.research_v2.network_warning_multiple";

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
            ResearchV2Networking.syncTeam(server, teamId);
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
        Set<String> linkedStationIds = new LinkedHashSet<>();
        Set<String> stationsWithLinkPort = new LinkedHashSet<>();
        Map<String, String> stationNetworkIds = new LinkedHashMap<>();

        for (ResearchControllerBlockEntity controller : controllers) {
            String stationId = controller.stationId();
            if (stationId == null || stationId.isBlank() || !(controller.getLevel() instanceof ServerLevel level)) {
                continue;
            }
            StationNetworkGraphManager graph = StationNetworkGraphManager.get(level);
            StationNetworkComponent component = graph.componentForStation(stationId);
            if (component == null) {
                continue;
            }
            activeStationIds.add(stationId);
            stationNetworkIds.put(stationId, component.id());
            if (component.linked()) {
                linkedStationIds.add(stationId);
            }
            if (graph.hasLinkPort(stationId)) {
                stationsWithLinkPort.add(stationId);
            }
        }

        Set<String> componentIds = new LinkedHashSet<>(stationNetworkIds.values());
        boolean valid = componentIds.size() <= 1;
        Set<String> executableStationIds = valid ? Set.copyOf(activeStationIds) : Set.of();
        String status;
        if (componentIds.size() > 1) {
            status = "conflict";
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
                valid ? "" : WARNING_MULTIPLE_NETWORKS,
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
        if (controller == null || !(controller.getLevel() instanceof ServerLevel level)) {
            return "";
        }
        return StationNetworkGraphManager.get(level).stationNetworkId(controller.stationId());
    }

    public static boolean hasLinkPort(ResearchControllerBlockEntity controller) {
        if (controller == null || !(controller.getLevel() instanceof ServerLevel level)) {
            return false;
        }
        return StationNetworkGraphManager.get(level).hasLinkPort(controller.stationId());
    }
}
