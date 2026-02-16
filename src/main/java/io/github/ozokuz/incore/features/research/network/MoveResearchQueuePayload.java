package io.github.ozokuz.incore.features.research.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record MoveResearchQueuePayload(int fromIndex, int toIndex) implements CustomPacketPayload {
    public static final Type<MoveResearchQueuePayload> TYPE = new Type<>(ResourceLocation.parse("incore:move_research_queue"));
    public static final StreamCodec<ByteBuf, MoveResearchQueuePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            MoveResearchQueuePayload::fromIndex,
            ByteBufCodecs.VAR_INT,
            MoveResearchQueuePayload::toIndex,
            MoveResearchQueuePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(MoveResearchQueuePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            ResearchNetworking.moveQueue(player, payload.fromIndex(), payload.toIndex());
        });
    }
}
