package io.github.ozokuz.incore.features.researchv2.model;

import net.minecraft.resources.ResourceLocation;

import java.util.Set;

public record ResearchNetworkDefinition(ResourceLocation id, String name, Set<ResourceLocation> nodeIds) {
}
