package io.github.ozokuz.incore.features.cards;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.List;

public final class CardChaoticService {
    private CardChaoticService() {
    }

    public static ChaoticRoll roll(CardModuleData module, RandomSource random) {
        if (module.moduleType() != CardModuleType.CHAOTIC) {
            return new ChaoticRoll(List.of(), List.of());
        }

        List<ResourceLocation> attributePool = new ArrayList<>();
        for (CardAttributeEffect effect : module.effects()) {
            if (!attributePool.contains(effect.attributeId())) {
                attributePool.add(effect.attributeId());
            }
        }
        for (CardAttributeEffect effect : module.downsides()) {
            if (!attributePool.contains(effect.attributeId())) {
                attributePool.add(effect.attributeId());
            }
        }

        if (attributePool.isEmpty()) {
            return new ChaoticRoll(List.of(), List.of());
        }

        return new ChaoticRoll(
                rollEffects(module.effects(), attributePool, module.chaoticMin(), module.chaoticMax(), random),
                rollEffects(module.downsides(), attributePool, module.chaoticMin(), module.chaoticMax(), random)
        );
    }

    private static List<CardAttributeEffect> rollEffects(
            List<CardAttributeEffect> source,
            List<ResourceLocation> attributePool,
            double min,
            double max,
            RandomSource random
    ) {
        List<CardAttributeEffect> rolled = new ArrayList<>();
        for (CardAttributeEffect effect : source) {
            ResourceLocation attributeId = attributePool.get(random.nextInt(attributePool.size()));
            double multiplier = min + random.nextDouble() * (max - min);
            rolled.add(new CardAttributeEffect(attributeId, CardNumberFormat.round(effect.amount() * multiplier, 2), effect.operation()));
        }
        return List.copyOf(rolled);
    }

    public record ChaoticRoll(List<CardAttributeEffect> effects, List<CardAttributeEffect> downsides) {
    }
}
