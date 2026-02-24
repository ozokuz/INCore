package io.github.ozokuz.incore.features.roguelike.instance;

import io.github.ozokuz.incore.features.roguelike.RoguelikeConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record DungeonInstanceData(
        DungeonInstanceId id,
        int slotIndex,
        int originChunkX,
        int originChunkZ,
        ResourceLocation themeId,
        ResourceLocation objectiveId,
        List<ResourceLocation> modifiers,
        long endGameTime,
        State state,
        CleanupStage cleanupStage,
        CleanupMode cleanupMode,
        ResourceLocation portalDimension,
        BlockPos portalPos,
        BlockPos startRoomOrigin,
        BlockPos entryPos,
        long partyId,
        UUID ownerPlayerId
) {
    public DungeonInstanceData {
        slotIndex = Math.max(0, slotIndex);
        modifiers = modifiers == null ? List.of() : List.copyOf(modifiers);
        endGameTime = Math.max(0L, endGameTime);
        state = state == null ? State.CREATED : state;
        cleanupStage = cleanupStage == null ? CleanupStage.NONE : cleanupStage;
        cleanupMode = cleanupMode == null ? CleanupMode.NONE : cleanupMode;
        portalDimension = portalDimension == null ? Level.OVERWORLD.location() : portalDimension;
        portalPos = portalPos == null ? BlockPos.ZERO : portalPos.immutable();
        startRoomOrigin = startRoomOrigin == null ? BlockPos.ZERO : startRoomOrigin.immutable();
        entryPos = entryPos == null ? BlockPos.ZERO : entryPos.immutable();
        partyId = Math.max(0L, partyId);
    }

    public DungeonInstanceData withState(State value) {
        return new DungeonInstanceData(
                id,
                slotIndex,
                originChunkX,
                originChunkZ,
                themeId,
                objectiveId,
                modifiers,
                endGameTime,
                value,
                cleanupStage,
                cleanupMode,
                portalDimension,
                portalPos,
                startRoomOrigin,
                entryPos,
                partyId,
                ownerPlayerId
        );
    }

    public DungeonInstanceData withEndGameTime(long value) {
        return new DungeonInstanceData(
                id,
                slotIndex,
                originChunkX,
                originChunkZ,
                themeId,
                objectiveId,
                modifiers,
                Math.max(0L, value),
                state,
                cleanupStage,
                cleanupMode,
                portalDimension,
                portalPos,
                startRoomOrigin,
                entryPos,
                partyId,
                ownerPlayerId
        );
    }

    public DungeonInstanceData withCleanup(CleanupStage stage, CleanupMode mode) {
        return new DungeonInstanceData(
                id,
                slotIndex,
                originChunkX,
                originChunkZ,
                themeId,
                objectiveId,
                modifiers,
                endGameTime,
                state,
                stage,
                mode,
                portalDimension,
                portalPos,
                startRoomOrigin,
                entryPos,
                partyId,
                ownerPlayerId
        );
    }

    public DungeonInstanceData withCleanupStage(CleanupStage stage) {
        return new DungeonInstanceData(
                id,
                slotIndex,
                originChunkX,
                originChunkZ,
                themeId,
                objectiveId,
                modifiers,
                endGameTime,
                state,
                stage,
                cleanupMode,
                portalDimension,
                portalPos,
                startRoomOrigin,
                entryPos,
                partyId,
                ownerPlayerId
        );
    }

    public DungeonInstanceData withPlacement(BlockPos startOrigin, BlockPos newEntryPos) {
        return new DungeonInstanceData(
                id,
                slotIndex,
                originChunkX,
                originChunkZ,
                themeId,
                objectiveId,
                modifiers,
                endGameTime,
                state,
                cleanupStage,
                cleanupMode,
                portalDimension,
                portalPos,
                startOrigin,
                newEntryPos,
                partyId,
                ownerPlayerId
        );
    }

    public int maxChunkX() {
        return originChunkX + RoguelikeConstants.INSTANCE_SIZE_CHUNKS - 1;
    }

    public int maxChunkZ() {
        return originChunkZ + RoguelikeConstants.INSTANCE_SIZE_CHUNKS - 1;
    }

    public boolean containsBlock(BlockPos pos) {
        int chunkX = pos.getX() >> 4;
        int chunkZ = pos.getZ() >> 4;
        return chunkX >= originChunkX
                && chunkX <= maxChunkX()
                && chunkZ >= originChunkZ
                && chunkZ <= maxChunkZ();
    }

    public int minRegionX() {
        return Math.floorDiv(originChunkX, RoguelikeConstants.REGION_SIZE_CHUNKS);
    }

    public int maxRegionX() {
        return Math.floorDiv(maxChunkX(), RoguelikeConstants.REGION_SIZE_CHUNKS);
    }

    public int minRegionZ() {
        return Math.floorDiv(originChunkZ, RoguelikeConstants.REGION_SIZE_CHUNKS);
    }

    public int maxRegionZ() {
        return Math.floorDiv(maxChunkZ(), RoguelikeConstants.REGION_SIZE_CHUNKS);
    }

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putLong("id", id.value());
        tag.putInt("slotIndex", slotIndex);
        tag.putInt("originChunkX", originChunkX);
        tag.putInt("originChunkZ", originChunkZ);
        tag.putString("themeId", themeId.toString());
        tag.putString("objectiveId", objectiveId.toString());

        ListTag modifierList = new ListTag();
        for (ResourceLocation modifier : modifiers) {
            modifierList.add(net.minecraft.nbt.StringTag.valueOf(modifier.toString()));
        }
        tag.put("modifiers", modifierList);

        tag.putLong("endGameTime", endGameTime);
        tag.putString("state", state.name());
        tag.putString("cleanupStage", cleanupStage.name());
        tag.putString("cleanupMode", cleanupMode.name());
        tag.putString("portalDimension", portalDimension.toString());
        tag.putIntArray("portalPos", new int[]{portalPos.getX(), portalPos.getY(), portalPos.getZ()});
        tag.putIntArray("startRoomOrigin", new int[]{startRoomOrigin.getX(), startRoomOrigin.getY(), startRoomOrigin.getZ()});
        tag.putIntArray("entryPos", new int[]{entryPos.getX(), entryPos.getY(), entryPos.getZ()});
        tag.putLong("partyId", partyId);
        if (ownerPlayerId != null) {
            tag.putUUID("ownerPlayerId", ownerPlayerId);
        }
        return tag;
    }

    public static DungeonInstanceData fromTag(CompoundTag tag) {
        long rawId = tag.getLong("id");
        if (rawId <= 0L) {
            return null;
        }

        ResourceLocation themeId = ResourceLocation.tryParse(tag.getString("themeId"));
        ResourceLocation objectiveId = ResourceLocation.tryParse(tag.getString("objectiveId"));
        if (themeId == null || objectiveId == null) {
            return null;
        }

        List<ResourceLocation> modifiers = new ArrayList<>();
        ListTag modifierTag = tag.getList("modifiers", Tag.TAG_STRING);
        for (Tag entry : modifierTag) {
            ResourceLocation modifier = ResourceLocation.tryParse(entry.getAsString());
            if (modifier != null) {
                modifiers.add(modifier);
            }
        }

        ResourceLocation portalDimension = ResourceLocation.tryParse(tag.getString("portalDimension"));
        if (portalDimension == null) {
            portalDimension = Level.OVERWORLD.location();
        }

        int[] portalPosArr = tag.getIntArray("portalPos");
        BlockPos portalPos = portalPosArr.length >= 3
                ? new BlockPos(portalPosArr[0], portalPosArr[1], portalPosArr[2])
                : BlockPos.ZERO;

        int[] startOriginArr = tag.getIntArray("startRoomOrigin");
        BlockPos startRoomOrigin = startOriginArr.length >= 3
                ? new BlockPos(startOriginArr[0], startOriginArr[1], startOriginArr[2])
                : BlockPos.ZERO;

        int[] entryPosArr = tag.getIntArray("entryPos");
        BlockPos entryPos = entryPosArr.length >= 3
                ? new BlockPos(entryPosArr[0], entryPosArr[1], entryPosArr[2])
                : BlockPos.ZERO;

        long partyId = Math.max(0L, tag.getLong("partyId"));
        UUID ownerPlayerId = tag.hasUUID("ownerPlayerId") ? tag.getUUID("ownerPlayerId") : null;

        return new DungeonInstanceData(
                new DungeonInstanceId(rawId),
                Math.max(0, tag.getInt("slotIndex")),
                tag.getInt("originChunkX"),
                tag.getInt("originChunkZ"),
                themeId,
                objectiveId,
                modifiers,
                Math.max(0L, tag.getLong("endGameTime")),
                State.fromString(tag.getString("state")),
                CleanupStage.fromString(tag.getString("cleanupStage")),
                CleanupMode.fromString(tag.getString("cleanupMode")),
                portalDimension,
                portalPos,
                startRoomOrigin,
                entryPos,
                partyId,
                ownerPlayerId
        );
    }

    public enum State {
        CREATED,
        ACTIVE,
        COMPLETED,
        EXPIRED,
        CLEANING,
        DELETED;

        public static State fromString(String raw) {
            try {
                return State.valueOf(raw);
            } catch (Exception ignored) {
                return CREATED;
            }
        }
    }

    public enum CleanupStage {
        NONE,
        DENY_ENTRY,
        EVICT_PLAYERS,
        WAIT_UNLOAD,
        DELETE_FILES,
        REMOVE_METADATA;

        public static CleanupStage fromString(String raw) {
            try {
                return CleanupStage.valueOf(raw);
            } catch (Exception ignored) {
                return NONE;
            }
        }
    }

    public enum CleanupMode {
        NONE,
        EVICT,
        KILL;

        public static CleanupMode fromString(String raw) {
            try {
                return CleanupMode.valueOf(raw);
            } catch (Exception ignored) {
                return NONE;
            }
        }
    }
}
