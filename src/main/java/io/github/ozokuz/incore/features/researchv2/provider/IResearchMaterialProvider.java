package io.github.ozokuz.incore.features.researchv2.provider;

import io.github.ozokuz.incore.features.researchv2.model.ResearchCostDefinition;
import net.minecraft.server.MinecraftServer;

import java.util.List;

public interface IResearchMaterialProvider {
    boolean hasRequiredMaterials(MinecraftServer server, String teamId, List<ResearchCostDefinition.ResearchMaterialRequirement> requirements);

    boolean consumeRequiredMaterials(MinecraftServer server, String teamId, List<ResearchCostDefinition.ResearchMaterialRequirement> requirements);
}
