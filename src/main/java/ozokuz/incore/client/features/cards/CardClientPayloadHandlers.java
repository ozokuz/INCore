package ozokuz.incore.client.features.cards;

import com.google.gson.Gson;
import net.minecraft.client.Minecraft;
import ozokuz.incore.features.cards.CardPackService;
import ozokuz.incore.integration.ldlib.ui.cards.CardPackOpeningLdLibScreen;

public final class CardClientPayloadHandlers {
    private static final Gson GSON = new Gson();

    private CardClientPayloadHandlers() {
    }

    public static void openPackScreen(String json) {
        CardPackService.PackRevealScreenData data = GSON.fromJson(json, CardPackService.PackRevealScreenData.class);
        if (data == null || data.pulls() == null) {
            return;
        }

        Minecraft.getInstance().setScreen(new CardPackOpeningLdLibScreen(data));
    }
}
