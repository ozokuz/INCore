package io.github.ozokuz.incore.features.roguelike.state;

import io.github.ozokuz.incore.features.roguelike.DungeonDeathDifficulty;
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
import net.minecraft.world.item.ItemStack;
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
    private final Map<Integer, DungeonInstanceData> instancesBySlot = new HashMap<>();
    private final Map<UUID, PlayerDungeonSettings> playerDungeonSettings = new HashMap<>();
    private final Map<Long, ObjectiveStateRecord> objectiveStates = new HashMap<>();
    private final Map<UUID, RecoveryStrongboxRecord> recoveryStrongboxes = new HashMap<>();
    private final Map<UUID, UUID> pendingRecoveryDeliveries = new HashMap<>();
    private final Map<UUID, PendingInventoryRestore> pendingInventoryRestores = new HashMap<>();
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
                data.instancesBySlot.put(instance.slotIndex(), instance);
            }
        }

        ListTag settingsTag = tag.getList("playerDungeonSettingsV1", Tag.TAG_COMPOUND);
        for (Tag settingsEntry : settingsTag) {
            PlayerDungeonSettings settings = PlayerDungeonSettings.fromTag((CompoundTag) settingsEntry);
            if (settings != null) {
                data.playerDungeonSettings.put(settings.playerId(), settings);
            }
        }

        ListTag objectiveStatesTag = tag.getList("objectiveStatesV1", Tag.TAG_COMPOUND);
        for (Tag objectiveTag : objectiveStatesTag) {
            ObjectiveStateRecord record = ObjectiveStateRecord.fromTag((CompoundTag) objectiveTag);
            if (record != null) {
                data.objectiveStates.put(record.instanceId().value(), record);
            }
        }

        ListTag recoveryTag = tag.getList("recoveryStrongboxesV1", Tag.TAG_COMPOUND);
        for (Tag recoveryEntry : recoveryTag) {
            RecoveryStrongboxRecord record = RecoveryStrongboxRecord.fromTag((CompoundTag) recoveryEntry);
            if (record != null) {
                data.recoveryStrongboxes.put(record.recoveryId(), record);
            }
        }

        ListTag pendingRecoveryTag = tag.getList("pendingRecoveryDeliveriesV1", Tag.TAG_COMPOUND);
        for (Tag pendingEntry : pendingRecoveryTag) {
            CompoundTag row = (CompoundTag) pendingEntry;
            if (!row.hasUUID("player") || !row.hasUUID("recoveryId")) {
                continue;
            }
            data.pendingRecoveryDeliveries.put(row.getUUID("player"), row.getUUID("recoveryId"));
        }

        ListTag pendingInventoryTag = tag.getList("pendingInventoryRestoresV1", Tag.TAG_COMPOUND);
        for (Tag pendingEntry : pendingInventoryTag) {
            PendingInventoryRestore restore = PendingInventoryRestore.fromTag((CompoundTag) pendingEntry);
            if (restore != null) {
                data.pendingInventoryRestores.put(restore.playerId(), restore);
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

        ListTag settingsTag = new ListTag();
        for (PlayerDungeonSettings settings : playerDungeonSettings.values()) {
            settingsTag.add(settings.toTag());
        }
        tag.put("playerDungeonSettingsV1", settingsTag);

        ListTag objectiveStatesTag = new ListTag();
        for (ObjectiveStateRecord record : objectiveStates.values()) {
            objectiveStatesTag.add(record.toTag());
        }
        tag.put("objectiveStatesV1", objectiveStatesTag);

        ListTag recoveryTag = new ListTag();
        for (RecoveryStrongboxRecord record : recoveryStrongboxes.values()) {
            recoveryTag.add(record.toTag());
        }
        tag.put("recoveryStrongboxesV1", recoveryTag);

        ListTag pendingRecoveryTag = new ListTag();
        for (Map.Entry<UUID, UUID> entry : pendingRecoveryDeliveries.entrySet()) {
            CompoundTag row = new CompoundTag();
            row.putUUID("player", entry.getKey());
            row.putUUID("recoveryId", entry.getValue());
            pendingRecoveryTag.add(row);
        }
        tag.put("pendingRecoveryDeliveriesV1", pendingRecoveryTag);

        ListTag pendingInventoryTag = new ListTag();
        for (PendingInventoryRestore restore : pendingInventoryRestores.values()) {
            pendingInventoryTag.add(restore.toTag());
        }
        tag.put("pendingInventoryRestoresV1", pendingInventoryTag);

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
        instancesBySlot.put(instance.slotIndex(), instance);
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
        if (id != null) {
            DungeonInstanceData removed = instances.remove(id.value());
            if (removed == null) {
                return;
            }
            instancesBySlot.remove(removed.slotIndex());
            objectiveStates.remove(id.value());
            setDirty();
        }
    }

    public DungeonInstanceData getWorldgenInstanceForChunk(int chunkX, int chunkZ) {
        if (chunkX < 0 || chunkZ < 0) {
            return null;
        }

        int gridX = Math.floorDiv(chunkX, RoguelikeConstants.INSTANCE_SLOT_STRIDE_CHUNKS);
        int gridZ = Math.floorDiv(chunkZ, RoguelikeConstants.INSTANCE_SLOT_STRIDE_CHUNKS);
        int slotIndex = gridX + (gridZ * 1024);
        DungeonInstanceData instance = instancesBySlot.get(slotIndex);
        if (instance == null || instance.state() == DungeonInstanceData.State.CLEANING) {
            return null;
        }

        return instance.containsChunk(chunkX, chunkZ) ? instance : null;
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

    public DungeonDeathDifficulty dungeonDeathDifficulty(UUID playerId) {
        if (playerId == null) {
            return DungeonDeathDifficulty.SOFTCORE;
        }
        return playerDungeonSettings.getOrDefault(playerId, PlayerDungeonSettings.DEFAULT.withPlayerId(playerId)).deathDifficulty();
    }

    public void setDungeonDeathDifficulty(UUID playerId, DungeonDeathDifficulty difficulty) {
        if (playerId == null) {
            return;
        }
        playerDungeonSettings.put(playerId, new PlayerDungeonSettings(playerId, difficulty == null ? DungeonDeathDifficulty.SOFTCORE : difficulty));
        setDirty();
    }

    public ObjectiveStateRecord objectiveState(DungeonInstanceId instanceId) {
        return instanceId == null ? null : objectiveStates.get(instanceId.value());
    }

    public void putObjectiveState(ObjectiveStateRecord record) {
        if (record == null) {
            return;
        }
        objectiveStates.put(record.instanceId().value(), record);
        setDirty();
    }

    public void removeObjectiveState(DungeonInstanceId instanceId) {
        if (instanceId != null && objectiveStates.remove(instanceId.value()) != null) {
            setDirty();
        }
    }

    public RecoveryStrongboxRecord recoveryStrongbox(UUID recoveryId) {
        return recoveryId == null ? null : recoveryStrongboxes.get(recoveryId);
    }

    public void putRecoveryStrongbox(RecoveryStrongboxRecord record) {
        if (record == null) {
            return;
        }
        recoveryStrongboxes.put(record.recoveryId(), record);
        setDirty();
    }

    public void removeRecoveryStrongbox(UUID recoveryId) {
        if (recoveryId != null && recoveryStrongboxes.remove(recoveryId) != null) {
            pendingRecoveryDeliveries.entrySet().removeIf(entry -> recoveryId.equals(entry.getValue()));
            setDirty();
        }
    }

    public void setPendingRecoveryDelivery(UUID playerId, UUID recoveryId) {
        if (playerId == null || recoveryId == null) {
            return;
        }
        pendingRecoveryDeliveries.put(playerId, recoveryId);
        setDirty();
    }

    public Optional<UUID> takePendingRecoveryDelivery(UUID playerId) {
        UUID recoveryId = pendingRecoveryDeliveries.remove(playerId);
        if (recoveryId != null) {
            setDirty();
        }
        return Optional.ofNullable(recoveryId);
    }

    public void setPendingInventoryRestore(PendingInventoryRestore restore) {
        if (restore == null) {
            return;
        }
        pendingInventoryRestores.put(restore.playerId(), restore);
        setDirty();
    }

    public Optional<PendingInventoryRestore> takePendingInventoryRestore(UUID playerId) {
        PendingInventoryRestore restore = pendingInventoryRestores.remove(playerId);
        if (restore != null) {
            setDirty();
        }
        return Optional.ofNullable(restore);
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

    public record AltarRequirement(UUID id, ResourceLocation offeringId, int requiredAmount, int submittedAmount) {
        public AltarRequirement(ResourceLocation offeringId, int requiredAmount, int submittedAmount) {
            this(UUID.randomUUID(), offeringId, requiredAmount, submittedAmount);
        }

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

            return new AltarRequirement(id, offeringId, requiredAmount, Math.min(requiredAmount, submittedAmount + amount));
        }

        public CompoundTag toTag() {
            CompoundTag tag = new CompoundTag();
            tag.putUUID("id", id);
            tag.putString("offeringId", offeringId.toString());
            tag.putInt("required", requiredAmount);
            tag.putInt("submitted", submittedAmount);
            return tag;
        }

        public static AltarRequirement fromTag(CompoundTag tag) {
            UUID id = tag.hasUUID("id") ? tag.getUUID("id") : UUID.randomUUID();
            ResourceLocation offeringId = ResourceLocation.tryParse(tag.getString("offeringId"));
            if (offeringId == null) {
                return null;
            }

            int required = Math.max(1, tag.getInt("required"));
            int submitted = Math.max(0, Math.min(required, tag.getInt("submitted")));
            return new AltarRequirement(id, offeringId, required, submitted);
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

    public record PlayerDungeonSettings(UUID playerId, DungeonDeathDifficulty deathDifficulty) {
        public static final PlayerDungeonSettings DEFAULT = new PlayerDungeonSettings(new UUID(0L, 0L), DungeonDeathDifficulty.SOFTCORE);

        public PlayerDungeonSettings withPlayerId(UUID playerId) {
            return new PlayerDungeonSettings(playerId, deathDifficulty);
        }

        public CompoundTag toTag() {
            CompoundTag tag = new CompoundTag();
            tag.putUUID("player", playerId);
            tag.putString("deathDifficulty", deathDifficulty.name());
            return tag;
        }

        public static PlayerDungeonSettings fromTag(CompoundTag tag) {
            if (!tag.hasUUID("player")) {
                return null;
            }
            return new PlayerDungeonSettings(tag.getUUID("player"), DungeonDeathDifficulty.fromString(tag.getString("deathDifficulty")));
        }
    }

    public record ObjectiveStateRecord(
            DungeonInstanceId instanceId,
            String type,
            int target,
            int rewardCrates,
            Set<BlockPos> requiredPylons,
            List<BlockPos> objectiveAltars,
            Set<Long> openedChests,
            Map<UUID, PlayerObjectiveProgress> playerProgress
    ) {
        public ObjectiveStateRecord {
            type = type == null ? "" : type;
            target = Math.max(1, target);
            rewardCrates = Math.max(1, rewardCrates);
            requiredPylons = copyPositions(requiredPylons);
            objectiveAltars = copyPositionList(objectiveAltars);
            openedChests = openedChests == null ? Set.of() : Set.copyOf(openedChests);
            playerProgress = playerProgress == null ? Map.of() : Map.copyOf(playerProgress);
        }

        public PlayerObjectiveProgress progress(UUID playerId) {
            return playerProgress.getOrDefault(playerId, PlayerObjectiveProgress.EMPTY);
        }

        public ObjectiveStateRecord withObjectiveAltars(List<BlockPos> value) {
            return new ObjectiveStateRecord(instanceId, type, target, rewardCrates, requiredPylons, value, openedChests, playerProgress);
        }

        public ObjectiveStateRecord withOpenedChest(BlockPos pos) {
            Set<Long> updated = new HashSet<>(openedChests);
            updated.add(pos.asLong());
            return new ObjectiveStateRecord(instanceId, type, target, rewardCrates, requiredPylons, objectiveAltars, updated, playerProgress);
        }

        public ObjectiveStateRecord withPlayerProgress(UUID playerId, PlayerObjectiveProgress progress) {
            Map<UUID, PlayerObjectiveProgress> updated = new HashMap<>(playerProgress);
            updated.put(playerId, progress);
            return new ObjectiveStateRecord(instanceId, type, target, rewardCrates, requiredPylons, objectiveAltars, openedChests, updated);
        }

        public CompoundTag toTag() {
            CompoundTag tag = new CompoundTag();
            tag.putLong("instanceId", instanceId.value());
            tag.putString("type", type);
            tag.putInt("target", target);
            tag.putInt("rewardCrates", rewardCrates);
            tag.put("requiredPylons", writeBlockPosList(requiredPylons));
            tag.put("objectiveAltars", writeBlockPosList(objectiveAltars));
            tag.putLongArray("openedChests", openedChests.stream().mapToLong(Long::longValue).toArray());
            ListTag playersTag = new ListTag();
            for (Map.Entry<UUID, PlayerObjectiveProgress> entry : playerProgress.entrySet()) {
                CompoundTag row = entry.getValue().toTag();
                row.putUUID("player", entry.getKey());
                playersTag.add(row);
            }
            tag.put("players", playersTag);
            return tag;
        }

        public static ObjectiveStateRecord fromTag(CompoundTag tag) {
            long rawId = tag.getLong("instanceId");
            if (rawId <= 0L) {
                return null;
            }
            Map<UUID, PlayerObjectiveProgress> players = new HashMap<>();
            ListTag playersTag = tag.getList("players", Tag.TAG_COMPOUND);
            for (Tag playerTag : playersTag) {
                CompoundTag row = (CompoundTag) playerTag;
                if (!row.hasUUID("player")) {
                    continue;
                }
                players.put(row.getUUID("player"), PlayerObjectiveProgress.fromTag(row));
            }
            Set<Long> openedChests = new HashSet<>();
            for (long value : tag.getLongArray("openedChests")) {
                openedChests.add(value);
            }
            return new ObjectiveStateRecord(
                    new DungeonInstanceId(rawId),
                    tag.getString("type"),
                    Math.max(1, tag.getInt("target")),
                    Math.max(1, tag.getInt("rewardCrates")),
                    readBlockPosSet(tag.getList("requiredPylons", Tag.TAG_COMPOUND)),
                    readBlockPosList(tag.getList("objectiveAltars", Tag.TAG_COMPOUND)),
                    openedChests,
                    players
            );
        }
    }

    public record PlayerObjectiveProgress(int progress, ObjectiveResolution resolution, Set<BlockPos> activatedPylons) {
        public static final PlayerObjectiveProgress EMPTY = new PlayerObjectiveProgress(0, ObjectiveResolution.ACTIVE, Set.of());

        public PlayerObjectiveProgress {
            progress = Math.max(0, progress);
            resolution = resolution == null ? ObjectiveResolution.ACTIVE : resolution;
            activatedPylons = copyPositions(activatedPylons);
        }

        public boolean resolved() {
            return resolution != ObjectiveResolution.ACTIVE;
        }

        public CompoundTag toTag() {
            CompoundTag tag = new CompoundTag();
            tag.putInt("progress", progress);
            tag.putString("resolution", resolution.name());
            tag.put("activatedPylons", writeBlockPosList(activatedPylons));
            return tag;
        }

        public static PlayerObjectiveProgress fromTag(CompoundTag tag) {
            return new PlayerObjectiveProgress(
                    Math.max(0, tag.getInt("progress")),
                    ObjectiveResolution.fromString(tag.getString("resolution")),
                    readBlockPosSet(tag.getList("activatedPylons", Tag.TAG_COMPOUND))
            );
        }
    }

    public enum ObjectiveResolution {
        ACTIVE,
        COMPLETED,
        FAILED,
        ABANDONED;

        public static ObjectiveResolution fromString(String raw) {
            try {
                return ObjectiveResolution.valueOf(raw);
            } catch (IllegalArgumentException ignored) {
                return ACTIVE;
            }
        }
    }

    public record RecoveryStrongboxRecord(UUID recoveryId, UUID ownerId, List<ItemStackRecord> contents, boolean opened) {
        public RecoveryStrongboxRecord {
            contents = contents == null ? List.of() : List.copyOf(contents);
        }

        public RecoveryStrongboxRecord withOpened(boolean opened) {
            return new RecoveryStrongboxRecord(recoveryId, ownerId, contents, opened);
        }

        public CompoundTag toTag() {
            CompoundTag tag = new CompoundTag();
            tag.putUUID("recoveryId", recoveryId);
            tag.putUUID("ownerId", ownerId);
            tag.putBoolean("opened", opened);
            ListTag contentsTag = new ListTag();
            for (ItemStackRecord record : contents) {
                contentsTag.add(record.toTag());
            }
            tag.put("contents", contentsTag);
            return tag;
        }

        public static RecoveryStrongboxRecord fromTag(CompoundTag tag) {
            if (!tag.hasUUID("recoveryId") || !tag.hasUUID("ownerId")) {
                return null;
            }
            List<ItemStackRecord> contents = new ArrayList<>();
            for (Tag contentTag : tag.getList("contents", Tag.TAG_COMPOUND)) {
                ItemStackRecord record = ItemStackRecord.fromTag((CompoundTag) contentTag);
                if (record != null) {
                    contents.add(record);
                }
            }
            return new RecoveryStrongboxRecord(tag.getUUID("recoveryId"), tag.getUUID("ownerId"), contents, tag.getBoolean("opened"));
        }
    }

    public record PendingInventoryRestore(UUID playerId, int selectedSlot, List<ItemStackRecord> contents) {
        public PendingInventoryRestore {
            selectedSlot = Math.max(0, selectedSlot);
            contents = contents == null ? List.of() : List.copyOf(contents);
        }

        public CompoundTag toTag() {
            CompoundTag tag = new CompoundTag();
            tag.putUUID("player", playerId);
            tag.putInt("selectedSlot", selectedSlot);
            ListTag contentsTag = new ListTag();
            for (ItemStackRecord record : contents) {
                contentsTag.add(record.toTag());
            }
            tag.put("contents", contentsTag);
            return tag;
        }

        public static PendingInventoryRestore fromTag(CompoundTag tag) {
            if (!tag.hasUUID("player")) {
                return null;
            }
            List<ItemStackRecord> contents = new ArrayList<>();
            for (Tag contentTag : tag.getList("contents", Tag.TAG_COMPOUND)) {
                ItemStackRecord record = ItemStackRecord.fromTag((CompoundTag) contentTag);
                if (record != null) {
                    contents.add(record);
                }
            }
            return new PendingInventoryRestore(tag.getUUID("player"), Math.max(0, tag.getInt("selectedSlot")), contents);
        }
    }

    public record ItemStackRecord(int slot, CompoundTag stackTag) {
        public ItemStackRecord {
            slot = Math.max(-1, slot);
            stackTag = stackTag == null ? new CompoundTag() : stackTag.copy();
        }

        public CompoundTag toTag() {
            CompoundTag tag = new CompoundTag();
            tag.putInt("slot", slot);
            tag.put("stack", stackTag.copy());
            return tag;
        }

        public static ItemStackRecord fromTag(CompoundTag tag) {
            return tag.contains("stack", Tag.TAG_COMPOUND)
                    ? new ItemStackRecord(tag.getInt("slot"), tag.getCompound("stack").copy())
                    : null;
        }

        public static ItemStackRecord of(int slot, ItemStack stack, HolderLookup.Provider registries) {
            return new ItemStackRecord(slot, (CompoundTag) stack.saveOptional(registries));
        }

        public ItemStack toStack(HolderLookup.Provider registries) {
            return ItemStack.parseOptional(registries, stackTag.copy());
        }
    }

    private static ListTag writeBlockPosList(Iterable<BlockPos> positions) {
        ListTag list = new ListTag();
        for (BlockPos pos : positions) {
            CompoundTag row = new CompoundTag();
            row.putInt("x", pos.getX());
            row.putInt("y", pos.getY());
            row.putInt("z", pos.getZ());
            list.add(row);
        }
        return list;
    }

    private static List<BlockPos> readBlockPosList(ListTag tag) {
        List<BlockPos> positions = new ArrayList<>();
        for (Tag rowTag : tag) {
            CompoundTag row = (CompoundTag) rowTag;
            positions.add(new BlockPos(row.getInt("x"), row.getInt("y"), row.getInt("z")));
        }
        return List.copyOf(positions);
    }

    private static Set<BlockPos> readBlockPosSet(ListTag tag) {
        return Set.copyOf(readBlockPosList(tag));
    }

    private static Set<BlockPos> copyPositions(Iterable<BlockPos> positions) {
        Set<BlockPos> copy = new HashSet<>();
        if (positions == null) {
            return Set.of();
        }
        for (BlockPos pos : positions) {
            if (pos != null) {
                copy.add(pos.immutable());
            }
        }
        return Set.copyOf(copy);
    }

    private static List<BlockPos> copyPositionList(Iterable<BlockPos> positions) {
        List<BlockPos> copy = new ArrayList<>();
        if (positions == null) {
            return List.of();
        }
        for (BlockPos pos : positions) {
            if (pos != null) {
                copy.add(pos.immutable());
            }
        }
        return List.copyOf(copy);
    }
}
