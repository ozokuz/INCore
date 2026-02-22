package io.github.ozokuz.incore.features.market.network;

import com.google.gson.Gson;
import io.github.ozokuz.incore.features.market.MarketService;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class MarketNetworking {
    private static final Gson GSON = new Gson();

    private MarketNetworking() {
    }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(OpenMarketScreenPayload.TYPE, OpenMarketScreenPayload.STREAM_CODEC, OpenMarketScreenPayload::handle);
        registrar.playToServer(RequestOpenMarketScreenPayload.TYPE, RequestOpenMarketScreenPayload.STREAM_CODEC, RequestOpenMarketScreenPayload::handle);
        registrar.playToServer(MarketActionPayload.TYPE, MarketActionPayload.STREAM_CODEC, MarketActionPayload::handle);
        registrar.playToServer(MarketAutoBuyerConfigPayload.TYPE, MarketAutoBuyerConfigPayload.STREAM_CODEC, MarketAutoBuyerConfigPayload::handle);
    }

    public static void requestOpenMarketScreen() {
        PacketDistributor.sendToServer(new RequestOpenMarketScreenPayload(true));
    }

    public static void sendRefresh(long terminalPos) {
        PacketDistributor.sendToServer(new MarketActionPayload(MarketActionPayload.ACTION_REFRESH, terminalPos, "", 1));
    }

    public static void sendBuy(long terminalPos, ResourceLocation itemId, int quantity) {
        PacketDistributor.sendToServer(new MarketActionPayload(
                MarketActionPayload.ACTION_BUY,
                terminalPos,
                itemId.toString(),
                Math.max(1, quantity)
        ));
    }

    public static void sendSell(long terminalPos, ResourceLocation itemId, int quantity) {
        PacketDistributor.sendToServer(new MarketActionPayload(
                MarketActionPayload.ACTION_SELL,
                terminalPos,
                itemId.toString(),
                Math.max(1, quantity)
        ));
    }

    public static void sendAutoBuyerConfig(long blockPos, String targetItemId, int priceCapSpur, int batchSize, boolean enabled) {
        PacketDistributor.sendToServer(new MarketAutoBuyerConfigPayload(
                blockPos,
                targetItemId == null ? "" : targetItemId,
                Math.max(1, priceCapSpur),
                Math.clamp(batchSize, 1, 64),
                enabled
        ));
    }

    public static void openMarketScreen(ServerPlayer player, MarketService.ScreenData data) {
        PacketDistributor.sendToPlayer(player, new OpenMarketScreenPayload(GSON.toJson(data)));
    }

    static void openReadOnlyScreenFor(ServerPlayer player) {
        MarketService.openReadOnlyScreen(player);
    }

    static void handleAction(ServerPlayer player, MarketActionPayload payload) {
        BlockPos terminalPos = BlockPos.of(payload.terminalPos());
        switch (payload.action()) {
            case MarketActionPayload.ACTION_REFRESH -> MarketService.requestRefresh(player, terminalPos);
            case MarketActionPayload.ACTION_BUY -> {
                ResourceLocation itemId = ResourceLocation.tryParse(payload.itemId());
                if (itemId == null) {
                    return;
                }
                if (MarketService.buyFromMarket(player, terminalPos, itemId, payload.quantity())) {
                    MarketService.requestRefresh(player, terminalPos);
                }
            }
            case MarketActionPayload.ACTION_SELL -> {
                ResourceLocation itemId = ResourceLocation.tryParse(payload.itemId());
                if (itemId == null) {
                    return;
                }
                if (MarketService.sellToMarket(player, terminalPos, itemId, payload.quantity())) {
                    MarketService.requestRefresh(player, terminalPos);
                }
            }
            default -> {
            }
        }
    }
}
