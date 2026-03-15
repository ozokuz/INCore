package ozokuz.incore.features.party.network;

import ozokuz.incore.client.features.party.PartyClientCache;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record PartySyncPayload(
        boolean inParty,
        long partyId,
        UUID leaderId,
        String leaderName,
        List<MemberEntry> members,
        List<UUID> outgoingInviteTargetIds,
        boolean hasPendingInvite,
        long invitePartyId,
        UUID inviteInviterId,
        String inviteInviterName
) implements CustomPacketPayload {
    public static final Type<PartySyncPayload> TYPE = new Type<>(ResourceLocation.parse("incore:party_sync"));
    public static final StreamCodec<ByteBuf, PartySyncPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public PartySyncPayload decode(ByteBuf buffer) {
            FriendlyByteBuf buf = new FriendlyByteBuf(buffer);
            boolean inParty = buf.readBoolean();
            long partyId = buf.readLong();
            UUID leaderId = buf.readBoolean() ? buf.readUUID() : null;
            String leaderName = buf.readUtf(64);
            
            int memberCount = buf.readVarInt();
            List<MemberEntry> members = new ArrayList<>(memberCount);
            for (int i = 0; i < memberCount; i++) {
                members.add(new MemberEntry(buf.readUUID(), buf.readUtf(64)));
            }

            int outgoingInviteCount = buf.readVarInt();
            List<UUID> outgoingInviteTargetIds = new ArrayList<>(outgoingInviteCount);
            for (int i = 0; i < outgoingInviteCount; i++) {
                outgoingInviteTargetIds.add(buf.readUUID());
            }
            
            boolean hasPendingInvite = buf.readBoolean();
            long invitePartyId = buf.readLong();
            UUID inviteInviterId = buf.readBoolean() ? buf.readUUID() : null;
            String inviteInviterName = buf.readUtf(64);
            
            return new PartySyncPayload(
                    inParty,
                    partyId,
                    leaderId,
                    leaderName,
                    members,
                    outgoingInviteTargetIds,
                    hasPendingInvite,
                    invitePartyId,
                    inviteInviterId,
                    inviteInviterName
            );
        }

        @Override
        public void encode(ByteBuf buffer, PartySyncPayload payload) {
            FriendlyByteBuf buf = new FriendlyByteBuf(buffer);
            buf.writeBoolean(payload.inParty());
            buf.writeLong(payload.partyId());
            buf.writeBoolean(payload.leaderId() != null);
            if (payload.leaderId() != null) {
                buf.writeUUID(payload.leaderId());
            }
            buf.writeUtf(payload.leaderName(), 64);
            
            buf.writeVarInt(payload.members().size());
            for (MemberEntry member : payload.members()) {
                buf.writeUUID(member.playerId());
                buf.writeUtf(member.playerName(), 64);
            }

            buf.writeVarInt(payload.outgoingInviteTargetIds().size());
            for (UUID outgoingInviteTargetId : payload.outgoingInviteTargetIds()) {
                buf.writeUUID(outgoingInviteTargetId);
            }
            
            buf.writeBoolean(payload.hasPendingInvite());
            buf.writeLong(payload.invitePartyId());
            buf.writeBoolean(payload.inviteInviterId() != null);
            if (payload.inviteInviterId() != null) {
                buf.writeUUID(payload.inviteInviterId());
            }
            buf.writeUtf(payload.inviteInviterName(), 64);
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PartySyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> PartyClientCache.updateParty(payload));
    }

    public record MemberEntry(UUID playerId, String playerName) {
    }
}
