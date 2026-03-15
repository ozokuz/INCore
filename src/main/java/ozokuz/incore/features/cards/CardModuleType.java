package ozokuz.incore.features.cards;

public enum CardModuleType {
    REGULAR,
    CRYPTIC,
    CURSED,
    CHAOTIC,
    CORRUPTED;

    public static CardModuleType fromString(String value) {
        if (value == null || value.isBlank()) {
            return REGULAR;
        }

        return switch (value.toLowerCase()) {
            case "cryptic" -> CRYPTIC;
            case "cursed" -> CURSED;
            case "chaotic" -> CHAOTIC;
            case "corrupted" -> CORRUPTED;
            default -> REGULAR;
        };
    }
}
