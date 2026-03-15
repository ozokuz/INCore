package ozokuz.incore.features.battlepass;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import ozokuz.incore.INCore;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
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
    private static volatile Integer forcedWeek;

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
        orderedSets = parsed.values().stream()
                .sorted(Comparator.comparingInt(BattlePassDefinition::order).thenComparing(set -> set.id().toString()))
                .toList();

        INCore.LOGGER.info("Loaded {} battle pass set(s).", setsById.size());
    }

    public static Optional<BattlePassDefinition> getActiveSet(MinecraftServer server, Instant now) {
        if (server == null || orderedSets.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(resolveScheduledSet(server, now).definition());
    }

    public static Optional<BattlePassDefinition> setForcedSet(MinecraftServer server, ResourceLocation setId, Instant now) {
        if (server == null) {
            return Optional.empty();
        }

        BattlePassDefinition definition = setsById.get(setId);
        if (definition == null) {
            return Optional.empty();
        }

        Instant start = weekAlignedStart(now);
        BattlePassScheduleSavedData.get(server).setActiveSet(definition.id().toString(), start.toEpochMilli());
        forcedWeek = null;
        return Optional.of(definition.withStart(start));
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

    public static Optional<BattlePassDefinition> rotateForcedSet(MinecraftServer server, int direction, Instant now) {
        if (server == null || orderedSets.isEmpty()) {
            return Optional.empty();
        }

        int normalizedDirection = direction >= 0 ? 1 : -1;
        ResolvedSchedule current = resolveScheduledSet(server, now);
        int nextIndex = Math.floorMod(current.index() + normalizedDirection, orderedSets.size());

        BattlePassDefinition next = orderedSets.get(nextIndex);
        Instant start = weekAlignedStart(now);
        BattlePassScheduleSavedData.get(server).setActiveSet(next.id().toString(), start.toEpochMilli());
        forcedWeek = null;
        return Optional.of(next.withStart(start));
    }

    public static List<String> getKnownSetIds() {
        return orderedSets.stream().map(set -> set.id().toString()).toList();
    }

    static ResolvedSchedule resolveScheduledSet(MinecraftServer server, Instant now) {
        BattlePassScheduleSavedData data = BattlePassScheduleSavedData.get(server);
        ScheduleCursor cursor = ensureInitialSchedule(data, now);
        if (cursor == null) {
            throw new IllegalStateException("Cannot resolve battle pass schedule without loaded definitions.");
        }

        boolean advanced = false;
        int advancedSteps = 0;
        BattlePassDefinition initialDefinition = cursor.definition();
        Instant lastTransitionAt = cursor.definition().startsAt();
        boolean wrapped = false;
        while (!now.isBefore(cursor.definition().endsAt())) {
            int previousIndex = cursor.index();
            BattlePassDefinition previous = cursor.definition();
            int nextIndex = Math.floorMod(previousIndex + 1, orderedSets.size());
            BattlePassDefinition nextTemplate = orderedSets.get(nextIndex);
            Instant nextStart = previous.endsAt();
            cursor = new ScheduleCursor(nextIndex, nextTemplate.withStart(nextStart));
            advanced = true;
            advancedSteps++;
            lastTransitionAt = nextStart;
            wrapped |= nextIndex == 0;
        }

        if (advancedSteps == 1) {
            if (wrapped) {
                INCore.LOGGER.info("Battle pass schedule wrapped from {} to {} at {}.", initialDefinition.id(), cursor.definition().id(), lastTransitionAt);
            } else {
                INCore.LOGGER.info("Battle pass schedule advanced from {} to {} at {}.", initialDefinition.id(), cursor.definition().id(), lastTransitionAt);
            }
        } else if (advancedSteps > 1) {
            INCore.LOGGER.info(
                    "Battle pass schedule advanced {} steps from {} to {} at {}{}.",
                    advancedSteps,
                    initialDefinition.id(),
                    cursor.definition().id(),
                    lastTransitionAt,
                    wrapped ? " (wrapped)" : ""
            );
        }

        if (advanced || !data.activeSetId().equals(cursor.definition().id().toString())
                || data.activeStartEpochMillis() != cursor.definition().startsAt().toEpochMilli()) {
            data.setActiveSet(cursor.definition().id().toString(), cursor.definition().startsAt().toEpochMilli());
        }
        return new ResolvedSchedule(cursor.index(), cursor.definition());
    }

    private static ScheduleCursor ensureInitialSchedule(BattlePassScheduleSavedData data, Instant now) {
        if (orderedSets.isEmpty()) {
            return null;
        }

        if (!data.hasActiveSet()) {
            BattlePassDefinition first = orderedSets.getFirst();
            Instant start = weekAlignedStart(now);
            data.setActiveSet(first.id().toString(), start.toEpochMilli());
            INCore.LOGGER.info("Initialized battle pass schedule with {} starting at {}.", first.id(), start);
            return new ScheduleCursor(0, first.withStart(start));
        }

        ResourceLocation savedId = ResourceLocation.tryParse(data.activeSetId());
        if (savedId == null) {
            BattlePassDefinition first = orderedSets.getFirst();
            Instant start = weekAlignedStart(now);
            data.setActiveSet(first.id().toString(), start.toEpochMilli());
            INCore.LOGGER.warn("Battle pass schedule contained invalid set id '{}'; reset to {}.", data.activeSetId(), first.id());
            return new ScheduleCursor(0, first.withStart(start));
        }

        int index = indexOfSet(savedId);
        if (index < 0) {
            BattlePassDefinition first = orderedSets.getFirst();
            Instant start = weekAlignedStart(now);
            data.setActiveSet(first.id().toString(), start.toEpochMilli());
            INCore.LOGGER.warn("Battle pass schedule referenced missing set {}; reset to {}.", savedId, first.id());
            return new ScheduleCursor(0, first.withStart(start));
        }

        return new ScheduleCursor(index, orderedSets.get(index).withStart(Instant.ofEpochMilli(data.activeStartEpochMillis())));
    }

    private static int indexOfSet(ResourceLocation setId) {
        for (int i = 0; i < orderedSets.size(); i++) {
            if (orderedSets.get(i).id().equals(setId)) {
                return i;
            }
        }
        return -1;
    }

    private static BattlePassDefinition parseDefinition(ResourceLocation id, JsonObject object) {
        int order = Math.max(0, GsonHelper.getAsInt(object, "order"));
        int lengthWeeks = GsonHelper.getAsInt(object, "length_weeks");
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

        return new BattlePassDefinition(id, order, Instant.EPOCH, lengthWeeks, xpPerLevel, tierXp, tasks, lanes, rewardsByLane);
    }

    private static List<String> parseLanes(JsonObject object) {
        List<String> lanes = new ArrayList<>();

        List<String> alwaysAvailable = BattlePassLaneManager.getAlwaysAvailableLaneIds();
        lanes.addAll(alwaysAvailable);

        if (object.has("lanes")) {
            for (JsonElement laneElement : GsonHelper.getAsJsonArray(object, "lanes")) {
                String lane = BattlePassLane.normalize(laneElement.getAsString());
                if (!BattlePassLane.isValid(lane)) {
                    throw new IllegalArgumentException("Unknown battle pass lane: " + laneElement.getAsString());
                }
                lanes.remove(lane);
                lanes.add(lane);
            }
        }

        if (lanes.isEmpty()) {
            return BattlePassLaneManager.getAllLaneIds();
        }

        lanes.sort((a, b) -> {
            var defA = BattlePassLaneManager.getLaneDefinition(a);
            var defB = BattlePassLaneManager.getLaneDefinition(b);
            int orderA = defA != null ? defA.order() : 0;
            int orderB = defB != null ? defB.order() : 0;
            return Integer.compare(orderA, orderB);
        });

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

        List<String> trackLanes = BattlePassLaneManager.getAllLaneIds();
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
            case "entropy_cap_bonus" -> new BattlePassReward.EntropyCapBonusReward(
                    Math.max(1, GsonHelper.getAsInt(rewardObject, "amount"))
            );
            default -> throw new IllegalArgumentException("Unknown reward type: " + type);
        };
    }

    record ResolvedSchedule(int index, BattlePassDefinition definition) {
    }

    private record ScheduleCursor(int index, BattlePassDefinition definition) {
    }
}
