package io.github.ozokuz.incore.features.gacha;

import com.google.gson.Gson;
import io.github.ozokuz.incore.Registration;
import io.github.ozokuz.incore.features.gacha.GachaBannerData.BannerType;
import io.github.ozokuz.incore.features.gacha.network.GachaNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

public final class GachaService {
    public static final int PULLS_PER_CRATE = 10;
    public static final int FIVE_STAR_PITY_THRESHOLD = 40;
    public static final int SIX_STAR_PITY_THRESHOLD = 80;

    private static final Gson GSON = new Gson();

    private GachaService() {
    }

    public static void openBannerScreen(ServerPlayer player) {
        GachaBannerData selected = resolveSelectedBanner(player, true);
        if (selected == null) {
            player.sendSystemMessage(Component.translatable("incore.gacha.banner.none_configured"));
            return;
        }

        ScreenData screenData = new ScreenData(
                GachaBannerManager.all().stream()
                        .sorted(
                                Comparator.comparingInt((GachaBannerData banner) -> banner.bannerType() == BannerType.EVENT ? 0 : 1)
                                        .thenComparing(GachaBannerData::name)
                        )
                        .map(banner -> toBannerView(player, banner))
                        .toList()
        );

        GachaNetworking.openBannerScreen(player, GSON.toJson(screenData));
    }

    public static void setSelectedBanner(ServerPlayer player, ResourceLocation bannerId) {
        GachaBannerData banner = GachaBannerManager.get(bannerId);
        if (banner == null) {
            player.sendSystemMessage(Component.translatable("incore.gacha.banner.invalid", bannerId.toString()));
            return;
        }

        GachaPityManager.setSelectedBanner(player, banner.id());
        player.sendSystemMessage(Component.translatable("incore.gacha.banner.selected", banner.name()));
    }

    public static void acquireCrateForBanner(ServerPlayer player, ResourceLocation bannerId) {
        GachaBannerData banner = GachaBannerManager.get(bannerId);
        if (banner == null) {
            player.sendSystemMessage(Component.translatable("incore.gacha.banner.invalid", bannerId.toString()));
            return;
        }

        if (!consumePermits(player, banner)) {
            int available = countTotalPermitsForBanner(player, banner);
            player.sendSystemMessage(Component.translatable(
                    "incore.gacha.purchase.not_enough_permits",
                    PULLS_PER_CRATE,
                    banner.name(),
                    available
            ));
            return;
        }

        ItemStack crate = GachaCrateData.createBannerCrate(banner.id(), 1);
        if (!player.addItem(crate)) {
            player.drop(crate, false);
        }

        GachaPityManager.setSelectedBanner(player, banner.id());
        player.sendSystemMessage(Component.translatable("incore.gacha.purchase.success", banner.name(), PULLS_PER_CRATE));
    }

    public static boolean pullCrateForBanner(ServerPlayer player, ResourceLocation bannerId, double x, double y, double z) {
        GachaBannerData banner = GachaBannerManager.get(bannerId);
        if (banner == null) {
            player.sendSystemMessage(Component.translatable("incore.gacha.banner.invalid", bannerId.toString()));
            return false;
        }

        PullOutcome outcome = doPull(player, banner);
        for (ItemStack reward : outcome.rewards()) {
            if (!player.addItem(reward)) {
                player.drop(reward, false);
            }
        }

        GachaAnimationManager.start(player.serverLevel(), x, y, z, outcome.rarities(), outcome.bestRarity());
        player.sendSystemMessage(Component.translatable(
                "incore.gacha.pull.opened",
                banner.name(),
                outcome.bestRarity(),
                outcome.pityFive(),
                FIVE_STAR_PITY_THRESHOLD,
                outcome.pitySix(),
                SIX_STAR_PITY_THRESHOLD
        ));
        return true;
    }

    private static PullOutcome doPull(ServerPlayer player, GachaBannerData banner) {
        GachaPityManager.PityState pity = GachaPityManager.getPity(player, banner);
        int pity5 = pity.fiveStarMisses();
        int pity6 = pity.sixStarMisses();

        List<ItemStack> rewards = new ArrayList<>();
        List<Integer> rarities = new ArrayList<>();
        int bestRarity = 2;

        for (int i = 0; i < PULLS_PER_CRATE; i++) {
            int minimumRarity = 2;
            if (pity6 >= SIX_STAR_PITY_THRESHOLD - 1) {
                minimumRarity = 6;
            } else if (pity5 >= FIVE_STAR_PITY_THRESHOLD - 1) {
                minimumRarity = 5;
            }

            GachaRewardEntry entry = banner.roll(player.getRandom(), minimumRarity);
            ItemStack rolled = entry.createStack(player.getRandom());
            if (!rolled.isEmpty()) {
                rewards.add(rolled);
            }

            int rarity = entry.rarity();
            rarities.add(rarity);
            bestRarity = Math.max(bestRarity, rarity);

            if (rarity >= 6) {
                pity6 = 0;
            } else {
                pity6++;
            }

            if (rarity >= 5) {
                pity5 = 0;
            } else {
                pity5++;
            }
        }

        GachaPityManager.setPity(player, banner, pity5, pity6);
        return new PullOutcome(rewards, rarities, bestRarity, pity5, pity6);
    }

    private static boolean consumePermits(ServerPlayer player, GachaBannerData banner) {
        if (player.isCreative()) {
            return true;
        }

        int available = countTotalPermitsForBanner(player, banner);
        if (available < PULLS_PER_CRATE) {
            return false;
        }

        int remaining = PULLS_PER_CRATE;
        if (banner.bannerType() == BannerType.BASIC) {
            remaining = consumeMatching(player, remaining, stack -> stack.getItem() == Registration.BASIC_BANNER_PERMIT_ITEM.get());
        } else {
            remaining = consumeMatching(player, remaining, stack -> stack.getItem() == Registration.BANNER_PERMIT_ITEM.get()
                    && GachaPermitItem.matchesBanner(stack, banner.id()));
            if (remaining > 0) {
                remaining = consumeMatching(player, remaining, stack -> stack.getItem() == Registration.CHARTERED_BANNER_PERMIT_ITEM.get());
            }
        }

        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
        return remaining <= 0;
    }

    private static int countTotalPermitsForBanner(ServerPlayer player, GachaBannerData banner) {
        if (player.isCreative()) {
            return PULLS_PER_CRATE;
        }

        if (banner.bannerType() == BannerType.BASIC) {
            return countMatching(player, stack -> stack.getItem() == Registration.BASIC_BANNER_PERMIT_ITEM.get());
        }

        int specific = countMatching(player, stack -> stack.getItem() == Registration.BANNER_PERMIT_ITEM.get()
                && GachaPermitItem.matchesBanner(stack, banner.id()));
        int chartered = countMatching(player, stack -> stack.getItem() == Registration.CHARTERED_BANNER_PERMIT_ITEM.get());
        return specific + chartered;
    }

    private static int countMatching(ServerPlayer player, Predicate<ItemStack> matcher) {
        int total = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && matcher.test(stack)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    private static int consumeMatching(ServerPlayer player, int required, Predicate<ItemStack> matcher) {
        int remaining = required;
        for (int i = 0; i < player.getInventory().getContainerSize() && remaining > 0; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.isEmpty() || !matcher.test(stack)) {
                continue;
            }

            int consume = Math.min(remaining, stack.getCount());
            stack.shrink(consume);
            remaining -= consume;
            if (stack.isEmpty()) {
                player.getInventory().setItem(i, ItemStack.EMPTY);
            }
        }
        return remaining;
    }

    private static GachaBannerData resolveSelectedBanner(ServerPlayer player, boolean writeBackIfMissing) {
        List<GachaBannerData> all = GachaBannerManager.all();
        if (all.isEmpty()) {
            return null;
        }

        ResourceLocation selectedId = GachaPityManager.getSelectedBanner(player);
        GachaBannerData selected = selectedId == null ? null : GachaBannerManager.get(selectedId);
        if (selected != null) {
            return selected;
        }

        ResourceLocation defaultId = GachaBannerManager.getDefaultBannerId();
        if (defaultId == null) {
            return null;
        }

        selected = GachaBannerManager.get(defaultId);
        if (selected == null) {
            return null;
        }

        if (writeBackIfMissing) {
            GachaPityManager.setSelectedBanner(player, selected.id());
        }
        return selected;
    }

    private static BannerView toBannerView(ServerPlayer player, GachaBannerData banner) {
        GachaPityManager.PityState pityState = GachaPityManager.getPity(player, banner);
        int totalWeight = banner.rewards().stream().mapToInt(GachaRewardEntry::weight).sum();
        double safeTotal = Math.max(1.0D, totalWeight);
        List<RewardView> rewards = banner.rewards().stream()
                .sorted(
                        Comparator.comparingInt(GachaRewardEntry::rarity).reversed()
                                .thenComparing(Comparator.comparingInt(GachaRewardEntry::weight).reversed())
                )
                .map(entry -> new RewardView(
                        entry.itemId().toString(),
                        entry.rarity(),
                        (entry.weight() * 100.0D) / safeTotal
                ))
                .toList();

        return new BannerView(
                banner.id().toString(),
                banner.name(),
                banner.bannerType() == BannerType.BASIC ? "basic" : "event",
                pityState.fiveStarMisses(),
                pityState.sixStarMisses(),
                banner.resolvedFeaturedItems().stream().map(ResourceLocation::toString).toList(),
                rewards
        );
    }

    public static ItemStack createSpecificPermit(ResourceLocation bannerId, int count) {
        Item permitItem = Registration.BANNER_PERMIT_ITEM.get();
        return GachaPermitItem.createBannerPermit(permitItem, bannerId, count);
    }

    public record PullOutcome(
            List<ItemStack> rewards,
            List<Integer> rarities,
            int bestRarity,
            int pityFive,
            int pitySix
    ) {
    }

    public record ScreenData(List<BannerView> banners) {
    }

    public record BannerView(
            String id,
            String name,
            String type,
            int pityFive,
            int pitySix,
            List<String> featuredItems,
            List<RewardView> rewards
    ) {
    }

    public record RewardView(String itemId, int rarity, double chancePercent) {
    }
}
