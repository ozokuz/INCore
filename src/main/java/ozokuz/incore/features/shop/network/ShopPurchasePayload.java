package ozokuz.incore.features.shop.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ShopPurchasePayload(String offerId, int quantity, String selectedCategoryId) implements CustomPacketPayload {
    public static final Type<ShopPurchasePayload> TYPE = new Type<>(ResourceLocation.parse("incore:shop_purchase"));
    public static final StreamCodec<ByteBuf, ShopPurchasePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            ShopPurchasePayload::offerId,
            ByteBufCodecs.VAR_INT,
            ShopPurchasePayload::quantity,
            ByteBufCodecs.STRING_UTF8,
            ShopPurchasePayload::selectedCategoryId,
            ShopPurchasePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ShopPurchasePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            ShopNetworking.handlePurchase(player, payload);
        });
    }
}
