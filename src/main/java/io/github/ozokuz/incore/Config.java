package io.github.ozokuz.incore;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue SANITY_REGEN_PER_MINUTE = BUILDER
            .comment("How much sanity players regain on each regen tick.")
            .defineInRange("sanityRegenPerMinute", 1, 0, Integer.MAX_VALUE);

    public static final ModConfigSpec.IntValue SANITY_REGEN_INTERVAL_SECONDS = BUILDER
            .comment("How often a sanity regen tick happens, in real-world seconds.")
            .defineInRange("sanityRegenIntervalSeconds", 60, 1, Integer.MAX_VALUE);

    public static final ModConfigSpec.IntValue SANITY_BASE_CAP = BUILDER
            .comment("Default sanity cap before any bonus cap extensions are applied.")
            .defineInRange("sanityBaseCap", 120, 0, Integer.MAX_VALUE);

    public static final ModConfigSpec.IntValue SANITY_CRATE_COST = BUILDER
            .comment("Sanity cost to open one sanity crate.")
            .defineInRange("sanityCrateCost", 60, 0, Integer.MAX_VALUE);

    public static final ModConfigSpec.IntValue SANITY_CAP_UPGRADE_AMOUNT = BUILDER
            .comment("How much max sanity a single sanity vessel upgrades.")
            .defineInRange("sanityCapUpgradeAmount", 20, 1, Integer.MAX_VALUE);

    public static final ModConfigSpec.IntValue PLAYER_LEVEL_BASE_XP_COST = BUILDER
            .comment("Base custom experience needed for the first player level up.")
            .defineInRange("playerLevelBaseXpCost", 100, 1, Integer.MAX_VALUE);

    public static final ModConfigSpec.IntValue PLAYER_LEVEL_XP_COST_INCREASE = BUILDER
            .comment("How much extra custom experience each next level requires.")
            .defineInRange("playerLevelXpCostIncrease", 20, 0, Integer.MAX_VALUE);

    public static final ModConfigSpec.IntValue ENCOUNTER_TRIGGER_RADIUS = BUILDER
            .comment("How close players must be to trigger an encounter spawner.")
            .defineInRange("encounterTriggerRadius", 8, 1, Integer.MAX_VALUE);

    public static final ModConfigSpec.IntValue WALL_JUMP_STAMINA_COST = BUILDER
            .comment("Stamina cost consumed by each wall jump.")
            .defineInRange("wallJumpStaminaCost", 200, 0, Integer.MAX_VALUE);

    static final ModConfigSpec SPEC = BUILDER.build();
}
