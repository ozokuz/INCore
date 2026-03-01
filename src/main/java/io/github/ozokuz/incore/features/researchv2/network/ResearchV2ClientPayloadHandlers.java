package io.github.ozokuz.incore.features.researchv2.network;

import io.github.ozokuz.incore.INCore;

public final class ResearchV2ClientPayloadHandlers {
    private ResearchV2ClientPayloadHandlers() {
    }

    public static void handleSnapshot(String json) {
        INCore.LOGGER.info("[ResearchV2] current research state snapshot: {}", json);
    }
}
