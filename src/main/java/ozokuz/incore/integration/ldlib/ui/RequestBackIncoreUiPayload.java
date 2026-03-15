package ozokuz.incore.integration.ldlib.ui;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record RequestBackIncoreUiPayload() implements CustomPacketPayload {
    public static final RequestBackIncoreUiPayload INSTANCE = new RequestBackIncoreUiPayload();
    public static final Type<RequestBackIncoreUiPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("incore", "request_back_incore_ui"));
    public static final StreamCodec<ByteBuf, RequestBackIncoreUiPayload> STREAM_CODEC =
            StreamCodec.of((buffer, payload) -> {
            }, buffer -> INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RequestBackIncoreUiPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                INCorePlayerUiNavigator.goBack(player);
            }
        });
    }
}
