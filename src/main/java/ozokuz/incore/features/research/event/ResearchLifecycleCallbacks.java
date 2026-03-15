package ozokuz.incore.features.research.event;

import ozokuz.incore.INCore;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class ResearchLifecycleCallbacks {
    private static final List<Listener> LISTENERS = new CopyOnWriteArrayList<>();

    private ResearchLifecycleCallbacks() {
    }

    public static void register(Listener listener) {
        if (listener != null) {
            LISTENERS.add(listener);
        }
    }

    public static void unregister(Listener listener) {
        LISTENERS.remove(listener);
    }

    public static void onResearchStarted(String teamId, ResourceLocation nodeId, int requiredTime) {
        for (Listener listener : LISTENERS) {
            try {
                listener.onResearchStarted(teamId, nodeId, requiredTime);
            } catch (Exception exception) {
                INCore.LOGGER.warn("Research start listener failed: {}", exception.getMessage());
            }
        }
    }

    public static void onResearchProgress(String teamId, ResourceLocation nodeId, int timeProgress, int requiredTime, int rpConsumed) {
        for (Listener listener : LISTENERS) {
            try {
                listener.onResearchProgress(teamId, nodeId, timeProgress, requiredTime, rpConsumed);
            } catch (Exception exception) {
                INCore.LOGGER.warn("Research progress listener failed: {}", exception.getMessage());
            }
        }
    }

    public static void onResearchCompleted(String teamId, ResourceLocation nodeId) {
        for (Listener listener : LISTENERS) {
            try {
                listener.onResearchCompleted(teamId, nodeId);
            } catch (Exception exception) {
                INCore.LOGGER.warn("Research complete listener failed: {}", exception.getMessage());
            }
        }
    }

    public interface Listener {
        default void onResearchStarted(String teamId, ResourceLocation nodeId, int requiredTime) {
        }

        default void onResearchProgress(String teamId, ResourceLocation nodeId, int timeProgress, int requiredTime, int rpConsumed) {
        }

        default void onResearchCompleted(String teamId, ResourceLocation nodeId) {
        }
    }
}
