package ozokuz.incore.features.shop.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.jetbrains.annotations.Nullable;
import ozokuz.incore.features.shop.ShopService;

public final class ShopNetworking {

    private ShopNetworking() {
    }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(ShopPurchasePayload.TYPE, ShopPurchasePayload.STREAM_CODEC, ShopPurchasePayload::handle);
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

    static void handlePurchase(ServerPlayer player, ShopPurchasePayload payload) {
        ResourceLocation offerId = ResourceLocation.tryParse(payload.offerId());
        if (offerId != null) {
            ShopService.purchase(player, offerId, payload.quantity());
        }
    }
}
