package ozokuz.incore.features.roguelike.network;

import ozokuz.incore.client.features.roguelike.RoguelikeMinimapClientCache;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record RoguelikeMinimapPartyPayload(long instanceId, List<Marker> markers) implements CustomPacketPayload {
    public static final Type<RoguelikeMinimapPartyPayload> TYPE = new Type<>(ResourceLocation.parse("incore:roguelike_minimap_party"));
    public static final StreamCodec<ByteBuf, RoguelikeMinimapPartyPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public RoguelikeMinimapPartyPayload decode(ByteBuf buffer) {
            FriendlyByteBuf buf = new FriendlyByteBuf(buffer);
            long instanceId = buf.readVarLong();
            int count = Math.max(0, buf.readVarInt());
            List<Marker> markers = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                markers.add(new Marker(buf.readUUID(), buf.readVarInt()));
            }
            return new RoguelikeMinimapPartyPayload(instanceId, markers);
        }

        @Override
        public void encode(ByteBuf buffer, RoguelikeMinimapPartyPayload payload) {
            FriendlyByteBuf buf = new FriendlyByteBuf(buffer);
            buf.writeVarLong(payload.instanceId());
            buf.writeVarInt(payload.markers().size());
            for (Marker marker : payload.markers()) {
                buf.writeUUID(marker.playerId());
                buf.writeVarInt(marker.roomId());
            }
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RoguelikeMinimapPartyPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> RoguelikeMinimapClientCache.updatePartyMarkers(
                payload.instanceId(),
                payload.markers().stream().map(marker -> new RoguelikeMinimapClientCache.PartyMarker(marker.playerId(), marker.roomId())).toList()
        ));
    }

    public record Marker(UUID playerId, int roomId) {
    }
}
