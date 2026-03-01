package io.github.ozokuz.incore.features.researchv2.station;

import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

public final class CrudeResearchStationRegistry {
    private static final Set<CrudeResearchStationBlockEntity> STATIONS = java.util.Collections.newSetFromMap(new WeakHashMap<>());

    private CrudeResearchStationRegistry() {
    }

    public static void register(CrudeResearchStationBlockEntity station) {
        if (station != null) {
            STATIONS.add(station);
        }
    }

    public static void unregister(CrudeResearchStationBlockEntity station) {
        STATIONS.remove(station);
    }

    public static List<CrudeResearchStationBlockEntity> stationsForTeam(MinecraftServer server, String teamId) {
        if (server == null || teamId == null || teamId.isBlank()) {
            return List.of();
        }

        List<CrudeResearchStationBlockEntity> matched = new ArrayList<>();
        Iterator<CrudeResearchStationBlockEntity> iterator = STATIONS.iterator();
        while (iterator.hasNext()) {
            CrudeResearchStationBlockEntity station = iterator.next();
            if (station == null || station.isRemoved() || station.getLevel() == null || station.getLevel().isClientSide) {
                iterator.remove();
                continue;
            }
            if (station.getLevel().getServer() != server) {
                iterator.remove();
                continue;
            }
            if (teamId.equals(station.teamId())) {
                matched.add(station);
            }
        }

        matched.sort(Comparator
                .comparing((CrudeResearchStationBlockEntity station) -> station.getLevel().dimension().location().toString())
                .thenComparing(station -> station.getBlockPos().asLong()));
        return List.copyOf(matched);
    }
}
