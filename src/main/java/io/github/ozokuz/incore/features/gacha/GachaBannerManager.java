package io.github.ozokuz.incore.features.gacha;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import io.github.ozokuz.incore.INCore;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GachaBannerManager extends SimpleJsonResourceReloadListener {
    private static volatile Map<ResourceLocation, GachaBannerData> banners = Map.of();

    public GachaBannerManager() {
        super(new Gson(), "gacha_banners");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> jsons, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profilerFiller) {
        Map<ResourceLocation, GachaBannerData> next = new LinkedHashMap<>();
        jsons.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString)))
                .forEach(entry -> {
                    ResourceLocation id = entry.getKey();
                    JsonElement json = entry.getValue();
                    if (!json.isJsonObject()) {
                        return;
                    }
                    GachaBannerData banner = GachaBannerData.fromJson(id, json.getAsJsonObject());
                    if (banner != null) {
                        next.put(id, banner);
                    } else {
                        INCore.LOGGER.warn("Skipping invalid gacha banner '{}'", id);
                    }
                });
        banners = Map.copyOf(next);
        INCore.LOGGER.info("Loaded {} gacha banner definitions.", banners.size());
    }

    public static List<GachaBannerData> all() {
        return banners.values().stream().toList();
    }

    public static List<GachaBannerData> visible() {
        return GachaEventRotation.visibleBanners();
    }

    @Nullable
    public static GachaBannerData get(ResourceLocation id) {
        return banners.get(id);
    }

    @Nullable
    public static ResourceLocation getDefaultBannerId() {
        List<GachaBannerData> visible = visible();
        if (visible.isEmpty()) {
            return null;
        }

        if (visible.stream().anyMatch(banner -> banner.id().equals(ResourceLocation.parse("incore:basic")))) {
            return ResourceLocation.parse("incore:basic");
        }

        return visible.getFirst().id();
    }
}
