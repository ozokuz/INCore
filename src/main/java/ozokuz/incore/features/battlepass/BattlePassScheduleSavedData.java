package ozokuz.incore.features.battlepass;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;

public final class BattlePassScheduleSavedData extends SavedData {
    private static final String DATA_NAME = "incore_battlepass_schedule";
    private static final long UNSET_START = Long.MIN_VALUE;

    private String activeSetId = "";
    private long activeStartEpochMillis = UNSET_START;

    public static BattlePassScheduleSavedData get(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        return overworld.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(BattlePassScheduleSavedData::new, BattlePassScheduleSavedData::load, DataFixTypes.LEVEL),
                DATA_NAME
        );
    }

    public static BattlePassScheduleSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        BattlePassScheduleSavedData data = new BattlePassScheduleSavedData();
        data.activeSetId = tag.getString("activeSetId");
        data.activeStartEpochMillis = tag.contains("activeStartEpochMillis", Tag.TAG_LONG)
                ? tag.getLong("activeStartEpochMillis")
                : UNSET_START;
        if (data.activeSetId.isBlank() || data.activeStartEpochMillis == UNSET_START) {
            data.activeSetId = "";
            data.activeStartEpochMillis = UNSET_START;
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        if (!activeSetId.isBlank()) {
            tag.putString("activeSetId", activeSetId);
        }
        if (activeStartEpochMillis != UNSET_START) {
            tag.putLong("activeStartEpochMillis", activeStartEpochMillis);
        }
        return tag;
    }

    public String activeSetId() {
        return activeSetId;
    }

    public long activeStartEpochMillis() {
        return activeStartEpochMillis;
    }

    public boolean hasActiveSet() {
        return !activeSetId.isBlank() && activeStartEpochMillis != UNSET_START;
    }

    public void setActiveSet(String nextActiveSetId, long nextActiveStartEpochMillis) {
        String normalizedId = nextActiveSetId == null ? "" : nextActiveSetId.trim();
        boolean validPair = !normalizedId.isBlank() && nextActiveStartEpochMillis != UNSET_START;
        String effectiveId = validPair ? normalizedId : "";
        long effectiveStart = validPair ? nextActiveStartEpochMillis : UNSET_START;

        if (activeSetId.equals(effectiveId) && activeStartEpochMillis == effectiveStart) {
            return;
        }

        activeSetId = effectiveId;
        activeStartEpochMillis = effectiveStart;
        setDirty();
    }
}
