package io.github.ozokuz.incore.features.arena.state;

import io.github.ozokuz.incore.features.arena.ArenaConstants;
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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class ArenaSavedData extends SavedData {
    private static final String DATA_NAME = "incore_arena";

    private int nextSlotIndex;
    private final Map<UUID, Integer> playerSlots = new HashMap<>();
    private final Map<UUID, RunRecord> runs = new HashMap<>();
    private final Map<UUID, ReturnTarget> pendingReturns = new HashMap<>();

    public static ArenaSavedData get(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        return overworld.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(ArenaSavedData::new, ArenaSavedData::load, DataFixTypes.LEVEL),
                DATA_NAME
        );
    }

    public static ArenaSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        ArenaSavedData data = new ArenaSavedData();
        data.nextSlotIndex = Math.max(0, tag.getInt("nextSlotIndex"));

        ListTag slotsTag = tag.getList("playerSlots", Tag.TAG_COMPOUND);
        for (Tag slotTag : slotsTag) {
            CompoundTag row = (CompoundTag) slotTag;
            if (!row.hasUUID("player")) {
                continue;
            }

            UUID playerId = row.getUUID("player");
            int slot = Math.max(0, row.getInt("slot"));
            data.playerSlots.put(playerId, slot);
        }

        ListTag runsTag = tag.getList("runs", Tag.TAG_COMPOUND);
        for (Tag runTag : runsTag) {
            RunRecord run = RunRecord.fromTag((CompoundTag) runTag);
            data.runs.put(run.playerId(), run);
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
        tag.putInt("nextSlotIndex", nextSlotIndex);

        ListTag slotsTag = new ListTag();
        for (Map.Entry<UUID, Integer> entry : playerSlots.entrySet()) {
            CompoundTag row = new CompoundTag();
            row.putUUID("player", entry.getKey());
            row.putInt("slot", Math.max(0, entry.getValue()));
            slotsTag.add(row);
        }
        tag.put("playerSlots", slotsTag);

        ListTag runsTag = new ListTag();
        for (RunRecord run : runs.values()) {
            runsTag.add(run.toTag());
        }
        tag.put("runs", runsTag);

        ListTag returnsTag = new ListTag();
        for (ReturnTarget target : pendingReturns.values()) {
            returnsTag.add(target.toTag());
        }
        tag.put("pendingReturns", returnsTag);

        return tag;
    }

    public int getOrAssignSlot(UUID playerId) {
        if (playerSlots.containsKey(playerId)) {
            return playerSlots.get(playerId);
        }

        int slot = nextSlotIndex;
        nextSlotIndex++;
        playerSlots.put(playerId, slot);
        setDirty();
        return slot;
    }

    public Optional<RunRecord> getRun(UUID playerId) {
        return Optional.ofNullable(runs.get(playerId));
    }

    public void putRun(RunRecord run) {
        runs.put(run.playerId(), run);
        setDirty();
    }

    public void clearRun(UUID playerId) {
        if (runs.remove(playerId) != null) {
            setDirty();
        }
    }

    public Optional<RunRecord> findRunByGateway(UUID gatewayEntityId) {
        if (gatewayEntityId == null) {
            return Optional.empty();
        }

        return runs.values().stream()
                .filter(run -> gatewayEntityId.equals(run.gatewayEntityId()))
                .findFirst();
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

    public static BlockPos slotOrigin(int slotIndex) {
        int gridX = slotIndex % 64;
        int gridZ = slotIndex / 64;
        int x = (gridX * ArenaConstants.ARENA_SPACING) + 8;
        int y = ArenaConstants.ARENA_BASE_Y;
        int z = (gridZ * ArenaConstants.ARENA_SPACING) + 8;
        return new BlockPos(x, y, z);
    }

    public enum RunState {
        PREPARED,
        ACTIVE,
        ENDED_SUCCESS,
        ENDED_FAIL;

        public static RunState fromString(String raw) {
            try {
                return RunState.valueOf(raw);
            } catch (Exception ignored) {
                return PREPARED;
            }
        }

        public boolean hasEnded() {
            return this == ENDED_SUCCESS || this == ENDED_FAIL;
        }
    }

    public record RunRecord(
            UUID playerId,
            int slotIndex,
            BlockPos origin,
            ResourceLocation entryId,
            ResourceLocation gatewayId,
            UUID gatewayEntityId,
            RunState state,
            ResourceLocation returnDimension,
            BlockPos returnPos
    ) {
        public RunRecord withState(RunState newState) {
            return new RunRecord(playerId, slotIndex, origin, entryId, gatewayId, gatewayEntityId, newState, returnDimension, returnPos);
        }

        public RunRecord withGatewayEntity(UUID entityId, RunState newState) {
            return new RunRecord(playerId, slotIndex, origin, entryId, gatewayId, entityId, newState, returnDimension, returnPos);
        }

        public ResourceKey<Level> returnDimensionKey() {
            return ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, returnDimension);
        }

        public CompoundTag toTag() {
            CompoundTag tag = new CompoundTag();
            tag.putUUID("player", playerId);
            tag.putInt("slotIndex", slotIndex);
            tag.putIntArray("origin", new int[]{origin.getX(), origin.getY(), origin.getZ()});
            tag.putString("entryId", entryId.toString());
            tag.putString("gatewayId", gatewayId.toString());
            if (gatewayEntityId != null) {
                tag.putUUID("gatewayEntityId", gatewayEntityId);
            }
            tag.putString("state", state.name());
            tag.putString("returnDimension", returnDimension.toString());
            tag.putIntArray("returnPos", new int[]{returnPos.getX(), returnPos.getY(), returnPos.getZ()});
            return tag;
        }

        public static RunRecord fromTag(CompoundTag tag) {
            UUID playerId = tag.hasUUID("player") ? tag.getUUID("player") : UUID.randomUUID();
            int slotIndex = Math.max(0, tag.getInt("slotIndex"));
            int[] originArr = tag.getIntArray("origin");
            BlockPos origin = originArr.length >= 3 ? new BlockPos(originArr[0], originArr[1], originArr[2]) : BlockPos.ZERO;

            ResourceLocation entryId = ResourceLocation.tryParse(tag.getString("entryId"));
            if (entryId == null) {
                entryId = ResourceLocation.fromNamespaceAndPath("incore", "missing_entry");
            }

            ResourceLocation gatewayId = ResourceLocation.tryParse(tag.getString("gatewayId"));
            if (gatewayId == null) {
                gatewayId = ResourceLocation.fromNamespaceAndPath("incore", "missing_gateway");
            }

            UUID gatewayEntityId = tag.hasUUID("gatewayEntityId") ? tag.getUUID("gatewayEntityId") : null;
            RunState state = RunState.fromString(tag.getString("state"));

            ResourceLocation returnDimension = ResourceLocation.tryParse(tag.getString("returnDimension"));
            if (returnDimension == null) {
                returnDimension = Level.OVERWORLD.location();
            }

            int[] returnPosArr = tag.getIntArray("returnPos");
            BlockPos returnPos = returnPosArr.length >= 3 ? new BlockPos(returnPosArr[0], returnPosArr[1], returnPosArr[2]) : BlockPos.ZERO;

            return new RunRecord(playerId, slotIndex, origin, entryId, gatewayId, gatewayEntityId, state, returnDimension, returnPos);
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
