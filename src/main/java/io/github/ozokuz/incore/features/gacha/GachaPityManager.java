package io.github.ozokuz.incore.features.gacha;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

public final class GachaPityManager {
    private static final String ROOT_GACHA = "incore:gacha";
    private static final String KEY_LAST_BANNER = "last_banner";
    private static final String KEY_SELECTED_BANNER_LEGACY = "selected_banner";
    private static final String KEY_PITY = "pity";
    private static final String KEY_PITY_FIVE = "five_star_miss";
    private static final String KEY_PITY_SIX = "six_star_miss";

    private GachaPityManager() {
    }

    public static @Nullable ResourceLocation getLastBanner(ServerPlayer player) {
        CompoundTag root = getRoot(player, false);
        if (root == null) {
            return null;
        }

        if (root.contains(KEY_LAST_BANNER, Tag.TAG_STRING)) {
            return ResourceLocation.tryParse(root.getString(KEY_LAST_BANNER));
        }

        if (root.contains(KEY_SELECTED_BANNER_LEGACY, Tag.TAG_STRING)) {
            return ResourceLocation.tryParse(root.getString(KEY_SELECTED_BANNER_LEGACY));
        }

        return null;
    }

    public static void setLastBanner(ServerPlayer player, ResourceLocation bannerId) {
        CompoundTag root = getRoot(player, true);
        root.putString(KEY_LAST_BANNER, bannerId.toString());
    }

    public static PityState getPity(ServerPlayer player, GachaBannerData banner) {
        CompoundTag pityTag = getPityEntry(player, banner.pityKey(), true);
        return new PityState(
                Math.max(0, pityTag.getInt(KEY_PITY_FIVE)),
                Math.max(0, pityTag.getInt(KEY_PITY_SIX))
        );
    }

    public static void setPity(ServerPlayer player, GachaBannerData banner, int fiveStarMisses, int sixStarMisses) {
        CompoundTag pityTag = getPityEntry(player, banner.pityKey(), true);
        pityTag.putInt(KEY_PITY_FIVE, Math.max(0, fiveStarMisses));
        pityTag.putInt(KEY_PITY_SIX, Math.max(0, sixStarMisses));
    }

    public static void copyData(ServerPlayer from, ServerPlayer to) {
        CompoundTag fromTag = getRoot(from, false);
        if (fromTag == null) {
            return;
        }

        getRoot(to, true).merge(fromTag.copy());
    }

    private static @Nullable CompoundTag getRoot(ServerPlayer player, boolean create) {
        CompoundTag persistent = player.getPersistentData();
        if (!persistent.contains(ROOT_GACHA, Tag.TAG_COMPOUND)) {
            if (!create) {
                return null;
            }
            persistent.put(ROOT_GACHA, new CompoundTag());
        }
        return persistent.getCompound(ROOT_GACHA);
    }

    private static CompoundTag getPityEntry(ServerPlayer player, String pityKey, boolean create) {
        CompoundTag root = getRoot(player, create);
        if (root == null) {
            return new CompoundTag();
        }

        if (!root.contains(KEY_PITY, Tag.TAG_COMPOUND)) {
            if (!create) {
                return new CompoundTag();
            }
            root.put(KEY_PITY, new CompoundTag());
        }

        CompoundTag pityRoot = root.getCompound(KEY_PITY);
        if (!pityRoot.contains(pityKey, Tag.TAG_COMPOUND)) {
            if (!create) {
                return new CompoundTag();
            }
            pityRoot.put(pityKey, new CompoundTag());
        }

        return pityRoot.getCompound(pityKey);
    }

    public record PityState(int fiveStarMisses, int sixStarMisses) {
    }
}
