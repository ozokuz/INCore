package ozokuz.incore.features.research.material;

import dev.latvian.mods.kubejs.script.ScriptType;
import ozokuz.incore.INCore;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;

public final class ResearchMaterialKubeJsBridge {
    private static volatile Map<ResourceLocation, ResearchMaterialDefinition> cache = Map.of();

    private ResearchMaterialKubeJsBridge() {
    }

    public static Map<ResourceLocation, ResearchMaterialDefinition> collect() {
        try {
            ResearchMaterialKubeEvent event = new ResearchMaterialKubeEvent();
            ResearchKubeJsEvents.RESEARCH_MATERIALS.post(ScriptType.STARTUP, event);
            cache = Map.copyOf(event.additions());
        } catch (Throwable t) {
            INCore.LOGGER.warn("Failed to collect KubeJS research materials: {}", t.getMessage());
            cache = Map.of();
        }
        return cache;
    }
}
