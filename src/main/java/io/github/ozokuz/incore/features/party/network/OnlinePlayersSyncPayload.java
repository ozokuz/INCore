package io.github.ozokuz.incore.features.party.network;

import io.github.ozokuz.incore.client.features.party.PartyClientCache;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record OnlinePlayersSyncPayload(List<PlayerEntry> players) implements CustomPacketPayload {
    public static final Type<OnlinePlayersSyncPayload> TYPE = new Type<>(ResourceLocation.parse("incore:online_players_sync"));
    public static final StreamCodec<ByteBuf, OnlinePlayersSyncPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public OnlinePlayersSyncPayload decode(ByteBuf buffer) {
            FriendlyByteBuf buf = new FriendlyByteBuf(buffer);
            int count = buf.readVarInt();
            List<PlayerEntry> players = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                players.add(new PlayerEntry(buf.readUUID(), buf.readUtf(64)));
            }
            return new OnlinePlayersSyncPayload(players);
        }

        @Override
        public void encode(ByteBuf buffer, OnlinePlayersSyncPayload payload) {
            FriendlyByteBuf buf = new FriendlyByteBuf(buffer);
            buf.writeVarInt(payload.players().size());
            for (PlayerEntry player : payload.players()) {
                buf.writeUUID(player.playerId());
                buf.writeUtf(player.playerName(), 64);
            }
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OnlinePlayersSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> PartyClientCache.updateOnlinePlayers(payload.players()));
    }

    public record PlayerEntry(UUID playerId, String playerName) {
    }
}
