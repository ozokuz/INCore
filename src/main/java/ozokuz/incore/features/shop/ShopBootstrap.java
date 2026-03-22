package ozokuz.incore.features.shop;

public final class ShopBootstrap {
    private static boolean initialized;

    private ShopBootstrap() {
    }

    public static synchronized void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;

        ShopCurrencyRegistry.register(new BankSpurShopCurrencyType());
        ShopCurrencyRegistry.register(new ItemShopCurrencyType());
    }
}
