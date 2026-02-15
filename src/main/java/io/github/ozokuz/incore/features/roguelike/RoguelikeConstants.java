package io.github.ozokuz.incore.features.roguelike;

import io.github.ozokuz.incore.INCore;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

public final class RoguelikeConstants {
    public static final ResourceLocation ROGUELIKE_ID = ResourceLocation.parse(INCore.MODID + ":roguelike");
    public static final ResourceKey<Level> ROGUELIKE_DIMENSION = ResourceKey.create(Registries.DIMENSION, ROGUELIKE_ID);
    public static final int DUNGEON_ROOM_SIZE = 33;
    public static final int DUNGEON_ROOM_HEIGHT = 9;
    public static final int DUNGEON_SPACING = 512;
    public static final int DUNGEON_BASE_Y = 64;
    public static final int DUNGEON_TIME_LIMIT_TICKS = 20 * 60 * 15;

    private RoguelikeConstants() {
    }
}
