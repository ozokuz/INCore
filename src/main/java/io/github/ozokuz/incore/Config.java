package io.github.ozokuz.incore;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue SANITY_REGEN_PER_MINUTE = BUILDER
            .comment("How much sanity players regain every real-world minute.")
            .defineInRange("sanityRegenPerMinute", 1, 0, Integer.MAX_VALUE);

    public static final ModConfigSpec.IntValue SANITY_BASE_CAP = BUILDER
            .comment("Default sanity cap before any bonus cap extensions are applied.")
            .defineInRange("sanityBaseCap", 120, 0, Integer.MAX_VALUE);

    public static final ModConfigSpec.IntValue SANITY_CRATE_COST = BUILDER
            .comment("Sanity cost to open one sanity crate.")
            .defineInRange("sanityCrateCost", 60, 0, Integer.MAX_VALUE);

    public static final ModConfigSpec.IntValue SANITY_CAP_UPGRADE_AMOUNT = BUILDER
            .comment("How much max sanity a single sanity vessel upgrades.")
            .defineInRange("sanityCapUpgradeAmount", 20, 1, Integer.MAX_VALUE);

    public static final ModConfigSpec.IntValue ENCOUNTER_TRIGGER_RADIUS = BUILDER
            .comment("How close players must be to trigger an encounter spawner.")
            .defineInRange("encounterTriggerRadius", 8, 1, Integer.MAX_VALUE);

    static final ModConfigSpec SPEC = BUILDER.build();
}
