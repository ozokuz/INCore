package ozokuz.incore.features.playerlevel;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.LinkedHashSet;
import java.util.Set;

public final class PlayerFeatureUnlockService {
    private static final String KEY_UNLOCKED = "incore:player_feature_unlocks";

    private PlayerFeatureUnlockService() {
    }

    public static Set<ResourceLocation> unlocked(ServerPlayer player) {
        return readSet(root(player));
    }

    public static boolean hasUnlocked(ServerPlayer player, ResourceLocation unlockId) {
        return unlocked(player).contains(unlockId);
    }

    public static boolean reconcile(ServerPlayer player) {
        return reconcileUpToLevel(player, PlayerLevelManager.getLevel(player));
    }

    public static boolean reconcileUpToLevel(ServerPlayer player, int levelInclusive) {
        Set<ResourceLocation> unlocked = readSet(root(player));
        boolean changed = false;

        for (PlayerFeatureUnlockDefinition definition : PlayerFeatureUnlockManager.all()) {
            if (definition.requiredLevel() > levelInclusive || unlocked.contains(definition.id())) {
                continue;
            }

            unlocked.add(definition.id());
            changed = true;
            player.sendSystemMessage(Component.translatable(
                    "incore.progression.feature_unlocked",
                    definition.displayName(),
                    definition.requiredLevel()
            ));
        }

        if (changed) {
            writeSet(root(player), unlocked);
        }

        return changed;
    }

    public static void copyData(ServerPlayer from, ServerPlayer to) {
        CompoundTag oldData = from.getPersistentData();
        CompoundTag newData = to.getPersistentData();
        if (oldData.contains(KEY_UNLOCKED, Tag.TAG_LIST)) {
            newData.put(KEY_UNLOCKED, oldData.getList(KEY_UNLOCKED, Tag.TAG_STRING).copy());
        }
    }

    public static int requiredLevel(ResourceLocation unlockId) {
        PlayerFeatureUnlockDefinition definition = PlayerFeatureUnlockManager.get(unlockId);
        return definition == null ? 0 : definition.requiredLevel();
    }

    public static MutableComponent lockedMessage(ResourceLocation unlockId) {
        PlayerFeatureUnlockDefinition definition = PlayerFeatureUnlockManager.get(unlockId);
        if (definition == null) {
            return Component.translatable("incore.progression.locked_generic");
        }
        return Component.translatable("incore.progression.locked_feature", definition.displayName(), definition.requiredLevel());
    }

    private static CompoundTag root(ServerPlayer player) {
        return player.getPersistentData();
    }

    private static Set<ResourceLocation> readSet(CompoundTag root) {
        Set<ResourceLocation> unlocked = new LinkedHashSet<>();
        ListTag tag = root.getList(KEY_UNLOCKED, Tag.TAG_STRING);
        for (int index = 0; index < tag.size(); index++) {
            ResourceLocation id = ResourceLocation.tryParse(tag.getString(index));
            if (id != null) {
                unlocked.add(id);
            }
        }
        return unlocked;
    }

    private static void writeSet(CompoundTag root, Set<ResourceLocation> unlocked) {
        ListTag tag = new ListTag();
        unlocked.stream()
                .map(ResourceLocation::toString)
                .sorted()
                .forEach(value -> tag.add(StringTag.valueOf(value)));
        root.put(KEY_UNLOCKED, tag);
    }
}
