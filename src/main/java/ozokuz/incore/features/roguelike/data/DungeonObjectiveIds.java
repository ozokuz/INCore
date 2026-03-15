package ozokuz.incore.features.roguelike.data;

import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.Set;

public final class DungeonObjectiveIds {
    public static final ResourceLocation SIGNAL_EMISSION = ResourceLocation.parse("incore:signal_emission");
    public static final ResourceLocation SCAVENGER_HUNT = ResourceLocation.parse("incore:scavenger_hunt");
    public static final ResourceLocation ESSENCE_GATHERING = ResourceLocation.parse("incore:essence_gathering");

    public static final ResourceLocation SKIRMISH = ResourceLocation.parse("incore:skirmish");
    public static final ResourceLocation SLAUGHTER = ResourceLocation.parse("incore:slaughter");
    public static final ResourceLocation ONSLAUGHT = ResourceLocation.parse("incore:onslaught");

    private static final Map<ResourceLocation, ResourceLocation> LEGACY_TO_CANONICAL = Map.of(
            SKIRMISH, SIGNAL_EMISSION,
            SLAUGHTER, SCAVENGER_HUNT,
            ONSLAUGHT, ESSENCE_GATHERING
    );

    private static final Set<ResourceLocation> CANONICAL = Set.of(
            SIGNAL_EMISSION,
            SCAVENGER_HUNT,
            ESSENCE_GATHERING
    );

    private DungeonObjectiveIds() {
    }

    public static ResourceLocation resolve(ResourceLocation id) {
        if (id == null) {
            return null;
        }
        return LEGACY_TO_CANONICAL.getOrDefault(id, id);
    }

    public static boolean isCanonical(ResourceLocation id) {
        return id != null && CANONICAL.contains(resolve(id));
    }

    public static Map<ResourceLocation, ResourceLocation> legacyToCanonical() {
        return LEGACY_TO_CANONICAL;
    }
}
