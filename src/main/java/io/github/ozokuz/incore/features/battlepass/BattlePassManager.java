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
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class BattlePassManager extends SimpleJsonResourceReloadListener {
    private static volatile Map<ResourceLocation, BattlePassDefinition> setsById = Map.of();
    private static volatile List<BattlePassDefinition> orderedSets = List.of();
    private static volatile ResourceLocation forcedSetId;
    private static volatile Integer forcedWeek;
    private static volatile Instant forcedSetStart;

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

        ResourceLocation forced = forcedSetId;
        if (forced != null && !setsById.containsKey(forced)) {
            INCore.LOGGER.warn("Clearing forced battle pass set {} because it no longer exists after reload.", forced);
            forcedSetId = null;
            forcedSetStart = null;
            forcedWeek = null;
        }

        INCore.LOGGER.info("Loaded {} battle pass set(s).", setsById.size());
    }

    public static Optional<BattlePassDefinition> getActiveSet(Instant now) {
        ResourceLocation forced = forcedSetId;
        if (forced != null) {
            BattlePassDefinition forcedDefinition = setsById.get(forced);
            if (forcedDefinition != null) {
                Instant start = forcedSetStart != null ? forcedSetStart : weekAlignedStart(now);
                return Optional.of(forcedDefinition.withStart(start));
            }
        }

        return orderedSets.stream().filter(definition -> definition.isActive(now)).findFirst();
    }

    public static Optional<BattlePassDefinition> setForcedSet(ResourceLocation setId, Instant now) {
        BattlePassDefinition definition = setsById.get(setId);
        if (definition == null) {
            return Optional.empty();
        }

        forcedSetId = setId;
        forcedSetStart = weekAlignedStart(now);
        forcedWeek = null;
        return Optional.of(definition.withStart(forcedSetStart));
    }

    public static void setForcedWeek(Integer week) {
        forcedWeek = week;
    }

    public static int resolveCurrentWeek(BattlePassDefinition definition, Instant now) {
        int totalWeeks = Math.max(1, (int) definition.durationWeeks());
        Integer override = forcedWeek;
        if (override != null) {
            return Math.max(1, Math.min(totalWeeks, override));
        }

        if (now.isBefore(definition.startsAt())) {
            return 1;
        }

        ZoneId zone = ZoneId.systemDefault();
        ZonedDateTime startWeek = BattlePassWeekTime.weekStart(ZonedDateTime.ofInstant(definition.startsAt(), zone));
        ZonedDateTime nowWeek = BattlePassWeekTime.weekStart(ZonedDateTime.ofInstant(now, zone));
        long elapsedWeeks = Math.max(0L, ChronoUnit.WEEKS.between(startWeek, nowWeek));
        long week = elapsedWeeks + 1L;
        return (int) Math.max(1L, Math.min(totalWeeks, week));
    }

    public static Optional<BattlePassDefinition> rotateForcedSet(int direction, Instant now) {
        if (orderedSets.isEmpty()) {
            return Optional.empty();
        }

        int normalizedDirection = direction >= 0 ? 1 : -1;
        int size = orderedSets.size();
        int currentIndex = currentSetIndex(now);
        int nextIndex;
        if (currentIndex < 0) {
            nextIndex = normalizedDirection > 0 ? 0 : size - 1;
        } else {
            nextIndex = Math.floorMod(currentIndex + normalizedDirection, size);
        }

        BattlePassDefinition next = orderedSets.get(nextIndex);
        forcedSetId = next.id();
        forcedSetStart = weekAlignedStart(now);
        forcedWeek = null;
        return Optional.of(next.withStart(forcedSetStart));
    }

    public static List<String> getKnownSetIds() {
        return orderedSets.stream().map(set -> set.id().toString()).toList();
    }

    private static int currentSetIndex(Instant now) {
        ResourceLocation forced = forcedSetId;
        if (forced != null) {
            for (int i = 0; i < orderedSets.size(); i++) {
                if (orderedSets.get(i).id().equals(forced)) {
                    return i;
                }
            }
        }

        for (int i = 0; i < orderedSets.size(); i++) {
            if (orderedSets.get(i).isActive(now)) {
                return i;
            }
        }

        return -1;
    }

    private static BattlePassDefinition parseDefinition(ResourceLocation id, JsonObject object) {
        Instant configuredStart = Instant.parse(GsonHelper.getAsString(object, "starts_at"));
        Instant normalizedStart = normalizeToWeekStart(configuredStart);

        int lengthWeeks = parseLengthWeeks(object, configuredStart);
        if (lengthWeeks <= 0) {
            throw new IllegalArgumentException("length_weeks must be greater than zero");
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
            int progressGoal = Math.max(1, GsonHelper.getAsInt(taskObject, "goal", 1));
            String description = GsonHelper.getAsString(taskObject, "description", taskId);
            BattlePassDefinition.TriggerType triggerType = parseTriggerType(GsonHelper.getAsString(taskObject, "trigger_type", "none"));
            tasks.add(new BattlePassDefinition.BattlePassTask(taskId, taskType, week, tier, xpReward, progressGoal, description, triggerType));
        }

        List<String> lanes = parseLanes(object);
        Map<String, Map<Integer, List<BattlePassReward>>> rewardsByLane = parseRewardsByLane(object, lanes);

        return new BattlePassDefinition(id, normalizedStart, lengthWeeks, xpPerLevel, tierXp, tasks, lanes, rewardsByLane);
    }

    private static int parseLengthWeeks(JsonObject object, Instant startsAt) {
        if (object.has("length_weeks")) {
            return GsonHelper.getAsInt(object, "length_weeks");
        }

        if (object.has("ends_at")) {
            Instant endsAt = Instant.parse(GsonHelper.getAsString(object, "ends_at"));
            if (!endsAt.isAfter(startsAt)) {
                throw new IllegalArgumentException("ends_at must be later than starts_at");
            }
            long seconds = Math.max(0L, endsAt.getEpochSecond() - startsAt.getEpochSecond());
            return Math.max(1, (int) Math.ceil(seconds / (7d * 24d * 60d * 60d)));
        }

        return 1;
    }

    private static Instant normalizeToWeekStart(Instant configuredStart) {
        ZoneId zone = ZoneId.systemDefault();
        ZonedDateTime start = ZonedDateTime.ofInstant(configuredStart, zone);
        return BattlePassWeekTime.weekStart(start).toInstant();
    }

    private static List<String> parseLanes(JsonObject object) {
        if (!object.has("lanes")) {
            return BattlePassLane.defaultOrder();
        }

        List<String> lanes = new ArrayList<>();
        for (JsonElement laneElement : GsonHelper.getAsJsonArray(object, "lanes")) {
            String lane = BattlePassLane.normalize(laneElement.getAsString());
            if (!BattlePassLane.isValid(lane)) {
                throw new IllegalArgumentException("Unknown battle pass lane: " + laneElement.getAsString());
            }
            if (!lanes.contains(lane)) {
                lanes.add(lane);
            }
        }

        if (lanes.isEmpty()) {
            return BattlePassLane.defaultOrder();
        }

        return List.copyOf(lanes);
    }

    private static Map<String, Map<Integer, List<BattlePassReward>>> parseRewardsByLane(JsonObject object, List<String> lanes) {
        Map<String, Map<Integer, List<BattlePassReward>>> rewardsByLane = new HashMap<>();
        for (String lane : lanes) {
            rewardsByLane.put(lane, new HashMap<>());
        }

        if (object.has("rewards_by_lane")) {
            JsonObject laneRewardsObject = GsonHelper.getAsJsonObject(object, "rewards_by_lane");
            for (String lane : lanes) {
                if (!laneRewardsObject.has(lane)) {
                    continue;
                }

                Map<Integer, List<BattlePassReward>> laneRewards = rewardsByLane.get(lane);
                for (JsonElement levelRewardElement : GsonHelper.getAsJsonArray(laneRewardsObject, lane)) {
                    JsonObject levelObject = levelRewardElement.getAsJsonObject();
                    int level = Math.max(0, GsonHelper.getAsInt(levelObject, "level"));
                    List<BattlePassReward> rewards = laneRewards.computeIfAbsent(level, ignored -> new ArrayList<>());
                    for (JsonElement rewardElement : GsonHelper.getAsJsonArray(levelObject, "rewards")) {
                        rewards.add(parseReward(rewardElement.getAsJsonObject()));
                    }
                }
            }

            return finalizeRewardsByLane(rewardsByLane);
        }

        if (!object.has("level_rewards")) {
            return finalizeRewardsByLane(rewardsByLane);
        }

        List<String> trackLanes = BattlePassLane.defaultOrder();
        for (JsonElement levelRewardElement : GsonHelper.getAsJsonArray(object, "level_rewards")) {
            JsonObject levelObject = levelRewardElement.getAsJsonObject();
            int level = Math.max(0, GsonHelper.getAsInt(levelObject, "level"));

            int trackIndex = 0;
            for (JsonElement rewardElement : GsonHelper.getAsJsonArray(levelObject, "rewards")) {
                String lane = trackIndex < trackLanes.size() ? trackLanes.get(trackIndex) : trackLanes.get(trackLanes.size() - 1);
                Map<Integer, List<BattlePassReward>> laneRewards = rewardsByLane.computeIfAbsent(lane, ignored -> new HashMap<>());
                laneRewards.computeIfAbsent(level, ignored -> new ArrayList<>()).add(parseReward(rewardElement.getAsJsonObject()));
                trackIndex++;
            }
        }

        return finalizeRewardsByLane(rewardsByLane);
    }

    private static Map<String, Map<Integer, List<BattlePassReward>>> finalizeRewardsByLane(
            Map<String, Map<Integer, List<BattlePassReward>>> rewardsByLane
    ) {
        Map<String, Map<Integer, List<BattlePassReward>>> immutable = new HashMap<>();
        rewardsByLane.forEach((lane, laneRewards) -> {
            Map<Integer, List<BattlePassReward>> immutableLane = new HashMap<>();
            laneRewards.forEach((level, rewards) -> immutableLane.put(level, List.copyOf(rewards)));
            immutable.put(lane, Map.copyOf(immutableLane));
        });
        return Map.copyOf(immutable);
    }

    private static Instant weekAlignedStart(Instant now) {
        ZoneId zone = ZoneId.systemDefault();
        ZonedDateTime localNow = ZonedDateTime.ofInstant(now, zone);
        return BattlePassWeekTime.weekStart(localNow).toInstant();
    }

    private static BattlePassDefinition.TriggerType parseTriggerType(String value) {
        if (value == null || value.isBlank()) {
            return BattlePassDefinition.TriggerType.NONE;
        }
        try {
            return BattlePassDefinition.TriggerType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return BattlePassDefinition.TriggerType.NONE;
        }
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
