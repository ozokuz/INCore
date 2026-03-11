package io.github.ozokuz.incore.features.researchv2.network;

import io.github.ozokuz.incore.INCore;
import io.github.ozokuz.incore.features.researchv2.client.ResearchSampleFabricatorScreen;
import io.github.ozokuz.incore.features.researchv2.client.ResearchV2ClientCache;
import io.github.ozokuz.incore.features.researchv2.client.ResearchV2TreeScreen;
import net.minecraft.client.Minecraft;

public final class ResearchV2ClientPayloadHandlers {
    private ResearchV2ClientPayloadHandlers() {
    }

    public static void handleSnapshot(String json) {
        ResearchV2ClientCache.updateFromJson(json);
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof ResearchV2TreeScreen screen) {
            screen.updateFromCache();
        } else if (minecraft.screen instanceof ResearchSampleFabricatorScreen screen) {
            screen.updateFromCache();
        }
        INCore.LOGGER.info("[ResearchV2] snapshot received and cached.");
    }
}
