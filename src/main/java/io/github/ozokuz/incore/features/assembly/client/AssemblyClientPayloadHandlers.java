package io.github.ozokuz.incore.features.assembly.client;

import io.github.ozokuz.incore.features.assembly.content.AssemblyStationScreen;
import io.github.ozokuz.incore.features.assembly.content.AutoAssemblerScreen;
import net.minecraft.client.Minecraft;

import java.util.List;

public final class AssemblyClientPayloadHandlers {
    private AssemblyClientPayloadHandlers() {
    }

    public static void handleSnapshot(List<String> unlockedRecipeIds) {
        AssemblyClientCache.update(unlockedRecipeIds);
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof AssemblyStationScreen screen) {
            screen.updateFromCache();
        } else if (minecraft.screen instanceof AutoAssemblerScreen screen) {
            screen.updateFromCache();
        }
    }
}
