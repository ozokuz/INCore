package io.github.ozokuz.incore.features.gacha;

import com.google.gson.Gson;
import io.github.ozokuz.incore.Registration;
import io.github.ozokuz.incore.features.battlepass.BattlePassTaskHooks;
import io.github.ozokuz.incore.features.gacha.GachaBannerData.BannerType;
import io.github.ozokuz.incore.features.gacha.network.GachaNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

public final class GachaService {
    public static final int PULLS_PER_CRATE = 10;
    public static final int FIVE_STAR_PITY_THRESHOLD = 40;
    public static final int SIX_STAR_PITY_THRESHOLD = 80;
    public static final int EVENT_FEATURED_SIX_PITY_THRESHOLD = 120;
    public static final int BASIC_SELECTED_SIX_THRESHOLD = 240;

    private static final Gson GSON = new Gson();

    private GachaService() {
    }

    public static void openBannerScreen(ServerPlayer player) {
        GachaBannerData selected = resolveLastBanner(player, true);
        if (selected == null) {
            player.sendSystemMessage(Component.translatable("incore.gacha.banner.none_configured"));
            return;
        }

        ScreenData screenData = new ScreenData(
                GachaBannerManager.visible().stream()
                        .sorted(
                                Comparator.comparingInt((GachaBannerData banner) -> banner.bannerType() == BannerType.EVENT ? 0 : 1)
                                        .thenComparing(GachaBannerData::name)
                        )
                        .map(banner -> toBannerView(player, banner))
                        .toList()
        );

        GachaNetworking.openBannerScreen(player, GSON.toJson(screenData));
    }

    public static void acquireCrateForBanner(ServerPlayer player, ResourceLocation bannerId) {
        GachaBannerData banner = GachaBannerManager.get(bannerId);
        if (banner == null) {
            player.sendSystemMessage(Component.translatable("incore.gacha.banner.invalid", bannerId.toString()));
            return;
        }
        if (!GachaEventRotation.isCurrentlyVisible(banner)) {
            player.sendSystemMessage(Component.translatable("incore.gacha.banner.not_active", banner.name()));
            return;
        }
        if (isBasicGuaranteedSelectionRequired(player, banner)) {
            player.sendSystemMessage(Component.translatable("incore.gacha.basic_guarantee.required", banner.name(), BASIC_SELECTED_SIX_THRESHOLD));
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

        GachaPityManager.setLastBanner(player, banner.id());
        BattlePassTaskHooks.onBannerPermitUsed(player, PULLS_PER_CRATE);
        player.sendSystemMessage(Component.translatable("incore.gacha.purchase.success", banner.name(), PULLS_PER_CRATE));
    }

    public static boolean pullCrateForBanner(ServerPlayer player, ResourceLocation bannerId, double x, double y, double z) {
        GachaBannerData banner = GachaBannerManager.get(bannerId);
        if (banner == null) {
            player.sendSystemMessage(Component.translatable("incore.gacha.banner.invalid", bannerId.toString()));
            return false;
        }
        if (isBasicGuaranteedSelectionRequired(player, banner)) {
            player.sendSystemMessage(Component.translatable("incore.gacha.basic_guarantee.required", banner.name(), BASIC_SELECTED_SIX_THRESHOLD));
            return false;
        }

        PullOutcome outcome = doPull(player, banner);
        GachaAnimationManager.start(
                player.serverLevel(),
                x,
                y,
                z,
                outcome.rarities(),
                outcome.bestRarity(),
                outcome.rewards(),
                outcome.highRarityRewards(),
                player.getGameProfile().getName(),
                banner.name()
        );
        BattlePassTaskHooks.onGachaCrateOpened(player);
        return true;
    }

    public static boolean claimBasicGuaranteedSix(ServerPlayer player, ResourceLocation bannerId, ResourceLocation selectedItemId) {
        GachaBannerData banner = GachaBannerManager.get(bannerId);
        if (banner == null) {
            player.sendSystemMessage(Component.translatable("incore.gacha.banner.invalid", bannerId.toString()));
            return false;
        }
        if (banner.bannerType() != BannerType.BASIC) {
            player.sendSystemMessage(Component.translatable("incore.gacha.basic_guarantee.not_basic", banner.name()));
            return false;
        }

        GachaPityManager.PityState pity = getNormalizedPityState(player, banner, true);
        if (pity.basicSelectedSixProgress() < BASIC_SELECTED_SIX_THRESHOLD) {
            player.sendSystemMessage(Component.translatable(
                    "incore.gacha.basic_guarantee.not_ready",
                    pity.basicSelectedSixProgress(),
                    BASIC_SELECTED_SIX_THRESHOLD
            ));
            return false;
        }

        GachaRewardEntry selectedReward = resolveGuaranteedSixEntryForItem(banner, selectedItemId, player.getRandom());
        if (selectedReward == null) {
            player.sendSystemMessage(Component.translatable("incore.gacha.basic_guarantee.invalid_selection", selectedItemId.toString()));
            return false;
        }

        ItemStack guaranteedReward = selectedReward.createStack(player.getRandom());
        if (guaranteedReward.isEmpty()) {
            player.sendSystemMessage(Component.translatable("incore.gacha.basic_guarantee.invalid_selection", selectedItemId.toString()));
            return false;
        }

        if (!player.addItem(guaranteedReward.copy())) {
            player.drop(guaranteedReward.copy(), false);
        }

        GachaPityManager.setPity(
                player,
                banner,
                pity.fiveStarMisses(),
                pity.sixStarMisses(),
                pity.featuredSixStarMisses(),
                0,
                pity.featuredRotationToken()
        );
        player.sendSystemMessage(Component.translatable(
                "incore.gacha.basic_guarantee.claimed",
                guaranteedReward.getHoverName(),
                banner.name()
        ));
        openBannerScreen(player);
        return true;
    }

    public static boolean setPityForBanner(
            ServerPlayer player,
            ResourceLocation bannerId,
            int pityFive,
            int pitySix,
            int featuredSixMisses,
            int basicSelectedSixProgress
    ) {
        GachaBannerData banner = GachaBannerManager.get(bannerId);
        if (banner == null) {
            return false;
        }

        String featuredToken = banner.bannerType() == BannerType.EVENT
                ? GachaEventRotation.getFeaturedRotationTokenForBanner(banner.id())
                : null;

        GachaPityManager.setPity(
                player,
                banner,
                Math.max(0, pityFive),
                Math.max(0, pitySix),
                Math.max(0, featuredSixMisses),
                Math.max(0, basicSelectedSixProgress),
                featuredToken
        );
        return true;
    }

    public static @Nullable PityView getPityForBanner(ServerPlayer player, ResourceLocation bannerId) {
        GachaBannerData banner = GachaBannerManager.get(bannerId);
        if (banner == null) {
            return null;
        }
        GachaPityManager.PityState pity = getNormalizedPityState(player, banner, true);
        return new PityView(
                banner.id(),
                pity.fiveStarMisses(),
                pity.sixStarMisses(),
                pity.featuredSixStarMisses(),
                pity.basicSelectedSixProgress(),
                pity.featuredRotationToken()
        );
    }

    private static PullOutcome doPull(ServerPlayer player, GachaBannerData banner) {
        GachaPityManager.PityState pity = getNormalizedPityState(player, banner, true);
        int pity5 = pity.fiveStarMisses();
        int pity6 = pity.sixStarMisses();
        int featured6 = pity.featuredSixStarMisses();
        int basicSelectedSixProgress = pity.basicSelectedSixProgress();
        String featuredToken = pity.featuredRotationToken();

        ResourceLocation featuredMainSixItem = resolveEventMainFeaturedSixItem(banner);
        boolean eventFeaturedGuaranteeEnabled = featuredMainSixItem != null;

        List<ItemStack> rewards = new ArrayList<>();
        List<Integer> rarities = new ArrayList<>();
        List<HighRarityReward> highRarityRewards = new ArrayList<>();
        int bestRarity = 2;

        for (int i = 0; i < PULLS_PER_CRATE; i++) {
            GachaRewardEntry entry;
            if (eventFeaturedGuaranteeEnabled && featured6 >= EVENT_FEATURED_SIX_PITY_THRESHOLD - 1) {
                entry = resolveGuaranteedSixEntryForItem(banner, featuredMainSixItem, player.getRandom());
            } else {
                int minimumRarity = 2;
                if (pity6 >= SIX_STAR_PITY_THRESHOLD - 1) {
                    minimumRarity = 6;
                } else if (pity5 >= FIVE_STAR_PITY_THRESHOLD - 1) {
                    minimumRarity = 5;
                }
                entry = banner.roll(player.getRandom(), minimumRarity);
            }

            if (entry == null) {
                entry = banner.roll(player.getRandom(), 2);
            }

            ItemStack rolled = entry.createStack(player.getRandom());
            int rarity = entry.rarity();
            if (!rolled.isEmpty()) {
                rewards.add(rolled);
                if (rarity >= 5) {
                    highRarityRewards.add(new HighRarityReward(rolled.copy(), rarity));
                }
            }

            rarities.add(rarity);
            bestRarity = Math.max(bestRarity, rarity);

            if (banner.bannerType() == BannerType.BASIC) {
                basicSelectedSixProgress++;
            }

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

            if (banner.bannerType() == BannerType.EVENT && eventFeaturedGuaranteeEnabled) {
                boolean featuredHit = rarity >= 6 && Objects.equals(entry.itemId(), featuredMainSixItem);
                if (featuredHit) {
                    featured6 = 0;
                } else {
                    featured6++;
                }
            }
        }

        if (banner.bannerType() != BannerType.EVENT || !eventFeaturedGuaranteeEnabled) {
            featured6 = 0;
            featuredToken = banner.bannerType() == BannerType.EVENT
                    ? GachaEventRotation.getFeaturedRotationTokenForBanner(banner.id())
                    : null;
        }

        GachaPityManager.setPity(player, banner, pity5, pity6, featured6, basicSelectedSixProgress, featuredToken);
        return new PullOutcome(rewards, rarities, highRarityRewards, bestRarity, pity5, pity6);
    }

    private static GachaPityManager.PityState getNormalizedPityState(ServerPlayer player, GachaBannerData banner, boolean persistIfChanged) {
        GachaPityManager.PityState state = GachaPityManager.getPity(player, banner);
        if (banner.bannerType() != BannerType.EVENT) {
            if (state.featuredSixStarMisses() != 0 || state.featuredRotationToken() != null) {
                GachaPityManager.PityState normalized = new GachaPityManager.PityState(
                        state.fiveStarMisses(),
                        state.sixStarMisses(),
                        0,
                        state.basicSelectedSixProgress(),
                        null
                );
                if (persistIfChanged) {
                    GachaPityManager.setPity(
                            player,
                            banner,
                            normalized.fiveStarMisses(),
                            normalized.sixStarMisses(),
                            normalized.featuredSixStarMisses(),
                            normalized.basicSelectedSixProgress(),
                            normalized.featuredRotationToken()
                    );
                }
                return normalized;
            }
            return state;
        }

        String expectedToken = GachaEventRotation.getFeaturedRotationTokenForBanner(banner.id());
        if (!Objects.equals(state.featuredRotationToken(), expectedToken)) {
            GachaPityManager.PityState normalized = new GachaPityManager.PityState(
                    state.fiveStarMisses(),
                    state.sixStarMisses(),
                    0,
                    state.basicSelectedSixProgress(),
                    expectedToken
            );
            if (persistIfChanged) {
                GachaPityManager.setPity(
                        player,
                        banner,
                        normalized.fiveStarMisses(),
                        normalized.sixStarMisses(),
                        normalized.featuredSixStarMisses(),
                        normalized.basicSelectedSixProgress(),
                        normalized.featuredRotationToken()
                );
            }
            return normalized;
        }

        return state;
    }

    private static @Nullable ResourceLocation resolveEventMainFeaturedSixItem(GachaBannerData banner) {
        if (banner.bannerType() != BannerType.EVENT || banner.mainItem() == null) {
            return null;
        }
        ResourceLocation mainItem = banner.mainItem();
        boolean existsInSixStarPool = banner.rewards().stream()
                .anyMatch(entry -> entry.rarity() >= 6 && entry.itemId().equals(mainItem));
        return existsInSixStarPool ? mainItem : null;
    }

    private static @Nullable GachaRewardEntry resolveGuaranteedSixEntryForItem(
            GachaBannerData banner,
            ResourceLocation itemId,
            net.minecraft.util.RandomSource random
    ) {
        List<GachaRewardEntry> matching = banner.rewards().stream()
                .filter(entry -> entry.rarity() >= 6 && entry.itemId().equals(itemId))
                .toList();
        if (matching.isEmpty()) {
            return null;
        }
        return matching.get(random.nextInt(matching.size()));
    }

    private static boolean isBasicGuaranteedSelectionRequired(ServerPlayer player, GachaBannerData banner) {
        if (banner.bannerType() != BannerType.BASIC) {
            return false;
        }
        GachaPityManager.PityState pity = getNormalizedPityState(player, banner, true);
        return pity.basicSelectedSixProgress() >= BASIC_SELECTED_SIX_THRESHOLD;
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
            remaining = consumeMatching(player, remaining, stack -> stack.getItem() == Registration.BANNER_PERMIT_ITEM.get()
                    && GachaPermitItem.matchesBanner(stack, banner.id()));
            if (remaining > 0) {
                remaining = consumeMatching(player, remaining, stack -> stack.getItem() == Registration.BASIC_BANNER_PERMIT_ITEM.get());
            }
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
            int specific = countMatching(player, stack -> stack.getItem() == Registration.BANNER_PERMIT_ITEM.get()
                    && GachaPermitItem.matchesBanner(stack, banner.id()));
            int basic = countMatching(player, stack -> stack.getItem() == Registration.BASIC_BANNER_PERMIT_ITEM.get());
            return specific + basic;
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

    private static GachaBannerData resolveLastBanner(ServerPlayer player, boolean writeBackIfMissing) {
        List<GachaBannerData> all = GachaBannerManager.visible();
        if (all.isEmpty()) {
            return null;
        }

        ResourceLocation selectedId = GachaPityManager.getLastBanner(player);
        GachaBannerData selected = selectedId == null ? null : GachaBannerManager.get(selectedId);
        if (selected != null && GachaEventRotation.isCurrentlyVisible(selected)) {
            return selected;
        }

        ResourceLocation defaultId = GachaBannerManager.getDefaultBannerId();
        if (defaultId == null) {
            return null;
        }

        selected = GachaBannerManager.get(defaultId);
        if (selected == null || !GachaEventRotation.isCurrentlyVisible(selected)) {
            return null;
        }

        if (writeBackIfMissing) {
            GachaPityManager.setLastBanner(player, selected.id());
        }
        return selected;
    }

    private static BannerView toBannerView(ServerPlayer player, GachaBannerData banner) {
        GachaPityManager.PityState pityState = getNormalizedPityState(player, banner, true);
        List<RewardView> rewards = banner.rewards().stream()
                .sorted(
                        Comparator.comparingInt(GachaRewardEntry::rarity).reversed()
                                .thenComparing(entry -> entry.itemId().toString())
                )
                .map(entry -> new RewardView(
                        entry.itemId().toString(),
                        entry.rarity(),
                        banner.chanceForReward(entry)
                ))
                .toList();

        List<String> basicSelectableSix = collectSixStarItemIds(banner).stream()
                .map(ResourceLocation::toString)
                .toList();
        boolean eventFeaturedEligible = resolveEventMainFeaturedSixItem(banner) != null;

        return new BannerView(
                banner.id().toString(),
                banner.name(),
                banner.bannerType() == BannerType.BASIC ? "basic" : "event",
                banner.sidebarColor(),
                banner.resolvedMainItem() == null ? "" : banner.resolvedMainItem().toString(),
                GachaEventRotation.getRemainingMillisForBanner(banner.id()),
                pityState.fiveStarMisses(),
                pityState.sixStarMisses(),
                pityState.featuredSixStarMisses(),
                pityState.basicSelectedSixProgress(),
                eventFeaturedEligible,
                banner.bannerType() == BannerType.BASIC && pityState.basicSelectedSixProgress() >= BASIC_SELECTED_SIX_THRESHOLD,
                basicSelectableSix,
                banner.resolvedFeaturedItems().stream().map(ResourceLocation::toString).toList(),
                rewards
        );
    }

    private static List<ResourceLocation> collectSixStarItemIds(GachaBannerData banner) {
        if (banner.bannerType() != BannerType.BASIC) {
            return List.of();
        }

        Set<ResourceLocation> unique = new LinkedHashSet<>();
        for (GachaRewardEntry reward : banner.rewards()) {
            if (reward.rarity() >= 6) {
                unique.add(reward.itemId());
            }
        }
        return List.copyOf(unique);
    }

    public static ItemStack createSpecificPermit(ResourceLocation bannerId, int count) {
        Item permitItem = Registration.BANNER_PERMIT_ITEM.get();
        GachaBannerData banner = GachaBannerManager.get(bannerId);
        if (banner == null) {
            return GachaPermitItem.createBannerPermit(permitItem, bannerId, count);
        }
        return GachaPermitItem.createBannerPermit(permitItem, banner.id(), banner.name(), count);
    }

    public record PullOutcome(
            List<ItemStack> rewards,
            List<Integer> rarities,
            List<HighRarityReward> highRarityRewards,
            int bestRarity,
            int pityFive,
            int pitySix
    ) {
    }

    public record HighRarityReward(ItemStack stack, int rarity) {
    }

    public record ScreenData(List<BannerView> banners) {
    }

    public record BannerView(
            String id,
            String name,
            String type,
            int sidebarColor,
            String mainItemId,
            long remainingMillis,
            int pityFive,
            int pitySix,
            int eventFeaturedPity,
            int basicSelectedSixPity,
            boolean eventFeaturedPityEnabled,
            boolean basicGuaranteeBlocked,
            List<String> basicSelectableSixItems,
            List<String> featuredItems,
            List<RewardView> rewards
    ) {
    }

    public record RewardView(String itemId, int rarity, double chancePercent) {
    }

    public record PityView(
            ResourceLocation bannerId,
            int pityFive,
            int pitySix,
            int eventFeaturedPity,
            int basicSelectedSixPity,
            @Nullable String eventRotationToken
    ) {
    }
}
