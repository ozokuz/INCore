package io.github.ozokuz.incore.features.researchv2.provider;

import io.github.ozokuz.incore.features.researchv2.model.ResearchCostDefinition;
import net.minecraft.server.MinecraftServer;

import java.util.List;

public interface ILogicModuleProvider {
    boolean hasRequiredModules(MinecraftServer server, String teamId, List<ResearchCostDefinition.LogicModuleRequirement> requirements);

    boolean consumeRequiredModules(MinecraftServer server, String teamId, List<ResearchCostDefinition.LogicModuleRequirement> requirements);
}
