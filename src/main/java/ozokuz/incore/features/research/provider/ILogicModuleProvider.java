package ozokuz.incore.features.research.provider;

import ozokuz.incore.features.research.model.ResearchCostDefinition;
import net.minecraft.server.MinecraftServer;

import java.util.List;

public interface ILogicModuleProvider {
    boolean hasRequiredModules(MinecraftServer server, String teamId, List<ResearchCostDefinition.LogicModuleRequirement> requirements);

    boolean consumeRequiredModules(MinecraftServer server, String teamId, List<ResearchCostDefinition.LogicModuleRequirement> requirements);
}
