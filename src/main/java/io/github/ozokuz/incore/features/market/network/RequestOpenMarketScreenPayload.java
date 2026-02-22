package io.github.ozokuz.incore.features.market.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record RequestOpenMarketScreenPayload(boolean request) implements CustomPacketPayload {
    public static final Type<RequestOpenMarketScreenPayload> TYPE = new Type<>(ResourceLocation.parse("incore:request_open_market_screen"));
    public static final StreamCodec<ByteBuf, RequestOpenMarketScreenPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL,
            RequestOpenMarketScreenPayload::request,
            RequestOpenMarketScreenPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RequestOpenMarketScreenPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!payload.request() || !(context.player() instanceof ServerPlayer player)) {
                return;
            }
            MarketNetworking.openReadOnlyScreenFor(player);
        });
    }
}
