package io.github.ozokuz.incore.features.arena.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record RequestOpenArenaCatalogPayload(boolean request) implements CustomPacketPayload {
    public static final Type<RequestOpenArenaCatalogPayload> TYPE = new Type<>(ResourceLocation.parse("incore:request_open_arena_catalog"));
    public static final StreamCodec<ByteBuf, RequestOpenArenaCatalogPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL,
            RequestOpenArenaCatalogPayload::request,
            RequestOpenArenaCatalogPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RequestOpenArenaCatalogPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!payload.request() || !(context.player() instanceof ServerPlayer player)) {
                return;
            }

            ArenaNetworking.openCatalogFor(player);
        });
    }
}
