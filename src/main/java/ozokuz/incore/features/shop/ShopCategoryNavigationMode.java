package ozokuz.incore.features.shop;

import org.jetbrains.annotations.Nullable;

public enum ShopCategoryNavigationMode {
    SIDEBAR("sidebar"),
    INLINE_HEADER_STRIP("inline_header_strip"),
    INLINE_SEGMENTED_SELECTOR("inline_segmented_selector"),
    INLINE_CHIPS("inline_chips"),
    INLINE_MODULE_STRIP("inline_module_strip");

    private final String serialized;

    ShopCategoryNavigationMode(String serialized) {
        this.serialized = serialized;
    }

    public String serialized() {
        return serialized;
    }

    public static ShopCategoryNavigationMode fromString(@Nullable String value) {
        if (value != null) {
            for (ShopCategoryNavigationMode mode : values()) {
                if (mode.serialized.equalsIgnoreCase(value)) {
                    return mode;
                }
            }
        }
        return SIDEBAR;
    }
}
