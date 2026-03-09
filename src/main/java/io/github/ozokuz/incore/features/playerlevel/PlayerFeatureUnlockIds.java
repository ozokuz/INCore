package io.github.ozokuz.incore.features.playerlevel;

import net.minecraft.resources.ResourceLocation;

public final class PlayerFeatureUnlockIds {
    public static final ResourceLocation BATTLEPASS_SCREEN = id("battlepass_screen");
    public static final ResourceLocation GACHA_BASIC = id("gacha_basic");
    public static final ResourceLocation ARENA_TIER_1 = id("arena_tier_1");
    public static final ResourceLocation SHOP_SCREEN = id("shop_screen");
    public static final ResourceLocation SHOP_BASIC_SUPPLIES = id("shop_basic_supplies");
    public static final ResourceLocation MARKET_BASIC = id("market_basic");
    public static final ResourceLocation BATTLEPASS_LANE_NORTHIUM_ACCESS = id("battlepass_lane_northium_access");
    public static final ResourceLocation SHOP_DAILY_EXCHANGE = id("shop_daily_exchange");
    public static final ResourceLocation ARENA_TIER_2 = id("arena_tier_2");
    public static final ResourceLocation GACHA_CHARTERED = id("gacha_chartered");
    public static final ResourceLocation MARKET_SHIPMENT_TERMINAL = id("market_shipment_terminal");
    public static final ResourceLocation ARENA_TIER_3 = id("arena_tier_3");
    public static final ResourceLocation BATTLEPASS_LANE_INTEGRATED_ACCESS = id("battlepass_lane_integrated_access");
    public static final ResourceLocation SHOP_CHARTERED_ROTATION = id("shop_chartered_rotation");
    public static final ResourceLocation ARENA_TIER_4 = id("arena_tier_4");
    public static final ResourceLocation GACHA_EXPEDITION = id("gacha_expedition");
    public static final ResourceLocation MARKET_AUTOTRADER = id("market_autotrader");
    public static final ResourceLocation ARENA_TIER_5 = id("arena_tier_5");
    public static final ResourceLocation SHOP_EXPEDITION_CACHE = id("shop_expedition_cache");
    public static final ResourceLocation MARKET_SHIPMENT_TERMINAL_MK2 = id("market_shipment_terminal_mk2");
    public static final ResourceLocation MARKET_AUTOTRADER_MK2 = id("market_autotrader_mk2");

    private PlayerFeatureUnlockIds() {
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.parse("incore:" + path);
    }
}
