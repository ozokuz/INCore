package io.github.ozokuz.incore.features.roguelike.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class RoguelikeMinimapClientCache {
    private static long instanceId;
    private static int originChunkX;
    private static int originChunkZ;
    private static boolean hasGraph;
    private static final Set<Integer> revealedRooms = new HashSet<>();
    private static final Map<UUID, Integer> partyRoomMarkers = new HashMap<>();

    private RoguelikeMinimapClientCache() {
    }

    public static synchronized void setGraph(long instance, int originX, int originZ) {
        instanceId = instance;
        originChunkX = originX;
        originChunkZ = originZ;
        hasGraph = instance > 0L;
        revealedRooms.clear();
        partyRoomMarkers.clear();
    }

    public static synchronized void revealRoom(long instance, int roomId) {
        if (!hasGraph || instanceId != instance) {
            return;
        }
        revealedRooms.add(roomId);
    }

    public static synchronized void updatePartyMarkers(long instance, List<PartyMarker> markers) {
        if (!hasGraph || instanceId != instance) {
            return;
        }
        partyRoomMarkers.clear();
        for (PartyMarker marker : markers) {
            partyRoomMarkers.put(marker.playerId(), marker.roomId());
        }
    }

    public static synchronized Snapshot snapshot() {
        return new Snapshot(
                instanceId,
                originChunkX,
                originChunkZ,
                hasGraph,
                new HashSet<>(revealedRooms),
                new HashMap<>(partyRoomMarkers)
        );
    }

    public static synchronized void clear() {
        instanceId = 0L;
        originChunkX = 0;
        originChunkZ = 0;
        hasGraph = false;
        revealedRooms.clear();
        partyRoomMarkers.clear();
    }

    public record PartyMarker(UUID playerId, int roomId) {
    }

    public record Snapshot(
            long instanceId,
            int originChunkX,
            int originChunkZ,
            boolean hasGraph,
            Set<Integer> revealedRooms,
            Map<UUID, Integer> partyRoomMarkers
    ) {
    }
}
