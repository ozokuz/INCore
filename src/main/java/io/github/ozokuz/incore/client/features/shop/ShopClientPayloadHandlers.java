package io.github.ozokuz.incore.client.features.shop;

import net.minecraft.client.Minecraft;

public final class ShopClientPayloadHandlers {
    private ShopClientPayloadHandlers() {
    }

    public static void openShopScreen(String json) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof ShopPayloadUpdatable updatable) {
            updatable.updatePayload(json);
            return;
        }
        minecraft.setScreen(new ShopSelectionScreen(json));
    }
}
