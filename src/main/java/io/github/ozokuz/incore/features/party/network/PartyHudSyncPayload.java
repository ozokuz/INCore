package io.github.ozokuz.incore.features.party.network;

import io.github.ozokuz.incore.client.features.party.PartyHudClientCache;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record PartyHudSyncPayload(List<MemberEntry> members) implements CustomPacketPayload {
    public static final Type<PartyHudSyncPayload> TYPE = new Type<>(ResourceLocation.parse("incore:party_hud_sync"));
    public static final StreamCodec<ByteBuf, PartyHudSyncPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public PartyHudSyncPayload decode(ByteBuf buffer) {
            FriendlyByteBuf buf = new FriendlyByteBuf(buffer);
            int count = Math.max(0, buf.readVarInt());
            List<MemberEntry> rows = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                UUID memberId = buf.readUUID();
                String name = buf.readUtf(64);
                float health = buf.readFloat();
                float maxHealth = buf.readFloat();
                rows.add(new MemberEntry(memberId, name, health, maxHealth));
            }
            return new PartyHudSyncPayload(rows);
        }

        @Override
        public void encode(ByteBuf buffer, PartyHudSyncPayload payload) {
            FriendlyByteBuf buf = new FriendlyByteBuf(buffer);
            buf.writeVarInt(payload.members().size());
            for (MemberEntry row : payload.members()) {
                buf.writeUUID(row.memberId());
                buf.writeUtf(row.name(), 64);
                buf.writeFloat(row.health());
                buf.writeFloat(row.maxHealth());
            }
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PartyHudSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> PartyHudClientCache.update(payload.members().stream()
                .map(row -> new PartyHudClientCache.MemberView(
                        row.memberId(),
                        row.name(),
                        row.health(),
                        row.maxHealth()
                ))
                .toList()));
    }

    public record MemberEntry(UUID memberId, String name, float health, float maxHealth) {
    }
}
