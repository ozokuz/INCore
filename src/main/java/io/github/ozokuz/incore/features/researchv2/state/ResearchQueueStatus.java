package io.github.ozokuz.incore.features.researchv2.state;

import org.jetbrains.annotations.Nullable;

public enum ResearchQueueStatus {
    QUEUED,
    RUNNING,
    PAUSED_MISSING_INPUTS,
    PAUSED_NO_POWER;

    public static ResearchQueueStatus fromSerialized(@Nullable String value, int timeProgress) {
        if (value != null) {
            try {
                return ResearchQueueStatus.valueOf(value);
            } catch (IllegalArgumentException ignored) {
            }
        }
        return timeProgress > 0 ? RUNNING : QUEUED;
    }
}
