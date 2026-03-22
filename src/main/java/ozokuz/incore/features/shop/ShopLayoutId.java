package ozokuz.incore.features.shop;

import org.jetbrains.annotations.Nullable;

public enum ShopLayoutId {
    INDUSTRIAL_MARKET("industrial_market"),
    COMMODITY_EXCHANGE("commodity_exchange"),
    LUXURY_BOUTIQUE("luxury_boutique"),
    ARCADE_VENDOR("arcade_vendor"),
    ARCHIVE_EDITORIAL("archive_editorial"),
    ABYSSAL_TERMINAL("abyssal_terminal");

    private final String serialized;

    ShopLayoutId(String serialized) {
        this.serialized = serialized;
    }

    public String serialized() {
        return serialized;
    }

    public static ShopLayoutId fromString(@Nullable String value) {
        if (value != null) {
            for (ShopLayoutId layoutId : values()) {
                if (layoutId.serialized.equalsIgnoreCase(value)) {
                    return layoutId;
                }
            }
        }
        return INDUSTRIAL_MARKET;
    }
}
