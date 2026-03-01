package io.github.ozokuz.incore.features.researchv2.station;

import io.github.ozokuz.incore.features.researchv2.ResearchManager;
import io.github.ozokuz.incore.features.researchv2.model.ResearchCostDefinition;
import io.github.ozokuz.incore.features.researchv2.provider.ILogicModuleProvider;
import io.github.ozokuz.incore.features.researchv2.provider.IResearchMaterialProvider;
import io.github.ozokuz.incore.features.researchv2.provider.IResearchPowerProvider;
import io.github.ozokuz.incore.features.researchv2.state.TeamResearchState;
import net.minecraft.server.MinecraftServer;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class CrudeResearchStationResourceProvider implements ILogicModuleProvider, IResearchMaterialProvider, IResearchPowerProvider {
    private static final String BASIC_TIER = "basic";
    private static final String STARTER_DATA = "incore:starter_data";

    @Override
    public boolean hasRequiredModules(MinecraftServer server, String teamId, List<ResearchCostDefinition.LogicModuleRequirement> requirements) {
        Map<String, Integer> requiredByTier = foldModuleRequirements(requirements);
        if (requiredByTier.isEmpty()) {
            return true;
        }

        TeamResearchState state = ResearchManager.ensureTeamState(server, teamId);
        List<CrudeResearchStationBlockEntity> stations = CrudeResearchStationRegistry.stationsForTeam(server, teamId);
        for (var entry : requiredByTier.entrySet()) {
            String tier = entry.getKey();
            int required = entry.getValue();
            int available = Math.max(0, state.devLogicModules().getOrDefault(tier, 0)) + countTierFromStations(stations, tier);
            if (available < required) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean consumeRequiredModules(MinecraftServer server, String teamId, List<ResearchCostDefinition.LogicModuleRequirement> requirements) {
        Map<String, Integer> requiredByTier = foldModuleRequirements(requirements);
        if (requiredByTier.isEmpty()) {
            return true;
        }
        if (!hasRequiredModules(server, teamId, requirements)) {
            return false;
        }

        TeamResearchState state = ResearchManager.ensureTeamState(server, teamId);
        List<CrudeResearchStationBlockEntity> stations = CrudeResearchStationRegistry.stationsForTeam(server, teamId);

        for (var entry : requiredByTier.entrySet()) {
            String tier = entry.getKey();
            int remaining = entry.getValue();

            int devCount = Math.max(0, state.devLogicModules().getOrDefault(tier, 0));
            if (devCount > 0) {
                int consumeDev = Math.min(devCount, remaining);
                int nextDev = devCount - consumeDev;
                if (nextDev <= 0) {
                    state.devLogicModules().remove(tier);
                } else {
                    state.devLogicModules().put(tier, nextDev);
                }
                remaining -= consumeDev;
            }

            if (remaining <= 0) {
                continue;
            }

            if (!BASIC_TIER.equalsIgnoreCase(tier)) {
                return false;
            }

            for (CrudeResearchStationBlockEntity station : stations) {
                if (remaining <= 0) {
                    break;
                }
                remaining -= station.consumeBasicLogicModules(remaining);
            }

            if (remaining > 0) {
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean hasRequiredMaterials(MinecraftServer server, String teamId, List<ResearchCostDefinition.ResearchMaterialRequirement> requirements) {
        Map<String, Integer> requiredByMaterial = foldMaterialRequirements(requirements);
        if (requiredByMaterial.isEmpty()) {
            return true;
        }

        TeamResearchState state = ResearchManager.ensureTeamState(server, teamId);
        List<CrudeResearchStationBlockEntity> stations = CrudeResearchStationRegistry.stationsForTeam(server, teamId);
        for (var entry : requiredByMaterial.entrySet()) {
            String materialId = entry.getKey();
            int required = entry.getValue();
            int available = Math.max(0, state.devResearchMaterials().getOrDefault(materialId, 0)) + countMaterialFromStations(stations, materialId);
            if (available < required) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean consumeRequiredMaterials(MinecraftServer server, String teamId, List<ResearchCostDefinition.ResearchMaterialRequirement> requirements) {
        Map<String, Integer> requiredByMaterial = foldMaterialRequirements(requirements);
        if (requiredByMaterial.isEmpty()) {
            return true;
        }
        if (!hasRequiredMaterials(server, teamId, requirements)) {
            return false;
        }

        TeamResearchState state = ResearchManager.ensureTeamState(server, teamId);
        List<CrudeResearchStationBlockEntity> stations = CrudeResearchStationRegistry.stationsForTeam(server, teamId);

        for (var entry : requiredByMaterial.entrySet()) {
            String materialId = entry.getKey();
            int remaining = entry.getValue();

            int devCount = Math.max(0, state.devResearchMaterials().getOrDefault(materialId, 0));
            if (devCount > 0) {
                int consumeDev = Math.min(devCount, remaining);
                int nextDev = devCount - consumeDev;
                if (nextDev <= 0) {
                    state.devResearchMaterials().remove(materialId);
                } else {
                    state.devResearchMaterials().put(materialId, nextDev);
                }
                remaining -= consumeDev;
            }

            if (remaining <= 0) {
                continue;
            }

            if (!STARTER_DATA.equalsIgnoreCase(materialId)) {
                return false;
            }

            for (CrudeResearchStationBlockEntity station : stations) {
                if (remaining <= 0) {
                    break;
                }
                remaining -= station.consumeStarterData(remaining);
            }

            if (remaining > 0) {
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean hasPower(MinecraftServer server, String teamId, int amount) {
        return amount <= 0 || availablePower(server, teamId) >= amount;
    }

    @Override
    public boolean consumePower(MinecraftServer server, String teamId, int amount) {
        int required = Math.max(0, amount);
        if (required <= 0) {
            return true;
        }
        if (!hasPower(server, teamId, required)) {
            return false;
        }

        TeamResearchState state = ResearchManager.ensureTeamState(server, teamId);
        int stored = Math.max(0, state.storedResearchPowerBuffer());
        int consumeStored = Math.min(stored, required);
        if (consumeStored > 0) {
            state.setStoredResearchPowerBuffer(stored - consumeStored);
        }

        int remaining = required - consumeStored;
        if (remaining <= 0) {
            return true;
        }

        for (CrudeResearchStationBlockEntity station : CrudeResearchStationRegistry.stationsForTeam(server, teamId)) {
            if (remaining <= 0) {
                break;
            }
            remaining -= station.consumeResearchPower(remaining);
        }

        return remaining <= 0;
    }

    @Override
    public int availablePower(MinecraftServer server, String teamId) {
        TeamResearchState state = ResearchManager.ensureTeamState(server, teamId);
        long total = Math.max(0, state.storedResearchPowerBuffer());
        for (CrudeResearchStationBlockEntity station : CrudeResearchStationRegistry.stationsForTeam(server, teamId)) {
            total += station.researchPowerBuffer();
            if (total >= Integer.MAX_VALUE) {
                return Integer.MAX_VALUE;
            }
        }
        return (int) Math.max(0L, total);
    }

    private static Map<String, Integer> foldModuleRequirements(List<ResearchCostDefinition.LogicModuleRequirement> requirements) {
        Map<String, Integer> folded = new LinkedHashMap<>();
        for (ResearchCostDefinition.LogicModuleRequirement requirement : requirements) {
            if (requirement == null) {
                continue;
            }
            String tier = requirement.moduleTier() == null ? "" : requirement.moduleTier().strip();
            int count = Math.max(0, requirement.count());
            if (tier.isBlank() || count <= 0) {
                continue;
            }
            folded.merge(tier, count, Integer::sum);
        }
        return folded;
    }

    private static Map<String, Integer> foldMaterialRequirements(List<ResearchCostDefinition.ResearchMaterialRequirement> requirements) {
        Map<String, Integer> folded = new LinkedHashMap<>();
        for (ResearchCostDefinition.ResearchMaterialRequirement requirement : requirements) {
            if (requirement == null) {
                continue;
            }
            String materialId = requirement.materialId() == null ? "" : requirement.materialId().strip();
            int count = Math.max(0, requirement.count());
            if (materialId.isBlank() || count <= 0) {
                continue;
            }
            folded.merge(materialId, count, Integer::sum);
        }
        return folded;
    }

    private static int countTierFromStations(List<CrudeResearchStationBlockEntity> stations, String tier) {
        if (!BASIC_TIER.equalsIgnoreCase(tier)) {
            return 0;
        }

        int total = 0;
        for (CrudeResearchStationBlockEntity station : stations) {
            total += station.countBasicLogicModules();
        }
        return total;
    }

    private static int countMaterialFromStations(List<CrudeResearchStationBlockEntity> stations, String materialId) {
        if (!STARTER_DATA.equalsIgnoreCase(materialId)) {
            return 0;
        }

        int total = 0;
        for (CrudeResearchStationBlockEntity station : stations) {
            total += station.countStarterData();
        }
        return total;
    }
}
