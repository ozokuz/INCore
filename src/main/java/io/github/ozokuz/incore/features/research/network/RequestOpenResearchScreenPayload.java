package io.github.ozokuz.incore.features.research.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record RequestOpenResearchScreenPayload(boolean request) implements CustomPacketPayload {
    public static final Type<RequestOpenResearchScreenPayload> TYPE = new Type<>(ResourceLocation.parse("incore:request_open_research_screen"));
    public static final StreamCodec<ByteBuf, RequestOpenResearchScreenPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL,
            RequestOpenResearchScreenPayload::request,
            RequestOpenResearchScreenPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RequestOpenResearchScreenPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!payload.request() || !(context.player() instanceof ServerPlayer player)) {
                return;
            }
            ResearchNetworking.openFor(player);
        });
    }
}
