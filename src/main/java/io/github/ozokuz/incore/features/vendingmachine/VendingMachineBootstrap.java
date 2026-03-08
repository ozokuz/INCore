package io.github.ozokuz.incore.features.vendingmachine;

public final class VendingMachineBootstrap {
    private static boolean initialized;

    private VendingMachineBootstrap() {
    }

    public static synchronized void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;

        VendingMachineProductRegistry.register(new ItemVendingMachineProductType());
        VendingMachineCurrencyRegistry.register(new ItemVendingMachineCurrencyType());
        VendingMachineCurrencyRegistry.register(new BankSpurVendingMachineCurrencyType());
    }
}
