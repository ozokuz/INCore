package ozokuz.incore.features.numismatics.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record RequestOpenNumismaticsBankPayload(boolean request) implements CustomPacketPayload {
    public static final Type<RequestOpenNumismaticsBankPayload> TYPE = new Type<>(ResourceLocation.parse("incore:request_open_numismatics_bank"));
    public static final StreamCodec<ByteBuf, RequestOpenNumismaticsBankPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL,
            RequestOpenNumismaticsBankPayload::request,
            RequestOpenNumismaticsBankPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RequestOpenNumismaticsBankPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!payload.request() || !(context.player() instanceof ServerPlayer player)) {
                return;
            }
            NumismaticsNetworking.openBankScreenFor(player);
        });
    }
}
