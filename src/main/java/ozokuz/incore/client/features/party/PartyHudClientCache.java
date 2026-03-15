package ozokuz.incore.client.features.party;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class PartyHudClientCache {
    private static List<MemberView> members = List.of();

    private PartyHudClientCache() {
    }

    public static synchronized void update(List<MemberView> rows) {
        members = rows == null ? List.of() : List.copyOf(rows);
    }

    public static synchronized List<MemberView> members() {
        return new ArrayList<>(members);
    }

    public static synchronized void clear() {
        members = List.of();
    }

    public record MemberView(UUID memberId, String name, float health, float maxHealth) {
    }
}
