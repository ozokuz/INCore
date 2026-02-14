package io.github.ozokuz.incore.features.playerlevel;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.ozokuz.incore.INCore;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlayerLevelRewardManager extends SimpleJsonResourceReloadListener {
    private static volatile Map<Integer, List<PlayerLevelReward>> rewardsByLevel = Map.of();

    public PlayerLevelRewardManager() {
        super(new Gson(), "player_level_rewards");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> jsons, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profilerFiller) {
        Map<Integer, List<PlayerLevelReward>> parsed = new HashMap<>();

        jsons.forEach((id, json) -> {
            try {
                JsonObject object = GsonHelper.convertToJsonObject(json, id.toString());
                int level = GsonHelper.getAsInt(object, "level");
                if (level <= 0) {
                    throw new IllegalArgumentException("Level must be greater than 0.");
                }

                List<PlayerLevelReward> rewards = new ArrayList<>();
                for (JsonElement rewardElement : GsonHelper.getAsJsonArray(object, "rewards")) {
                    rewards.add(parseReward(rewardElement.getAsJsonObject()));
                }

                parsed.computeIfAbsent(level, ignored -> new ArrayList<>()).addAll(rewards);
            } catch (Exception e) {
                INCore.LOGGER.error("Failed to parse player level reward file {}.", id, e);
            }
        });

        Map<Integer, List<PlayerLevelReward>> immutable = new HashMap<>();
        parsed.forEach((level, rewards) -> immutable.put(level, List.copyOf(rewards)));
        rewardsByLevel = Map.copyOf(immutable);
        INCore.LOGGER.info("Loaded {} player level reward definition level(s).", rewardsByLevel.size());
    }

    public static List<PlayerLevelReward> getRewardsForLevel(int level) {
        return rewardsByLevel.getOrDefault(level, List.of());
    }

    public static List<PlayerLevelRewardPreview> getAllRewardPreviews() {
        return rewardsByLevel.entrySet().stream()
                .sorted(Comparator.comparingInt(Map.Entry::getKey))
                .map(entry -> new PlayerLevelRewardPreview(
                        entry.getKey(),
                        entry.getValue().stream().map(PlayerLevelReward::previewText).toList()
                ))
                .toList();
    }

    public static int getHighestRewardLevel() {
        return rewardsByLevel.keySet().stream().mapToInt(Integer::intValue).max().orElse(0);
    }

    private static PlayerLevelReward parseReward(JsonObject rewardObject) {
        String type = GsonHelper.getAsString(rewardObject, "type");

        return switch (type) {
            case "item" -> parseItemReward(rewardObject);
            case "sanity_cap_bonus" -> parseSanityCapReward(rewardObject);
            case "command" -> parseCommandReward(rewardObject);
            default -> throw new IllegalArgumentException("Unknown reward type: " + type);
        };
    }

    private static PlayerLevelReward parseItemReward(JsonObject rewardObject) {
        String itemIdString = GsonHelper.getAsString(rewardObject, "item");
        ResourceLocation itemId = ResourceLocation.tryParse(itemIdString);
        if (itemId == null) {
            throw new IllegalArgumentException("Invalid item id: " + itemIdString);
        }

        Item item = BuiltInRegistries.ITEM.get(itemId);
        if (item == Items.AIR) {
            throw new IllegalArgumentException("Unknown item id: " + itemIdString);
        }

        int count = Math.max(1, GsonHelper.getAsInt(rewardObject, "count", 1));
        return new PlayerLevelReward.ItemReward(item, count);
    }

    private static PlayerLevelReward parseSanityCapReward(JsonObject rewardObject) {
        int amount = Math.max(1, GsonHelper.getAsInt(rewardObject, "amount"));
        return new PlayerLevelReward.SanityCapBonusReward(amount);
    }

    private static PlayerLevelReward parseCommandReward(JsonObject rewardObject) {
        String command = GsonHelper.getAsString(rewardObject, "command");
        String preview = GsonHelper.getAsString(rewardObject, "preview", "Custom command reward");
        return new PlayerLevelReward.CommandReward(command, preview);
    }

    public record PlayerLevelRewardPreview(int level, List<String> rewards) {
    }
}
