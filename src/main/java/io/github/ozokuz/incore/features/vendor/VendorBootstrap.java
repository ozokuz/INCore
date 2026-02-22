package io.github.ozokuz.incore.features.vendor;

public final class VendorBootstrap {
    private static boolean initialized;

    private VendorBootstrap() {
    }

    public static synchronized void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;

        VendorProductRegistry.register(new ItemVendorProductType());
        VendorCurrencyRegistry.register(new ItemVendorCurrencyType());
        VendorCurrencyRegistry.register(new BankSpurVendorCurrencyType());
    }
}
