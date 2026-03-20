package ozokuz.incore.features.shop;

import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public enum ShopTabId {
    SUPPLIES("supplies", "screen.incore.shop.tab.supplies"),
    ROTATIONS("rotations", "screen.incore.shop.tab.rotations"),
    CACHES("caches", "screen.incore.shop.tab.caches");

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

    public static ShopTabId fromString(@Nullable String value) {
        if (value != null) {
            for (ShopTabId tabId : values()) {
                if (tabId.serialized.equalsIgnoreCase(value)) {
                    return tabId;
                }
            }
        }
        return SUPPLIES;
    }
}
