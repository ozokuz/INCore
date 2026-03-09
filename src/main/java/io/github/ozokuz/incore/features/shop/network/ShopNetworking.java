package io.github.ozokuz.incore.features.shop.network;

import com.google.gson.Gson;
import io.github.ozokuz.incore.features.playerlevel.PlayerFeatureUnlockIds;
import io.github.ozokuz.incore.features.playerlevel.PlayerFeatureUnlockService;
import io.github.ozokuz.incore.features.shop.ShopService;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.jetbrains.annotations.Nullable;

public final class ShopNetworking {
    private static final Gson GSON = new Gson();

    private ShopNetworking() {
    }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(OpenShopScreenPayload.TYPE, OpenShopScreenPayload.STREAM_CODEC, OpenShopScreenPayload::handle);
        registrar.playToServer(RequestOpenShopScreenPayload.TYPE, RequestOpenShopScreenPayload.STREAM_CODEC, RequestOpenShopScreenPayload::handle);
        registrar.playToServer(ShopPurchasePayload.TYPE, ShopPurchasePayload.STREAM_CODEC, ShopPurchasePayload::handle);
    }

    public static void requestOpenShopScreen() {
        requestOpenShopScreen(null, null);
    }

    public static void requestOpenShopScreen(
            @Nullable ResourceLocation selectedCategoryId,
            @Nullable ResourceLocation selectedOfferId
    ) {
        PacketDistributor.sendToServer(new RequestOpenShopScreenPayload(
                true,
                selectedCategoryId == null ? "" : selectedCategoryId.toString(),
                selectedOfferId == null ? "" : selectedOfferId.toString()
        ));
    }

    public static void sendPurchase(
            ResourceLocation offerId,
            int quantity,
            @Nullable ResourceLocation selectedCategoryId
    ) {
        PacketDistributor.sendToServer(new ShopPurchasePayload(
                offerId.toString(),
                Math.clamp(quantity, 1, 64),
                selectedCategoryId == null ? "" : selectedCategoryId.toString()
        ));
    }

    public static void openShopScreen(ServerPlayer player, ShopService.ScreenData data) {
        PacketDistributor.sendToPlayer(player, new OpenShopScreenPayload(GSON.toJson(data)));
    }

    static void handleOpenRequest(ServerPlayer player, RequestOpenShopScreenPayload payload) {
        if (!PlayerFeatureUnlockService.hasUnlocked(player, PlayerFeatureUnlockIds.SHOP_SCREEN)) {
            player.sendSystemMessage(PlayerFeatureUnlockService.lockedMessage(PlayerFeatureUnlockIds.SHOP_SCREEN));
            return;
        }
        ShopService.openShopScreen(
                player,
                parseOptional(payload.selectedCategoryId()),
                parseOptional(payload.selectedOfferId())
        );
    }

    static void handlePurchase(ServerPlayer player, ShopPurchasePayload payload) {
        ResourceLocation offerId = ResourceLocation.tryParse(payload.offerId());
        ResourceLocation selectedCategoryId = parseOptional(payload.selectedCategoryId());

        if (offerId != null) {
            ShopService.purchase(player, offerId, payload.quantity());
            ShopService.openShopScreen(player, selectedCategoryId, offerId);
            return;
        }

        ShopService.openShopScreen(player, selectedCategoryId, null);
    }

    private static @Nullable ResourceLocation parseOptional(@Nullable String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return ResourceLocation.tryParse(raw);
    }
}
