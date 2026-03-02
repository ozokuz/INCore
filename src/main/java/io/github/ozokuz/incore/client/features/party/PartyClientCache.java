package io.github.ozokuz.incore.client.features.party;

import io.github.ozokuz.incore.features.party.network.OnlinePlayersSyncPayload;
import io.github.ozokuz.incore.features.party.network.PartySyncPayload;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class PartyClientCache {
    private static boolean inParty = false;
    private static long partyId = 0L;
    private static UUID leaderId = null;
    private static String leaderName = "";
    private static List<MemberView> members = List.of();
    private static boolean hasPendingInvite = false;
    private static long invitePartyId = 0L;
    private static UUID inviteInviterId = null;
    private static String inviteInviterName = "";
    private static List<PlayerView> onlinePlayers = List.of();

    private PartyClientCache() {
    }

    public static synchronized void updateParty(PartySyncPayload payload) {
        inParty = payload.inParty();
        partyId = payload.partyId();
        leaderId = payload.leaderId();
        leaderName = payload.leaderName();
        members = payload.members().stream()
                .map(m -> new MemberView(m.playerId(), m.playerName()))
                .toList();
        hasPendingInvite = payload.hasPendingInvite();
        invitePartyId = payload.invitePartyId();
        inviteInviterId = payload.inviteInviterId();
        inviteInviterName = payload.inviteInviterName();
    }

    public static synchronized void updateOnlinePlayers(List<OnlinePlayersSyncPayload.PlayerEntry> players) {
        onlinePlayers = players.stream()
                .map(p -> new PlayerView(p.playerId(), p.playerName()))
                .toList();
    }

    public static synchronized boolean isInParty() {
        return inParty;
    }

    public static synchronized long getPartyId() {
        return partyId;
    }

    public static synchronized UUID getLeaderId() {
        return leaderId;
    }

    public static synchronized String getLeaderName() {
        return leaderName;
    }

    public static synchronized List<MemberView> getMembers() {
        return new ArrayList<>(members);
    }

    public static synchronized boolean hasPendingInvite() {
        return hasPendingInvite;
    }

    public static synchronized long getInvitePartyId() {
        return invitePartyId;
    }

    public static synchronized UUID getInviteInviterId() {
        return inviteInviterId;
    }

    public static synchronized String getInviteInviterName() {
        return inviteInviterName;
    }

    public static synchronized List<PlayerView> getOnlinePlayers() {
        return new ArrayList<>(onlinePlayers);
    }

    public static synchronized boolean isLeader(UUID playerId) {
        return leaderId != null && leaderId.equals(playerId);
    }

    public static synchronized void clear() {
        inParty = false;
        partyId = 0L;
        leaderId = null;
        leaderName = "";
        members = List.of();
        hasPendingInvite = false;
        invitePartyId = 0L;
        inviteInviterId = null;
        inviteInviterName = "";
        onlinePlayers = List.of();
    }

    public record MemberView(UUID playerId, String playerName) {
    }

    public record PlayerView(UUID playerId, String playerName) {
    }
}
