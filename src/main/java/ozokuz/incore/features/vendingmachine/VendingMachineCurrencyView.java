package ozokuz.incore.features.vendingmachine;

import org.jetbrains.annotations.Nullable;

public record VendingMachineCurrencyView(
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
