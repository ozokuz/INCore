package ozokuz.incore.client.features.market;

import ozokuz.incore.client.features.status.StatusScreenReturnTracker;
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
        minecraft.setScreen(new MarketSelectionScreen(
                MarketScreenDataUtil.parse(json),
                0,
                null,
                StatusScreenReturnTracker.consumePendingParent()
        ));
    }

    public static void syncMarketSnapshot(String json) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof MarketPayloadUpdatable updatable) {
            updatable.updatePayload(json);
        }
    }
}
