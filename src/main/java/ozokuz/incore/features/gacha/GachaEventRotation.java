package ozokuz.incore.features.gacha;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class GachaEventRotation {
    private static final Object FORCE_LOCK = new Object();
    private static final HashMap<ResourceLocation, ForcedCategoryOverride> FORCED_CATEGORY_OVERRIDES = new HashMap<>();
    private static long nextForceNonce = 1L;

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

    public static List<ResourceLocation> getKnownCategoryIds() {
        return GachaEventCategoryManager.all().stream()
                .map(GachaEventCategoryData::id)
                .sorted(Comparator.comparing(ResourceLocation::toString))
                .toList();
    }

    public static Optional<ForcedRotationResult> rotateCategory(ResourceLocation categoryId, int direction) {
        GachaEventCategoryData category = GachaEventCategoryManager.get(categoryId);
        if (category == null) {
            return Optional.empty();
        }

        List<ResourceLocation> order = category.bannerOrder().stream()
                .map(GachaEventRotation::normalizeEventBanner)
                .filter(Objects::nonNull)
                .toList();
        if (order.isEmpty()) {
            return Optional.empty();
        }

        long now = System.currentTimeMillis();
        long durationMillis = Math.max(1L, category.durationHours()) * 60L * 60L * 1000L;
        long cycle = Math.floorDiv(now, durationMillis);
        int scheduledIndex = (int) Math.floorMod(cycle, order.size());

        ForcedCategoryOverride forced = getActiveForcedOverride(categoryId, cycle, order);
        int currentIndex = scheduledIndex;
        if (forced != null) {
            int forcedIndex = order.indexOf(forced.bannerId());
            if (forcedIndex >= 0) {
                currentIndex = forcedIndex;
            }
        }

        int step = direction >= 0 ? 1 : -1;
        int nextIndex = Math.floorMod(currentIndex + step, order.size());
        ResourceLocation nextBanner = order.get(nextIndex);
        long remainingMillis = Math.max(0L, durationMillis - Math.floorMod(now, durationMillis));

        synchronized (FORCE_LOCK) {
            FORCED_CATEGORY_OVERRIDES.put(categoryId, new ForcedCategoryOverride(cycle, nextBanner, nextForceNonce++));
        }

        return Optional.of(new ForcedRotationResult(categoryId, nextBanner, remainingMillis));
    }

    public static String getFeaturedRotationTokenForBanner(ResourceLocation bannerId) {
        GachaBannerData banner = GachaBannerManager.get(bannerId);
        if (banner == null || banner.bannerType() != GachaBannerData.BannerType.EVENT) {
            return "banner:" + bannerId;
        }

        BannerCategoryResolution resolution = resolveCategoryForBanner(bannerId);
        if (resolution == null) {
            return "event:static:" + bannerId;
        }

        long now = System.currentTimeMillis();
        long cycle = Math.floorDiv(now, resolution.durationMillis());
        String token = resolution.categoryId() + "#" + cycle;
        ForcedCategoryOverride forced = getActiveForcedOverride(resolution.categoryId(), cycle, resolution.order());
        if (forced != null) {
            token += "#f" + forced.nonce();
        }
        return token;
    }

    public static String getRotationTokenForCategory(ResourceLocation categoryId) {
        GachaEventCategoryData category = GachaEventCategoryManager.get(categoryId);
        if (category == null) {
            return "missing:" + categoryId;
        }

        List<ResourceLocation> order = category.bannerOrder().stream()
                .map(GachaEventRotation::normalizeEventBanner)
                .filter(Objects::nonNull)
                .toList();
        if (order.isEmpty()) {
            return "empty:" + categoryId;
        }

        long now = System.currentTimeMillis();
        long durationMillis = Math.max(1L, category.durationHours()) * 60L * 60L * 1000L;
        long cycle = Math.floorDiv(now, durationMillis);

        String token = categoryId + "#" + cycle;
        ForcedCategoryOverride forced = getActiveForcedOverride(categoryId, cycle, order);
        if (forced != null) {
            token += "#f" + forced.nonce();
        }
        return token;
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
            int index = (int) Math.floorMod(cycle, order.size());
            Long overrideNonce = null;
            ForcedCategoryOverride forced = getActiveForcedOverride(category.id(), cycle, order);
            if (forced != null) {
                int forcedIndex = order.indexOf(forced.bannerId());
                if (forcedIndex >= 0) {
                    index = forcedIndex;
                    overrideNonce = forced.nonce();
                }
            }

            long elapsedInWindow = Math.floorMod(now, durationMillis);
            long remainingMillis = Math.max(0L, durationMillis - elapsedInWindow);
            active.add(new ActiveCategoryState(category.id(), order.get(index), remainingMillis, cycle, index, order.size(), overrideNonce));
        }
        return active;
    }

    private static @Nullable BannerCategoryResolution resolveCategoryForBanner(ResourceLocation bannerId) {
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

            int index = order.indexOf(bannerId);
            if (index < 0) {
                continue;
            }

            long durationMillis = Math.max(1L, category.durationHours()) * 60L * 60L * 1000L;
            return new BannerCategoryResolution(category.id(), order, durationMillis);
        }
        return null;
    }

    private static @Nullable ForcedCategoryOverride getActiveForcedOverride(ResourceLocation categoryId, long cycle, List<ResourceLocation> order) {
        synchronized (FORCE_LOCK) {
            ForcedCategoryOverride forced = FORCED_CATEGORY_OVERRIDES.get(categoryId);
            if (forced == null) {
                return null;
            }

            if (forced.cycle() != cycle || !order.contains(forced.bannerId())) {
                FORCED_CATEGORY_OVERRIDES.remove(categoryId);
                return null;
            }
            return forced;
        }
    }

    private static @Nullable ResourceLocation normalizeEventBanner(ResourceLocation bannerId) {
        GachaBannerData banner = GachaBannerManager.get(bannerId);
        if (banner == null || banner.bannerType() != GachaBannerData.BannerType.EVENT) {
            return null;
        }
        return banner.id();
    }

    public record ForcedRotationResult(ResourceLocation categoryId, ResourceLocation bannerId, long remainingMillis) {
    }

    private record BannerCategoryResolution(
            ResourceLocation categoryId,
            List<ResourceLocation> order,
            long durationMillis
    ) {
    }

    private record ActiveCategoryState(
            ResourceLocation categoryId,
            ResourceLocation bannerId,
            long remainingMillis,
            long cycle,
            int bannerIndex,
            int orderSize,
            @Nullable Long overrideNonce
    ) {
    }

    private record ForcedCategoryOverride(long cycle, ResourceLocation bannerId, long nonce) {
    }
}
