package io.github.ozokuz.incore.features.tasks;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TaskDataManager extends SimpleJsonResourceReloadListener {
    private static volatile List<TaskDefinition> dailyTasks = List.of();
    private static volatile List<TaskDefinition> weeklyTasks = List.of();
    private static volatile Map<String, List<TaskReward>> rewardsByPool = Map.of();

    public TaskDataManager() {
        super(new Gson(), "tasks");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> jsons, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profilerFiller) {
        List<TaskDefinition> parsedDaily = new ArrayList<>();
        List<TaskDefinition> parsedWeekly = new ArrayList<>();

        for (Map.Entry<ResourceLocation, JsonElement> entry : jsons.entrySet()) {
            ResourceLocation id = entry.getKey();
            try {
                JsonObject object = GsonHelper.convertToJsonObject(entry.getValue(), id.toString());
                TaskDefinition definition = parseTask(id, object);
                if (definition.period() == TaskDefinition.Period.DAILY) {
                    parsedDaily.add(definition);
                } else {
                    parsedWeekly.add(definition);
                }
            } catch (Exception e) {
                INCore.LOGGER.error("Failed to parse task definition {}.", id, e);
            }
        }

        Map<String, List<TaskReward>> parsedRewards = loadRewards(resourceManager);
        dailyTasks = List.copyOf(parsedDaily);
        weeklyTasks = List.copyOf(parsedWeekly);
        rewardsByPool = Map.copyOf(parsedRewards);

        INCore.LOGGER.info("Loaded {} daily task(s), {} weekly task(s), and {} reward pool(s).", dailyTasks.size(), weeklyTasks.size(), rewardsByPool.size());
    }

    private static TaskDefinition parseTask(ResourceLocation id, JsonObject object) {
        TaskDefinition.Period period = switch (GsonHelper.getAsString(object, "period", "daily")) {
            case "daily" -> TaskDefinition.Period.DAILY;
            case "weekly" -> TaskDefinition.Period.WEEKLY;
            default -> throw new IllegalArgumentException("Unknown period");
        };

        TaskDefinition.TaskType type = switch (GsonHelper.getAsString(object, "task_type")) {
            case "item_collection" -> TaskDefinition.TaskType.ITEM_COLLECTION;
            case "mob_kill" -> TaskDefinition.TaskType.MOB_KILL;
            default -> throw new IllegalArgumentException("Unknown task type");
        };

        ResourceLocation target = parseResourceLocation(GsonHelper.getAsString(object, "target"), "target");
        int goal = Math.max(1, GsonHelper.getAsInt(object, "goal", 1));
        String title = GsonHelper.getAsString(object, "title", id.toString());
        String description = GsonHelper.getAsString(object, "description", "");

        TaskDefinition.WeeklyDifficulty difficulty = TaskDefinition.WeeklyDifficulty.NONE;
        if (period == TaskDefinition.Period.WEEKLY) {
            difficulty = switch (GsonHelper.getAsString(object, "difficulty", "easy")) {
                case "easy" -> TaskDefinition.WeeklyDifficulty.EASY;
                case "medium" -> TaskDefinition.WeeklyDifficulty.MEDIUM;
                case "hard" -> TaskDefinition.WeeklyDifficulty.HARD;
                default -> throw new IllegalArgumentException("Unknown weekly difficulty");
            };
        }

        return new TaskDefinition(id, period, type, target, goal, difficulty, title, description);
    }

    private static Map<String, List<TaskReward>> loadRewards(ResourceManager resourceManager) {
        Map<String, List<TaskReward>> rewardPools = new HashMap<>();
        resourceManager.listResources("task_rewards", location -> location.getPath().endsWith(".json"))
                .forEach((id, resource) -> {
                    try (var reader = resource.openAsReader()) {
                        JsonObject object = GsonHelper.parse(reader);
                        String pool = GsonHelper.getAsString(object, "pool");
                        List<TaskReward> rewards = rewardPools.computeIfAbsent(pool, ignored -> new ArrayList<>());
                        for (JsonElement rewardElement : GsonHelper.getAsJsonArray(object, "rewards")) {
                            rewards.add(parseReward(rewardElement.getAsJsonObject()));
                        }
                    } catch (Exception e) {
                        INCore.LOGGER.error("Failed to parse task reward definition {}.", id, e);
                    }
                });

        Map<String, List<TaskReward>> immutable = new HashMap<>();
        rewardPools.forEach((key, value) -> immutable.put(key, List.copyOf(value)));
        return immutable;
    }

    private static TaskReward parseReward(JsonObject rewardObject) {
        return switch (GsonHelper.getAsString(rewardObject, "type")) {
            case "item" -> {
                ResourceLocation itemId = parseResourceLocation(GsonHelper.getAsString(rewardObject, "item"), "item");
                Item item = BuiltInRegistries.ITEM.get(itemId);
                if (item == Items.AIR) {
                    throw new IllegalArgumentException("Unknown item id: " + itemId);
                }
                int count = Math.max(1, GsonHelper.getAsInt(rewardObject, "count", 1));
                yield new TaskReward.ItemReward(item, count);
            }
            case "command" -> new TaskReward.CommandReward(GsonHelper.getAsString(rewardObject, "command"));
            case "entropy" -> new TaskReward.EntropyReward(Math.max(1, GsonHelper.getAsInt(rewardObject, "amount", 1)));
            default -> throw new IllegalArgumentException("Unknown reward type");
        };
    }

    private static ResourceLocation parseResourceLocation(String raw, String label) {
        ResourceLocation parsed = ResourceLocation.tryParse(raw);
        if (parsed == null) {
            throw new IllegalArgumentException("Invalid " + label + " id: " + raw);
        }
        return parsed;
    }

    public static List<TaskDefinition> dailyTasks() {
        return dailyTasks;
    }

    public static List<TaskDefinition> weeklyTasks() {
        return weeklyTasks;
    }

    public static List<TaskReward> rewardsForPool(String pool) {
        return rewardsByPool.getOrDefault(pool, List.of());
    }
}
