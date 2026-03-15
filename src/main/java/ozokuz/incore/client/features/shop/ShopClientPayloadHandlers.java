package ozokuz.incore.client.features.shop;

import ozokuz.incore.client.features.status.StatusScreenReturnTracker;
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
        minecraft.setScreen(new ShopSelectionScreen(
                ShopScreenDataUtil.parse(json),
                null,
                0,
                StatusScreenReturnTracker.consumePendingParent()
        ));
    }
}
