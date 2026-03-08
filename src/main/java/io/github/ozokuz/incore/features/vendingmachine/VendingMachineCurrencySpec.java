package io.github.ozokuz.incore.features.vendingmachine;

import net.minecraft.resources.ResourceLocation;

public interface VendingMachineCurrencySpec {
    ResourceLocation typeId();

    int unitAmount();

    int spurConversionRate();
}
