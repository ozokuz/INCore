package io.github.ozokuz.incore.features.researchv2.model;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public record ResearchNodeDefinition(
        ResourceLocation id,
        ResourceLocation treeId,
        ResourceLocation categoryId,
        List<ResourceLocation> prerequisites,
        @Nullable String discoveryRules,
        ResearchCostDefinition researchCost,
        int researchTime,
        List<String> outputs
) {
}
