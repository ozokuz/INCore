package io.github.ozokuz.incore.features.research.network;

import io.github.ozokuz.incore.features.research.client.ResearchRecipeLockClientCache;
import io.netty.buffer.ByteBuf;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.HashSet;
import java.util.Set;

public record ResearchLockStateSyncPayload(String json) implements CustomPacketPayload {
    public static final Type<ResearchLockStateSyncPayload> TYPE = new Type<>(ResourceLocation.parse("incore:research_lock_state"));
    public static final StreamCodec<ByteBuf, ResearchLockStateSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            ResearchLockStateSyncPayload::json,
            ResearchLockStateSyncPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ResearchLockStateSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Set<ResourceLocation> ids = new HashSet<>();
            JsonElement root = JsonParser.parseString(payload.json());
            if (root.isJsonArray()) {
                JsonArray array = root.getAsJsonArray();
                for (JsonElement element : array) {
                    ResourceLocation id = ResourceLocation.tryParse(element.getAsString());
                    if (id != null) {
                        ids.add(id);
                    }
                }
            }
            ResearchRecipeLockClientCache.setLockedRecipeIds(ids);
        });
    }
}
