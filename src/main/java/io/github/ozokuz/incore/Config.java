package io.github.ozokuz.incore;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue ENTROPY_REGEN_PER_MINUTE = BUILDER
            .comment("How much entropy players regain on each regen tick.")
            .defineInRange("entropyRegenPerMinute", 1, 0, Integer.MAX_VALUE);

    public static final ModConfigSpec.IntValue ENTROPY_REGEN_INTERVAL_SECONDS = BUILDER
            .comment("How often a entropy regen tick happens, in real-world seconds.")
            .defineInRange("entropyRegenIntervalSeconds", 60, 1, Integer.MAX_VALUE);

    public static final ModConfigSpec.IntValue ENTROPY_BASE_CAP = BUILDER
            .comment("Default entropy cap before any bonus cap extensions are applied.")
            .defineInRange("entropyBaseCap", 120, 0, Integer.MAX_VALUE);

    public static final ModConfigSpec.IntValue ENTROPY_CRATE_COST = BUILDER
            .comment("Entropy cost to open one entropy crate.")
            .defineInRange("entropyCrateCost", 60, 0, Integer.MAX_VALUE);

    public static final ModConfigSpec.IntValue ENTROPY_CAP_UPGRADE_AMOUNT = BUILDER
            .comment("How much max entropy a single entropy vessel upgrades.")
            .defineInRange("entropyCapUpgradeAmount", 20, 1, Integer.MAX_VALUE);

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
        BUILDER.push("vendingMachineDiscounts");
    }

    public static final ModConfigSpec.IntValue VENDING_MACHINE_OFFER_DISCOUNT_CHANCE_PERCENT = BUILDER
            .comment("Base chance for each normal vending machine offer to roll a discount.")
            .defineInRange("offerDiscountChancePercent", 12, 0, 100);

    public static final ModConfigSpec.IntValue VENDING_MACHINE_OFFER_DISCOUNT_MIN_PERCENT = BUILDER
            .comment("Minimum discount percent applied when a normal vending machine offer discount roll succeeds.")
            .defineInRange("offerDiscountMinPercent", 10, 0, 100);

    public static final ModConfigSpec.IntValue VENDING_MACHINE_OFFER_DISCOUNT_MAX_PERCENT = BUILDER
            .comment("Maximum discount percent applied when a normal vending machine offer discount roll succeeds.")
            .defineInRange("offerDiscountMaxPercent", 35, 0, 100);

    public static final ModConfigSpec.IntValue VENDING_MACHINE_OFFER_DISCOUNT_CHANCE_CAP_PERCENT = BUILDER
            .comment("Hard cap for final per-offer discount chance after all bonuses are applied.")
            .defineInRange("offerDiscountChanceCapPercent", 95, 0, 100);

    public static final ModConfigSpec.IntValue VENDING_MACHINE_DISCOUNT_CURIO_BONUS_CHANCE_PERCENT = BUILDER
            .comment("Discount chance bonus granted when the vending machine discount charm curio is equipped.")
            .defineInRange("curioBonusChancePercent", 15, 0, 100);

    public static final ModConfigSpec.IntValue VENDING_MACHINE_DISCOUNT_CURIO_BONUS_AMOUNT_PERCENT = BUILDER
            .comment("Discount amount bonus granted when the vending machine discount charm curio is equipped.")
            .defineInRange("curioBonusAmountPercent", 20, 0, 100);

    static {
        BUILDER.pop();
    }

    public static final ModConfigSpec.DoubleValue MARKET_BUY_IMPACT_PER_ITEM = BUILDER
            .comment("Demand index increase applied per bought item.")
            .defineInRange("marketBuyImpactPerItem", 0.0015D, 0D, 100D);

    public static final ModConfigSpec.DoubleValue MARKET_SELL_IMPACT_PER_ITEM = BUILDER
            .comment("Demand index decrease applied per sold item.")
            .defineInRange("marketSellImpactPerItem", 0.0015D, 0D, 100D);

    public static final ModConfigSpec.DoubleValue MARKET_SELL_TAX_RATE = BUILDER
            .comment("Tax rate applied to all market sells (0-1).")
            .defineInRange("marketSellTaxRate", 0.05D, 0D, 1D);

    public static final ModConfigSpec.DoubleValue MARKET_HOURLY_MEAN_REVERSION = BUILDER
            .comment("Hourly reversion amount toward neutral demand (0-1).")
            .defineInRange("marketHourlyMeanReversion", 0.03D, 0D, 1D);

    public static final ModConfigSpec.DoubleValue MARKET_HOURLY_SMALL_REVERSION_CHANCE = BUILDER
            .comment("Chance each item applies small mean-reversion each hour (0-1).")
            .defineInRange("marketHourlySmallReversionChance", 0.10D, 0D, 1D);

    public static final ModConfigSpec.DoubleValue MARKET_HOURLY_NORMAL_MOVE_CHANCE = BUILDER
            .comment("Chance each item applies normal hourly movement (0-1).")
            .defineInRange("marketHourlyNormalMoveChance", 0.82D, 0D, 1D);

    public static final ModConfigSpec.DoubleValue MARKET_HOURLY_RADICAL_CHANCE = BUILDER
            .comment("Chance each item applies radical hourly movement (0-1).")
            .defineInRange("marketHourlyRadicalChance", 0.08D, 0D, 1D);

    public static final ModConfigSpec.DoubleValue MARKET_HOURLY_NORMAL_MAX_CHANGE_PCT = BUILDER
            .comment("Maximum absolute percentage change for normal hourly movement.")
            .defineInRange("marketHourlyNormalMaxChangePct", 0.035D, 0D, 10D);

    public static final ModConfigSpec.DoubleValue MARKET_HOURLY_RADICAL_SPIKE_MIN_PCT = BUILDER
            .comment("Minimum positive percentage for radical hourly spikes.")
            .defineInRange("marketHourlyRadicalSpikeMinPct", 0.10D, 0D, 20D);

    public static final ModConfigSpec.DoubleValue MARKET_HOURLY_RADICAL_SPIKE_MAX_PCT = BUILDER
            .comment("Maximum positive percentage for radical hourly spikes.")
            .defineInRange("marketHourlyRadicalSpikeMaxPct", 0.40D, 0D, 20D);

    public static final ModConfigSpec.DoubleValue MARKET_HOURLY_RADICAL_CRASH_MIN_PCT = BUILDER
            .comment("Minimum percentage for radical hourly crashes.")
            .defineInRange("marketHourlyRadicalCrashMinPct", 0.08D, 0D, 0.99D);

    public static final ModConfigSpec.DoubleValue MARKET_HOURLY_RADICAL_CRASH_MAX_PCT = BUILDER
            .comment("Maximum percentage for radical hourly crashes.")
            .defineInRange("marketHourlyRadicalCrashMaxPct", 0.25D, 0D, 0.99D);

    public static final ModConfigSpec.DoubleValue MARKET_HOURLY_RADICAL_CRASH_VS_SPIKE_BIAS = BUILDER
            .comment("Probability a radical hourly event is a crash instead of a spike (0-1).")
            .defineInRange("marketHourlyRadicalCrashVsSpikeBias", 0.50D, 0D, 1D);

    public static final ModConfigSpec.DoubleValue MARKET_NOON_MOVE_MULTIPLIER = BUILDER
            .comment("Movement magnitude multiplier applied at local noon.")
            .defineInRange("marketNoonMoveMultiplier", 1.35D, 1D, 10D);

    public static final ModConfigSpec.DoubleValue MARKET_MIDNIGHT_MOVE_MULTIPLIER = BUILDER
            .comment("Movement magnitude multiplier applied at local midnight.")
            .defineInRange("marketMidnightMoveMultiplier", 1.50D, 1D, 10D);

    public static final ModConfigSpec.DoubleValue MARKET_BOOTSTRAP_HOURLY_NOISE_PCT = BUILDER
            .comment("Hourly synthetic history movement magnitude for items with no market history.")
            .defineInRange("marketBootstrapHourlyNoisePct", 0.025D, 0D, 1D);

    public static final ModConfigSpec.DoubleValue MARKET_BOOTSTRAP_WICK_NOISE_PCT = BUILDER
            .comment("Synthetic history wick noise percentage for generated candles.")
            .defineInRange("marketBootstrapWickNoisePct", 0.008D, 0D, 1D);

    public static final ModConfigSpec.IntValue MARKET_HISTORY_RETENTION_DAYS = BUILDER
            .comment("How many days of hourly market candles are retained.")
            .defineInRange("marketHistoryRetentionDays", 30, 1, 365);

    public static final ModConfigSpec.DoubleValue MARKET_MIN_PRICE_MULTIPLIER = BUILDER
            .comment("Lower global price multiplier clamp.")
            .defineInRange("marketMinPriceMultiplier", 0.25D, 0.01D, 100D);

    public static final ModConfigSpec.DoubleValue MARKET_MAX_PRICE_MULTIPLIER = BUILDER
            .comment("Upper global price multiplier clamp.")
            .defineInRange("marketMaxPriceMultiplier", 4.0D, 0.01D, 100D);

    public static final ModConfigSpec.IntValue MARKET_SHIPMENT_INTERVAL_TICKS = BUILDER
            .comment("Shipment terminal sell interval in ticks.")
            .defineInRange("marketShipmentIntervalTicks", 40, 1, Integer.MAX_VALUE);

    public static final ModConfigSpec.IntValue MARKET_AUTOBUYER_INTERVAL_TICKS = BUILDER
            .comment("Market autobuyer purchase interval in ticks.")
            .defineInRange("marketAutobuyerIntervalTicks", 40, 1, Integer.MAX_VALUE);

    static final ModConfigSpec SPEC = BUILDER.build();
}
