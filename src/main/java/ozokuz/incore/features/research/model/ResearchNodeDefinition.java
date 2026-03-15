package ozokuz.incore.features.research.model;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public record ResearchNodeDefinition(
        ResourceLocation id,
        String name,
        ResourceLocation treeId,
        ResourceLocation categoryId,
        List<ResourceLocation> prerequisites,
        @Nullable String discoveryRules,
        ResearchCostDefinition researchCost,
        ResearchPowerDefinition researchPower,
        int researchTime,
        int requiredRuns,
        List<String> outputs
) {
}
