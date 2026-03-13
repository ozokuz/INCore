package io.github.ozokuz.incore.features.research.provider;

import io.github.ozokuz.incore.features.research.model.ResearchCostDefinition;
import net.minecraft.server.MinecraftServer;

import java.util.List;

public interface IResearchMaterialProvider {
    boolean hasRequiredMaterials(MinecraftServer server, String teamId, List<ResearchCostDefinition.ResearchMaterialRequirement> requirements);

    boolean consumeRequiredMaterials(MinecraftServer server, String teamId, List<ResearchCostDefinition.ResearchMaterialRequirement> requirements);
}
