package ozokuz.incore.integration.ldlib.ui.player;

import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;
import ozokuz.incore.features.gacha.GachaRarity;
import ozokuz.incore.features.gacha.GachaService;

final class GachaAppUiSupport {
    static final int SIDEBAR_WIDTH = 178;
    static final int BANNER_ROW_HEIGHT = 30;
    static final int CONTENT_PADDING = 12;
    static final int FOOTER_GAP = 6;
    static final int INFO_PAGE_SIZE = 10;
    static final int GUARANTEE_CARD_WIDTH = 96;
    static final int GUARANTEE_CARD_HEIGHT = 90;
    static final int GUARANTEE_CARD_GAP = 8;
    static final int BANNER_PANEL_FILL = 0x33333333;

    private static final Gson GSON = new Gson();

    private GachaAppUiSupport() {
    }

    static Font font() {
        return Minecraft.getInstance().font;
    }

    static GachaService.ScreenData parseScreenData(@Nullable String json) {
        if (json == null || json.isBlank()) {
            return new GachaService.ScreenData("", List.of());
        }
        try {
            GachaService.ScreenData parsed = GSON.fromJson(json, GachaService.ScreenData.class);
            return parsed == null || parsed.banners() == null
                    ? new GachaService.ScreenData("", List.of())
                    : new GachaService.ScreenData(parsed.selectedBannerId(), List.copyOf(parsed.banners()));
        } catch (Exception ignored) {
            return new GachaService.ScreenData("", List.of());
        }
    }

    static boolean isUiEquivalent(GachaService.ScreenData left, GachaService.ScreenData right) {
        return stableScreenData(left).equals(stableScreenData(right));
    }

    private static GachaService.ScreenData stableScreenData(@Nullable GachaService.ScreenData data) {
        if (data == null || data.banners() == null) {
            return new GachaService.ScreenData("", List.of());
        }
        return new GachaService.ScreenData(data.selectedBannerId(), data.banners().stream()
                .map(GachaAppUiSupport::stableBannerView)
                .toList());
    }

    private static GachaService.BannerView stableBannerView(GachaService.BannerView banner) {
        return new GachaService.BannerView(
                banner.id(),
                banner.name(),
                banner.type(),
                banner.sidebarColor(),
                banner.mainItemId(),
                0L,
                banner.pityFive(),
                banner.pitySix(),
                banner.eventFeaturedPity(),
                banner.basicSelectedSixPity(),
                banner.eventFeaturedPityEnabled(),
                banner.basicGuaranteeBlocked(),
                banner.locked(),
                banner.requiredLevel(),
                banner.basicSelectableSixItems(),
                banner.featuredItems(),
                banner.rewards(),
                banner.permitUsage()
        );
    }

    static @Nullable GachaService.BannerView findBanner(GachaService.ScreenData data, @Nullable String bannerId) {
        String effectiveBannerId = bannerId == null || bannerId.isBlank()
                ? (data == null ? null : data.selectedBannerId())
                : bannerId;
        if (effectiveBannerId == null || effectiveBannerId.isBlank() || data == null || data.banners() == null) {
            return data == null || data.banners().isEmpty() ? null : data.banners().getFirst();
        }
        return data.banners().stream()
                .filter(banner -> banner.id().equals(effectiveBannerId))
                .findFirst()
                .orElse(data.banners().isEmpty() ? null : data.banners().getFirst());
    }

    static Item itemFromId(@Nullable String rawItemId) {
        if (rawItemId == null || rawItemId.isBlank()) {
            return Items.AIR;
        }
        ResourceLocation itemId = ResourceLocation.tryParse(rawItemId);
        return itemId == null ? Items.AIR : BuiltInRegistries.ITEM.get(itemId);
    }

    static ItemStack stackForId(@Nullable String rawItemId) {
        Item item = itemFromId(rawItemId);
        return item == Items.AIR ? ItemStack.EMPTY : item.getDefaultInstance();
    }

    static ItemStack stackForId(@Nullable ResourceLocation itemId) {
        if (itemId == null) {
            return ItemStack.EMPTY;
        }
        Item item = BuiltInRegistries.ITEM.get(itemId);
        return item == Items.AIR ? ItemStack.EMPTY : item.getDefaultInstance();
    }

    static String renderRemainingLabel(GachaService.BannerView banner, long syncedAtMs) {
        if (!"event".equals(banner.type())) {
            return Component.translatable("screen.incore.gacha_banners.rotation.static").getString();
        }
        if (banner.remainingMillis() < 0L) {
            return "";
        }
        long elapsed = Math.max(0L, System.currentTimeMillis() - syncedAtMs);
        long remaining = Math.max(0L, banner.remainingMillis() - elapsed);
        return formatDuration(remaining);
    }

    static String formatDuration(long millis) {
        long totalSeconds = Math.max(0L, millis / 1000L);
        long days = totalSeconds / 86_400L;
        long hours = (totalSeconds % 86_400L) / 3_600L;
        long minutes = (totalSeconds % 3_600L) / 60L;
        long seconds = totalSeconds % 60L;

        if (days > 0L) {
            return String.format(Locale.ROOT, "%dd %02d:%02d:%02d", days, hours, minutes, seconds);
        }
        return String.format(Locale.ROOT, "%02d:%02d:%02d", hours, minutes, seconds);
    }

    static int brightenColor(int color, float amount) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        r = Math.min(255, (int) (r + (255 - r) * amount));
        g = Math.min(255, (int) (g + (255 - g) * amount));
        b = Math.min(255, (int) (b + (255 - b) * amount));
        return (r << 16) | (g << 8) | b;
    }

    static int withAlpha(int alpha, int rgb) {
        return ((alpha & 0xFF) << 24) | (rgb & 0x00FFFFFF);
    }

    static List<Item> uniqueRewardsByRarity(GachaService.BannerView banner, int rarity) {
        LinkedHashSet<Item> items = new LinkedHashSet<>();
        for (GachaService.RewardView reward : banner.rewards()) {
            if (reward.rarity() != rarity) {
                continue;
            }
            Item item = itemFromId(reward.itemId());
            if (item != Items.AIR) {
                items.add(item);
            }
        }
        return new ArrayList<>(items);
    }

    static List<GachaService.RewardView> rewardsForPage(GachaService.BannerView banner, int page) {
        int totalPages = totalRewardPages(banner);
        int clampedPage = Math.clamp(page, 0, totalPages - 1);
        int start = clampedPage * INFO_PAGE_SIZE;
        int end = Math.min(banner.rewards().size(), start + INFO_PAGE_SIZE);
        return banner.rewards().subList(start, end);
    }

    static int totalRewardPages(GachaService.BannerView banner) {
        return Math.max(1, (banner.rewards().size() + INFO_PAGE_SIZE - 1) / INFO_PAGE_SIZE);
    }

    static List<ResourceLocation> selectableSixItems(GachaService.BannerView banner) {
        List<ResourceLocation> result = new ArrayList<>();
        for (String rawId : banner.basicSelectableSixItems()) {
            ResourceLocation parsed = ResourceLocation.tryParse(rawId);
            if (parsed != null && !result.contains(parsed)) {
                result.add(parsed);
            }
        }
        return result;
    }

    static int rarityColor(int rarity) {
        return GachaRarity.fromStars(rarity).rgb();
    }
}
