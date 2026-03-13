package io.github.ozokuz.incore.features.researchv2.material;

import net.minecraft.resources.ResourceLocation;

public record ResearchMaterialDefinition(
        ResourceLocation id,
        ResourceLocation itemId,
        int color
) {
}
