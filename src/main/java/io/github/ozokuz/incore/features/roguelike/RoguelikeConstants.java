package io.github.ozokuz.incore.features.roguelike;

import io.github.ozokuz.incore.INCore;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

public final class RoguelikeConstants {
    public static final ResourceLocation DUNGEON_ID = ResourceLocation.parse(INCore.MODID + ":dungeon");
    public static final ResourceKey<Level> DUNGEON_DIMENSION = ResourceKey.create(Registries.DIMENSION, DUNGEON_ID);

    @Deprecated
    public static final ResourceKey<Level> ROGUELIKE_DIMENSION = DUNGEON_DIMENSION;

    public static final int INSTANCE_SIZE_CHUNKS = 43;
    public static final int INSTANCE_SLOT_STRIDE_CHUNKS = 64;
    public static final int REGION_SIZE_CHUNKS = 32;
    public static final int DUNGEON_FLOOR_Y = 40;
    public static final int DUNGEON_TIME_LIMIT_TICKS = 20 * 60 * 15;
    public static final int MANAGER_TICK_INTERVAL = 20;

    private RoguelikeConstants() {
    }
}
