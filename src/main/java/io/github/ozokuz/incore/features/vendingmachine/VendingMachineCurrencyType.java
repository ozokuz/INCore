package io.github.ozokuz.incore.features.vendingmachine;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

public interface VendingMachineCurrencyType {
    ResourceLocation id();

    @Nullable
    VendingMachineCurrencySpec parse(JsonObject json);

    VendingMachineCurrencyView buildView(ServerPlayer player, VendingMachineCurrencySpec spec);

    boolean consume(ServerPlayer player, VendingMachineCurrencySpec spec, int quantity, boolean allowConversion);

    VendingMachineCurrencySpec withUnitAmount(VendingMachineCurrencySpec spec, int unitAmount);
}
