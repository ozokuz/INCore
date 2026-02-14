package io.github.ozokuz.incore.features.gacha;

import org.joml.Vector3f;

public enum GachaRarity {
    TWO_STAR(2, 0x3FD35C),
    THREE_STAR(3, 0x3A7CFF),
    FOUR_STAR(4, 0xA15CFF),
    FIVE_STAR(5, 0xFFB347),
    SIX_STAR(6, 0xFF3B30);

    private final int stars;
    private final int rgb;

    GachaRarity(int stars, int rgb) {
        this.stars = stars;
        this.rgb = rgb;
    }

    public int stars() {
        return stars;
    }

    public int rgb() {
        return rgb;
    }

    public Vector3f dustColor() {
        float red = ((rgb >> 16) & 0xFF) / 255.0F;
        float green = ((rgb >> 8) & 0xFF) / 255.0F;
        float blue = (rgb & 0xFF) / 255.0F;
        return new Vector3f(red, green, blue);
    }

    public static GachaRarity fromStars(int stars) {
        for (GachaRarity rarity : values()) {
            if (rarity.stars == stars) {
                return rarity;
            }
        }
        return TWO_STAR;
    }
}
