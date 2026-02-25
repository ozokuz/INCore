package io.github.ozokuz.incore.features.party.network;

import io.github.ozokuz.incore.features.party.PartyService;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record PartyActionPayload(ActionType actionType, UUID targetPlayerId) implements CustomPacketPayload {
    public static final Type<PartyActionPayload> TYPE = new Type<>(ResourceLocation.parse("incore:party_action"));
    public static final StreamCodec<ByteBuf, PartyActionPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public PartyActionPayload decode(ByteBuf buffer) {
            FriendlyByteBuf buf = new FriendlyByteBuf(buffer);
            ActionType actionType = ActionType.values()[buf.readVarInt()];
            boolean hasTarget = buf.readBoolean();
            UUID targetPlayerId = hasTarget ? buf.readUUID() : null;
            return new PartyActionPayload(actionType, targetPlayerId);
        }

        @Override
        public void encode(ByteBuf buffer, PartyActionPayload payload) {
            FriendlyByteBuf buf = new FriendlyByteBuf(buffer);
            buf.writeVarInt(payload.actionType().ordinal());
            buf.writeBoolean(payload.targetPlayerId() != null);
            if (payload.targetPlayerId() != null) {
                buf.writeUUID(payload.targetPlayerId());
            }
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PartyActionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            
            MinecraftServer server = player.getServer();
            if (server == null) {
                return;
            }
            
            switch (payload.actionType()) {
                case REQUEST_SYNC -> syncPartyData(server, player);
                case CREATE -> {
                    PartyService.createParty(player);
                    syncPartyData(server, player);
                }
                case INVITE -> {
                    if (payload.targetPlayerId() != null) {
                        ServerPlayer target = server.getPlayerList().getPlayer(payload.targetPlayerId());
                        if (target != null) {
                            PartyService.invite(player, target);
                        }
                    }
                    syncPartyData(server, player);
                }
                case ACCEPT -> {
                    PartyService.acceptInvite(player);
                    syncPartyData(server, player);
                }
                case DECLINE -> {
                    PartyService.declineInvite(player);
                    syncPartyData(server, player);
                }
                case LEAVE -> {
                    PartyService.leaveParty(player);
                    syncPartyData(server, player);
                }
                case KICK -> {
                    if (payload.targetPlayerId() != null) {
                        ServerPlayer target = server.getPlayerList().getPlayer(payload.targetPlayerId());
                        if (target != null) {
                            PartyService.kickMember(player, target);
                        }
                    }
                    syncPartyData(server, player);
                }
                case PROMOTE -> {
                    if (payload.targetPlayerId() != null) {
                        ServerPlayer target = server.getPlayerList().getPlayer(payload.targetPlayerId());
                        if (target != null) {
                            PartyService.promoteLeader(player, target);
                        }
                    }
                    syncPartyData(server, player);
                }
            }
        });
    }

    private static void syncPartyData(MinecraftServer server, ServerPlayer player) {
        var data = io.github.ozokuz.incore.features.party.state.PartySavedData.get(server);
        
        var partyOpt = data.partyForMember(player.getUUID());
        var inviteOpt = data.inviteFor(player.getUUID());
        
        boolean inParty = partyOpt.isPresent();
        long partyId = 0L;
        UUID leaderId = null;
        String leaderName = "";
        List<PartySyncPayload.MemberEntry> members = new ArrayList<>();
        
        if (partyOpt.isPresent()) {
            var party = partyOpt.get();
            partyId = party.id();
            leaderId = party.leaderId();
            leaderName = resolvePlayerName(server, leaderId);
            for (UUID memberId : party.members()) {
                members.add(new PartySyncPayload.MemberEntry(memberId, resolvePlayerName(server, memberId)));
            }
        }
        
        boolean hasPendingInvite = inviteOpt.isPresent();
        long invitePartyId = 0L;
        UUID inviteInviterId = null;
        String inviteInviterName = "";
        
        if (inviteOpt.isPresent()) {
            var invite = inviteOpt.get();
            invitePartyId = invite.partyId();
            inviteInviterId = invite.inviterId();
            inviteInviterName = resolvePlayerName(server, inviteInviterId);
        }
        
        PacketDistributor.sendToPlayer(player, new PartySyncPayload(
                inParty, partyId, leaderId, leaderName, members,
                hasPendingInvite, invitePartyId, inviteInviterId, inviteInviterName
        ));
        
        Set<UUID> excludeFromOnline = new HashSet<>();
        excludeFromOnline.add(player.getUUID());
        if (partyOpt.isPresent()) {
            excludeFromOnline.addAll(partyOpt.get().members());
        }
        
        List<OnlinePlayersSyncPayload.PlayerEntry> onlinePlayers = new ArrayList<>();
        for (ServerPlayer onlinePlayer : server.getPlayerList().getPlayers()) {
            if (!excludeFromOnline.contains(onlinePlayer.getUUID())) {
                if (data.partyForMember(onlinePlayer.getUUID()).isEmpty()) {
                    onlinePlayers.add(new OnlinePlayersSyncPayload.PlayerEntry(
                            onlinePlayer.getUUID(),
                            onlinePlayer.getGameProfile().getName()
                    ));
                }
            }
        }
        
        PacketDistributor.sendToPlayer(player, new OnlinePlayersSyncPayload(onlinePlayers));
    }

    private static String resolvePlayerName(MinecraftServer server, UUID playerId) {
        if (playerId == null) {
            return "";
        }
        ServerPlayer online = server.getPlayerList().getPlayer(playerId);
        if (online != null) {
            return online.getGameProfile().getName();
        }
        return server.getProfileCache().get(playerId)
                .map(com.mojang.authlib.GameProfile::getName)
                .orElse(playerId.toString().substring(0, 8));
    }

    public enum ActionType {
        REQUEST_SYNC,
        CREATE,
        INVITE,
        ACCEPT,
        DECLINE,
        LEAVE,
        KICK,
        PROMOTE
    }
}
