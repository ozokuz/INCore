package io.github.ozokuz.incore.features.research;

import net.minecraft.resources.ResourceLocation;

public record ResearchMaterialDefinition(
        ResourceLocation id,
        ResourceLocation itemId,
        int color
) {
}

