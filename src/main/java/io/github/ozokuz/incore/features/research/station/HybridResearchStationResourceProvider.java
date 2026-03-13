package io.github.ozokuz.incore.features.research.station;

import io.github.ozokuz.incore.features.research.model.ResearchCostDefinition;
import io.github.ozokuz.incore.features.research.provider.ILogicModuleProvider;
import io.github.ozokuz.incore.features.research.provider.IResearchMaterialProvider;
import io.github.ozokuz.incore.features.research.provider.IResearchPowerProvider;
import io.github.ozokuz.incore.features.research.station.network.StationNetworkService;
import net.minecraft.server.MinecraftServer;

import java.util.List;

public final class HybridResearchStationResourceProvider implements ILogicModuleProvider, IResearchMaterialProvider, IResearchPowerProvider {
    private final CrudeResearchStationResourceProvider crudeProvider = new CrudeResearchStationResourceProvider();

    @Override
    public boolean hasRequiredModules(MinecraftServer server, String teamId, List<ResearchCostDefinition.LogicModuleRequirement> requirements) {
        return crudeProvider.hasRequiredModules(server, teamId, requirements);
    }

    @Override
    public boolean consumeRequiredModules(MinecraftServer server, String teamId, List<ResearchCostDefinition.LogicModuleRequirement> requirements) {
        return crudeProvider.consumeRequiredModules(server, teamId, requirements);
    }

    @Override
    public boolean hasRequiredMaterials(MinecraftServer server, String teamId, List<ResearchCostDefinition.ResearchMaterialRequirement> requirements) {
        return crudeProvider.hasRequiredMaterials(server, teamId, requirements);
    }

    @Override
    public boolean consumeRequiredMaterials(MinecraftServer server, String teamId, List<ResearchCostDefinition.ResearchMaterialRequirement> requirements) {
        return crudeProvider.consumeRequiredMaterials(server, teamId, requirements);
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
        if (server == null || teamId == null || teamId.isBlank()) {
            return false;
        }

        int remaining = required;
        for (ResearchControllerBlockEntity controller : StationNetworkService.executableControllers(server, teamId)) {
            if (remaining <= 0) {
                break;
            }
            remaining -= controller.consumeResearchPower(remaining);
        }

        if (remaining <= 0) {
            return true;
        }
        return crudeProvider.consumePower(server, teamId, remaining);
    }

    @Override
    public int availablePower(MinecraftServer server, String teamId) {
        if (server == null || teamId == null || teamId.isBlank()) {
            return 0;
        }

        long total = crudeProvider.availablePower(server, teamId);
        for (ResearchControllerBlockEntity controller : StationNetworkService.executableControllers(server, teamId)) {
            total += controller.availableResearchPower(Integer.MAX_VALUE);
            if (total >= Integer.MAX_VALUE) {
                return Integer.MAX_VALUE;
            }
        }
        return (int) Math.max(0L, total);
    }
}
