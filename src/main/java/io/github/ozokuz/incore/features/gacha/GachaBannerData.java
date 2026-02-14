package io.github.ozokuz.incore.features.gacha;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public record GachaBannerData(
        ResourceLocation id,
        String name,
        BannerType bannerType,
        String pityGroup,
        List<ResourceLocation> featuredItems,
        List<GachaRewardEntry> rewards
) {
    public enum BannerType {
        BASIC,
        EVENT
    }

    @Nullable
    public static GachaBannerData fromJson(ResourceLocation id, JsonObject json) {
        if (!json.has("name") || !json.has("rewards")) {
            return null;
        }

        String name = json.get("name").getAsString();
        String type = json.has("type") ? json.get("type").getAsString() : "event";
        BannerType bannerType = "basic".equalsIgnoreCase(type) ? BannerType.BASIC : BannerType.EVENT;
        String pityGroup = json.has("pity_group") ? json.get("pity_group").getAsString() : id.toString();

        List<ResourceLocation> featuredItems = new ArrayList<>();
        if (json.has("featured_items")) {
            for (JsonElement element : json.getAsJsonArray("featured_items")) {
                ResourceLocation parsed = ResourceLocation.tryParse(element.getAsString());
                if (parsed != null) {
                    featuredItems.add(parsed);
                }
            }
        }

        List<GachaRewardEntry> rewards = new ArrayList<>();
        for (JsonElement element : json.getAsJsonArray("rewards")) {
            if (!element.isJsonObject()) {
                continue;
            }
            GachaRewardEntry reward = GachaRewardEntry.fromJson(element.getAsJsonObject());
            if (reward != null) {
                rewards.add(reward);
            }
        }

        if (rewards.isEmpty()) {
            return null;
        }

        return new GachaBannerData(id, name, bannerType, pityGroup, List.copyOf(featuredItems), List.copyOf(rewards));
    }

    public String pityKey() {
        if (bannerType == BannerType.EVENT) {
            return "event:" + pityGroup;
        }
        return "banner:" + id;
    }

    public List<ResourceLocation> resolvedFeaturedItems() {
        if (!featuredItems.isEmpty()) {
            return featuredItems;
        }

        return rewards.stream()
                .sorted(
                        Comparator.comparingInt(GachaRewardEntry::rarity).reversed()
                                .thenComparing(Comparator.comparingInt(GachaRewardEntry::weight).reversed())
                )
                .map(GachaRewardEntry::itemId)
                .distinct()
                .limit(3)
                .toList();
    }

    public GachaRewardEntry roll(RandomSource random, int minimumRarity) {
        List<GachaRewardEntry> pool = rewards.stream()
                .filter(entry -> entry.rarity() >= minimumRarity)
                .toList();
        if (pool.isEmpty()) {
            pool = rewards;
        }

        int totalWeight = pool.stream().mapToInt(GachaRewardEntry::weight).sum();
        if (totalWeight <= 0) {
            return pool.getFirst();
        }

        int roll = random.nextInt(totalWeight);
        int cumulative = 0;
        for (GachaRewardEntry entry : pool) {
            cumulative += entry.weight();
            if (roll < cumulative) {
                return entry;
            }
        }
        return pool.getLast();
    }
}
