package io.github.ozokuz.incore.features.research.network;

import io.github.ozokuz.incore.INCore;
import io.github.ozokuz.incore.features.research.client.ResearchSampleFabricatorScreen;
import io.github.ozokuz.incore.features.research.client.ResearchClientCache;
import io.github.ozokuz.incore.features.research.client.ResearchTreeScreen;
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
