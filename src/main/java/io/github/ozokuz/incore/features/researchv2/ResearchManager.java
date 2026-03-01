package io.github.ozokuz.incore.features.researchv2;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.github.ozokuz.incore.features.researchv2.model.ResearchNetworkDefinition;
import io.github.ozokuz.incore.features.researchv2.model.ResearchNodeDefinition;
import io.github.ozokuz.incore.features.researchv2.network.ResearchV2Networking;
import io.github.ozokuz.incore.features.researchv2.registry.ResearchRegistry;
import io.github.ozokuz.incore.features.researchv2.state.ResearchNetworkSavedData;
import io.github.ozokuz.incore.features.researchv2.state.ResearchQueueEntry;
import io.github.ozokuz.incore.features.researchv2.state.TeamResearchState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class ResearchManager {
    private ResearchManager() {
    }

    public static boolean isDiscovered(MinecraftServer server, String teamId, ResourceLocation nodeId) {
        TeamResearchState state = getTeamState(server, teamId);
        return state != null && state.discoveredNodes().contains(nodeId);
    }

    public static boolean isResearched(MinecraftServer server, String teamId, ResourceLocation nodeId) {
        TeamResearchState state = getTeamState(server, teamId);
        return state != null && state.completedNodes().contains(nodeId);
    }

    public static boolean canDiscover(MinecraftServer server, String teamId, ResourceLocation nodeId) {
        TeamResearchState state = ensureTeamState(server, teamId);
        if (!hasValidNetwork(state)) {
            return false;
        }

        ResearchNodeDefinition node = ResearchRegistry.nodes().get(nodeId);
        if (node == null || !isNodeInActiveNetwork(state, nodeId)) {
            return false;
        }
        if (state.discoveredNodes().contains(nodeId) || state.completedNodes().contains(nodeId)) {
            return false;
        }
        return state.completedNodes().containsAll(node.prerequisites());
    }

    public static boolean canQueue(MinecraftServer server, String teamId, ResourceLocation nodeId) {
        TeamResearchState state = ensureTeamState(server, teamId);
        if (!hasValidNetwork(state) || !isNodeInActiveNetwork(state, nodeId)) {
            return false;
        }
        if (!state.discoveredNodes().contains(nodeId) || state.completedNodes().contains(nodeId)) {
            return false;
        }
        return state.researchQueue().stream().map(ResearchQueueEntry::nodeId).noneMatch(nodeId::equals);
    }

    public static boolean queueResearch(MinecraftServer server, String teamId, ResourceLocation nodeId) {
        if (!canQueue(server, teamId, nodeId)) {
            return false;
        }

        ResearchNetworkSavedData data = ResearchNetworkSavedData.get(server);
        TeamResearchState state = ensureTeamState(server, teamId);
        state.researchQueue().add(new ResearchQueueEntry(nodeId, 0, List.of()));
        data.setDirty();
        ResearchV2Networking.syncTeam(server, teamId);
        return true;
    }

    public static boolean tickResearch(MinecraftServer server, String teamId) {
        TeamResearchState state = getTeamState(server, teamId);
        if (state == null || !hasValidNetwork(state)) {
            return false;
        }
        return false;
    }

    public static boolean grantDiscovery(MinecraftServer server, String teamId, ResourceLocation nodeId, String reason) {
        if (!canDiscover(server, teamId, nodeId)) {
            return false;
        }

        TeamResearchState state = ensureTeamState(server, teamId);
        boolean changed = state.discoveredNodes().add(nodeId);
        if (changed) {
            ResearchNetworkSavedData.get(server).setDirty();
            ResearchV2Networking.syncTeam(server, teamId);
        }
        return changed;
    }

    public static boolean grantCompletion(MinecraftServer server, String teamId, ResourceLocation nodeId) {
        TeamResearchState state = ensureTeamState(server, teamId);
        if (!hasValidNetwork(state) || !isNodeInActiveNetwork(state, nodeId)) {
            return false;
        }
        if (!ResearchRegistry.nodes().containsKey(nodeId)) {
            return false;
        }

        boolean changed = false;
        changed = state.discoveredNodes().add(nodeId) || changed;
        changed = state.completedNodes().add(nodeId) || changed;
        changed = state.researchQueue().removeIf(entry -> nodeId.equals(entry.nodeId())) || changed;

        if (changed) {
            ResearchNetworkSavedData.get(server).setDirty();
            ResearchV2Networking.syncTeam(server, teamId);
        }
        return changed;
    }

    public static boolean clearResearch(MinecraftServer server, String teamId) {
        TeamResearchState state = getTeamState(server, teamId);
        if (state == null) {
            return false;
        }

        boolean changed = !state.discoveredNodes().isEmpty()
                || !state.completedNodes().isEmpty()
                || !state.researchQueue().isEmpty()
                || state.storedResearchPowerBuffer() > 0;

        state.discoveredNodes().clear();
        state.completedNodes().clear();
        state.researchQueue().clear();
        state.setStoredResearchPowerBuffer(0);

        if (changed) {
            ResearchNetworkSavedData.get(server).setDirty();
            ResearchV2Networking.syncTeam(server, teamId);
        }
        return changed;
    }

    public static String snapshotJson(MinecraftServer server, String teamId) {
        TeamResearchState state = ensureTeamState(server, teamId);
        JsonObject root = new JsonObject();
        root.addProperty("teamId", teamId);
        root.addProperty("activeNetworkId", state.activeNetworkId() == null ? "" : state.activeNetworkId().toString());
        root.addProperty("researchEnabled", hasValidNetwork(state));

        JsonArray discovered = new JsonArray();
        state.discoveredNodes().stream().map(ResourceLocation::toString).sorted().forEach(discovered::add);
        root.add("discoveredNodes", discovered);

        JsonArray completed = new JsonArray();
        state.completedNodes().stream().map(ResourceLocation::toString).sorted().forEach(completed::add);
        root.add("completedNodes", completed);

        JsonArray queue = new JsonArray();
        state.researchQueue().forEach(entry -> {
            JsonObject row = new JsonObject();
            row.addProperty("nodeId", entry.nodeId().toString());
            row.addProperty("progress", entry.progress());
            JsonArray stations = new JsonArray();
            entry.assignedStationIds().forEach(stations::add);
            row.add("assignedStationIds", stations);
            queue.add(row);
        });
        root.add("researchQueue", queue);

        root.addProperty("storedResearchPowerBuffer", state.storedResearchPowerBuffer());

        JsonArray trees = new JsonArray();
        ResearchRegistry.trees().keySet().stream().map(ResourceLocation::toString).sorted().forEach(trees::add);
        root.add("trees", trees);

        JsonArray nodes = new JsonArray();
        ResearchRegistry.nodes().values().stream()
                .sorted(Comparator.comparing(node -> node.id().toString()))
                .forEach(node -> {
                    JsonObject row = new JsonObject();
                    row.addProperty("id", node.id().toString());
                    row.addProperty("treeId", node.treeId().toString());
                    row.addProperty("categoryId", node.categoryId().toString());
                    JsonArray prerequisites = new JsonArray();
                    node.prerequisites().stream().map(ResourceLocation::toString).sorted().forEach(prerequisites::add);
                    row.add("prerequisites", prerequisites);
                    row.addProperty("researchTime", node.researchTime());
                    nodes.add(row);
                });
        root.add("nodes", nodes);
        return root.toString();
    }

    public static TeamResearchState ensureTeamState(MinecraftServer server, String teamId) {
        ResearchNetworkSavedData data = ResearchNetworkSavedData.get(server);
        TeamResearchState state = data.getOrCreateTeamState(teamId);

        ResourceLocation current = state.activeNetworkId();
        if (current != null && ResearchRegistry.networks().containsKey(current)) {
            return state;
        }

        if (ResearchRegistry.networks().size() == 1) {
            ResourceLocation single = ResearchRegistry.networks().keySet().iterator().next();
            if (!Objects.equals(single, current)) {
                state.setActiveNetworkId(single);
                data.setDirty();
            }
        }
        return state;
    }

    private static TeamResearchState getTeamState(MinecraftServer server, String teamId) {
        if (teamId == null || teamId.isBlank()) {
            return null;
        }
        return ResearchNetworkSavedData.get(server).getTeamState(teamId);
    }

    private static boolean hasValidNetwork(TeamResearchState state) {
        ResourceLocation activeNetworkId = state.activeNetworkId();
        if (activeNetworkId == null) {
            return false;
        }
        return ResearchRegistry.networks().containsKey(activeNetworkId);
    }

    private static boolean isNodeInActiveNetwork(TeamResearchState state, ResourceLocation nodeId) {
        ResourceLocation activeNetworkId = state.activeNetworkId();
        if (activeNetworkId == null) {
            return false;
        }

        ResearchNetworkDefinition network = ResearchRegistry.networks().get(activeNetworkId);
        if (network == null) {
            return false;
        }
        Set<ResourceLocation> nodes = network.nodeIds();
        return nodes.contains(nodeId);
    }
}
