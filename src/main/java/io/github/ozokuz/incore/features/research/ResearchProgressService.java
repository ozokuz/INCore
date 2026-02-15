package io.github.ozokuz.incore.features.research;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;

import java.util.HashSet;
import java.util.Set;

public final class ResearchProgressService {
    private static final String KEY_ROOT = "incore_research";
    private static final String KEY_POINTS = "points";
    private static final String KEY_UNLOCKED = "unlocked";
    private static final String KEY_TASKS = "tasks";

    private ResearchProgressService() {}

    private static CompoundTag root(ServerPlayer player) {
        CompoundTag persistent = player.getPersistentData();
        if (!persistent.contains(KEY_ROOT, Tag.TAG_COMPOUND)) {
            persistent.put(KEY_ROOT, new CompoundTag());
        }
        return persistent.getCompound(KEY_ROOT);
    }

    public static int getPoints(ServerPlayer player) {
        return root(player).getInt(KEY_POINTS);
    }

    public static void addPoints(ServerPlayer player, int amount) {
        CompoundTag root = root(player);
        root.putInt(KEY_POINTS, Math.max(0, root.getInt(KEY_POINTS) + amount));
    }

    public static Set<ResourceLocation> unlocked(ServerPlayer player) {
        return readSet(root(player).getList(KEY_UNLOCKED, Tag.TAG_STRING));
    }

    public static Set<ResourceLocation> completedTasks(ServerPlayer player) {
        return readSet(root(player).getList(KEY_TASKS, Tag.TAG_STRING));
    }

    public static boolean unlock(ServerPlayer player, ResourceLocation id) {
        Set<ResourceLocation> unlocked = unlocked(player);
        if (unlocked.contains(id)) {
            return false;
        }

        ResearchEntryData entry = ResearchEntryManager.all().get(id);
        if (entry == null || getPoints(player) < entry.cost()) {
            return false;
        }

        if (!unlocked.containsAll(entry.prerequisites()) || !completedTasks(player).containsAll(entry.requiredTasks())) {
            return false;
        }

        addPoints(player, -entry.cost());
        unlocked.add(id);
        writeSet(root(player), KEY_UNLOCKED, unlocked);
        return true;
    }

    public static boolean submitTask(ServerPlayer player, ResourceLocation taskId) {
        ManualResearchTaskData task = ManualResearchTaskManager.all().get(taskId);
        if (task == null || task.itemId() == null) {
            return false;
        }

        Set<ResourceLocation> completed = completedTasks(player);
        if (!task.repeatable() && completed.contains(taskId)) {
            return false;
        }

        Item item = BuiltInRegistries.ITEM.get(task.itemId());
        if (item == null) {
            return false;
        }

        if (!removeItems(player.getInventory(), item, task.itemCount())) {
            return false;
        }

        addPoints(player, task.rewardPoints());
        if (!task.repeatable()) {
            completed.add(taskId);
            writeSet(root(player), KEY_TASKS, completed);
        }
        return true;
    }

    private static boolean removeItems(Inventory inventory, Item item, int count) {
        int remaining = count;
        for (int i = 0; i < inventory.getContainerSize() && remaining > 0; i++) {
            var stack = inventory.getItem(i);
            if (!stack.is(item)) {
                continue;
            }
            int removed = Math.min(remaining, stack.getCount());
            stack.shrink(removed);
            remaining -= removed;
        }
        return remaining <= 0;
    }

    private static Set<ResourceLocation> readSet(ListTag listTag) {
        Set<ResourceLocation> result = new HashSet<>();
        for (Tag tag : listTag) {
            ResourceLocation id = ResourceLocation.tryParse(tag.getAsString());
            if (id != null) {
                result.add(id);
            }
        }
        return result;
    }

    private static void writeSet(CompoundTag root, String key, Set<ResourceLocation> values) {
        ListTag listTag = new ListTag();
        values.forEach(id -> listTag.add(StringTag.valueOf(id.toString())));
        root.put(key, listTag);
    }
}
