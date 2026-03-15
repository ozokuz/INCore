package ozokuz.incore.features.status.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record RequestPlayerStatusCurrencyPayload(boolean request) implements CustomPacketPayload {
    public static final Type<RequestPlayerStatusCurrencyPayload> TYPE =
            new Type<>(ResourceLocation.parse("incore:request_player_status_currency"));
    public static final StreamCodec<ByteBuf, RequestPlayerStatusCurrencyPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL,
            RequestPlayerStatusCurrencyPayload::request,
            RequestPlayerStatusCurrencyPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RequestPlayerStatusCurrencyPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!payload.request() || !(context.player() instanceof ServerPlayer player)) {
                return;
            }
            PlayerStatusNetworking.syncCurrencyToPlayer(player);
        });
    }
}
