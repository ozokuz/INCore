package io.github.ozokuz.incore.features.market.client;

import net.minecraft.client.Minecraft;

public final class MarketClientPayloadHandlers {
    private MarketClientPayloadHandlers() {
    }

    public static void openMarketScreen(String json) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof MarketPayloadUpdatable updatable) {
            updatable.updatePayload(json);
            return;
        }
        minecraft.setScreen(new MarketSelectionScreen(json));
    }
}
