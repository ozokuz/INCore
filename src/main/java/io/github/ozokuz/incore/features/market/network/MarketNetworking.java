package io.github.ozokuz.incore.features.market.network;

import com.google.gson.Gson;
import io.github.ozokuz.incore.features.market.MarketService;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.jetbrains.annotations.Nullable;

public final class MarketNetworking {
    private static final Gson GSON = new Gson();

    private MarketNetworking() {
    }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(OpenMarketScreenPayload.TYPE, OpenMarketScreenPayload.STREAM_CODEC, OpenMarketScreenPayload::handle);
        registrar.playToClient(MarketSnapshotSyncPayload.TYPE, MarketSnapshotSyncPayload.STREAM_CODEC, MarketSnapshotSyncPayload::handle);
        registrar.playToServer(RequestOpenMarketScreenPayload.TYPE, RequestOpenMarketScreenPayload.STREAM_CODEC, RequestOpenMarketScreenPayload::handle);
        registrar.playToServer(MarketActionPayload.TYPE, MarketActionPayload.STREAM_CODEC, MarketActionPayload::handle);
        registrar.playToServer(MarketAutoBuyerConfigPayload.TYPE, MarketAutoBuyerConfigPayload.STREAM_CODEC, MarketAutoBuyerConfigPayload::handle);
        registrar.playToServer(MarketViewSubscriptionPayload.TYPE, MarketViewSubscriptionPayload.STREAM_CODEC, MarketViewSubscriptionPayload::handle);
    }

    public static void requestOpenMarketScreen() {
        requestOpenMarketScreen(null);
    }

    public static void requestOpenMarketScreen(@Nullable ResourceLocation detailItemId) {
        PacketDistributor.sendToServer(new RequestOpenMarketScreenPayload(
                true,
                detailItemId == null ? "" : detailItemId.toString()
        ));
    }

    public static void sendRefresh(long terminalPos) {
        sendRefresh(terminalPos, null);
    }

    public static void sendRefresh(long terminalPos, @Nullable ResourceLocation detailItemId) {
        PacketDistributor.sendToServer(new MarketActionPayload(
                MarketActionPayload.ACTION_REFRESH,
                terminalPos,
                detailItemId == null ? "" : detailItemId.toString(),
                1
        ));
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

    public static void sendAutoBuyerConfig(long blockPos, String targetItemId, int priceCapSpur, int batchSize) {
        PacketDistributor.sendToServer(new MarketAutoBuyerConfigPayload(
                blockPos,
                targetItemId == null ? "" : targetItemId,
                Math.max(1, priceCapSpur),
                Math.clamp(batchSize, 1, 64)
        ));
    }

    public static void openMarketScreen(ServerPlayer player, MarketService.ScreenData data) {
        PacketDistributor.sendToPlayer(player, new OpenMarketScreenPayload(GSON.toJson(data)));
    }

    public static void syncMarketSnapshot(ServerPlayer player, MarketService.ScreenData data) {
        PacketDistributor.sendToPlayer(player, new MarketSnapshotSyncPayload(GSON.toJson(data)));
    }

    public static void subscribeMarketView(boolean subscribed, @Nullable Long terminalPos, @Nullable ResourceLocation detailItemId) {
        PacketDistributor.sendToServer(new MarketViewSubscriptionPayload(
                subscribed,
                terminalPos != null,
                terminalPos == null ? 0L : terminalPos,
                detailItemId == null ? "" : detailItemId.toString()
        ));
    }

    static void openReadOnlyScreenFor(ServerPlayer player) {
        openReadOnlyScreenFor(player, null);
    }

    static void openReadOnlyScreenFor(ServerPlayer player, @Nullable ResourceLocation detailItemId) {
        MarketService.openReadOnlyScreen(player, detailItemId);
    }

    static void handleAction(ServerPlayer player, MarketActionPayload payload) {
        BlockPos terminalPos = BlockPos.of(payload.terminalPos());
        switch (payload.action()) {
            case MarketActionPayload.ACTION_REFRESH -> {
                ResourceLocation detailItemId = parseOptionalItemId(payload.itemId());
                MarketService.requestRefresh(player, terminalPos, detailItemId);
            }
            case MarketActionPayload.ACTION_BUY -> {
                ResourceLocation itemId = ResourceLocation.tryParse(payload.itemId());
                if (itemId == null) {
                    return;
                }
                MarketService.buyFromMarket(player, terminalPos, itemId, payload.quantity());
            }
            case MarketActionPayload.ACTION_SELL -> {
                ResourceLocation itemId = ResourceLocation.tryParse(payload.itemId());
                if (itemId == null) {
                    return;
                }
                MarketService.sellToMarket(player, terminalPos, itemId, payload.quantity());
            }
            default -> {
            }
        }
    }

    private static @Nullable ResourceLocation parseOptionalItemId(@Nullable String rawItemId) {
        if (rawItemId == null || rawItemId.isBlank()) {
            return null;
        }
        return ResourceLocation.tryParse(rawItemId);
    }
}
