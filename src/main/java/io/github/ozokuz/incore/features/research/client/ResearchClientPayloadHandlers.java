package io.github.ozokuz.incore.features.research.client;

import net.minecraft.client.Minecraft;

public final class ResearchClientPayloadHandlers {
    private ResearchClientPayloadHandlers() {
    }

    public static void openResearchScreen(String json) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof ResearchTechTreeScreen screen) {
            screen.updatePayload(json);
            return;
        }
        minecraft.setScreen(new ResearchTechTreeScreen(json));
    }
}
