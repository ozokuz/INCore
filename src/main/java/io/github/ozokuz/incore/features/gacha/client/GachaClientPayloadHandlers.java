package io.github.ozokuz.incore.features.gacha.client;

import com.google.gson.Gson;
import io.github.ozokuz.incore.features.gacha.GachaService;
import net.minecraft.client.Minecraft;

public final class GachaClientPayloadHandlers {
    private static final Gson GSON = new Gson();

    private GachaClientPayloadHandlers() {
    }

    public static void openBannersScreen(String json) {
        GachaService.ScreenData data = GSON.fromJson(json, GachaService.ScreenData.class);
        if (data == null || data.banners() == null) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        minecraft.setScreen(new GachaBannerScreen(data));
    }
}
