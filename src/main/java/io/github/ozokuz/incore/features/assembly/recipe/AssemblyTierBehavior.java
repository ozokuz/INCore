package io.github.ozokuz.incore.features.assembly.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.List;

public record AssemblyTierBehavior(
        TierOutcome t1,
        TierOutcome t2,
        TierOutcome t3
) {
    public static final Codec<AssemblyTierBehavior> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            TierOutcome.CODEC.optionalFieldOf("t1", TierOutcome.EMPTY).forGetter(AssemblyTierBehavior::t1),
            TierOutcome.CODEC.optionalFieldOf("t2", TierOutcome.EMPTY).forGetter(AssemblyTierBehavior::t2),
            TierOutcome.CODEC.optionalFieldOf("t3", TierOutcome.EMPTY).forGetter(AssemblyTierBehavior::t3)
    ).apply(instance, AssemblyTierBehavior::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, AssemblyTierBehavior> STREAM_CODEC = StreamCodec.of(
            (buffer, value) -> {
                TierOutcome.STREAM_CODEC.encode(buffer, value.t1());
                TierOutcome.STREAM_CODEC.encode(buffer, value.t2());
                TierOutcome.STREAM_CODEC.encode(buffer, value.t3());
            },
            buffer -> new AssemblyTierBehavior(
                    TierOutcome.STREAM_CODEC.decode(buffer),
                    TierOutcome.STREAM_CODEC.decode(buffer),
                    TierOutcome.STREAM_CODEC.decode(buffer)
            )
    );

    public TierOutcome forTier(int tier) {
        return switch (tier) {
            case 1 -> t1;
            case 2 -> t2;
            default -> t3;
        };
    }

    public static AssemblyTierBehavior fromJson(JsonObject root) {
        return new AssemblyTierBehavior(
                TierOutcome.fromJson(root.has("t1") ? root.getAsJsonObject("t1") : null),
                TierOutcome.fromJson(root.has("t2") ? root.getAsJsonObject("t2") : null),
                TierOutcome.fromJson(root.has("t3") ? root.getAsJsonObject("t3") : null)
        );
    }

    public record TierOutcome(
            double failureChance,
            List<AssemblyOutputDefinition> failureOutputs,
            List<AssemblyOutputDefinition> recycleOutputs,
            List<AssemblyOutputDefinition> leftoverOutputs
    ) {
        public static final TierOutcome EMPTY = new TierOutcome(0.0D, List.of(), List.of(), List.of());
        public static final Codec<TierOutcome> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.doubleRange(0.0D, 1.0D).optionalFieldOf("failureChance", 0.0D).forGetter(TierOutcome::failureChance),
                AssemblyOutputDefinition.CODEC.listOf().optionalFieldOf("failureOutputs", List.of()).forGetter(TierOutcome::failureOutputs),
                AssemblyOutputDefinition.CODEC.listOf().optionalFieldOf("recycleOutputs", List.of()).forGetter(TierOutcome::recycleOutputs),
                AssemblyOutputDefinition.CODEC.listOf().optionalFieldOf("leftoverOutputs", List.of()).forGetter(TierOutcome::leftoverOutputs)
        ).apply(instance, TierOutcome::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, TierOutcome> STREAM_CODEC = StreamCodec.of(
                (buffer, value) -> {
                    ByteBufCodecs.DOUBLE.encode(buffer, value.failureChance());
                    writeOutputs(buffer, value.failureOutputs());
                    writeOutputs(buffer, value.recycleOutputs());
                    writeOutputs(buffer, value.leftoverOutputs());
                },
                buffer -> new TierOutcome(
                        ByteBufCodecs.DOUBLE.decode(buffer),
                        readOutputs(buffer),
                        readOutputs(buffer),
                        readOutputs(buffer)
                )
        );

        public TierOutcome {
            failureChance = Math.clamp(failureChance, 0.0D, 1.0D);
            failureOutputs = List.copyOf(failureOutputs);
            recycleOutputs = List.copyOf(recycleOutputs);
            leftoverOutputs = List.copyOf(leftoverOutputs);
        }

        public static TierOutcome fromJson(JsonObject object) {
            if (object == null) {
                return new TierOutcome(0.0D, List.of(), List.of(), List.of());
            }
            double failureChance = object.has("failureChance") ? object.get("failureChance").getAsDouble() : 0.0D;
            return new TierOutcome(
                    failureChance,
                    readOutputs(object.getAsJsonArray("failureOutputs")),
                    readOutputs(object.getAsJsonArray("recycleOutputs")),
                    readOutputs(object.getAsJsonArray("leftoverOutputs"))
            );
        }

        private static List<AssemblyOutputDefinition> readOutputs(JsonArray array) {
            if (array == null) {
                return List.of();
            }
            List<AssemblyOutputDefinition> outputs = new ArrayList<>();
            array.forEach(element -> outputs.add(AssemblyOutputDefinition.fromJson(element)));
            return List.copyOf(outputs);
        }

        private static void writeOutputs(RegistryFriendlyByteBuf buffer, List<AssemblyOutputDefinition> outputs) {
            buffer.writeVarInt(outputs.size());
            outputs.forEach(output -> AssemblyOutputDefinition.STREAM_CODEC.encode(buffer, output));
        }

        private static List<AssemblyOutputDefinition> readOutputs(RegistryFriendlyByteBuf buffer) {
            int count = buffer.readVarInt();
            List<AssemblyOutputDefinition> outputs = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                outputs.add(AssemblyOutputDefinition.STREAM_CODEC.decode(buffer));
            }
            return List.copyOf(outputs);
        }
    }
}
