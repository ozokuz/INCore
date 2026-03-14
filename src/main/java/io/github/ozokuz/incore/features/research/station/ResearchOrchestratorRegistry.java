package io.github.ozokuz.incore.features.research.station;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.WeakHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

public final class ResearchOrchestratorRegistry {
    private static final java.util.Set<ResearchOrchestratorControllerBlockEntity> ORCHESTRATORS =
            java.util.Collections.newSetFromMap(new WeakHashMap<>());

    private ResearchOrchestratorRegistry() {
    }

    public static void register(ResearchOrchestratorControllerBlockEntity orchestrator) {
        if (orchestrator != null) {
            ORCHESTRATORS.add(orchestrator);
        }
    }

    public static void unregister(ResearchOrchestratorControllerBlockEntity orchestrator) {
        ORCHESTRATORS.remove(orchestrator);
    }

    public static List<ResearchOrchestratorControllerBlockEntity> orchestratorsForLevel(ServerLevel level) {
        if (level == null) {
            return List.of();
        }
        List<ResearchOrchestratorControllerBlockEntity> matched = new ArrayList<>();
        Iterator<ResearchOrchestratorControllerBlockEntity> iterator = ORCHESTRATORS.iterator();
        while (iterator.hasNext()) {
            ResearchOrchestratorControllerBlockEntity orchestrator = iterator.next();
            if (orchestrator == null || orchestrator.isRemoved() || orchestrator.getLevel() == null || orchestrator.getLevel().isClientSide) {
                iterator.remove();
                continue;
            }
            if (orchestrator.getLevel() != level) {
                continue;
            }
            if (orchestrator.isFormed()) {
                matched.add(orchestrator);
            }
        }
        matched.sort(Comparator.comparingLong(o -> o.getBlockPos().asLong()));
        return List.copyOf(matched);
    }

    public static List<ResearchOrchestratorControllerBlockEntity> orchestratorsForTeam(MinecraftServer server, String teamId) {
        if (server == null || teamId == null || teamId.isBlank()) {
            return List.of();
        }
        List<ResearchOrchestratorControllerBlockEntity> matched = new ArrayList<>();
        Iterator<ResearchOrchestratorControllerBlockEntity> iterator = ORCHESTRATORS.iterator();
        while (iterator.hasNext()) {
            ResearchOrchestratorControllerBlockEntity orchestrator = iterator.next();
            if (orchestrator == null || orchestrator.isRemoved() || orchestrator.getLevel() == null || orchestrator.getLevel().isClientSide) {
                iterator.remove();
                continue;
            }
            if (orchestrator.getLevel().getServer() != server) {
                iterator.remove();
                continue;
            }
            if (orchestrator.isFormed() && teamId.equals(orchestrator.teamId())) {
                matched.add(orchestrator);
            }
        }
        matched.sort(Comparator
                .comparing((ResearchOrchestratorControllerBlockEntity o) -> o.getLevel().dimension().location().toString())
                .thenComparing(o -> o.getBlockPos().asLong()));
        return List.copyOf(matched);
    }
}
