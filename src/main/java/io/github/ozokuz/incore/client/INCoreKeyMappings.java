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

    private INCoreKeyMappings() {
    }
}
