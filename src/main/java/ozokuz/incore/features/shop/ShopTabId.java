package ozokuz.incore.features.shop;

import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public enum ShopTabId {
    INDUSTRIAL_MARKET("industrial_market", "screen.incore.shop.tab.industrial_market"),
    COMMODITY_EXCHANGE("commodity_exchange", "screen.incore.shop.tab.commodity_exchange"),
    LUXURY_BOUTIQUE("luxury_boutique", "screen.incore.shop.tab.luxury_boutique"),
    ARCADE_VENDOR("arcade_vendor", "screen.incore.shop.tab.arcade_vendor"),
    ARCHIVE_EDITORIAL("archive_editorial", "screen.incore.shop.tab.archive_editorial"),
    ABYSSAL_TERMINAL("abyssal_terminal", "screen.incore.shop.tab.abyssal_terminal");

    private final String serialized;
    private final String translationKey;

    ShopTabId(String serialized, String translationKey) {
        this.serialized = serialized;
        this.translationKey = translationKey;
    }

    public String serialized() {
        return serialized;
    }

    public Component displayName() {
        return Component.translatable(translationKey);
    }

    public static @Nullable ShopTabId tryParse(@Nullable String value) {
        if (value != null) {
            for (ShopTabId tabId : values()) {
                if (tabId.serialized.equalsIgnoreCase(value)) {
                    return tabId;
                }
            }
        }
        return null;
    }

    public static ShopTabId fromString(@Nullable String value) {
        ShopTabId parsed = tryParse(value);
        return parsed == null ? INDUSTRIAL_MARKET : parsed;
    }
}
