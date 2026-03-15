package ozokuz.incore.features.party.network;

import ozokuz.incore.features.party.state.PartySavedData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class PartyNetworking {
    private PartyNetworking() {
    }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(PartyHudSyncPayload.TYPE, PartyHudSyncPayload.STREAM_CODEC, PartyHudSyncPayload::handle);
        registrar.playToClient(PartySyncPayload.TYPE, PartySyncPayload.STREAM_CODEC, PartySyncPayload::handle);
        registrar.playToClient(OnlinePlayersSyncPayload.TYPE, OnlinePlayersSyncPayload.STREAM_CODEC, OnlinePlayersSyncPayload::handle);
        registrar.playToServer(PartyActionPayload.TYPE, PartyActionPayload.STREAM_CODEC, PartyActionPayload::handle);
    }

    public static void syncHudToPlayer(ServerPlayer player, List<PartyHudSyncPayload.MemberEntry> rows) {
        PacketDistributor.sendToPlayer(player, new PartyHudSyncPayload(rows));
    }

    public static void syncStateToPlayer(MinecraftServer server, ServerPlayer player) {
        PartySavedData data = PartySavedData.get(server);

        var partyOpt = data.partyForMember(player.getUUID());
        var inviteOpt = data.inviteFor(player.getUUID());

        boolean inParty = partyOpt.isPresent();
        long partyId = 0L;
        UUID leaderId = null;
        String leaderName = "";
        List<PartySyncPayload.MemberEntry> members = new ArrayList<>();
        List<UUID> outgoingInviteTargetIds = data.invitesFrom(player.getUUID()).stream()
                .map(PartySavedData.InviteRecord::targetId)
                .toList();

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
                inParty, partyId, leaderId, leaderName, members, outgoingInviteTargetIds,
                hasPendingInvite, invitePartyId, inviteInviterId, inviteInviterName
        ));

        Set<UUID> excludeFromOnline = new java.util.HashSet<>();
        excludeFromOnline.add(player.getUUID());
        partyOpt.ifPresent(party -> excludeFromOnline.addAll(party.members()));

        List<OnlinePlayersSyncPayload.PlayerEntry> onlinePlayers = new ArrayList<>();
        for (ServerPlayer onlinePlayer : server.getPlayerList().getPlayers()) {
            if (!excludeFromOnline.contains(onlinePlayer.getUUID())
                    && data.partyForMember(onlinePlayer.getUUID()).isEmpty()) {
                onlinePlayers.add(new OnlinePlayersSyncPayload.PlayerEntry(
                        onlinePlayer.getUUID(),
                        onlinePlayer.getGameProfile().getName()
                ));
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
}
