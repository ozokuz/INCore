package io.github.ozokuz.incore.features.battlepass;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.ozokuz.incore.INCore;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class BattlePassManager extends SimpleJsonResourceReloadListener {
    private static volatile Map<ResourceLocation, BattlePassDefinition> setsById = Map.of();
    private static volatile List<BattlePassDefinition> orderedSets = List.of();

    public BattlePassManager() {
        super(new Gson(), "battlepasses");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> jsons, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profilerFiller) {
        Map<ResourceLocation, BattlePassDefinition> parsed = new HashMap<>();

        jsons.forEach((id, json) -> {
            try {
                JsonObject object = GsonHelper.convertToJsonObject(json, id.toString());
                BattlePassDefinition definition = parseDefinition(id, object);
                parsed.put(id, definition);
            } catch (Exception e) {
                INCore.LOGGER.error("Failed to parse battle pass file {}.", id, e);
            }
        });

        setsById = Map.copyOf(parsed);
        orderedSets = parsed.values().stream().sorted(Comparator.comparing(BattlePassDefinition::startsAt)).toList();
        INCore.LOGGER.info("Loaded {} battle pass set(s).", setsById.size());
    }

    public static Optional<BattlePassDefinition> getActiveSet(Instant now) {
        return orderedSets.stream().filter(definition -> definition.isActive(now)).findFirst();
    }

    private static BattlePassDefinition parseDefinition(ResourceLocation id, JsonObject object) {
        Instant startsAt = Instant.parse(GsonHelper.getAsString(object, "starts_at"));
        Instant endsAt = Instant.parse(GsonHelper.getAsString(object, "ends_at"));
        if (!endsAt.isAfter(startsAt)) {
            throw new IllegalArgumentException("ends_at must be later than starts_at");
        }

        int xpPerLevel = Math.max(1, GsonHelper.getAsInt(object, "xp_per_level", 1000));
        Map<String, Integer> tierXp = new HashMap<>();
        JsonObject tierObject = object.has("tier_xp")
                ? GsonHelper.getAsJsonObject(object, "tier_xp")
                : new JsonObject();
        for (Map.Entry<String, JsonElement> entry : tierObject.entrySet()) {
            tierXp.put(entry.getKey(), Math.max(1, entry.getValue().getAsInt()));
        }

        List<BattlePassDefinition.BattlePassTask> tasks = new ArrayList<>();
        for (JsonElement taskElement : GsonHelper.getAsJsonArray(object, "tasks")) {
            JsonObject taskObject = taskElement.getAsJsonObject();
            String taskId = GsonHelper.getAsString(taskObject, "id");
            BattlePassDefinition.TaskType taskType = BattlePassDefinition.TaskType.valueOf(
                    GsonHelper.getAsString(taskObject, "type").toUpperCase()
            );
            int week = Math.max(1, GsonHelper.getAsInt(taskObject, "week", 1));
            String tier = GsonHelper.getAsString(taskObject, "tier", "bronze");
            int xpReward = Math.max(0, GsonHelper.getAsInt(taskObject, "xp", 0));
            String description = GsonHelper.getAsString(taskObject, "description", taskId);
            tasks.add(new BattlePassDefinition.BattlePassTask(taskId, taskType, week, tier, xpReward, description));
        }

        Map<Integer, List<BattlePassReward>> rewardsByLevel = new HashMap<>();
        for (JsonElement levelRewardElement : GsonHelper.getAsJsonArray(object, "level_rewards")) {
            JsonObject levelObject = levelRewardElement.getAsJsonObject();
            int level = Math.max(1, GsonHelper.getAsInt(levelObject, "level"));
            List<BattlePassReward> rewards = rewardsByLevel.computeIfAbsent(level, ignored -> new ArrayList<>());
            for (JsonElement rewardElement : GsonHelper.getAsJsonArray(levelObject, "rewards")) {
                rewards.add(parseReward(rewardElement.getAsJsonObject()));
            }
        }

        Map<Integer, List<BattlePassReward>> immutable = new HashMap<>();
        rewardsByLevel.forEach((level, rewards) -> immutable.put(level, List.copyOf(rewards)));

        return new BattlePassDefinition(id, startsAt, endsAt, xpPerLevel, Map.copyOf(tierXp), List.copyOf(tasks), Map.copyOf(immutable));
    }

    private static BattlePassReward parseReward(JsonObject rewardObject) {
        String type = GsonHelper.getAsString(rewardObject, "type");
        return switch (type) {
            case "item" -> new BattlePassReward.ItemReward(
                    BattlePassReward.parseItem(GsonHelper.getAsString(rewardObject, "item")),
                    Math.max(1, GsonHelper.getAsInt(rewardObject, "count", 1))
            );
            case "command" -> new BattlePassReward.CommandReward(
                    GsonHelper.getAsString(rewardObject, "command"),
                    GsonHelper.getAsString(rewardObject, "preview", "Battle pass command")
            );
            case "sanity_cap_bonus" -> new BattlePassReward.SanityCapBonusReward(
                    Math.max(1, GsonHelper.getAsInt(rewardObject, "amount"))
            );
            default -> throw new IllegalArgumentException("Unknown reward type: " + type);
        };
    }
}
