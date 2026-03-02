package io.github.ozokuz.incore.features.roguelike.network;

import io.github.ozokuz.incore.client.features.roguelike.RoguelikeMinimapClientCache;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record RoguelikeMinimapRevealPayload(long instanceId, int roomId) implements CustomPacketPayload {
    public static final Type<RoguelikeMinimapRevealPayload> TYPE = new Type<>(ResourceLocation.parse("incore:roguelike_minimap_reveal"));
    public static final StreamCodec<ByteBuf, RoguelikeMinimapRevealPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public RoguelikeMinimapRevealPayload decode(ByteBuf buffer) {
            FriendlyByteBuf buf = new FriendlyByteBuf(buffer);
            return new RoguelikeMinimapRevealPayload(buf.readVarLong(), buf.readVarInt());
        }

        @Override
        public void encode(ByteBuf buffer, RoguelikeMinimapRevealPayload payload) {
            FriendlyByteBuf buf = new FriendlyByteBuf(buffer);
            buf.writeVarLong(payload.instanceId());
            buf.writeVarInt(payload.roomId());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RoguelikeMinimapRevealPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> RoguelikeMinimapClientCache.revealRoom(payload.instanceId(), payload.roomId()));
    }
}
