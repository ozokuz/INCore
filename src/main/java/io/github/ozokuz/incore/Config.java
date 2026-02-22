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

    public static final ModConfigSpec.DoubleValue BURNER_LAB_SPEED_MULTIPLIER = BUILDER
            .comment("Burner lab processing speed multiplier.")
            .defineInRange("burnerLabSpeedMultiplier", 0.75D, 0.05D, 16.0D);

    public static final ModConfigSpec.DoubleValue MECHANICAL_LAB_SPEED_PER_32_RPM = BUILDER
            .comment("Mechanical lab speed multiplier factor per 32 RPM.")
            .defineInRange("mechanicalLabSpeedPer32Rpm", 1.0D, 0.01D, 64.0D);

    public static final ModConfigSpec.IntValue MECHANICAL_LAB_STRESS_PER_RPM = BUILDER
            .comment("Displayed mechanical lab stress demand per RPM.")
            .defineInRange("mechanicalLabStressPerRpm", 2, 0, Integer.MAX_VALUE);

    public static final ModConfigSpec.IntValue MODULAR_LAB_FE_CAPACITY = BUILDER
            .comment("Modular lab FE capacity.")
            .defineInRange("modularLabFeCapacity", 100000, 1, Integer.MAX_VALUE);

    public static final ModConfigSpec.IntValue MODULAR_LAB_FE_MAX_RECEIVE = BUILDER
            .comment("Modular lab FE max receive per tick.")
            .defineInRange("modularLabFeMaxReceive", 1000, 1, Integer.MAX_VALUE);

    public static final ModConfigSpec.IntValue MODULAR_LAB_FE_MAX_EXTRACT = BUILDER
            .comment("Modular lab FE max extract per tick.")
            .defineInRange("modularLabFeMaxExtract", 1000, 0, Integer.MAX_VALUE);

    public static final ModConfigSpec.IntValue MODULAR_LAB_FE_PER_TICK = BUILDER
            .comment("Base modular lab FE usage per tick while processing.")
            .defineInRange("modularLabFePerTick", 40, 1, Integer.MAX_VALUE);

    public static final ModConfigSpec.DoubleValue MODULAR_LAB_SPEED_CARD_BONUS = BUILDER
            .comment("Speed bonus per installed speed module card.")
            .defineInRange("modularLabSpeedCardBonus", 0.25D, 0.0D, 16.0D);

    public static final ModConfigSpec.DoubleValue MODULAR_LAB_MAX_SPEED_BONUS = BUILDER
            .comment("Maximum total speed bonus from module cards.")
            .defineInRange("modularLabMaxSpeedBonus", 2.0D, 0.0D, 64.0D);

    public static final ModConfigSpec.DoubleValue MODULAR_LAB_PRODUCTIVITY_CARD_BONUS = BUILDER
            .comment("Productivity chance bonus per installed productivity module card.")
            .defineInRange("modularLabProductivityCardBonus", 0.10D, 0.0D, 1.0D);

    public static final ModConfigSpec.DoubleValue MODULAR_LAB_MAX_PRODUCTIVITY_BONUS = BUILDER
            .comment("Maximum productivity chance bonus from module cards.")
            .defineInRange("modularLabMaxProductivityBonus", 0.50D, 0.0D, 1.0D);

    static {
        BUILDER.push("vendorDiscounts");
    }

    public static final ModConfigSpec.IntValue VENDOR_OFFER_DISCOUNT_CHANCE_PERCENT = BUILDER
            .comment("Base chance for each normal vendor offer to roll a discount.")
            .defineInRange("offerDiscountChancePercent", 12, 0, 100);

    public static final ModConfigSpec.IntValue VENDOR_OFFER_DISCOUNT_MIN_PERCENT = BUILDER
            .comment("Minimum discount percent applied when a normal vendor offer discount roll succeeds.")
            .defineInRange("offerDiscountMinPercent", 10, 0, 100);

    public static final ModConfigSpec.IntValue VENDOR_OFFER_DISCOUNT_MAX_PERCENT = BUILDER
            .comment("Maximum discount percent applied when a normal vendor offer discount roll succeeds.")
            .defineInRange("offerDiscountMaxPercent", 35, 0, 100);

    public static final ModConfigSpec.IntValue VENDOR_OFFER_DISCOUNT_CHANCE_CAP_PERCENT = BUILDER
            .comment("Hard cap for final per-offer discount chance after all bonuses are applied.")
            .defineInRange("offerDiscountChanceCapPercent", 95, 0, 100);

    public static final ModConfigSpec.IntValue VENDOR_DISCOUNT_CURIO_BONUS_CHANCE_PERCENT = BUILDER
            .comment("Discount chance bonus granted when the vendor discount charm curio is equipped.")
            .defineInRange("curioBonusChancePercent", 15, 0, 100);

    public static final ModConfigSpec.IntValue VENDOR_DISCOUNT_CURIO_BONUS_AMOUNT_PERCENT = BUILDER
            .comment("Discount amount bonus granted when the vendor discount charm curio is equipped.")
            .defineInRange("curioBonusAmountPercent", 20, 0, 100);

    static {
        BUILDER.pop();
    }

    static final ModConfigSpec SPEC = BUILDER.build();
}
