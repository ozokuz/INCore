package io.github.ozokuz.incore.features.vendor;

import net.minecraft.resources.ResourceLocation;

public interface VendorCurrencySpec {
    ResourceLocation typeId();

    int unitAmount();

    int spurConversionRate();
}
