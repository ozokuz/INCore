package ozokuz.incore.integration.ldlib.ui;

import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import ozokuz.incore.INCore;

public final class INCoreUiIds {
    public static final ResourceLocation PLAYER_STATUS = id("player_status");
    public static final ResourceLocation PLAYER_LEVEL_REWARDS = id("player_level_rewards");
    public static final ResourceLocation DUNGEON_DIFFICULTY = id("dungeon_difficulty");
    public static final ResourceLocation TASK_OVERVIEW = id("task_overview");
    public static final ResourceLocation BATTLE_PASS = id("battle_pass");
    public static final ResourceLocation RESEARCH_TREE = id("research_tree");
    public static final ResourceLocation COMBAT_CATALOG = id("combat_catalog");
    public static final ResourceLocation PARTY_MANAGEMENT = id("party_management");
    public static final ResourceLocation CARD_PACK_OPENING = id("card_pack_opening");
    public static final ResourceLocation MARKET_APP = id("market");
    public static final ResourceLocation SHOP_APP = id("shop");
    public static final ResourceLocation GACHA_APP = id("gacha");
    public static final ResourceLocation GACHA_INFO = id("gacha_info");
    public static final ResourceLocation GACHA_GUARANTEED_SELECTION = id("gacha_guaranteed_selection");

    public static final Set<ResourceLocation> ALL_PLAYER_UI_IDS = Set.of(
            PLAYER_STATUS,
            PLAYER_LEVEL_REWARDS,
            DUNGEON_DIFFICULTY,
            TASK_OVERVIEW,
            BATTLE_PASS,
            RESEARCH_TREE,
            COMBAT_CATALOG,
            PARTY_MANAGEMENT,
            CARD_PACK_OPENING,
            MARKET_APP,
            SHOP_APP,
            GACHA_APP,
            GACHA_INFO,
            GACHA_GUARANTEED_SELECTION
    );

    private INCoreUiIds() {
    }

    public static boolean isKnownPlayerUi(ResourceLocation routeId) {
        return ALL_PLAYER_UI_IDS.contains(routeId);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(INCore.MODID, "ui/" + path);
    }
}
