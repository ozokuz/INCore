package ozokuz.incore.client.features.vendingmachine;

import com.google.gson.Gson;
import ozokuz.incore.features.vendingmachine.VendingMachineService;
import net.minecraft.client.Minecraft;

public final class VendingMachineClientPayloadHandlers {
    private static final Gson GSON = new Gson();

    private VendingMachineClientPayloadHandlers() {
    }

    public static void openVendingMachineScreen(String json) {
        VendingMachineService.VendingMachineScreenData data = GSON.fromJson(json, VendingMachineService.VendingMachineScreenData.class);
        if (data == null || data.offers() == null) {
            return;
        }

        Minecraft.getInstance().setScreen(new VendingMachineScreen(data));
    }
}
