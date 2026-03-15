package ozokuz.incore.features.surfaceore;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;

public enum DimensionCategory {
    OVERWORLD,
    NETHER,
    END;

    public static DimensionCategory fromLevel(ResourceKey<Level> dimension) {
        if (dimension.equals(Level.NETHER)) {
            return NETHER;
        }
        if (dimension.equals(Level.END)) {
            return END;
        }
        return OVERWORLD;
    }

    public static DimensionCategory fromDimensionType(DimensionType dimensionType) {
        if (dimensionType.ultraWarm()) {
            return NETHER;
        }
        if (dimensionType.hasCeiling()) {
            return END;
        }
        return OVERWORLD;
    }
}
