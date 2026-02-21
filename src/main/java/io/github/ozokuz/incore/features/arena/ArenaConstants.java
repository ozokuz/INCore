package io.github.ozokuz.incore.features.arena;

import io.github.ozokuz.incore.INCore;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

public final class ArenaConstants {
    public static final ResourceLocation ARENA_ID = ResourceLocation.parse(INCore.MODID + ":arena");
    public static final ResourceKey<Level> ARENA_DIMENSION = ResourceKey.create(Registries.DIMENSION, ARENA_ID);

    public static final int ARENA_SPACING = 512;
    public static final int ARENA_BASE_Y = 64;
    public static final int ARENA_RADIUS = 44;
    public static final int ARENA_FLOOR_DEPTH = 10;
    public static final int ARENA_WALL_HEIGHT = 20;

    private ArenaConstants() {
    }
}
