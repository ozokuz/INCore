package io.github.ozokuz.incore.features.research.station.network;

import io.github.ozokuz.incore.features.research.station.LinkingPortBlockEntity;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

public final class LinkingPortRegistry {
    private static final Set<LinkingPortBlockEntity> PORTS = java.util.Collections.newSetFromMap(new WeakHashMap<>());

    private LinkingPortRegistry() {
    }

    public static void register(LinkingPortBlockEntity port) {
        if (port != null) {
            PORTS.add(port);
        }
    }

    public static void unregister(LinkingPortBlockEntity port) {
        PORTS.remove(port);
    }

    public static List<LinkingPortBlockEntity> portsForLevel(ServerLevel level) {
        if (level == null) {
            return List.of();
        }

        List<LinkingPortBlockEntity> matched = new ArrayList<>();
        Iterator<LinkingPortBlockEntity> iterator = PORTS.iterator();
        while (iterator.hasNext()) {
            LinkingPortBlockEntity port = iterator.next();
            if (port == null || port.isRemoved() || port.getLevel() == null || port.getLevel().isClientSide) {
                iterator.remove();
                continue;
            }
            if (port.getLevel() != level) {
                continue;
            }
            matched.add(port);
        }
        return List.copyOf(matched);
    }
}
