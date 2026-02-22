package io.github.ozokuz.incore.features.vendor;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

public interface VendorCurrencyType {
    ResourceLocation id();

    @Nullable
    VendorCurrencySpec parse(JsonObject json);

    VendorCurrencyView buildView(ServerPlayer player, VendorCurrencySpec spec);

    boolean consume(ServerPlayer player, VendorCurrencySpec spec, int quantity, boolean allowConversion);

    VendorCurrencySpec withUnitAmount(VendorCurrencySpec spec, int unitAmount);
}
