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

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BattlePassLaneManager extends SimpleJsonResourceReloadListener {
    private static volatile Map<String, LaneDefinition> lanesById = Map.of();
    private static volatile List<String> alwaysAvailableLaneIds = List.of();
    private static volatile List<LaneDefinition> orderedLanes = List.of();

    public BattlePassLaneManager() {
        super(new Gson(), "battlepass_lanes");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> jsons, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profilerFiller) {
        Map<String, LaneDefinition> parsed = new HashMap<>();
        List<String> alwaysAvailable = new java.util.ArrayList<>();

        jsons.forEach((id, json) -> {
            try {
                JsonObject object = GsonHelper.convertToJsonObject(json, id.toString());
                String laneId = id.getPath();
                String displayName = GsonHelper.getAsString(object, "display_name");
                int order = Math.max(0, GsonHelper.getAsInt(object, "order", 0));
                boolean alwaysAvailableFlag = GsonHelper.getAsBoolean(object, "always_available", false);

                LaneDefinition definition = new LaneDefinition(laneId, displayName, order, alwaysAvailableFlag);
                parsed.put(laneId, definition);

                if (alwaysAvailableFlag) {
                    alwaysAvailable.add(laneId);
                }
            } catch (Exception e) {
                INCore.LOGGER.error("Failed to parse battle pass lane file {}.", id, e);
            }
        });

        List<LaneDefinition> sorted = parsed.values().stream()
                .sorted(Comparator.comparingInt(LaneDefinition::order))
                .toList();

        lanesById = Map.copyOf(parsed);
        alwaysAvailableLaneIds = List.copyOf(alwaysAvailable);
        orderedLanes = sorted;

        INCore.LOGGER.info("Loaded {} battle pass lane(s). Always available: {}", lanesById.size(), alwaysAvailableLaneIds);
    }

    public static boolean isValid(String laneId) {
        return lanesById.containsKey(BattlePassLane.normalize(laneId));
    }

    public static String displayName(String laneId) {
        LaneDefinition definition = lanesById.get(BattlePassLane.normalize(laneId));
        return definition != null ? definition.displayName() : laneId;
    }

    public static List<String> getAlwaysAvailableLaneIds() {
        return alwaysAvailableLaneIds;
    }

    public static List<String> getOrderedLaneIds() {
        return orderedLanes.stream().map(LaneDefinition::id).toList();
    }

    public static List<String> getAllLaneIds() {
        return orderedLanes.stream().map(LaneDefinition::id).toList();
    }

    public static LaneDefinition getLaneDefinition(String laneId) {
        return lanesById.get(BattlePassLane.normalize(laneId));
    }

    public record LaneDefinition(String id, String displayName, int order, boolean alwaysAvailable) {
    }
}
