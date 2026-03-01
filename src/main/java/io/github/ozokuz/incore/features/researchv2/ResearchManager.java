package io.github.ozokuz.incore.features.researchv2;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.github.ozokuz.incore.features.researchv2.event.ResearchV2LifecycleCallbacks;
import io.github.ozokuz.incore.features.researchv2.model.ResearchCostDefinition;
import io.github.ozokuz.incore.features.researchv2.model.ResearchNetworkDefinition;
import io.github.ozokuz.incore.features.researchv2.model.ResearchNodeDefinition;
import io.github.ozokuz.incore.features.researchv2.model.ResearchPowerDefinition;
import io.github.ozokuz.incore.features.researchv2.network.ResearchV2Networking;
import io.github.ozokuz.incore.features.researchv2.provider.ResearchProviderManager;
import io.github.ozokuz.incore.features.researchv2.registry.ResearchRegistry;
import io.github.ozokuz.incore.features.researchv2.state.ResearchNetworkSavedData;
import io.github.ozokuz.incore.features.researchv2.state.ResearchQueueEntry;
import io.github.ozokuz.incore.features.researchv2.state.ResearchQueueStatus;
import io.github.ozokuz.incore.features.researchv2.state.TeamResearchState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
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

        ResearchNodeDefinition node = ResearchRegistry.nodes().get(nodeId);
        if (node == null) {
            return false;
        }

        if (!state.discoveredNodes().contains(nodeId) || state.completedNodes().contains(nodeId)) {
            return false;
        }
        if (!state.completedNodes().containsAll(node.prerequisites())) {
            return false;
        }
        if (state.researchQueue().stream().map(ResearchQueueEntry::nodeId).anyMatch(nodeId::equals)) {
            return false;
        }

        ResearchCostDefinition cost = node.researchCost();
        return ResearchProviderManager.hasRequiredModules(server, teamId, cost)
                && ResearchProviderManager.hasRequiredMaterials(server, teamId, cost);
    }

    public static boolean queueResearch(MinecraftServer server, String teamId, ResourceLocation nodeId) {
        if (!canQueue(server, teamId, nodeId)) {
            return false;
        }

        ResearchNodeDefinition node = ResearchRegistry.nodes().get(nodeId);
        if (node == null) {
            return false;
        }

        ResearchNetworkSavedData data = ResearchNetworkSavedData.get(server);
        TeamResearchState state = ensureTeamState(server, teamId);
        state.researchQueue().add(new ResearchQueueEntry(nodeId, 0, node.researchTime(), ResearchQueueStatus.QUEUED, List.of()));
        data.setDirty();
        ResearchV2Networking.syncTeam(server, teamId);
        return true;
    }

    public static boolean tickResearch(MinecraftServer server, String teamId) {
        TeamResearchState state = getTeamState(server, teamId);
        if (state == null || !hasValidNetwork(state) || state.researchQueue().isEmpty()) {
            return false;
        }

        ResearchNetworkSavedData data = ResearchNetworkSavedData.get(server);
        ResearchQueueEntry head = state.researchQueue().get(0);
        ResourceLocation nodeId = head.nodeId();
        ResearchNodeDefinition node = ResearchRegistry.nodes().get(nodeId);

        if (node == null || !isNodeInActiveNetwork(state, nodeId)) {
            state.researchQueue().remove(0);
            data.setDirty();
            return true;
        }

        if (state.completedNodes().contains(nodeId)) {
            state.researchQueue().remove(0);
            data.setDirty();
            return true;
        }

        int requiredTime = resolveRequiredTime(head, node);
        int progress = Math.min(requiredTime, Math.max(0, head.timeProgress()));
        ResearchQueueStatus status = head.status();

        if (!state.discoveredNodes().contains(nodeId)
                || !state.completedNodes().containsAll(node.prerequisites())) {
            return updateQueueHead(state, node, progress, requiredTime, ResearchQueueStatus.PAUSED_MISSING_INPUTS, false, data);
        }

        if (status == ResearchQueueStatus.QUEUED || status == ResearchQueueStatus.PAUSED_MISSING_INPUTS) {
            ResearchCostDefinition cost = node.researchCost();
            if (!ResearchProviderManager.hasRequiredModules(server, teamId, cost)
                    || !ResearchProviderManager.hasRequiredMaterials(server, teamId, cost)
                    || !ResearchProviderManager.consumeRequiredModules(server, teamId, cost)
                    || !ResearchProviderManager.consumeRequiredMaterials(server, teamId, cost)) {
                return updateQueueHead(state, node, progress, requiredTime, ResearchQueueStatus.PAUSED_MISSING_INPUTS, false, data);
            }

            updateQueueHead(state, node, progress, requiredTime, ResearchQueueStatus.RUNNING, true, data);
            ResearchV2LifecycleCallbacks.onResearchStarted(teamId, nodeId, requiredTime);
            status = ResearchQueueStatus.RUNNING;
        }

        int rpPerTick = computePowerCostPerTick(node.researchPower(), progress, requiredTime);
        if (!ResearchProviderManager.consumePower(server, teamId, rpPerTick)) {
            return updateQueueHead(state, node, progress, requiredTime, ResearchQueueStatus.PAUSED_NO_POWER, false, data);
        }

        int nextProgress = Math.min(requiredTime, progress + 1);
        updateQueueHead(state, node, nextProgress, requiredTime, ResearchQueueStatus.RUNNING, true, data);
        ResearchV2LifecycleCallbacks.onResearchProgress(teamId, nodeId, nextProgress, requiredTime, rpPerTick);

        if (nextProgress >= requiredTime) {
            state.discoveredNodes().add(nodeId);
            state.completedNodes().add(nodeId);
            state.researchQueue().remove(0);
            data.setDirty();
            ResearchV2LifecycleCallbacks.onResearchCompleted(teamId, nodeId);
        }
        return true;
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
                || state.storedResearchPowerBuffer() > 0
                || !state.devResearchMaterials().isEmpty()
                || !state.devLogicModules().isEmpty();

        state.discoveredNodes().clear();
        state.completedNodes().clear();
        state.researchQueue().clear();
        state.setStoredResearchPowerBuffer(0);
        state.devResearchMaterials().clear();
        state.devLogicModules().clear();

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
            row.addProperty("timeProgress", entry.timeProgress());
            row.addProperty("progress", entry.timeProgress());
            row.addProperty("requiredTime", entry.requiredTime());
            row.addProperty("status", entry.status().name());
            JsonArray stations = new JsonArray();
            entry.assignedStationIds().forEach(stations::add);
            row.add("assignedStationIds", stations);
            queue.add(row);
        });
        root.add("researchQueue", queue);

        root.addProperty("storedResearchPowerBuffer", state.storedResearchPowerBuffer());
        root.addProperty("availableResearchPower", ResearchProviderManager.availablePower(server, teamId));

        JsonObject materials = new JsonObject();
        state.devResearchMaterials().entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> materials.addProperty(entry.getKey(), entry.getValue()));
        root.add("devResearchMaterials", materials);

        JsonObject modules = new JsonObject();
        state.devLogicModules().entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> modules.addProperty(entry.getKey(), entry.getValue()));
        root.add("devLogicModules", modules);

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

    private static int resolveRequiredTime(ResearchQueueEntry entry, ResearchNodeDefinition node) {
        if (entry.requiredTime() > 0) {
            return entry.requiredTime();
        }
        return Math.max(1, node.researchTime());
    }

    private static int computePowerCostPerTick(ResearchPowerDefinition power, int timeProgress, int requiredTime) {
        if (power == null) {
            power = ResearchPowerDefinition.defaults();
        }
        double ratio = requiredTime <= 0 ? 0.0D : Math.max(0.0D, (double) timeProgress / (double) requiredTime);
        double value = power.baseRpPerTick() + (power.curveScaleRpPerTick() * Math.pow(ratio, power.curveExponent()));
        return Math.max(0, (int) Math.ceil(value));
    }

    private static boolean updateQueueHead(
            TeamResearchState state,
            ResearchNodeDefinition node,
            int progress,
            int requiredTime,
            ResearchQueueStatus status,
            boolean forceDirty,
            ResearchNetworkSavedData data
    ) {
        if (state.researchQueue().isEmpty()) {
            return false;
        }

        ResearchQueueEntry existing = state.researchQueue().get(0);
        int normalizedProgress = Math.max(0, Math.min(progress, requiredTime));
        ResearchQueueEntry replacement = new ResearchQueueEntry(
                existing.nodeId(),
                normalizedProgress,
                requiredTime <= 0 ? Math.max(1, node.researchTime()) : requiredTime,
                status,
                existing.assignedStationIds()
        );

        boolean changed = forceDirty || !existing.equals(replacement);
        if (changed) {
            state.researchQueue().set(0, replacement);
            data.setDirty();
        }
        return changed;
    }
}
