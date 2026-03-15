package ozokuz.incore.features.research;

import net.minecraft.resources.ResourceLocation;

import java.util.Random;

public final class ResearchDeterministicRng {
    private static final long FNV_OFFSET_BASIS = 0xcbf29ce484222325L;
    private static final long FNV_PRIME = 0x100000001b3L;

    private ResearchDeterministicRng() {
    }

    public static long seed(String teamId, String stationId, ResourceLocation nodeId, int completedRuns, String eventKey) {
        long hash = FNV_OFFSET_BASIS;
        hash = update(hash, teamId == null ? "" : teamId);
        hash = update(hash, "|");
        hash = update(hash, stationId == null ? "" : stationId);
        hash = update(hash, "|");
        hash = update(hash, nodeId == null ? "" : nodeId.toString());
        hash = update(hash, "|");
        hash = update(hash, Integer.toString(Math.max(0, completedRuns)));
        hash = update(hash, "|");
        hash = update(hash, eventKey == null ? "" : eventKey);
        return hash;
    }

    public static boolean rollChance(String teamId, String stationId, ResourceLocation nodeId, int completedRuns, String eventKey, double chance) {
        if (chance <= 0.0D) {
            return false;
        }
        if (chance >= 1.0D) {
            return true;
        }
        return new Random(seed(teamId, stationId, nodeId, completedRuns, eventKey)).nextDouble() < chance;
    }

    private static long update(long hash, String value) {
        for (int i = 0; i < value.length(); i++) {
            hash ^= value.charAt(i);
            hash *= FNV_PRIME;
        }
        return hash;
    }
}
