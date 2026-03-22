package ozokuz.incore.features.shop;

import org.jetbrains.annotations.Nullable;

public enum ShopPaletteId {
    TACTICAL_ARCHIVE("tactical_archive"),
    STEEL_AEGIS("steel_aegis"),
    OBSIDIAN_EMBER("obsidian_ember"),
    NEON_SHADOW("neon_shadow"),
    BLOOD_PROTOCOL("blood_protocol"),
    ABYSSAL_PROTOCOL("abyssal_protocol");

    private final String serialized;

    ShopPaletteId(String serialized) {
        this.serialized = serialized;
    }

    public String serialized() {
        return serialized;
    }

    public static ShopPaletteId fromString(@Nullable String value) {
        if (value != null) {
            for (ShopPaletteId paletteId : values()) {
                if (paletteId.serialized.equalsIgnoreCase(value)) {
                    return paletteId;
                }
            }
        }
        return TACTICAL_ARCHIVE;
    }
}
