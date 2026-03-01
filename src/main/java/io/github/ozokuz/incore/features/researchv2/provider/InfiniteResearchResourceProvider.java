package io.github.ozokuz.incore.features.researchv2.provider;

import io.github.ozokuz.incore.features.researchv2.model.ResearchCostDefinition;
import net.minecraft.server.MinecraftServer;

import java.util.List;

public final class InfiniteResearchResourceProvider implements ILogicModuleProvider, IResearchMaterialProvider, IResearchPowerProvider {
    @Override
    public boolean hasRequiredModules(MinecraftServer server, String teamId, List<ResearchCostDefinition.LogicModuleRequirement> requirements) {
        return true;
    }

    @Override
    public boolean consumeRequiredModules(MinecraftServer server, String teamId, List<ResearchCostDefinition.LogicModuleRequirement> requirements) {
        return true;
    }

    @Override
    public boolean hasRequiredMaterials(MinecraftServer server, String teamId, List<ResearchCostDefinition.ResearchMaterialRequirement> requirements) {
        return true;
    }

    @Override
    public boolean consumeRequiredMaterials(MinecraftServer server, String teamId, List<ResearchCostDefinition.ResearchMaterialRequirement> requirements) {
        return true;
    }

    @Override
    public boolean hasPower(MinecraftServer server, String teamId, int amount) {
        return true;
    }

    @Override
    public boolean consumePower(MinecraftServer server, String teamId, int amount) {
        return true;
    }

    @Override
    public int availablePower(MinecraftServer server, String teamId) {
        return Integer.MAX_VALUE;
    }
}
