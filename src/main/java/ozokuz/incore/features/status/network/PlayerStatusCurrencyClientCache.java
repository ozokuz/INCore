package ozokuz.incore.features.status.network;

import java.util.ArrayList;
import java.util.List;

public final class PlayerStatusCurrencyClientCache {
    private static boolean loaded;
    private static List<CurrencyEntry> entries = List.of();

    private PlayerStatusCurrencyClientCache() {
    }

    public static synchronized void update(List<CurrencyEntry> nextEntries) {
        entries = nextEntries.stream()
                .map(entry -> new CurrencyEntry(entry.iconItemId(), Math.max(0, entry.amount())))
                .toList();
        loaded = true;
    }

    public static synchronized Snapshot snapshot() {
        return new Snapshot(loaded, new ArrayList<>(entries));
    }

    public record CurrencyEntry(String iconItemId, int amount) {
    }

    public record Snapshot(boolean loaded, List<CurrencyEntry> entries) {
    }
}
