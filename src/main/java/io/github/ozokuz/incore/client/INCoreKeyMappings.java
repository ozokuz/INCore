package io.github.ozokuz.incore.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public final class INCoreKeyMappings {
    public static final KeyMapping OPEN_PLAYER_STATUS = new KeyMapping(
            "key.incore.open_player_status",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_O,
            "key.categories.incore"
    );
    public static final KeyMapping OPEN_GACHA_BANNERS = new KeyMapping(
            "key.incore.open_gacha_banners",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_P,
            "key.categories.incore"
    );
    public static final KeyMapping OPEN_TASK_OVERVIEW = new KeyMapping(
            "key.incore.open_task_overview",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_K,
            "key.categories.incore"
    );
    public static final KeyMapping OPEN_RESEARCH_TREE = new KeyMapping(
            "key.incore.open_research_tree",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_U,
            "key.categories.incore"
    );
    public static final KeyMapping OPEN_BATTLE_PASS = new KeyMapping(
            "key.incore.open_battle_pass",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_B,
            "key.categories.incore"
    );
    public static final KeyMapping OPEN_COMBAT_CATALOG = new KeyMapping(
            "key.incore.open_combat_catalog",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            "key.categories.incore"
    );
    public static final KeyMapping OPEN_NUMISMATICS_BANK = new KeyMapping(
            "key.incore.open_numismatics_bank",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_N,
            "key.categories.incore"
    );
    public static final KeyMapping OPEN_MARKET = new KeyMapping(
            "key.incore.open_market",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_M,
            "key.categories.incore"
    );
    public static final KeyMapping OPEN_SHOP = new KeyMapping(
            "key.incore.open_shop",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_L,
            "key.categories.incore"
    );

    private INCoreKeyMappings() {
    }
}
