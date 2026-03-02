package io.github.ozokuz.incore.features.roguelike.network;

import io.github.ozokuz.incore.client.features.roguelike.RoguelikeMinimapClientCache;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record RoguelikeMinimapGraphPayload(long instanceId, int originChunkX, int originChunkZ) implements CustomPacketPayload {
    public static final Type<RoguelikeMinimapGraphPayload> TYPE = new Type<>(ResourceLocation.parse("incore:roguelike_minimap_graph"));
    public static final StreamCodec<ByteBuf, RoguelikeMinimapGraphPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public RoguelikeMinimapGraphPayload decode(ByteBuf buffer) {
            FriendlyByteBuf buf = new FriendlyByteBuf(buffer);
            return new RoguelikeMinimapGraphPayload(buf.readVarLong(), buf.readVarInt(), buf.readVarInt());
        }

        @Override
        public void encode(ByteBuf buffer, RoguelikeMinimapGraphPayload payload) {
            FriendlyByteBuf buf = new FriendlyByteBuf(buffer);
            buf.writeVarLong(payload.instanceId());
            buf.writeVarInt(payload.originChunkX());
            buf.writeVarInt(payload.originChunkZ());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RoguelikeMinimapGraphPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> RoguelikeMinimapClientCache.setGraph(payload.instanceId(), payload.originChunkX(), payload.originChunkZ()));
    }
}
