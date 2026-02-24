package io.github.ozokuz.incore.features.research;

import io.github.ozokuz.incore.features.battlepass.BattlePassTaskHooks;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ResearchProgressService {
    // Legacy player-persistent root key, kept for one-time migration into scope saved data.
    private static final String KEY_ROOT = "incore_research";
    private static final String KEY_UNLOCKED = "unlocked";
    private static final String KEY_TASKS = "tasks";
    private static final String KEY_QUEUE = "queue";
    private static final String KEY_ACTIVE_PROGRESS = "active_progress";
    private static final String KEY_PROGRESS = "progress";

    private ResearchProgressService() {}

    private static OwnerState ownerState(ServerPlayer player) {
        ResearchProgressSavedData data = ResearchProgressSavedData.get(player.serverLevel().getServer());
        String ownerKey = ResearchScopeResolver.ownerKey(player);
        CompoundTag root = data.getOrCreateRoot(ownerKey);

        // One-time migration path from legacy player-persistent storage.
        if (root.isEmpty()) {
            CompoundTag persistent = player.getPersistentData();
            if (persistent.contains(KEY_ROOT, Tag.TAG_COMPOUND)) {
                root.merge(persistent.getCompound(KEY_ROOT).copy());
                data.setDirty();
            }
        }

        migrateLegacyActiveProgress(root);
        return new OwnerState(data, root);
    }

    public static void copyData(ServerPlayer oldPlayer, ServerPlayer newPlayer) {
        // No-op: research data now lives in scope saved data, not cloned player NBT.
    }

    public static Set<ResourceLocation> unlocked(ServerPlayer player) {
        return readSet(ownerState(player).root.getList(KEY_UNLOCKED, Tag.TAG_STRING));
    }

    public static Set<ResourceLocation> unlockedForOwnerKey(MinecraftServer server, String ownerKey) {
        if (ownerKey == null || ownerKey.isBlank()) {
            return Set.of();
        }
        CompoundTag root = ResearchProgressSavedData.get(server).getRoot(ownerKey);
        if (root == null) {
            return Set.of();
        }
        return readSet(root.getList(KEY_UNLOCKED, Tag.TAG_STRING));
    }

    public static Set<ResourceLocation> completedTasks(ServerPlayer player) {
        return readSet(ownerState(player).root.getList(KEY_TASKS, Tag.TAG_STRING));
    }

    public static List<ResourceLocation> queuedResearch(ServerPlayer player) {
        return readList(ownerState(player).root.getList(KEY_QUEUE, Tag.TAG_STRING));
    }

    public static int activeProgress(ServerPlayer player) {
        ResourceLocation active = activeResearch(player);
        return active == null ? 0 : getProgress(ownerState(player).root, active);
    }

    public static ResourceLocation activeResearch(ServerPlayer player) {
        List<ResourceLocation> queue = queuedResearch(player);
        if (queue.isEmpty()) {
            return null;
        }

        ResourceLocation id = queue.get(0);
        ResearchEntryData entry = ResearchEntryManager.all().get(id);
        if (entry == null || !canStartEntry(entry, unlocked(player), completedTasks(player))) {
            return null;
        }
        return id;
    }

    public static int progressFor(ServerPlayer player, ResourceLocation id) {
        if (id == null) {
            return 0;
        }
        return getProgress(ownerState(player).root, id);
    }

    public static Map<ResourceLocation, Integer> progressByEntry(ServerPlayer player) {
        CompoundTag progressTag = progressTag(ownerState(player).root);
        Map<ResourceLocation, Integer> progress = new HashMap<>();
        for (String key : progressTag.getAllKeys()) {
            ResourceLocation id = ResourceLocation.tryParse(key);
            if (id == null) {
                continue;
            }
            int value = Math.max(0, progressTag.getInt(key));
            if (value > 0) {
                progress.put(id, value);
            }
        }
        return progress;
    }

    public static boolean unlock(ServerPlayer player, ResourceLocation id) {
        return enqueueResearch(player, id);
    }

    public static boolean forceUnlockResearch(ServerPlayer player, ResourceLocation id) {
        if (id == null || !ResearchEntryManager.all().containsKey(id)) {
            return false;
        }

        OwnerState ownerState = ownerState(player);
        CompoundTag root = ownerState.root;
        Set<ResourceLocation> unlocked = unlocked(player);
        boolean unlockedChanged = unlocked.add(id);
        if (unlockedChanged) {
            writeSet(root, KEY_UNLOCKED, unlocked);
        }

        List<ResourceLocation> queue = queuedResearch(player);
        boolean queueChanged = queue.removeIf(id::equals);
        if (queueChanged) {
            writeList(root, KEY_QUEUE, queue);
        }
        boolean progressChanged = clearProgress(root, id);

        boolean changed = unlockedChanged || queueChanged || progressChanged;
        if (changed) {
            ownerState.data.setDirty();
            if (unlockedChanged) {
                BattlePassTaskHooks.onResearchCompleted(player);
            }
        }
        return changed;
    }

    public static boolean revokeResearch(ServerPlayer player, ResourceLocation id) {
        if (id == null || !ResearchEntryManager.all().containsKey(id)) {
            return false;
        }

        OwnerState ownerState = ownerState(player);
        CompoundTag root = ownerState.root;
        Set<ResourceLocation> unlocked = unlocked(player);
        boolean unlockedChanged = unlocked.remove(id);
        if (unlockedChanged) {
            writeSet(root, KEY_UNLOCKED, unlocked);
        }

        List<ResourceLocation> queue = queuedResearch(player);
        boolean queueChanged = queue.removeIf(id::equals);
        if (queueChanged) {
            writeList(root, KEY_QUEUE, queue);
        }
        boolean progressChanged = clearProgress(root, id);

        boolean changed = unlockedChanged || queueChanged || progressChanged;
        if (changed) {
            ownerState.data.setDirty();
        }
        return changed;
    }

    public static boolean enqueueResearch(ServerPlayer player, ResourceLocation id) {
        if (id == null) {
            return false;
        }

        Set<ResourceLocation> unlocked = unlocked(player);
        if (unlocked.contains(id)) {
            return false;
        }

        List<ResourceLocation> queue = queuedResearch(player);
        if (queue.contains(id)) {
            return false;
        }

        ResearchEntryData entry = ResearchEntryManager.all().get(id);
        Set<ResourceLocation> completed = completedTasks(player);
        Set<ResourceLocation> unlockedOrQueued = new HashSet<>(unlocked);
        unlockedOrQueued.addAll(queue);
        if (entry == null || !canQueueEntry(entry, unlockedOrQueued, completed)) {
            return false;
        }

        queue.add(id);
        OwnerState ownerState = ownerState(player);
        CompoundTag root = ownerState.root;
        writeList(root, KEY_QUEUE, queue);
        ownerState.data.setDirty();
        return true;
    }

    public static boolean completeTask(ServerPlayer player, ResourceLocation taskId) {
        if (taskId == null || !ManualResearchTaskManager.all().containsKey(taskId)) {
            return false;
        }

        Set<ResourceLocation> completed = completedTasks(player);
        if (completed.add(taskId)) {
            OwnerState ownerState = ownerState(player);
            writeSet(ownerState.root, KEY_TASKS, completed);
            ownerState.data.setDirty();
            return true;
        }

        return false;
    }

    public static boolean dequeueResearch(ServerPlayer player, ResourceLocation id) {
        if (id == null) {
            return false;
        }
        List<ResourceLocation> queue = queuedResearch(player);
        boolean changed = queue.removeIf(id::equals);
        if (!changed) {
            return false;
        }
        OwnerState ownerState = ownerState(player);
        writeList(ownerState.root, KEY_QUEUE, queue);
        ownerState.data.setDirty();
        return true;
    }

    public static void clearQueue(ServerPlayer player) {
        OwnerState ownerState = ownerState(player);
        CompoundTag root = ownerState.root;
        writeList(root, KEY_QUEUE, List.of());
        root.remove(KEY_PROGRESS);
        ownerState.data.setDirty();
    }

    public static boolean reorderQueue(ServerPlayer player, int fromIndex, int toIndex) {
        List<ResourceLocation> queue = queuedResearch(player);
        int size = queue.size();
        if (size < 2) {
            return false;
        }
        if (fromIndex < 0 || fromIndex >= size || toIndex < 0 || toIndex >= size || fromIndex == toIndex) {
            return false;
        }

        ResourceLocation moved = queue.remove(fromIndex);
        queue.add(toIndex, moved);
        if (!isValidQueueOrder(queue, unlocked(player))) {
            return false;
        }

        OwnerState ownerState = ownerState(player);
        CompoundTag root = ownerState.root;
        writeList(root, KEY_QUEUE, queue);
        ownerState.data.setDirty();
        return true;
    }

    public static boolean resetAllResearch(ServerPlayer player) {
        ResearchProgressSavedData data = ResearchProgressSavedData.get(player.serverLevel().getServer());
        String ownerKey = ResearchScopeResolver.ownerKey(player);
        CompoundTag existing = data.getRoot(ownerKey);
        boolean hadResearch = existing != null && !existing.isEmpty();
        boolean removed = data.removeOwner(ownerKey);
        if (removed) {
            data.setDirty();
        }
        return hadResearch;
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

        if (completed.add(taskId)) {
            OwnerState ownerState = ownerState(player);
            writeSet(ownerState.root, KEY_TASKS, completed);
            ownerState.data.setDirty();
        }
        return true;
    }

    public static boolean addResearchProgress(ServerPlayer player, ResourceLocation id, int amount) {
        if (id == null || amount <= 0) {
            return false;
        }
        OwnerState ownerState = ownerState(player);
        CompoundTag root = ownerState.root;
        Set<ResourceLocation> unlocked = unlocked(player);
        Set<ResourceLocation> completedTasks = completedTasks(player);
        List<ResourceLocation> queue = queuedResearch(player);
        boolean changed = false;

        changed = normalizeQueue(root, queue, unlocked) || changed;

        if (queue.isEmpty()) {
            if (changed) {
                writeList(root, KEY_QUEUE, queue);
            }
            return changed;
        }

        ResourceLocation activeId = queue.get(0);
        if (!activeId.equals(id)) {
            if (changed) {
                writeList(root, KEY_QUEUE, queue);
            }
            return changed;
        }

        ResearchEntryData activeEntry = ResearchEntryManager.all().get(activeId);
        if (activeEntry == null) {
            queue.remove(0);
            clearProgress(root, activeId);
            writeList(root, KEY_QUEUE, queue);
            return true;
        }
        if (!canStartEntry(activeEntry, unlocked, completedTasks)) {
            if (changed) {
                writeList(root, KEY_QUEUE, queue);
            }
            return changed;
        }

        int progress = getProgress(root, activeEntry.id()) + amount;
        setProgress(root, activeEntry.id(), progress);
        changed = true;

        changed = unlockReadyHeadWithCallback(root, queue, unlocked, player) || changed;

        if (changed) {
            writeList(root, KEY_QUEUE, queue);
            ownerState.data.setDirty();
        }
        return changed;
    }

    public static boolean tickResearch(ServerPlayer player) {
        OwnerState ownerState = ownerState(player);
        CompoundTag root = ownerState.root;
        Set<ResourceLocation> unlocked = unlocked(player);
        List<ResourceLocation> queue = queuedResearch(player);
        boolean changed = normalizeQueue(root, queue, unlocked);
        changed = unlockReadyHeadWithCallback(root, queue, unlocked, player) || changed;
        if (changed) {
            writeList(root, KEY_QUEUE, queue);
            ownerState.data.setDirty();
        }
        return changed;
    }

    private static boolean canStartEntry(ResearchEntryData entry, Set<ResourceLocation> unlocked, Set<ResourceLocation> completedTasks) {
        if (entry == null) {
            return false;
        }
        return !entry.researchMaterials().isEmpty()
                && unlocked.containsAll(entry.prerequisites())
                && completedTasks.containsAll(entry.requiredTasks());
    }

    private static boolean canQueueEntry(ResearchEntryData entry, Set<ResourceLocation> unlockedOrQueued, Set<ResourceLocation> completedTasks) {
        if (entry == null) {
            return false;
        }
        return !entry.researchMaterials().isEmpty()
                && unlockedOrQueued.containsAll(entry.prerequisites())
                && completedTasks.containsAll(entry.requiredTasks());
    }

    private static boolean isValidQueueOrder(List<ResourceLocation> queue, Set<ResourceLocation> unlocked) {
        for (int i = 0; i < queue.size(); i++) {
            ResourceLocation id = queue.get(i);
            ResearchEntryData entry = ResearchEntryManager.all().get(id);
            if (entry == null) {
                return false;
            }

            for (ResourceLocation prereq : entry.prerequisites()) {
                if (unlocked.contains(prereq)) {
                    continue;
                }
                int prereqIndex = queue.indexOf(prereq);
                if (prereqIndex < 0 || prereqIndex >= i) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean normalizeQueue(CompoundTag root, List<ResourceLocation> queue, Set<ResourceLocation> unlocked) {
        boolean changed = false;
        while (!queue.isEmpty() && unlocked.contains(queue.get(0))) {
            ResourceLocation removed = queue.remove(0);
            clearProgress(root, removed);
            changed = true;
        }
        return changed;
    }

    private static boolean unlockReadyHead(CompoundTag root, List<ResourceLocation> queue, Set<ResourceLocation> unlocked) {
        if (queue.isEmpty()) {
            return false;
        }

        ResourceLocation activeId = queue.get(0);
        ResearchEntryData activeEntry = ResearchEntryManager.all().get(activeId);
        if (activeEntry == null) {
            queue.remove(0);
            clearProgress(root, activeId);
            return true;
        }

        if (getProgress(root, activeId) < activeEntry.cost()) {
            return false;
        }

        unlocked.add(activeId);
        writeSet(root, KEY_UNLOCKED, unlocked);
        queue.remove(0);
        clearProgress(root, activeId);
        return true;
    }

    private static boolean unlockReadyHeadWithCallback(CompoundTag root, List<ResourceLocation> queue, Set<ResourceLocation> unlocked, ServerPlayer player) {
        boolean unlockedSomething = unlockReadyHead(root, queue, unlocked);
        if (unlockedSomething && player != null) {
            BattlePassTaskHooks.onResearchCompleted(player);
        }
        return unlockedSomething;
    }

    private static void migrateLegacyActiveProgress(CompoundTag root) {
        if (!root.contains(KEY_ACTIVE_PROGRESS, Tag.TAG_INT)) {
            return;
        }

        int legacyActiveProgress = Math.max(0, root.getInt(KEY_ACTIVE_PROGRESS));
        if (legacyActiveProgress > 0) {
            List<ResourceLocation> queue = readList(root.getList(KEY_QUEUE, Tag.TAG_STRING));
            if (!queue.isEmpty()) {
                ResourceLocation activeEntry = queue.get(0);
                if (getProgress(root, activeEntry) <= 0) {
                    setProgress(root, activeEntry, legacyActiveProgress);
                }
            }
        }

        root.remove(KEY_ACTIVE_PROGRESS);
    }

    private static int getProgress(CompoundTag root, ResourceLocation id) {
        return Math.max(0, progressTag(root).getInt(id.toString()));
    }

    private static void setProgress(CompoundTag root, ResourceLocation id, int progress) {
        CompoundTag progressTag = progressTag(root);
        String key = id.toString();
        int clamped = Math.max(0, progress);
        if (clamped == 0) {
            progressTag.remove(key);
            return;
        }
        progressTag.putInt(key, clamped);
    }

    private static boolean clearProgress(CompoundTag root, ResourceLocation id) {
        CompoundTag progressTag = progressTag(root);
        String key = id.toString();
        if (!progressTag.contains(key, Tag.TAG_INT)) {
            return false;
        }
        progressTag.remove(key);
        return true;
    }

    private static CompoundTag progressTag(CompoundTag root) {
        if (!root.contains(KEY_PROGRESS, Tag.TAG_COMPOUND)) {
            root.put(KEY_PROGRESS, new CompoundTag());
        }
        return root.getCompound(KEY_PROGRESS);
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

    private static List<ResourceLocation> readList(ListTag listTag) {
        List<ResourceLocation> result = new ArrayList<>();
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
        values.stream().sorted().forEach(id -> listTag.add(StringTag.valueOf(id.toString())));
        root.put(key, listTag);
    }

    private static void writeList(CompoundTag root, String key, List<ResourceLocation> values) {
        ListTag listTag = new ListTag();
        values.forEach(id -> listTag.add(StringTag.valueOf(id.toString())));
        root.put(key, listTag);
    }

    private record OwnerState(ResearchProgressSavedData data, CompoundTag root) {
    }
}
