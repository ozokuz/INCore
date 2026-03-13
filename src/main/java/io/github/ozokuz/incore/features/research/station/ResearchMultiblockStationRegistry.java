package io.github.ozokuz.incore.features.research.station;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

public final class ResearchMultiblockStationRegistry {
    private static final Set<ResearchControllerBlockEntity> CONTROLLERS = java.util.Collections.newSetFromMap(new WeakHashMap<>());

    private ResearchMultiblockStationRegistry() {
    }

    public static void register(ResearchControllerBlockEntity controller) {
        if (controller != null) {
            CONTROLLERS.add(controller);
        }
    }

    public static void unregister(ResearchControllerBlockEntity controller) {
        CONTROLLERS.remove(controller);
    }

    public static List<ResearchControllerBlockEntity> controllersForTeam(MinecraftServer server, String teamId) {
        if (server == null || teamId == null || teamId.isBlank()) {
            return List.of();
        }

        List<ResearchControllerBlockEntity> matched = new ArrayList<>();
        Iterator<ResearchControllerBlockEntity> iterator = CONTROLLERS.iterator();
        while (iterator.hasNext()) {
            ResearchControllerBlockEntity controller = iterator.next();
            if (controller == null || controller.isRemoved() || controller.getLevel() == null || controller.getLevel().isClientSide) {
                iterator.remove();
                continue;
            }
            if (controller.getLevel().getServer() != server) {
                iterator.remove();
                continue;
            }
            if (!controller.isFormed()) {
                continue;
            }
            if (teamId.equals(controller.teamId())) {
                matched.add(controller);
            }
        }

        matched.sort(Comparator
                .comparing((ResearchControllerBlockEntity controller) -> controller.getLevel().dimension().location().toString())
                .thenComparing(controller -> controller.getBlockPos().asLong()));
        return List.copyOf(matched);
    }

    public static List<ResearchControllerBlockEntity> controllersForLevel(ServerLevel level) {
        if (level == null) {
            return List.of();
        }

        List<ResearchControllerBlockEntity> matched = new ArrayList<>();
        Iterator<ResearchControllerBlockEntity> iterator = CONTROLLERS.iterator();
        while (iterator.hasNext()) {
            ResearchControllerBlockEntity controller = iterator.next();
            if (controller == null || controller.isRemoved() || controller.getLevel() == null || controller.getLevel().isClientSide) {
                iterator.remove();
                continue;
            }
            if (controller.getLevel() != level) {
                continue;
            }
            if (controller.isFormed()) {
                matched.add(controller);
            }
        }

        matched.sort(Comparator.comparing(controller -> controller.getBlockPos().asLong()));
        return List.copyOf(matched);
    }

    public static Set<String> teamIds(MinecraftServer server) {
        if (server == null) {
            return Set.of();
        }
        Set<String> teamIds = new LinkedHashSet<>();
        Iterator<ResearchControllerBlockEntity> iterator = CONTROLLERS.iterator();
        while (iterator.hasNext()) {
            ResearchControllerBlockEntity controller = iterator.next();
            if (controller == null || controller.isRemoved() || controller.getLevel() == null || controller.getLevel().isClientSide) {
                iterator.remove();
                continue;
            }
            if (controller.getLevel().getServer() != server) {
                iterator.remove();
                continue;
            }
            if (controller.isFormed() && controller.teamId() != null && !controller.teamId().isBlank()) {
                teamIds.add(controller.teamId());
            }
        }
        return Set.copyOf(teamIds);
    }

    public static List<ResearchStationDescriptor> stationsForTeam(MinecraftServer server, String teamId) {
        List<ResearchStationDescriptor> stations = new ArrayList<>();
        for (ResearchControllerBlockEntity controller : controllersForTeam(server, teamId)) {
            ResearchStationDescriptor descriptor = controller.describeStation();
            if (descriptor != null && descriptor.formed()) {
                stations.add(descriptor);
            }
        }
        return List.copyOf(stations);
    }

    public static ResearchControllerBlockEntity controllerByStationId(MinecraftServer server, String teamId, String stationId) {
        if (stationId == null || stationId.isBlank()) {
            return null;
        }
        for (ResearchControllerBlockEntity controller : controllersForTeam(server, teamId)) {
            if (stationId.equals(controller.stationId())) {
                return controller;
            }
        }
        return null;
    }
}
