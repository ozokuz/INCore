package io.github.ozokuz.incore.features.shop.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record RequestOpenShopScreenPayload(boolean request, String selectedCategoryId, String selectedOfferId) implements CustomPacketPayload {
    public static final Type<RequestOpenShopScreenPayload> TYPE = new Type<>(ResourceLocation.parse("incore:request_open_shop_screen"));
    public static final StreamCodec<ByteBuf, RequestOpenShopScreenPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL,
            RequestOpenShopScreenPayload::request,
            ByteBufCodecs.STRING_UTF8,
            RequestOpenShopScreenPayload::selectedCategoryId,
            ByteBufCodecs.STRING_UTF8,
            RequestOpenShopScreenPayload::selectedOfferId,
            RequestOpenShopScreenPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RequestOpenShopScreenPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!payload.request() || !(context.player() instanceof ServerPlayer player)) {
                return;
            }
            ShopNetworking.handleOpenRequest(player, payload);
        });
    }
}
