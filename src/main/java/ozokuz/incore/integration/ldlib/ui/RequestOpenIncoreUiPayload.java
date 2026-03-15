package ozokuz.incore.integration.ldlib.ui;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record RequestOpenIncoreUiPayload(ResourceLocation routeId) implements CustomPacketPayload {
    public static final Type<RequestOpenIncoreUiPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("incore", "request_open_incore_ui"));
    public static final StreamCodec<ByteBuf, RequestOpenIncoreUiPayload> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC,
            RequestOpenIncoreUiPayload::routeId,
            RequestOpenIncoreUiPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RequestOpenIncoreUiPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player) || !INCoreUiIds.isKnownPlayerUi(payload.routeId())) {
                return;
            }
            if (!INCorePlayerUiNavigator.openRoot(player, payload.routeId())) {
                player.sendSystemMessage(Component.literal("UI route is not available yet: " + payload.routeId()));
            }
        });
    }
}
