package io.github.ozokuz.incore.features.roguelike.state;

import io.github.ozokuz.incore.features.roguelike.RoguelikeConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class RoguelikeSavedData extends SavedData {
    private static final String DATA_NAME = "incore_roguelike";

    private final Map<UUID, AltarProfile> altarProfiles = new HashMap<>();
    private long nextDungeonId = 1L;
    private int nextSlotIndex;

    private final Map<Long, DungeonRecord> dungeons = new HashMap<>();
    private final Map<UUID, ActiveRun> activeRuns = new HashMap<>();
    private final Map<UUID, ReturnTarget> pendingReturns = new HashMap<>();

    public static RoguelikeSavedData get(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        return overworld.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(RoguelikeSavedData::new, RoguelikeSavedData::load, DataFixTypes.LEVEL),
                DATA_NAME
        );
    }

    public static RoguelikeSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        RoguelikeSavedData data = new RoguelikeSavedData();
        data.nextDungeonId = Math.max(1L, tag.getLong("nextDungeonId"));
        data.nextSlotIndex = Math.max(0, tag.getInt("nextSlotIndex"));

        ListTag playerAltarsTag = tag.getList("playerAltars", Tag.TAG_COMPOUND);
        for (Tag profileTag : playerAltarsTag) {
            CompoundTag row = (CompoundTag) profileTag;
            if (!row.hasUUID("player")) {
                continue;
            }

            UUID playerId = row.getUUID("player");
            int crystalsCrafted = Math.max(0, row.getInt("crystalsCrafted"));
            List<AltarRequirement> requirements = readAltarRequirements(row.getList("altarRequirements", Tag.TAG_COMPOUND));
            data.altarProfiles.put(playerId, new AltarProfile(crystalsCrafted, requirements));
        }

        ListTag dungeonsTag = tag.getList("dungeons", Tag.TAG_COMPOUND);
        for (Tag dungeonTag : dungeonsTag) {
            DungeonRecord record = DungeonRecord.fromTag((CompoundTag) dungeonTag);
            data.dungeons.put(record.dungeonId(), record);
        }

        ListTag runsTag = tag.getList("activeRuns", Tag.TAG_COMPOUND);
        for (Tag runTag : runsTag) {
            ActiveRun run = ActiveRun.fromTag((CompoundTag) runTag);
            data.activeRuns.put(run.playerId(), run);
        }

        ListTag returnsTag = tag.getList("pendingReturns", Tag.TAG_COMPOUND);
        for (Tag returnTag : returnsTag) {
            ReturnTarget target = ReturnTarget.fromTag((CompoundTag) returnTag);
            data.pendingReturns.put(target.playerId(), target);
        }

        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putLong("nextDungeonId", nextDungeonId);
        tag.putInt("nextSlotIndex", nextSlotIndex);

        ListTag playerAltarsTag = new ListTag();
        for (Map.Entry<UUID, AltarProfile> entry : altarProfiles.entrySet()) {
            CompoundTag row = new CompoundTag();
            row.putUUID("player", entry.getKey());
            row.putInt("crystalsCrafted", entry.getValue().crystalsCrafted());
            row.put("altarRequirements", toRequirementsTag(entry.getValue().altarRequirements()));
            playerAltarsTag.add(row);
        }
        tag.put("playerAltars", playerAltarsTag);

        ListTag dungeonsTag = new ListTag();
        for (DungeonRecord record : dungeons.values()) {
            dungeonsTag.add(record.toTag());
        }
        tag.put("dungeons", dungeonsTag);

        ListTag runsTag = new ListTag();
        for (ActiveRun run : activeRuns.values()) {
            runsTag.add(run.toTag());
        }
        tag.put("activeRuns", runsTag);

        ListTag returnsTag = new ListTag();
        for (ReturnTarget target : pendingReturns.values()) {
            returnsTag.add(target.toTag());
        }
        tag.put("pendingReturns", returnsTag);

        return tag;
    }

    private AltarProfile altarProfile(UUID playerId) {
        if (playerId == null) {
            return AltarProfile.EMPTY;
        }

        return altarProfiles.getOrDefault(playerId, AltarProfile.EMPTY);
    }

    private void putAltarProfile(UUID playerId, AltarProfile profile) {
        if (playerId == null) {
            return;
        }

        altarProfiles.put(playerId, profile);
        setDirty();
    }

    private static List<AltarRequirement> readAltarRequirements(ListTag altarTag) {
        List<AltarRequirement> requirements = new ArrayList<>();
        for (Tag requirementTag : altarTag) {
            AltarRequirement requirement = AltarRequirement.fromTag((CompoundTag) requirementTag);
            if (requirement != null) {
                requirements.add(requirement);
            }
        }

        return requirements;
    }

    private static ListTag toRequirementsTag(List<AltarRequirement> requirements) {
        ListTag altarTag = new ListTag();
        for (AltarRequirement requirement : requirements) {
            altarTag.add(requirement.toTag());
        }
        return altarTag;
    }

    private static List<AltarRequirement> normalizeRequirements(List<AltarRequirement> requirements) {
        LinkedHashMap<ResourceLocation, AltarRequirement> unique = new LinkedHashMap<>();
        for (AltarRequirement requirement : requirements) {
            if (requirement == null) {
                continue;
            }

            ResourceLocation id = requirement.offeringId();
            if (id == null || unique.containsKey(id)) {
                continue;
            }

            int required = Math.max(1, requirement.requiredAmount());
            int submitted = Math.max(0, Math.min(required, requirement.submittedAmount()));
            unique.put(id, new AltarRequirement(id, required, submitted));
        }

        return List.copyOf(unique.values());
    }

    public int crystalsCrafted(UUID playerId) {
        return altarProfile(playerId).crystalsCrafted();
    }

    public void setCrystalsCrafted(UUID playerId, int value) {
        AltarProfile profile = altarProfile(playerId);
        putAltarProfile(playerId, profile.withCrystalsCrafted(Math.max(0, value)));
    }

    public void incrementCrystalsCrafted(UUID playerId) {
        AltarProfile profile = altarProfile(playerId);
        putAltarProfile(playerId, profile.withCrystalsCrafted(profile.crystalsCrafted() + 1));
    }

    public List<AltarRequirement> altarRequirements(UUID playerId) {
        return altarProfile(playerId).altarRequirements();
    }

    public boolean isAltarComplete(UUID playerId) {
        List<AltarRequirement> requirements = altarRequirements(playerId);
        return !requirements.isEmpty() && requirements.stream().allMatch(AltarRequirement::isComplete);
    }

    public void setAltarRequirements(UUID playerId, List<AltarRequirement> requirements) {
        AltarProfile profile = altarProfile(playerId);
        putAltarProfile(playerId, profile.withAltarRequirements(normalizeRequirements(requirements)));
    }

    public int submitOffering(UUID playerId, ResourceLocation offeringId, int amount) {
        if (offeringId == null || amount <= 0) {
            return 0;
        }

        AltarProfile profile = altarProfile(playerId);
        List<AltarRequirement> currentRequirements = profile.altarRequirements();
        List<AltarRequirement> updated = new ArrayList<>(currentRequirements);

        for (int i = 0; i < updated.size(); i++) {
            AltarRequirement requirement = updated.get(i);
            if (!requirement.offeringId().equals(offeringId)) {
                continue;
            }

            int consumed = Math.min(requirement.remaining(), amount);
            if (consumed <= 0) {
                return 0;
            }

            updated.set(i, requirement.withAdditionalSubmitted(consumed));
            putAltarProfile(playerId, profile.withAltarRequirements(updated));
            return consumed;
        }

        return 0;
    }

    public record AltarProfile(int crystalsCrafted, List<AltarRequirement> altarRequirements) {
        public static final AltarProfile EMPTY = new AltarProfile(0, List.of());

        public AltarProfile {
            crystalsCrafted = Math.max(0, crystalsCrafted);
            altarRequirements = altarRequirements == null ? List.of() : List.copyOf(altarRequirements);
        }

        public AltarProfile withCrystalsCrafted(int newValue) {
            return new AltarProfile(newValue, altarRequirements);
        }

        public AltarProfile withAltarRequirements(List<AltarRequirement> requirements) {
            return new AltarProfile(crystalsCrafted, requirements);
        }
    }

    public long nextDungeonId() {
        long id = nextDungeonId;
        nextDungeonId++;
        setDirty();
        return id;
    }

    public int nextSlotIndex() {
        int slot = nextSlotIndex;
        nextSlotIndex++;
        setDirty();
        return slot;
    }

    public void putDungeon(DungeonRecord record) {
        dungeons.put(record.dungeonId(), record);
        setDirty();
    }

    public DungeonRecord getDungeon(long dungeonId) {
        return dungeons.get(dungeonId);
    }

    public Collection<DungeonRecord> dungeons() {
        return dungeons.values();
    }

    public void removeDungeon(long dungeonId) {
        dungeons.remove(dungeonId);
        setDirty();
    }

    public void startRun(UUID playerId, long dungeonId, ResourceKey<Level> returnDimension, BlockPos returnPos) {
        activeRuns.put(playerId, new ActiveRun(playerId, dungeonId, returnDimension.location(), returnPos));
        setDirty();
    }

    public Optional<ActiveRun> getRun(UUID playerId) {
        return Optional.ofNullable(activeRuns.get(playerId));
    }

    public Collection<ActiveRun> activeRuns() {
        return activeRuns.values();
    }

    public boolean hasActiveRunInDungeon(long dungeonId) {
        return activeRuns.values().stream().anyMatch(run -> run.dungeonId() == dungeonId);
    }

    public Set<UUID> activePlayersInDungeon(long dungeonId) {
        return activeRuns.values().stream()
                .filter(run -> run.dungeonId() == dungeonId)
                .map(ActiveRun::playerId)
                .collect(java.util.stream.Collectors.toSet());
    }

    public void clearRun(UUID playerId) {
        if (activeRuns.remove(playerId) != null) {
            setDirty();
        }
    }

    public void setPendingReturn(UUID playerId, ResourceKey<Level> dimension, BlockPos pos) {
        pendingReturns.put(playerId, new ReturnTarget(playerId, dimension.location(), pos));
        setDirty();
    }

    public Optional<ReturnTarget> takePendingReturn(UUID playerId) {
        ReturnTarget target = pendingReturns.remove(playerId);
        if (target != null) {
            setDirty();
        }
        return Optional.ofNullable(target);
    }

    public record AltarRequirement(ResourceLocation offeringId, int requiredAmount, int submittedAmount) {
        public boolean isComplete() {
            return submittedAmount >= requiredAmount;
        }

        public int remaining() {
            return Math.max(0, requiredAmount - submittedAmount);
        }

        public AltarRequirement withAdditionalSubmitted(int amount) {
            if (amount <= 0) {
                return this;
            }

            return new AltarRequirement(offeringId, requiredAmount, Math.min(requiredAmount, submittedAmount + amount));
        }

        public CompoundTag toTag() {
            CompoundTag tag = new CompoundTag();
            tag.putString("offeringId", offeringId.toString());
            tag.putInt("required", requiredAmount);
            tag.putInt("submitted", submittedAmount);
            return tag;
        }

        public static AltarRequirement fromTag(CompoundTag tag) {
            ResourceLocation offeringId = ResourceLocation.tryParse(tag.getString("offeringId"));
            if (offeringId == null) {
                return null;
            }

            int required = Math.max(1, tag.getInt("required"));
            int submitted = Math.max(0, Math.min(required, tag.getInt("submitted")));
            return new AltarRequirement(offeringId, required, submitted);
        }
    }

    public record DungeonRecord(
            long dungeonId,
            int slotIndex,
            State state,
            ResourceLocation themeId,
            ResourceLocation objectiveId,
            BlockPos origin,
            ResourceLocation portalDimension,
            BlockPos portalPos,
            long expiresAtGameTime,
            Map<UUID, PlayerProgress> playerProgress
    ) {
        public static DungeonRecord create(
                long dungeonId,
                int slotIndex,
                ResourceLocation themeId,
                ResourceLocation objectiveId,
                BlockPos origin,
                ResourceKey<Level> portalDimension,
                BlockPos portalPos,
                long expiresAtGameTime
        ) {
            return new DungeonRecord(
                    dungeonId,
                    slotIndex,
                    State.ACTIVE,
                    themeId,
                    objectiveId,
                    origin,
                    portalDimension.location(),
                    portalPos,
                    expiresAtGameTime,
                    new HashMap<>()
            );
        }

        public DungeonRecord withState(State newState) {
            return new DungeonRecord(dungeonId, slotIndex, newState, themeId, objectiveId, origin, portalDimension, portalPos, expiresAtGameTime, new HashMap<>(playerProgress));
        }

        public DungeonRecord withExpiry(long expiryTick) {
            return new DungeonRecord(dungeonId, slotIndex, state, themeId, objectiveId, origin, portalDimension, portalPos, expiryTick, new HashMap<>(playerProgress));
        }

        public DungeonRecord withPortal(ResourceKey<Level> dimension, BlockPos pos) {
            return new DungeonRecord(dungeonId, slotIndex, state, themeId, objectiveId, origin, dimension.location(), pos, expiresAtGameTime, new HashMap<>(playerProgress));
        }

        public DungeonRecord upsertProgress(UUID playerId, PlayerProgress progress) {
            HashMap<UUID, PlayerProgress> map = new HashMap<>(playerProgress);
            map.put(playerId, progress);
            return new DungeonRecord(dungeonId, slotIndex, state, themeId, objectiveId, origin, portalDimension, portalPos, expiresAtGameTime, map);
        }

        public PlayerProgress progressFor(UUID playerId) {
            return playerProgress.getOrDefault(playerId, PlayerProgress.EMPTY);
        }

        public boolean isRecyclable() {
            return state == State.COMPLETED || state == State.EXPIRED;
        }

        public CompoundTag toTag() {
            CompoundTag tag = new CompoundTag();
            tag.putLong("id", dungeonId);
            tag.putInt("slotIndex", slotIndex);
            tag.putString("state", state.name());
            tag.putString("themeId", themeId.toString());
            tag.putString("objectiveId", objectiveId.toString());
            tag.putIntArray("origin", new int[]{origin.getX(), origin.getY(), origin.getZ()});
            tag.putString("portalDimension", portalDimension.toString());
            tag.putIntArray("portalPos", new int[]{portalPos.getX(), portalPos.getY(), portalPos.getZ()});
            tag.putLong("expiresAtGameTime", expiresAtGameTime);

            ListTag progressTag = new ListTag();
            for (Map.Entry<UUID, PlayerProgress> entry : playerProgress.entrySet()) {
                CompoundTag row = new CompoundTag();
                row.putUUID("player", entry.getKey());
                row.put("data", entry.getValue().toTag());
                progressTag.add(row);
            }
            tag.put("playerProgress", progressTag);

            return tag;
        }

        public static DungeonRecord fromTag(CompoundTag tag) {
            long id = tag.getLong("id");
            int slotIndex = tag.getInt("slotIndex");
            State state = State.fromString(tag.getString("state"));
            ResourceLocation themeId = ResourceLocation.parse(tag.getString("themeId"));
            ResourceLocation objectiveId = ResourceLocation.parse(tag.getString("objectiveId"));
            int[] originArray = tag.getIntArray("origin");
            int[] portalArray = tag.getIntArray("portalPos");
            BlockPos origin = originArray.length >= 3 ? new BlockPos(originArray[0], originArray[1], originArray[2]) : BlockPos.ZERO;
            BlockPos portalPos = portalArray.length >= 3 ? new BlockPos(portalArray[0], portalArray[1], portalArray[2]) : BlockPos.ZERO;
            ResourceLocation portalDimension = ResourceLocation.tryParse(tag.getString("portalDimension"));
            if (portalDimension == null) {
                portalDimension = Level.OVERWORLD.location();
            }
            long expiresAt = tag.getLong("expiresAtGameTime");

            Map<UUID, PlayerProgress> progressMap = new HashMap<>();
            ListTag progressTag = tag.getList("playerProgress", Tag.TAG_COMPOUND);
            for (Tag progressEntry : progressTag) {
                CompoundTag row = (CompoundTag) progressEntry;
                if (!row.hasUUID("player")) {
                    continue;
                }

                UUID player = row.getUUID("player");
                PlayerProgress progress = PlayerProgress.fromTag(row.getCompound("data"));
                progressMap.put(player, progress);
            }

            return new DungeonRecord(id, slotIndex, state, themeId, objectiveId, origin, portalDimension, portalPos, expiresAt, progressMap);
        }
    }

    public enum State {
        ACTIVE,
        COMPLETED,
        EXPIRED;

        public static State fromString(String raw) {
            try {
                return State.valueOf(raw);
            } catch (Exception ignored) {
                return ACTIVE;
            }
        }
    }

    public record PlayerProgress(int kills, boolean entered, boolean died, boolean rewarded) {
        public static final PlayerProgress EMPTY = new PlayerProgress(0, false, false, false);

        public PlayerProgress addKills(int amount) {
            return new PlayerProgress(Math.max(0, kills + amount), entered, died, rewarded);
        }

        public PlayerProgress withEntered() {
            return new PlayerProgress(kills, true, died, rewarded);
        }

        public PlayerProgress withDeath() {
            return new PlayerProgress(kills, entered, true, rewarded);
        }

        public PlayerProgress withRewarded() {
            return new PlayerProgress(kills, entered, died, true);
        }

        public CompoundTag toTag() {
            CompoundTag tag = new CompoundTag();
            tag.putInt("kills", kills);
            tag.putBoolean("entered", entered);
            tag.putBoolean("died", died);
            tag.putBoolean("rewarded", rewarded);
            return tag;
        }

        public static PlayerProgress fromTag(CompoundTag tag) {
            return new PlayerProgress(
                    Math.max(0, tag.getInt("kills")),
                    tag.getBoolean("entered"),
                    tag.getBoolean("died"),
                    tag.getBoolean("rewarded")
            );
        }
    }

    public record ActiveRun(UUID playerId, long dungeonId, ResourceLocation returnDimension, BlockPos returnPos) {
        public CompoundTag toTag() {
            CompoundTag tag = new CompoundTag();
            tag.putUUID("player", playerId);
            tag.putLong("dungeonId", dungeonId);
            tag.putString("returnDimension", returnDimension.toString());
            tag.putIntArray("returnPos", new int[]{returnPos.getX(), returnPos.getY(), returnPos.getZ()});
            return tag;
        }

        public static ActiveRun fromTag(CompoundTag tag) {
            UUID playerId = tag.hasUUID("player") ? tag.getUUID("player") : UUID.randomUUID();
            long dungeonId = tag.getLong("dungeonId");
            ResourceLocation returnDimension = ResourceLocation.tryParse(tag.getString("returnDimension"));
            if (returnDimension == null) {
                returnDimension = Level.OVERWORLD.location();
            }
            int[] returnPosArr = tag.getIntArray("returnPos");
            BlockPos returnPos = returnPosArr.length >= 3 ? new BlockPos(returnPosArr[0], returnPosArr[1], returnPosArr[2]) : BlockPos.ZERO;
            return new ActiveRun(playerId, dungeonId, returnDimension, returnPos);
        }

        public ResourceKey<Level> returnDimensionKey() {
            return ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, returnDimension);
        }
    }

    public record ReturnTarget(UUID playerId, ResourceLocation dimension, BlockPos pos) {
        public CompoundTag toTag() {
            CompoundTag tag = new CompoundTag();
            tag.putUUID("player", playerId);
            tag.putString("dimension", dimension.toString());
            tag.putIntArray("pos", new int[]{pos.getX(), pos.getY(), pos.getZ()});
            return tag;
        }

        public static ReturnTarget fromTag(CompoundTag tag) {
            UUID playerId = tag.hasUUID("player") ? tag.getUUID("player") : UUID.randomUUID();
            ResourceLocation dimension = ResourceLocation.tryParse(tag.getString("dimension"));
            if (dimension == null) {
                dimension = Level.OVERWORLD.location();
            }
            int[] posArr = tag.getIntArray("pos");
            BlockPos pos = posArr.length >= 3 ? new BlockPos(posArr[0], posArr[1], posArr[2]) : BlockPos.ZERO;
            return new ReturnTarget(playerId, dimension, pos);
        }

        public ResourceKey<Level> dimensionKey() {
            return ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, dimension);
        }
    }

    public static BlockPos slotOrigin(int slotIndex) {
        int gridX = slotIndex % 64;
        int gridZ = slotIndex / 64;
        int x = (gridX * RoguelikeConstants.DUNGEON_SPACING) + 8;
        int y = RoguelikeConstants.DUNGEON_BASE_Y;
        int z = (gridZ * RoguelikeConstants.DUNGEON_SPACING) + 8;
        return new BlockPos(x, y, z);
    }
}
