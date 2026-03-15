package ozokuz.incore.features.market;

import net.neoforged.neoforge.event.tick.ServerTickEvent;

public final class MarketEvents {
    private MarketEvents() {
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        MarketService.onServerTick(event.getServer());
    }
}
