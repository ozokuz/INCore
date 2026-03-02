package io.github.ozokuz.incore.client.features.arena;

import net.minecraft.client.Minecraft;

public final class ArenaClientPayloadHandlers {
    private ArenaClientPayloadHandlers() {
    }

    public static void openCatalogScreen(String json) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof CombatCatalogScreen screen) {
            screen.updatePayload(json);
            return;
        }
        minecraft.setScreen(new CombatCatalogScreen(json));
    }
}
