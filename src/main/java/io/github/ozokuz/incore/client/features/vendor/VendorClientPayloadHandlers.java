package io.github.ozokuz.incore.client.features.vendor;

import com.google.gson.Gson;
import io.github.ozokuz.incore.features.vendor.VendorService;
import net.minecraft.client.Minecraft;

public final class VendorClientPayloadHandlers {
    private static final Gson GSON = new Gson();

    private VendorClientPayloadHandlers() {
    }

    public static void openVendorScreen(String json) {
        VendorService.VendorScreenData data = GSON.fromJson(json, VendorService.VendorScreenData.class);
        if (data == null || data.offers() == null) {
            return;
        }

        Minecraft.getInstance().setScreen(new VendorScreen(data));
    }
}
