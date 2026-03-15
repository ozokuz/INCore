package ozokuz.incore.features.gacha;

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
    private static final String KEY_PITY_FEATURED_SIX = "featured_six_star_miss";
    private static final String KEY_PITY_BASIC_SELECTED_SIX_PROGRESS = "basic_selected_six_progress";
    private static final String KEY_PITY_FEATURED_ROTATION_TOKEN = "featured_rotation_token";

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
        CompoundTag groupPityTag = getPityEntry(player, banner.pityKey(), true);
        CompoundTag perBannerTag = getPityEntry(player, perBannerPityKey(banner), true);
        return new PityState(
                Math.max(0, groupPityTag.getInt(KEY_PITY_FIVE)),
                Math.max(0, groupPityTag.getInt(KEY_PITY_SIX)),
                Math.max(0, perBannerTag.getInt(KEY_PITY_FEATURED_SIX)),
                Math.max(0, perBannerTag.getInt(KEY_PITY_BASIC_SELECTED_SIX_PROGRESS)),
                perBannerTag.contains(KEY_PITY_FEATURED_ROTATION_TOKEN, Tag.TAG_STRING)
                        ? perBannerTag.getString(KEY_PITY_FEATURED_ROTATION_TOKEN)
                        : null
        );
    }

    public static void setPity(ServerPlayer player, GachaBannerData banner, int fiveStarMisses, int sixStarMisses) {
        PityState existing = getPity(player, banner);
        setPity(
                player,
                banner,
                fiveStarMisses,
                sixStarMisses,
                existing.featuredSixStarMisses(),
                existing.basicSelectedSixProgress(),
                existing.featuredRotationToken()
        );
    }

    public static void setPity(
            ServerPlayer player,
            GachaBannerData banner,
            int fiveStarMisses,
            int sixStarMisses,
            int featuredSixStarMisses,
            int basicSelectedSixProgress,
            @Nullable String featuredRotationToken
    ) {
        CompoundTag groupPityTag = getPityEntry(player, banner.pityKey(), true);
        groupPityTag.putInt(KEY_PITY_FIVE, Math.max(0, fiveStarMisses));
        groupPityTag.putInt(KEY_PITY_SIX, Math.max(0, sixStarMisses));

        CompoundTag perBannerTag = getPityEntry(player, perBannerPityKey(banner), true);
        perBannerTag.putInt(KEY_PITY_FEATURED_SIX, Math.max(0, featuredSixStarMisses));
        perBannerTag.putInt(KEY_PITY_BASIC_SELECTED_SIX_PROGRESS, Math.max(0, basicSelectedSixProgress));
        if (featuredRotationToken == null || featuredRotationToken.isBlank()) {
            perBannerTag.remove(KEY_PITY_FEATURED_ROTATION_TOKEN);
        } else {
            perBannerTag.putString(KEY_PITY_FEATURED_ROTATION_TOKEN, featuredRotationToken);
        }
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

    private static String perBannerPityKey(GachaBannerData banner) {
        return "banner:" + banner.id();
    }

    public record PityState(
            int fiveStarMisses,
            int sixStarMisses,
            int featuredSixStarMisses,
            int basicSelectedSixProgress,
            @Nullable String featuredRotationToken
    ) {
    }
}
