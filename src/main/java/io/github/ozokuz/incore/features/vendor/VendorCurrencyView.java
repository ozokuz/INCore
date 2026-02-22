package io.github.ozokuz.incore.features.vendor;

import org.jetbrains.annotations.Nullable;

public record VendorCurrencyView(
        String typeId,
        String primaryIconItemId,
        String primaryLabel,
        int amountPerUnit,
        int availablePrimary,
        @Nullable String conversionTypeId,
        @Nullable String conversionIconItemId,
        @Nullable String conversionLabel,
        int conversionRatio,
        int availableConversion
) {
}
