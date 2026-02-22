package io.github.ozokuz.incore.features.cards.client;

import com.google.gson.Gson;
import io.github.ozokuz.incore.features.cards.CardPackService;
import io.github.ozokuz.incore.features.cards.CardVendorService;
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

    public static void openVendorScreen(String json) {
        CardVendorService.VendorScreenData data = GSON.fromJson(json, CardVendorService.VendorScreenData.class);
        if (data == null || data.offers() == null) {
            return;
        }

        Minecraft.getInstance().setScreen(new CardVendorScreen(data));
    }
}
