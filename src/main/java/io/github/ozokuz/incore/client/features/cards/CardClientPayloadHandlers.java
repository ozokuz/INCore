package io.github.ozokuz.incore.client.features.cards;

import com.google.gson.Gson;
import io.github.ozokuz.incore.features.cards.CardPackService;
import net.minecraft.client.Minecraft;

public final class CardClientPayloadHandlers {
    private static final Gson GSON = new Gson();

    private CardClientPayloadHandlers() {
    }

    public static void openPackScreen(String json) {
        CardPackService.PackRevealScreenData data = GSON.fromJson(json, CardPackService.PackRevealScreenData.class);
        if (data == null || data.pulls() == null) {
            return;
        }

        Minecraft.getInstance().setScreen(new CardPackOpeningScreen(data));
    }
}
