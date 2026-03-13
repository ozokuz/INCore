package io.github.ozokuz.incore.features.research.provider;

import io.github.ozokuz.incore.features.research.ResearchManager;
import io.github.ozokuz.incore.features.research.model.ResearchCostDefinition;
import io.github.ozokuz.incore.features.research.state.TeamResearchState;
import net.minecraft.server.MinecraftServer;

import java.util.List;
import java.util.Map;

public final class DevResearchResourceProvider implements ILogicModuleProvider, IResearchMaterialProvider, IResearchPowerProvider {
    @Override
    public boolean hasRequiredModules(MinecraftServer server, String teamId, List<ResearchCostDefinition.LogicModuleRequirement> requirements) {
        TeamResearchState state = ResearchManager.ensureTeamState(server, teamId);
        return hasAll(state.devLogicModules(), requirements.stream().map(req -> Map.entry(req.moduleTier(), req.durabilityCost())).toList());
    }

    @Override
    public boolean consumeRequiredModules(MinecraftServer server, String teamId, List<ResearchCostDefinition.LogicModuleRequirement> requirements) {
        TeamResearchState state = ResearchManager.ensureTeamState(server, teamId);
        List<Map.Entry<String, Integer>> entries = requirements.stream().map(req -> Map.entry(req.moduleTier(), req.durabilityCost())).toList();
        if (!hasAll(state.devLogicModules(), entries)) {
            return false;
        }
        consume(state.devLogicModules(), entries);
        return true;
    }

    @Override
    public boolean hasRequiredMaterials(MinecraftServer server, String teamId, List<ResearchCostDefinition.ResearchMaterialRequirement> requirements) {
        TeamResearchState state = ResearchManager.ensureTeamState(server, teamId);
        return hasAll(state.devResearchMaterials(), requirements.stream().map(req -> Map.entry(req.materialId(), req.count())).toList());
    }

    @Override
    public boolean consumeRequiredMaterials(MinecraftServer server, String teamId, List<ResearchCostDefinition.ResearchMaterialRequirement> requirements) {
        TeamResearchState state = ResearchManager.ensureTeamState(server, teamId);
        List<Map.Entry<String, Integer>> entries = requirements.stream().map(req -> Map.entry(req.materialId(), req.count())).toList();
        if (!hasAll(state.devResearchMaterials(), entries)) {
            return false;
        }
        consume(state.devResearchMaterials(), entries);
        return true;
    }

    @Override
    public boolean hasPower(MinecraftServer server, String teamId, int amount) {
        if (amount <= 0) {
            return true;
        }
        TeamResearchState state = ResearchManager.ensureTeamState(server, teamId);
        return state.storedResearchPowerBuffer() >= amount;
    }

    @Override
    public boolean consumePower(MinecraftServer server, String teamId, int amount) {
        if (amount <= 0) {
            return true;
        }
        TeamResearchState state = ResearchManager.ensureTeamState(server, teamId);
        int current = state.storedResearchPowerBuffer();
        if (current < amount) {
            return false;
        }
        state.setStoredResearchPowerBuffer(current - amount);
        return true;
    }

    @Override
    public int availablePower(MinecraftServer server, String teamId) {
        TeamResearchState state = ResearchManager.ensureTeamState(server, teamId);
        return state.storedResearchPowerBuffer();
    }

    public void setPower(MinecraftServer server, String teamId, int amount) {
        TeamResearchState state = ResearchManager.ensureTeamState(server, teamId);
        state.setStoredResearchPowerBuffer(Math.max(0, amount));
    }

    public void setMaterial(MinecraftServer server, String teamId, String materialId, int amount) {
        TeamResearchState state = ResearchManager.ensureTeamState(server, teamId);
        setCount(state.devResearchMaterials(), materialId, amount);
    }

    public void setModule(MinecraftServer server, String teamId, String moduleTier, int amount) {
        TeamResearchState state = ResearchManager.ensureTeamState(server, teamId);
        setCount(state.devLogicModules(), moduleTier, amount);
    }

    public int materialCount(MinecraftServer server, String teamId, String materialId) {
        TeamResearchState state = ResearchManager.ensureTeamState(server, teamId);
        return Math.max(0, state.devResearchMaterials().getOrDefault(materialId, 0));
    }

    public int moduleCount(MinecraftServer server, String teamId, String moduleTier) {
        TeamResearchState state = ResearchManager.ensureTeamState(server, teamId);
        return Math.max(0, state.devLogicModules().getOrDefault(moduleTier, 0));
    }

    private static void setCount(Map<String, Integer> map, String key, int amount) {
        if (key == null || key.isBlank()) {
            return;
        }
        int clamped = Math.max(0, amount);
        if (clamped <= 0) {
            map.remove(key);
        } else {
            map.put(key, clamped);
        }
    }

    private static boolean hasAll(Map<String, Integer> pool, List<Map.Entry<String, Integer>> requirements) {
        for (Map.Entry<String, Integer> requirement : requirements) {
            String key = requirement.getKey();
            int required = Math.max(0, requirement.getValue());
            if (required <= 0) {
                continue;
            }
            if (key == null || key.isBlank()) {
                return false;
            }
            if (pool.getOrDefault(key, 0) < required) {
                return false;
            }
        }
        return true;
    }

    private static void consume(Map<String, Integer> pool, List<Map.Entry<String, Integer>> requirements) {
        for (Map.Entry<String, Integer> requirement : requirements) {
            String key = requirement.getKey();
            int required = Math.max(0, requirement.getValue());
            if (required <= 0 || key == null || key.isBlank()) {
                continue;
            }
            int next = Math.max(0, pool.getOrDefault(key, 0) - required);
            if (next == 0) {
                pool.remove(key);
            } else {
                pool.put(key, next);
            }
        }
    }
}
