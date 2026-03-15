package ozokuz.incore.features.party;

import ozokuz.incore.features.party.network.PartyHudSyncPayload;
import ozokuz.incore.features.party.network.PartyNetworking;
import ozokuz.incore.features.party.state.PartySavedData;
import ozokuz.incore.features.roguelike.RoguelikeConstants;
import ozokuz.incore.features.roguelike.instance.DungeonInstanceData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class PartyService {
    private static final int HUD_SYNC_INTERVAL_TICKS = 5;
    private static final int HUD_FORCE_RESEND_TICKS = 20 * 3;
    private static final Map<UUID, HudSyncState> LAST_HUD_SYNC = new HashMap<>();

    private PartyService() {
    }

    public static long getPartyIdForPlayer(MinecraftServer server, UUID playerId) {
        return PartySavedData.get(server).partyIdForMember(playerId).orElse(0L);
    }

    public static Optional<PartySavedData.PartyRecord> getPartyForPlayer(MinecraftServer server, UUID playerId) {
        return PartySavedData.get(server).partyForMember(playerId);
    }

    public static boolean canEnterDungeonInstance(MinecraftServer server, DungeonInstanceData instance, UUID playerId) {
        if (instance.partyId() > 0L) {
            PartySavedData.PartyRecord party = PartySavedData.get(server).getParty(instance.partyId()).orElse(null);
            return party != null && party.members().contains(playerId);
        }

        if (instance.ownerPlayerId() != null) {
            return instance.ownerPlayerId().equals(playerId);
        }

        // Backward-compatibility: legacy instances created before owner/party binding stay open.
        return true;
    }

    public static boolean createParty(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return false;
        }

        PartySavedData data = PartySavedData.get(server);
        if (data.partyForMember(player.getUUID()).isPresent()) {
            player.sendSystemMessage(Component.translatable("incore.party.error.already_in_party"));
            syncPartyState(server, player);
            return false;
        }

        long partyId = data.nextPartyId();
        PartySavedData.PartyRecord party = new PartySavedData.PartyRecord(partyId, player.getUUID(), List.of(player.getUUID()));
        data.putParty(party);
        data.clearInvite(player.getUUID());

        player.sendSystemMessage(Component.translatable("incore.party.created", partyId).withStyle(ChatFormatting.GREEN));
        syncPartyState(server, player);
        return true;
    }

    public static boolean invite(ServerPlayer inviter, ServerPlayer target) {
        MinecraftServer server = inviter.getServer();
        if (server == null) {
            return false;
        }

        if (inviter.getUUID().equals(target.getUUID())) {
            inviter.sendSystemMessage(Component.translatable("incore.party.error.invite_self"));
            syncPartyState(server, inviter);
            return false;
        }

        PartySavedData data = PartySavedData.get(server);
        PartySavedData.PartyRecord party = data.partyForMember(inviter.getUUID()).orElse(null);
        if (party == null) {
            inviter.sendSystemMessage(Component.translatable("incore.party.error.not_in_party"));
            syncPartyState(server, inviter);
            return false;
        }

        if (!party.leaderId().equals(inviter.getUUID())) {
            inviter.sendSystemMessage(Component.translatable("incore.party.error.not_leader"));
            syncPartyState(server, inviter);
            return false;
        }

        if (data.partyForMember(target.getUUID()).isPresent()) {
            inviter.sendSystemMessage(Component.translatable("incore.party.error.target_in_party", target.getGameProfile().getName()));
            syncPartyState(server, inviter);
            syncPartyState(server, target);
            return false;
        }

        data.setInvite(new PartySavedData.InviteRecord(target.getUUID(), party.id(), inviter.getUUID()));

        inviter.sendSystemMessage(Component.translatable("incore.party.invite.sent", target.getGameProfile().getName()));
        target.sendSystemMessage(Component.translatable("incore.party.invite.received", inviter.getGameProfile().getName(), party.id()).withStyle(ChatFormatting.AQUA));
        target.sendSystemMessage(Component.translatable("incore.party.invite.hint").withStyle(ChatFormatting.GRAY));
        syncPartyState(server, inviter);
        syncPartyState(server, target);
        return true;
    }

    public static boolean acceptInvite(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return false;
        }

        PartySavedData data = PartySavedData.get(server);
        if (data.partyForMember(player.getUUID()).isPresent()) {
            data.clearInvite(player.getUUID());
            player.sendSystemMessage(Component.translatable("incore.party.error.already_in_party"));
            syncPartyState(server, player);
            return false;
        }

        PartySavedData.InviteRecord invite = data.inviteFor(player.getUUID()).orElse(null);
        if (invite == null) {
            player.sendSystemMessage(Component.translatable("incore.party.invite.none"));
            syncPartyState(server, player);
            return false;
        }

        PartySavedData.PartyRecord party = data.getParty(invite.partyId()).orElse(null);
        if (party == null) {
            data.clearInvite(player.getUUID());
            player.sendSystemMessage(Component.translatable("incore.party.invite.expired"));
            syncPartyState(server, player);
            return false;
        }

        List<UUID> members = new ArrayList<>(party.members());
        if (!members.contains(player.getUUID())) {
            members.add(player.getUUID());
        }

        PartySavedData.PartyRecord updatedParty = party.withMembers(members);
        data.putParty(updatedParty);
        data.clearInvite(player.getUUID());

        broadcastToOnlinePartyMembers(server, updatedParty, Component.translatable("incore.party.member.joined", player.getGameProfile().getName()));
        player.sendSystemMessage(Component.translatable("incore.party.invite.accepted", updatedParty.id()).withStyle(ChatFormatting.GREEN));
        syncPartyState(server, updatedParty.members());
        return true;
    }

    public static boolean declineInvite(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return false;
        }

        PartySavedData data = PartySavedData.get(server);
        PartySavedData.InviteRecord invite = data.inviteFor(player.getUUID()).orElse(null);
        if (invite == null) {
            player.sendSystemMessage(Component.translatable("incore.party.invite.none"));
            syncPartyState(server, player);
            return false;
        }

        data.clearInvite(player.getUUID());

        ServerPlayer inviter = server.getPlayerList().getPlayer(invite.inviterId());
        if (inviter != null) {
            inviter.sendSystemMessage(Component.translatable("incore.party.invite.declined.by", player.getGameProfile().getName()));
            syncPartyState(server, inviter);
        }
        player.sendSystemMessage(Component.translatable("incore.party.invite.declined"));
        syncPartyState(server, player);
        return true;
    }

    public static boolean leaveParty(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return false;
        }

        PartySavedData data = PartySavedData.get(server);
        PartySavedData.PartyRecord party = data.partyForMember(player.getUUID()).orElse(null);
        if (party == null) {
            player.sendSystemMessage(Component.translatable("incore.party.error.not_in_party"));
            syncPartyState(server, player);
            return false;
        }

        List<UUID> remaining = new ArrayList<>(party.members());
        remaining.remove(player.getUUID());

        if (remaining.isEmpty()) {
            data.removeParty(party.id());
            player.sendSystemMessage(Component.translatable("incore.party.left.disbanded", party.id()).withStyle(ChatFormatting.YELLOW));
            syncPartyState(server, player);
            return true;
        }

        UUID nextLeader = party.leaderId().equals(player.getUUID()) ? remaining.getFirst() : party.leaderId();
        PartySavedData.PartyRecord updatedParty = new PartySavedData.PartyRecord(party.id(), nextLeader, remaining);
        data.putParty(updatedParty);

        broadcastToOnlinePartyMembers(server, updatedParty, Component.translatable("incore.party.member.left", player.getGameProfile().getName()));
        if (!nextLeader.equals(party.leaderId())) {
            broadcastToOnlinePartyMembers(server, updatedParty, Component.translatable("incore.party.leader.promoted", resolvePlayerName(server, nextLeader)));
        }
        player.sendSystemMessage(Component.translatable("incore.party.left"));
        syncPartyState(server, updatedParty.members());
        syncPartyState(server, player);
        return true;
    }

    public static boolean kickMember(ServerPlayer actingLeader, ServerPlayer target) {
        MinecraftServer server = actingLeader.getServer();
        if (server == null) {
            return false;
        }

        if (actingLeader.getUUID().equals(target.getUUID())) {
            actingLeader.sendSystemMessage(Component.translatable("incore.party.error.kick_self"));
            syncPartyState(server, actingLeader);
            return false;
        }

        PartySavedData data = PartySavedData.get(server);
        PartySavedData.PartyRecord party = data.partyForMember(actingLeader.getUUID()).orElse(null);
        if (party == null) {
            actingLeader.sendSystemMessage(Component.translatable("incore.party.error.not_in_party"));
            syncPartyState(server, actingLeader);
            return false;
        }
        if (!party.leaderId().equals(actingLeader.getUUID())) {
            actingLeader.sendSystemMessage(Component.translatable("incore.party.error.not_leader"));
            syncPartyState(server, actingLeader);
            return false;
        }
        if (!party.members().contains(target.getUUID())) {
            actingLeader.sendSystemMessage(Component.translatable("incore.party.error.not_member", target.getGameProfile().getName()));
            syncPartyState(server, actingLeader);
            syncPartyState(server, target);
            return false;
        }

        List<UUID> remaining = new ArrayList<>(party.members());
        remaining.remove(target.getUUID());
        if (remaining.isEmpty()) {
            data.removeParty(party.id());
            actingLeader.sendSystemMessage(Component.translatable("incore.party.left.disbanded", party.id()).withStyle(ChatFormatting.YELLOW));
            syncPartyState(server, actingLeader);
            syncPartyState(server, target);
            return true;
        }

        PartySavedData.PartyRecord updatedParty = party.withMembers(remaining);
        data.putParty(updatedParty);

        broadcastToOnlinePartyMembers(server, updatedParty, Component.translatable("incore.party.member.kicked", target.getGameProfile().getName()));
        target.sendSystemMessage(Component.translatable("incore.party.kicked", party.id(), actingLeader.getGameProfile().getName()).withStyle(ChatFormatting.RED));
        syncPartyState(server, updatedParty.members());
        syncPartyState(server, target);
        return true;
    }

    public static boolean promoteLeader(ServerPlayer actingLeader, ServerPlayer target) {
        MinecraftServer server = actingLeader.getServer();
        if (server == null) {
            return false;
        }

        PartySavedData data = PartySavedData.get(server);
        PartySavedData.PartyRecord party = data.partyForMember(actingLeader.getUUID()).orElse(null);
        if (party == null) {
            actingLeader.sendSystemMessage(Component.translatable("incore.party.error.not_in_party"));
            syncPartyState(server, actingLeader);
            return false;
        }
        if (!party.leaderId().equals(actingLeader.getUUID())) {
            actingLeader.sendSystemMessage(Component.translatable("incore.party.error.not_leader"));
            syncPartyState(server, actingLeader);
            return false;
        }
        if (actingLeader.getUUID().equals(target.getUUID())) {
            actingLeader.sendSystemMessage(Component.translatable("incore.party.error.promote_self"));
            syncPartyState(server, actingLeader);
            return false;
        }
        if (!party.members().contains(target.getUUID())) {
            actingLeader.sendSystemMessage(Component.translatable("incore.party.error.not_member", target.getGameProfile().getName()));
            syncPartyState(server, actingLeader);
            syncPartyState(server, target);
            return false;
        }

        PartySavedData.PartyRecord updatedParty = party.withLeader(target.getUUID());
        data.putParty(updatedParty);
        broadcastToOnlinePartyMembers(server, updatedParty, Component.translatable("incore.party.leader.promoted", target.getGameProfile().getName()));
        syncPartyState(server, updatedParty.members());
        return true;
    }

    public static int sendInfo(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return 0;
        }

        PartySavedData data = PartySavedData.get(server);
        PartySavedData.PartyRecord party = data.partyForMember(player.getUUID()).orElse(null);
        if (party == null) {
            PartySavedData.InviteRecord invite = data.inviteFor(player.getUUID()).orElse(null);
            player.sendSystemMessage(Component.translatable("incore.party.error.not_in_party"));
            if (invite != null) {
                player.sendSystemMessage(Component.translatable("incore.party.info.invite_pending", resolvePlayerName(server, invite.inviterId()), invite.partyId()));
            }
            return 0;
        }

        String memberNames = party.members().stream()
                .map(memberId -> resolvePlayerName(server, memberId))
                .reduce((a, b) -> a + ", " + b)
                .orElse("-");
        player.sendSystemMessage(Component.translatable("incore.party.info.header", party.id()));
        player.sendSystemMessage(Component.translatable("incore.party.info.leader", resolvePlayerName(server, party.leaderId())));
        player.sendSystemMessage(Component.translatable("incore.party.info.members", party.members().size(), memberNames));
        return party.members().size();
    }

    public static void onPlayerLogin(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }

        syncPartyState(server, player);

        PartySavedData.InviteRecord invite = PartySavedData.get(server).inviteFor(player.getUUID()).orElse(null);
        if (invite == null) {
            return;
        }

        player.sendSystemMessage(Component.translatable("incore.party.invite.reminder", resolvePlayerName(server, invite.inviterId()), invite.partyId()).withStyle(ChatFormatting.AQUA));
        player.sendSystemMessage(Component.translatable("incore.party.invite.hint").withStyle(ChatFormatting.GRAY));
    }

    public static void onPlayerLogout(ServerPlayer player) {
        LAST_HUD_SYNC.remove(player.getUUID());
    }

    public static void onPlayerChangedDimension(ServerPlayer player) {
        LAST_HUD_SYNC.remove(player.getUUID());
    }

    public static void onServerTick(MinecraftServer server) {
        int tick = server.getTickCount();
        if (tick % HUD_SYNC_INTERVAL_TICKS != 0) {
            return;
        }

        List<ServerPlayer> onlinePlayers = server.getPlayerList().getPlayers();
        if (onlinePlayers.isEmpty()) {
            LAST_HUD_SYNC.clear();
            return;
        }

        PartySavedData data = PartySavedData.get(server);
        Map<Long, List<PartyHudSyncPayload.MemberEntry>> cachedPartyHudRows = new HashMap<>();
        Set<UUID> onlineIds = new HashSet<>();
        for (ServerPlayer player : onlinePlayers) {
            onlineIds.add(player.getUUID());
            if (!player.serverLevel().dimension().equals(RoguelikeConstants.DUNGEON_DIMENSION)) {
                LAST_HUD_SYNC.remove(player.getUUID());
                continue;
            }

            List<PartyHudSyncPayload.MemberEntry> entries = buildHudEntriesForPlayer(server, data, player, cachedPartyHudRows);
            syncHudIfNeeded(player, entries, tick);
        }

        LAST_HUD_SYNC.keySet().removeIf(uuid -> !onlineIds.contains(uuid));
    }

    private static List<PartyHudSyncPayload.MemberEntry> buildHudEntriesForPlayer(
            MinecraftServer server,
            PartySavedData data,
            ServerPlayer viewer,
            Map<Long, List<PartyHudSyncPayload.MemberEntry>> cachedPartyHudRows
    ) {
        PartySavedData.PartyRecord party = data.partyForMember(viewer.getUUID()).orElse(null);
        if (party == null) {
            return List.of(toHudEntry(viewer));
        }

        return cachedPartyHudRows.computeIfAbsent(party.id(), ignored -> {
            List<PartyHudSyncPayload.MemberEntry> rows = new ArrayList<>();
            for (UUID memberId : party.members()) {
                ServerPlayer member = server.getPlayerList().getPlayer(memberId);
                if (member == null) {
                    continue;
                }
                rows.add(toHudEntry(member));
            }
            return List.copyOf(rows);
        });
    }

    private static void syncHudIfNeeded(ServerPlayer player, List<PartyHudSyncPayload.MemberEntry> rows, int serverTick) {
        int signature = hudSignature(rows);
        HudSyncState state = LAST_HUD_SYNC.get(player.getUUID());
        if (state != null && state.signature() == signature && (serverTick - state.lastSentTick()) < HUD_FORCE_RESEND_TICKS) {
            return;
        }

        PartyNetworking.syncHudToPlayer(player, rows);
        LAST_HUD_SYNC.put(player.getUUID(), new HudSyncState(signature, serverTick));
    }

    private static PartyHudSyncPayload.MemberEntry toHudEntry(ServerPlayer player) {
        return new PartyHudSyncPayload.MemberEntry(
                player.getUUID(),
                player.getGameProfile().getName(),
                Math.max(0.0F, player.getHealth()),
                Math.max(1.0F, (float) player.getMaxHealth())
        );
    }

    private static int hudSignature(List<PartyHudSyncPayload.MemberEntry> rows) {
        int hash = 1;
        for (PartyHudSyncPayload.MemberEntry row : rows) {
            int entryHash = row.memberId().hashCode();
            entryHash = (31 * entryHash) + row.name().hashCode();
            entryHash = (31 * entryHash) + Math.round(Math.max(0.0F, row.health()) * 2.0F);
            entryHash = (31 * entryHash) + Math.round(Math.max(1.0F, row.maxHealth()) * 2.0F);
            hash = (31 * hash) + entryHash;
        }
        return hash;
    }

    private static void broadcastToOnlinePartyMembers(MinecraftServer server, PartySavedData.PartyRecord party, Component message) {
        for (UUID memberId : party.members()) {
            ServerPlayer member = server.getPlayerList().getPlayer(memberId);
            if (member != null) {
                member.sendSystemMessage(message);
            }
        }
    }

    private static void syncPartyState(MinecraftServer server, ServerPlayer player) {
        PartyNetworking.syncStateToPlayer(server, player);
    }

    private static void syncPartyState(MinecraftServer server, List<UUID> memberIds) {
        for (UUID memberId : memberIds) {
            ServerPlayer member = server.getPlayerList().getPlayer(memberId);
            if (member != null) {
                syncPartyState(server, member);
            }
        }
    }

    private static String resolvePlayerName(MinecraftServer server, @Nullable UUID playerId) {
        if (playerId == null) {
            return "unknown";
        }

        ServerPlayer online = server.getPlayerList().getPlayer(playerId);
        if (online != null) {
            return online.getGameProfile().getName();
        }

        String cachedName = server.getProfileCache().get(playerId)
                .map(com.mojang.authlib.GameProfile::getName)
                .orElse("");
        if (!cachedName.isBlank()) {
            return cachedName;
        }
        return playerId.toString();
    }

    private record HudSyncState(int signature, int lastSentTick) {
    }
}
