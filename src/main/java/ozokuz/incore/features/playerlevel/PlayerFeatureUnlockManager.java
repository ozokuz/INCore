package ozokuz.incore.features.playerlevel;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import ozokuz.incore.INCore;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PlayerFeatureUnlockManager extends SimpleJsonResourceReloadListener {
    private static volatile Map<ResourceLocation, PlayerFeatureUnlockDefinition> byId = Map.of();
    private static volatile List<PlayerFeatureUnlockDefinition> ordered = List.of();

    public PlayerFeatureUnlockManager() {
        super(new Gson(), "player_feature_unlocks");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> jsons, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profilerFiller) {
        Map<ResourceLocation, PlayerFeatureUnlockDefinition> next = new LinkedHashMap<>();
        jsons.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString)))
                .forEach(entry -> {
                    try {
                        JsonObject jsonObject = GsonHelper.convertToJsonObject(entry.getValue(), entry.getKey().toString());
                        PlayerFeatureUnlockDefinition definition = PlayerFeatureUnlockDefinition.fromJson(jsonObject);
                        next.put(definition.id(), definition);
                    } catch (Exception exception) {
                        INCore.LOGGER.error("Failed to parse player feature unlock {}", entry.getKey(), exception);
                    }
                });

        List<PlayerFeatureUnlockDefinition> sorted = new ArrayList<>(next.values());
        sorted.sort(Comparator
                .comparingInt(PlayerFeatureUnlockDefinition::requiredLevel)
                .thenComparing(definition -> definition.id().toString()));

        byId = Map.copyOf(next);
        ordered = List.copyOf(sorted);
        INCore.LOGGER.info("Loaded {} player feature unlock definition(s).", ordered.size());
    }

    public static List<PlayerFeatureUnlockDefinition> all() {
        return ordered;
    }

    public static @Nullable PlayerFeatureUnlockDefinition get(ResourceLocation id) {
        return byId.get(id);
    }

    public static List<PlayerFeatureUnlockDefinition> unlocksForLevel(int level) {
        return ordered.stream()
                .filter(definition -> definition.requiredLevel() == level)
                .toList();
    }

    public static int getHighestRequiredLevel() {
        return ordered.stream()
                .mapToInt(PlayerFeatureUnlockDefinition::requiredLevel)
                .max()
                .orElse(0);
    }
}
