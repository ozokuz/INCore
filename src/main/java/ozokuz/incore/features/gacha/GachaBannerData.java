package ozokuz.incore.features.gacha;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record GachaBannerData(
        ResourceLocation id,
        String name,
        BannerType bannerType,
        String pityGroup,
        int sidebarColor,
        @Nullable ResourceLocation mainItem,
        Map<Integer, Integer> rarityWeights,
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
        int sidebarColor = parseColor(json, "sidebar_color", bannerType == BannerType.EVENT ? 0xFF8A4A : 0x4A90FF);
        ResourceLocation mainItem = json.has("main_item")
                ? ResourceLocation.tryParse(json.get("main_item").getAsString())
                : null;
        Map<Integer, Integer> rarityWeights = parseRarityWeights(json);

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

        return new GachaBannerData(id, name, bannerType, pityGroup, sidebarColor, mainItem, Map.copyOf(rarityWeights), List.copyOf(featuredItems), List.copyOf(rewards));
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
                                .thenComparing(entry -> entry.itemId().toString())
                )
                .map(GachaRewardEntry::itemId)
                .distinct()
                .limit(3)
                .toList();
    }

    @Nullable
    public ResourceLocation resolvedMainItem() {
        if (mainItem != null) {
            return mainItem;
        }

        if (!featuredItems.isEmpty()) {
            return featuredItems.getFirst();
        }

        return rewards.stream()
                .sorted(
                        Comparator.comparingInt(GachaRewardEntry::rarity).reversed()
                                .thenComparing(entry -> entry.itemId().toString())
                )
                .map(GachaRewardEntry::itemId)
                .findFirst()
                .orElse(null);
    }

    public GachaRewardEntry roll(RandomSource random, int minimumRarity) {
        List<GachaRewardEntry> pool = rewards.stream()
                .filter(entry -> entry.rarity() >= minimumRarity)
                .toList();
        if (pool.isEmpty()) {
            pool = rewards;
        }

        Map<Integer, List<GachaRewardEntry>> byRarity = new HashMap<>();
        for (GachaRewardEntry entry : pool) {
            byRarity.computeIfAbsent(entry.rarity(), ignored -> new ArrayList<>()).add(entry);
        }

        List<Integer> rarityPool = byRarity.keySet().stream().sorted().toList();
        int totalRarityWeight = rarityPool.stream().mapToInt(this::weightForRarity).sum();
        if (totalRarityWeight <= 0) {
            int fallbackRarity = rarityPool.get(random.nextInt(rarityPool.size()));
            List<GachaRewardEntry> rarityEntries = byRarity.get(fallbackRarity);
            return rarityEntries.get(random.nextInt(rarityEntries.size()));
        }

        int roll = random.nextInt(totalRarityWeight);
        int cumulative = 0;
        int selectedRarity = rarityPool.getFirst();
        for (int rarity : rarityPool) {
            cumulative += weightForRarity(rarity);
            if (roll < cumulative) {
                selectedRarity = rarity;
                break;
            }
        }

        List<GachaRewardEntry> selectedPool = byRarity.get(selectedRarity);
        return selectedPool.get(random.nextInt(selectedPool.size()));
    }

    public double chanceForReward(GachaRewardEntry reward) {
        List<GachaRewardEntry> sameRarity = rewards.stream()
                .filter(entry -> entry.rarity() == reward.rarity())
                .toList();
        if (sameRarity.isEmpty()) {
            return 0.0D;
        }

        int totalRarityWeight = rewards.stream()
                .map(GachaRewardEntry::rarity)
                .distinct()
                .mapToInt(this::weightForRarity)
                .sum();
        if (totalRarityWeight <= 0) {
            return 100.0D / rewards.size();
        }

        double rarityChance = (weightForRarity(reward.rarity()) * 100.0D) / totalRarityWeight;
        return rarityChance / sameRarity.size();
    }

    private int weightForRarity(int rarity) {
        return Math.max(0, rarityWeights.getOrDefault(rarity, defaultWeightForRarity(rarity)));
    }

    private static Map<Integer, Integer> parseRarityWeights(JsonObject json) {
        Map<Integer, Integer> result = new HashMap<>();
        if (!json.has("rarity_weights") || !json.get("rarity_weights").isJsonObject()) {
            for (int rarity = 2; rarity <= 6; rarity++) {
                result.put(rarity, defaultWeightForRarity(rarity));
            }
            return result;
        }

        JsonObject weightsObj = json.getAsJsonObject("rarity_weights");
        for (int rarity = 2; rarity <= 6; rarity++) {
            String key = String.valueOf(rarity);
            int value = weightsObj.has(key) ? Math.max(0, weightsObj.get(key).getAsInt()) : defaultWeightForRarity(rarity);
            result.put(rarity, value);
        }
        return result;
    }

    private static int defaultWeightForRarity(int rarity) {
        return switch (rarity) {
            case 2 -> 2800;
            case 3 -> 1700;
            case 4 -> 900;
            case 5 -> 260;
            case 6 -> 20;
            default -> 0;
        };
    }

    private static int parseColor(JsonObject json, String key, int fallback) {
        if (!json.has(key)) {
            return fallback;
        }

        JsonElement value = json.get(key);
        if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber()) {
            return value.getAsInt() & 0xFFFFFF;
        }

        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
            return fallback;
        }

        String raw = value.getAsString().trim();
        if (raw.startsWith("#")) {
            raw = raw.substring(1);
        } else if (raw.startsWith("0x") || raw.startsWith("0X")) {
            raw = raw.substring(2);
        }

        try {
            return Integer.parseInt(raw, 16) & 0xFFFFFF;
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }
}
