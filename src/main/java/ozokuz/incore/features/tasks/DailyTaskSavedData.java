package ozokuz.incore.features.tasks;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DailyTaskSavedData extends SavedData {
    private static final String DATA_NAME = "incore_daily_tasks";

    private long currentDayIndex;
    private final Map<UUID, PlayerDailyData> playerData = new HashMap<>();

    public static DailyTaskSavedData get(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        return overworld.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(DailyTaskSavedData::new, DailyTaskSavedData::load, DataFixTypes.LEVEL),
                DATA_NAME
        );
    }

    public static DailyTaskSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        DailyTaskSavedData data = new DailyTaskSavedData();
        data.currentDayIndex = tag.getLong("currentDayIndex");

        ListTag playersTag = tag.getList("players", Tag.TAG_COMPOUND);
        for (Tag playerTag : playersTag) {
            CompoundTag row = (CompoundTag) playerTag;
            if (!row.hasUUID("uuid")) {
                continue;
            }
            UUID playerId = row.getUUID("uuid");
            PlayerDailyData playerData = PlayerDailyData.fromTag(row);
            data.playerData.put(playerId, playerData);
        }

        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putLong("currentDayIndex", currentDayIndex);

        ListTag playersTag = new ListTag();
        for (Map.Entry<UUID, PlayerDailyData> entry : playerData.entrySet()) {
            CompoundTag row = entry.getValue().toTag();
            row.putUUID("uuid", entry.getKey());
            playersTag.add(row);
        }
        tag.put("players", playersTag);

        return tag;
    }

    public long getCurrentDayIndex() {
        return currentDayIndex;
    }

    public void setCurrentDayIndex(long dayIndex) {
        if (this.currentDayIndex != dayIndex) {
            this.currentDayIndex = dayIndex;
            setDirty();
        }
    }

    public PlayerDailyData getPlayerData(UUID playerId) {
        return playerData.get(playerId);
    }

    public PlayerDailyData getOrCreatePlayerData(UUID playerId) {
        return playerData.computeIfAbsent(playerId, k -> new PlayerDailyData());
    }

    public void setPlayerData(UUID playerId, PlayerDailyData data) {
        playerData.put(playerId, data);
        setDirty();
    }

    public void markDirty() {
        setDirty();
    }

    public static class PlayerDailyData {
        private long dayIndex;
        private boolean login;
        private int shopPurchases;
        private int arenaCompletions;
        private int dungeonCompletions;
        private int vending_machinePurchases;
        private int numismaticsBuys;
        private int numismaticsSells;
        private boolean rewardClaimed;

        public PlayerDailyData() {
        }

        public long getDayIndex() {
            return dayIndex;
        }

        public void setDayIndex(long dayIndex) {
            this.dayIndex = dayIndex;
        }

        public boolean isLogin() {
            return login;
        }

        public void setLogin(boolean login) {
            this.login = login;
        }

        public int getShopPurchases() {
            return shopPurchases;
        }

        public void setShopPurchases(int shopPurchases) {
            this.shopPurchases = shopPurchases;
        }

        public int getArenaCompletions() {
            return arenaCompletions;
        }

        public void setArenaCompletions(int arenaCompletions) {
            this.arenaCompletions = arenaCompletions;
        }

        public int getDungeonCompletions() {
            return dungeonCompletions;
        }

        public void setDungeonCompletions(int dungeonCompletions) {
            this.dungeonCompletions = dungeonCompletions;
        }

        public int getVendingMachinePurchases() {
            return vending_machinePurchases;
        }

        public void setVendingMachinePurchases(int vending_machinePurchases) {
            this.vending_machinePurchases = vending_machinePurchases;
        }

        public int getNumismaticsBuys() {
            return numismaticsBuys;
        }

        public void setNumismaticsBuys(int numismaticsBuys) {
            this.numismaticsBuys = numismaticsBuys;
        }

        public int getNumismaticsSells() {
            return numismaticsSells;
        }

        public void setNumismaticsSells(int numismaticsSells) {
            this.numismaticsSells = numismaticsSells;
        }

        public boolean isRewardClaimed() {
            return rewardClaimed;
        }

        public void setRewardClaimed(boolean rewardClaimed) {
            this.rewardClaimed = rewardClaimed;
        }

        public void reset(long newDayIndex) {
            this.dayIndex = newDayIndex;
            this.login = false;
            this.shopPurchases = 0;
            this.arenaCompletions = 0;
            this.dungeonCompletions = 0;
            this.vending_machinePurchases = 0;
            this.numismaticsBuys = 0;
            this.numismaticsSells = 0;
            this.rewardClaimed = false;
        }

        public int getProgress(DailyTask task) {
            return switch (task) {
                case LOGIN -> login ? 1 : 0;
                case SHOP_PURCHASE -> shopPurchases;
                case ARENA_COMPLETION -> arenaCompletions;
                case DUNGEON_COMPLETION -> dungeonCompletions;
                case VENDING_MACHINE_PURCHASE -> vending_machinePurchases;
                case BUY_FROM_PLAYER -> numismaticsBuys;
                case SELL_TO_PLAYER -> numismaticsSells;
            };
        }

        public CompoundTag toTag() {
            CompoundTag tag = new CompoundTag();
            tag.putLong("dayIndex", dayIndex);
            tag.putBoolean("login", login);
            tag.putInt("shopPurchases", shopPurchases);
            tag.putInt("arenaCompletions", arenaCompletions);
            tag.putInt("dungeonCompletions", dungeonCompletions);
            tag.putInt("vending_machinePurchases", vending_machinePurchases);
            tag.putInt("numismaticsBuys", numismaticsBuys);
            tag.putInt("numismaticsSells", numismaticsSells);
            tag.putBoolean("rewardClaimed", rewardClaimed);
            return tag;
        }

        public static PlayerDailyData fromTag(CompoundTag tag) {
            PlayerDailyData data = new PlayerDailyData();
            data.dayIndex = tag.getLong("dayIndex");
            data.login = tag.getBoolean("login");
            data.shopPurchases = tag.getInt("shopPurchases");
            data.arenaCompletions = tag.getInt("arenaCompletions");
            data.dungeonCompletions = tag.getInt("dungeonCompletions");
            data.vending_machinePurchases = tag.getInt("vending_machinePurchases");
            data.numismaticsBuys = tag.getInt("numismaticsBuys");
            data.numismaticsSells = tag.getInt("numismaticsSells");
            data.rewardClaimed = tag.getBoolean("rewardClaimed");
            return data;
        }
    }
}
