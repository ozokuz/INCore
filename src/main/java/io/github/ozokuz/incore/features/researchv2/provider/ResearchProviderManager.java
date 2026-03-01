package io.github.ozokuz.incore.features.researchv2.provider;

import io.github.ozokuz.incore.features.researchv2.model.ResearchCostDefinition;
import net.minecraft.server.MinecraftServer;

public final class ResearchProviderManager {
    private static final DevResearchResourceProvider DEV_PROVIDER = new DevResearchResourceProvider();
    private static final InfiniteResearchResourceProvider INFINITE_PROVIDER = new InfiniteResearchResourceProvider();

    private static volatile ILogicModuleProvider logicModuleProvider = DEV_PROVIDER;
    private static volatile IResearchMaterialProvider researchMaterialProvider = DEV_PROVIDER;
    private static volatile IResearchPowerProvider researchPowerProvider = DEV_PROVIDER;

    private ResearchProviderManager() {
    }

    public static void enableInfiniteProviders(boolean enabled) {
        if (enabled) {
            logicModuleProvider = INFINITE_PROVIDER;
            researchMaterialProvider = INFINITE_PROVIDER;
            researchPowerProvider = INFINITE_PROVIDER;
            return;
        }
        logicModuleProvider = DEV_PROVIDER;
        researchMaterialProvider = DEV_PROVIDER;
        researchPowerProvider = DEV_PROVIDER;
    }

    public static void setLogicModuleProvider(ILogicModuleProvider provider) {
        logicModuleProvider = provider == null ? DEV_PROVIDER : provider;
    }

    public static void setResearchMaterialProvider(IResearchMaterialProvider provider) {
        researchMaterialProvider = provider == null ? DEV_PROVIDER : provider;
    }

    public static void setResearchPowerProvider(IResearchPowerProvider provider) {
        researchPowerProvider = provider == null ? DEV_PROVIDER : provider;
    }

    public static DevResearchResourceProvider devProvider() {
        return DEV_PROVIDER;
    }

    public static boolean hasRequiredModules(MinecraftServer server, String teamId, ResearchCostDefinition cost) {
        return logicModuleProvider.hasRequiredModules(server, teamId, cost.requiredLogicModules());
    }

    public static boolean consumeRequiredModules(MinecraftServer server, String teamId, ResearchCostDefinition cost) {
        return logicModuleProvider.consumeRequiredModules(server, teamId, cost.requiredLogicModules());
    }

    public static boolean hasRequiredMaterials(MinecraftServer server, String teamId, ResearchCostDefinition cost) {
        return researchMaterialProvider.hasRequiredMaterials(server, teamId, cost.requiredResearchMaterials());
    }

    public static boolean consumeRequiredMaterials(MinecraftServer server, String teamId, ResearchCostDefinition cost) {
        return researchMaterialProvider.consumeRequiredMaterials(server, teamId, cost.requiredResearchMaterials());
    }

    public static boolean consumePower(MinecraftServer server, String teamId, int amount) {
        return researchPowerProvider.consumePower(server, teamId, amount);
    }

    public static int availablePower(MinecraftServer server, String teamId) {
        return researchPowerProvider.availablePower(server, teamId);
    }
}
