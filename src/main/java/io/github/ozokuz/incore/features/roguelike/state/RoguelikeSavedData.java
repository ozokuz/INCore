package io.github.ozokuz.incore.features.roguelike.state;

import io.github.ozokuz.incore.features.roguelike.RoguelikeConstants;
import io.github.ozokuz.incore.features.roguelike.instance.DungeonInstanceData;
import io.github.ozokuz.incore.features.roguelike.instance.DungeonInstanceId;
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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class RoguelikeSavedData extends SavedData {
    private static final String DATA_NAME = "incore_roguelike";

    private final Map<UUID, AltarProfile> altarProfiles = new HashMap<>();

    private long nextInstanceId = 1L;
    private int nextSlotIndex;
    private final Set<Integer> freeSlots = new HashSet<>();

    private final Map<Long, DungeonInstanceData> instances = new HashMap<>();
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

        ListTag playerAltarsTag = tag.getList("playerAltars", Tag.TAG_COMPOUND);
        for (Tag profileTag : playerAltarsTag) {
            CompoundTag row = (CompoundTag) profileTag;
            if (!row.hasUUID("player")) {
                continue;
            }

            UUID playerId = row.getUUID("player");
            int crystalsCrafted = Math.max(0, row.getInt("crystalsCrafted"));
            boolean crystalPlaced = row.getBoolean("crystalPlaced");
            List<AltarRequirement> requirements = readAltarRequirements(row.getList("altarRequirements", Tag.TAG_COMPOUND));
            data.altarProfiles.put(playerId, new AltarProfile(crystalsCrafted, requirements, crystalPlaced));
        }

        data.nextInstanceId = Math.max(1L, tag.getLong("nextInstanceIdV2"));
        data.nextSlotIndex = Math.max(0, tag.getInt("nextSlotIndexV2"));
        for (int slot : tag.getIntArray("freeSlotsV2")) {
            if (slot >= 0) {
                data.freeSlots.add(slot);
            }
        }

        ListTag instancesTag = tag.getList("instancesV2", Tag.TAG_COMPOUND);
        for (Tag instanceTag : instancesTag) {
            DungeonInstanceData instance = DungeonInstanceData.fromTag((CompoundTag) instanceTag);
            if (instance != null) {
                data.instances.put(instance.id().value(), instance);
            }
        }

        ListTag runsTag = tag.getList("activeRunsV2", Tag.TAG_COMPOUND);
        for (Tag runTag : runsTag) {
            ActiveRun run = ActiveRun.fromTag((CompoundTag) runTag);
            data.activeRuns.put(run.playerId(), run);
        }

        ListTag returnsTag = tag.getList("pendingReturnsV2", Tag.TAG_COMPOUND);
        for (Tag returnTag : returnsTag) {
            ReturnTarget target = ReturnTarget.fromTag((CompoundTag) returnTag);
            data.pendingReturns.put(target.playerId(), target);
        }

        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag playerAltarsTag = new ListTag();
        for (Map.Entry<UUID, AltarProfile> entry : altarProfiles.entrySet()) {
            CompoundTag row = new CompoundTag();
            row.putUUID("player", entry.getKey());
            row.putInt("crystalsCrafted", entry.getValue().crystalsCrafted());
            row.putBoolean("crystalPlaced", entry.getValue().crystalPlaced());
            row.put("altarRequirements", toRequirementsTag(entry.getValue().altarRequirements()));
            playerAltarsTag.add(row);
        }
        tag.put("playerAltars", playerAltarsTag);

        tag.putLong("nextInstanceIdV2", nextInstanceId);
        tag.putInt("nextSlotIndexV2", nextSlotIndex);
        tag.putIntArray("freeSlotsV2", freeSlots.stream().mapToInt(Integer::intValue).sorted().toArray());

        ListTag instancesTag = new ListTag();
        for (DungeonInstanceData instance : instances.values()) {
            instancesTag.add(instance.toTag());
        }
        tag.put("instancesV2", instancesTag);

        ListTag runsTag = new ListTag();
        for (ActiveRun run : activeRuns.values()) {
            runsTag.add(run.toTag());
        }
        tag.put("activeRunsV2", runsTag);

        ListTag returnsTag = new ListTag();
        for (ReturnTarget target : pendingReturns.values()) {
            returnsTag.add(target.toTag());
        }
        tag.put("pendingReturnsV2", returnsTag);

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

    public boolean isCrystalPlaced(UUID playerId) {
        return altarProfile(playerId).crystalPlaced();
    }

    public void setCrystalPlaced(UUID playerId, boolean value) {
        AltarProfile profile = altarProfile(playerId);
        putAltarProfile(playerId, profile.withCrystalPlaced(value));
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

    public DungeonInstanceId nextInstanceId() {
        DungeonInstanceId id = new DungeonInstanceId(nextInstanceId);
        nextInstanceId++;
        setDirty();
        return id;
    }

    public int allocateSlot() {
        if (!freeSlots.isEmpty()) {
            int slot = freeSlots.stream().min(Integer::compareTo).orElse(0);
            freeSlots.remove(slot);
            setDirty();
            return slot;
        }

        int slot = nextSlotIndex;
        nextSlotIndex++;
        setDirty();
        return slot;
    }

    public void freeSlot(int slotIndex) {
        if (slotIndex < 0) {
            return;
        }

        freeSlots.add(slotIndex);
        setDirty();
    }

    public void putInstance(DungeonInstanceData instance) {
        instances.put(instance.id().value(), instance);
        setDirty();
    }

    public DungeonInstanceData getInstance(DungeonInstanceId id) {
        return id == null ? null : instances.get(id.value());
    }

    public DungeonInstanceData getInstance(long id) {
        return id <= 0L ? null : instances.get(id);
    }

    public Collection<DungeonInstanceData> instances() {
        return instances.values();
    }

    public void removeInstance(DungeonInstanceId id) {
        if (id != null && instances.remove(id.value()) != null) {
            setDirty();
        }
    }

    public void startRun(UUID playerId, DungeonInstanceId instanceId, ResourceKey<Level> returnDimension, BlockPos returnPos) {
        activeRuns.put(playerId, new ActiveRun(playerId, instanceId, returnDimension.location(), returnPos));
        setDirty();
    }

    public Optional<ActiveRun> getRun(UUID playerId) {
        return Optional.ofNullable(activeRuns.get(playerId));
    }

    public Collection<ActiveRun> activeRuns() {
        return activeRuns.values();
    }

    public List<ActiveRun> activeRunsForInstance(DungeonInstanceId instanceId) {
        if (instanceId == null) {
            return List.of();
        }

        return activeRuns.values().stream()
                .filter(run -> run.instanceId().equals(instanceId))
                .toList();
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

    public record SlotOriginChunks(int chunkX, int chunkZ) {
    }

    public static SlotOriginChunks slotOriginChunks(int slotIndex) {
        int safeSlot = Math.max(0, slotIndex);
        int gridX = safeSlot % 1024;
        int gridZ = safeSlot / 1024;
        int chunkX = gridX * RoguelikeConstants.INSTANCE_SLOT_STRIDE_CHUNKS;
        int chunkZ = gridZ * RoguelikeConstants.INSTANCE_SLOT_STRIDE_CHUNKS;
        return new SlotOriginChunks(chunkX, chunkZ);
    }

    public record AltarProfile(int crystalsCrafted, List<AltarRequirement> altarRequirements, boolean crystalPlaced) {
        public static final AltarProfile EMPTY = new AltarProfile(0, List.of(), false);

        public AltarProfile {
            crystalsCrafted = Math.max(0, crystalsCrafted);
            altarRequirements = altarRequirements == null ? List.of() : List.copyOf(altarRequirements);
        }

        public AltarProfile withCrystalsCrafted(int newValue) {
            return new AltarProfile(newValue, altarRequirements, crystalPlaced);
        }

        public AltarProfile withAltarRequirements(List<AltarRequirement> requirements) {
            return new AltarProfile(crystalsCrafted, requirements, crystalPlaced);
        }

        public AltarProfile withCrystalPlaced(boolean value) {
            return new AltarProfile(crystalsCrafted, altarRequirements, value);
        }
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

    public record ActiveRun(UUID playerId, DungeonInstanceId instanceId, ResourceLocation returnDimension, BlockPos returnPos) {
        public CompoundTag toTag() {
            CompoundTag tag = new CompoundTag();
            tag.putUUID("player", playerId);
            tag.putLong("instanceId", instanceId.value());
            tag.putString("returnDimension", returnDimension.toString());
            tag.putIntArray("returnPos", new int[]{returnPos.getX(), returnPos.getY(), returnPos.getZ()});
            return tag;
        }

        public static ActiveRun fromTag(CompoundTag tag) {
            UUID playerId = tag.hasUUID("player") ? tag.getUUID("player") : UUID.randomUUID();
            long rawInstanceId = Math.max(1L, tag.getLong("instanceId"));

            ResourceLocation returnDimension = ResourceLocation.tryParse(tag.getString("returnDimension"));
            if (returnDimension == null) {
                returnDimension = Level.OVERWORLD.location();
            }

            int[] returnPosArr = tag.getIntArray("returnPos");
            BlockPos returnPos = returnPosArr.length >= 3
                    ? new BlockPos(returnPosArr[0], returnPosArr[1], returnPosArr[2])
                    : BlockPos.ZERO;

            return new ActiveRun(playerId, new DungeonInstanceId(rawInstanceId), returnDimension, returnPos);
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
}
