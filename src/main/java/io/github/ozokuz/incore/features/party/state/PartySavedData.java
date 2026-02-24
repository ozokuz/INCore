package io.github.ozokuz.incore.features.party.state;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;

public class PartySavedData extends SavedData {
    private static final String DATA_NAME = "incore_party";

    private long nextPartyId = 1L;
    private final Map<Long, PartyRecord> parties = new HashMap<>();
    private final Map<UUID, Long> partyByMember = new HashMap<>();
    private final Map<UUID, InviteRecord> pendingInvites = new HashMap<>();

    public static PartySavedData get(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        return overworld.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(PartySavedData::new, PartySavedData::load, DataFixTypes.LEVEL),
                DATA_NAME
        );
    }

    public static PartySavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        PartySavedData data = new PartySavedData();
        data.nextPartyId = Math.max(1L, tag.getLong("nextPartyId"));

        ListTag partiesTag = tag.getList("parties", Tag.TAG_COMPOUND);
        for (Tag partyTag : partiesTag) {
            PartyRecord party = PartyRecord.fromTag((CompoundTag) partyTag);
            if (party == null) {
                continue;
            }

            data.parties.put(party.id(), party);
            data.indexPartyMembers(party);
        }

        ListTag invitesTag = tag.getList("pendingInvites", Tag.TAG_COMPOUND);
        for (Tag inviteTag : invitesTag) {
            InviteRecord invite = InviteRecord.fromTag((CompoundTag) inviteTag);
            if (invite == null || !data.parties.containsKey(invite.partyId())) {
                continue;
            }

            data.pendingInvites.put(invite.targetId(), invite);
        }

        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putLong("nextPartyId", nextPartyId);

        ListTag partiesTag = new ListTag();
        for (PartyRecord party : parties.values()) {
            partiesTag.add(party.toTag());
        }
        tag.put("parties", partiesTag);

        ListTag invitesTag = new ListTag();
        for (InviteRecord invite : pendingInvites.values()) {
            invitesTag.add(invite.toTag());
        }
        tag.put("pendingInvites", invitesTag);
        return tag;
    }

    public long nextPartyId() {
        long id = nextPartyId;
        nextPartyId++;
        setDirty();
        return id;
    }

    public Collection<PartyRecord> parties() {
        return parties.values();
    }

    public Optional<PartyRecord> getParty(long partyId) {
        if (partyId <= 0L) {
            return Optional.empty();
        }

        return Optional.ofNullable(parties.get(partyId));
    }

    public OptionalLong partyIdForMember(UUID playerId) {
        Long partyId = partyByMember.get(playerId);
        return partyId == null ? OptionalLong.empty() : OptionalLong.of(partyId);
    }

    public Optional<PartyRecord> partyForMember(UUID playerId) {
        OptionalLong partyId = partyIdForMember(playerId);
        return partyId.isPresent() ? getParty(partyId.getAsLong()) : Optional.empty();
    }

    public void putParty(PartyRecord party) {
        PartyRecord previous = parties.put(party.id(), party);
        if (previous != null) {
            deindexPartyMembers(previous);
        }
        indexPartyMembers(party);
        setDirty();
    }

    public void removeParty(long partyId) {
        PartyRecord removed = parties.remove(partyId);
        if (removed == null) {
            return;
        }

        deindexPartyMembers(removed);
        clearInvitesForParty(partyId);
        setDirty();
    }

    public Optional<InviteRecord> inviteFor(UUID targetId) {
        return Optional.ofNullable(pendingInvites.get(targetId));
    }

    public void setInvite(InviteRecord invite) {
        pendingInvites.put(invite.targetId(), invite);
        setDirty();
    }

    public void clearInvite(UUID targetId) {
        if (pendingInvites.remove(targetId) != null) {
            setDirty();
        }
    }

    public void clearInvitesForParty(long partyId) {
        if (partyId <= 0L || pendingInvites.isEmpty()) {
            return;
        }

        List<UUID> removals = new ArrayList<>();
        for (InviteRecord invite : pendingInvites.values()) {
            if (invite.partyId() == partyId) {
                removals.add(invite.targetId());
            }
        }

        if (removals.isEmpty()) {
            return;
        }

        for (UUID targetId : removals) {
            pendingInvites.remove(targetId);
        }
        setDirty();
    }

    private void indexPartyMembers(PartyRecord party) {
        for (UUID member : party.members()) {
            partyByMember.put(member, party.id());
        }
    }

    private void deindexPartyMembers(PartyRecord party) {
        for (UUID member : party.members()) {
            Long existing = partyByMember.get(member);
            if (existing != null && existing == party.id()) {
                partyByMember.remove(member);
            }
        }
    }

    public record PartyRecord(long id, UUID leaderId, List<UUID> members) {
        public PartyRecord {
            id = Math.max(1L, id);
            members = normalizeMembers(leaderId, members);
            if (leaderId == null && !members.isEmpty()) {
                leaderId = members.getFirst();
            }
            if (leaderId != null && !members.contains(leaderId)) {
                List<UUID> withLeader = new ArrayList<>(members.size() + 1);
                withLeader.add(leaderId);
                withLeader.addAll(members);
                members = List.copyOf(withLeader);
            }
        }

        public PartyRecord withLeader(UUID newLeader) {
            return new PartyRecord(id, newLeader, members);
        }

        public PartyRecord withMembers(List<UUID> newMembers) {
            return new PartyRecord(id, leaderId, newMembers);
        }

        public CompoundTag toTag() {
            CompoundTag tag = new CompoundTag();
            tag.putLong("id", id);
            if (leaderId != null) {
                tag.putUUID("leaderId", leaderId);
            }

            ListTag membersTag = new ListTag();
            for (UUID member : members) {
                CompoundTag memberTag = new CompoundTag();
                memberTag.putUUID("player", member);
                membersTag.add(memberTag);
            }
            tag.put("members", membersTag);
            return tag;
        }

        public static PartyRecord fromTag(CompoundTag tag) {
            long id = Math.max(1L, tag.getLong("id"));
            UUID leaderId = tag.hasUUID("leaderId") ? tag.getUUID("leaderId") : null;

            List<UUID> members = new ArrayList<>();
            ListTag membersTag = tag.getList("members", Tag.TAG_COMPOUND);
            for (Tag memberTag : membersTag) {
                CompoundTag row = (CompoundTag) memberTag;
                if (row.hasUUID("player")) {
                    members.add(row.getUUID("player"));
                }
            }

            if (leaderId == null && members.isEmpty()) {
                return null;
            }

            if (leaderId == null) {
                leaderId = members.getFirst();
            }

            if (members.isEmpty()) {
                members = List.of(leaderId);
            }
            return new PartyRecord(id, leaderId, members);
        }

        private static List<UUID> normalizeMembers(UUID leaderId, List<UUID> rawMembers) {
            LinkedHashSet<UUID> unique = new LinkedHashSet<>();
            if (leaderId != null) {
                unique.add(leaderId);
            }
            if (rawMembers != null) {
                for (UUID member : rawMembers) {
                    if (member != null) {
                        unique.add(member);
                    }
                }
            }
            return List.copyOf(unique);
        }
    }

    public record InviteRecord(UUID targetId, long partyId, UUID inviterId) {
        public InviteRecord {
            partyId = Math.max(1L, partyId);
        }

        public CompoundTag toTag() {
            CompoundTag tag = new CompoundTag();
            tag.putUUID("targetId", targetId);
            tag.putLong("partyId", partyId);
            tag.putUUID("inviterId", inviterId);
            return tag;
        }

        public static InviteRecord fromTag(CompoundTag tag) {
            if (!tag.hasUUID("targetId") || !tag.hasUUID("inviterId")) {
                return null;
            }

            long partyId = Math.max(1L, tag.getLong("partyId"));
            return new InviteRecord(tag.getUUID("targetId"), partyId, tag.getUUID("inviterId"));
        }
    }
}
