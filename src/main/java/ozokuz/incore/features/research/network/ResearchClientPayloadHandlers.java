package ozokuz.incore.features.research.network;

import ozokuz.incore.INCore;
import ozokuz.incore.client.features.research.ResearchSampleFabricatorScreen;
import ozokuz.incore.client.features.research.ResearchClientCache;
import ozokuz.incore.client.features.research.ResearchTreeScreen;
import net.minecraft.client.Minecraft;

public final class ResearchClientPayloadHandlers {
    private ResearchClientPayloadHandlers() {
    }

    public static void handleSnapshot(String json) {
        ResearchClientCache.updateFromJson(json);
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof ResearchTreeScreen screen) {
            screen.updateFromCache();
        } else if (minecraft.screen instanceof ResearchSampleFabricatorScreen screen) {
            screen.updateFromCache();
        }
        INCore.LOGGER.info("[Research] snapshot received and cached.");
    }
}
