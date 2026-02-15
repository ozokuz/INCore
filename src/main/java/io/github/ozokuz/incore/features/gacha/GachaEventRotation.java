package io.github.ozokuz.incore.features.gacha;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class GachaEventRotation {
    private GachaEventRotation() {
    }

    public static long getRemainingMillisForBanner(ResourceLocation bannerId) {
        long remaining = -1L;
        for (ActiveCategoryState state : getActiveCategoryStates()) {
            if (!state.bannerId().equals(bannerId)) {
                continue;
            }
            remaining = Math.max(remaining, state.remainingMillis());
        }
        return remaining;
    }

    public static List<ResourceLocation> getActiveEventBannerIds() {
        List<ResourceLocation> active = new ArrayList<>();
        for (ActiveCategoryState state : getActiveCategoryStates()) {
            if (!active.contains(state.bannerId())) {
                active.add(state.bannerId());
            }
        }
        return active;
    }

    public static List<GachaBannerData> visibleBanners() {
        List<GachaBannerData> basics = GachaBannerManager.all().stream()
                .filter(banner -> banner.bannerType() == GachaBannerData.BannerType.BASIC)
                .sorted(Comparator.comparing(GachaBannerData::name))
                .toList();

        List<ResourceLocation> activeEventIds = getActiveEventBannerIds();
        Set<ResourceLocation> categorizedEventIds = GachaEventCategoryManager.all().stream()
                .flatMap(category -> category.bannerOrder().stream())
                .collect(HashSet::new, Set::add, Set::addAll);

        List<GachaBannerData> visible = new ArrayList<>();
        for (ResourceLocation activeEvent : activeEventIds) {
            GachaBannerData eventBanner = GachaBannerManager.get(activeEvent);
            if (eventBanner != null && eventBanner.bannerType() == GachaBannerData.BannerType.EVENT) {
                visible.add(eventBanner);
            }
        }

        GachaBannerManager.all().stream()
                .filter(banner -> banner.bannerType() == GachaBannerData.BannerType.EVENT)
                .filter(banner -> !categorizedEventIds.contains(banner.id()))
                .sorted(Comparator.comparing(GachaBannerData::name))
                .forEach(visible::add);

        visible.addAll(basics);
        return visible;
    }

    public static boolean isCurrentlyVisible(GachaBannerData banner) {
        if (banner.bannerType() == GachaBannerData.BannerType.BASIC) {
            return true;
        }
        return visibleBanners().stream().anyMatch(visible -> visible.id().equals(banner.id()));
    }

    private static List<ActiveCategoryState> getActiveCategoryStates() {
        long now = System.currentTimeMillis();
        List<ActiveCategoryState> active = new ArrayList<>();
        for (GachaEventCategoryData category : GachaEventCategoryManager.all().stream()
                .sorted(Comparator.comparing(data -> data.id().toString()))
                .toList()) {
            List<ResourceLocation> order = category.bannerOrder().stream()
                    .map(GachaEventRotation::normalizeEventBanner)
                    .filter(Objects::nonNull)
                    .toList();
            if (order.isEmpty()) {
                continue;
            }

            long durationMillis = Math.max(1L, category.durationHours()) * 60L * 60L * 1000L;
            long cycle = Math.floorDiv(now, durationMillis);
            int index = Math.floorMod((int) cycle, order.size());
            long elapsedInWindow = Math.floorMod(now, durationMillis);
            long remainingMillis = Math.max(0L, durationMillis - elapsedInWindow);
            active.add(new ActiveCategoryState(category.id(), order.get(index), remainingMillis));
        }
        return active;
    }

    private static @Nullable ResourceLocation normalizeEventBanner(ResourceLocation bannerId) {
        GachaBannerData banner = GachaBannerManager.get(bannerId);
        if (banner == null || banner.bannerType() != GachaBannerData.BannerType.EVENT) {
            return null;
        }
        return banner.id();
    }

    private record ActiveCategoryState(ResourceLocation categoryId, ResourceLocation bannerId, long remainingMillis) {
    }
}
