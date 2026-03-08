package io.github.ozokuz.incore.features.researchv2.station;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;

public final class ResearchStationServices {
    private ResearchStationServices() {
    }

    public static ResearchStationAugmentSummary computeAugmentSummary(Level level, ResearchControllerBlockEntity controller) {
        if (level == null || controller == null || controller.augmenterPos() == null) {
            return ResearchStationAugmentSummary.DEFAULT;
        }
        if (!(level.getBlockEntity(controller.augmenterPos()) instanceof AugmenterBlockEntity augmenter)) {
            return ResearchStationAugmentSummary.DEFAULT;
        }

        int normalSpeed = 0;
        int normalProductivity = 0;
        int normalStabilizer = 0;
        int dungeonSpeed = 0;
        int dungeonProductivity = 0;
        int dungeonStabilizer = 0;
        int matchingSpecializer = 0;

        for (int slot = 0; slot < augmenter.activeSlotCount(); slot++) {
            ItemStack stack = augmenter.rawItemHandler().getStackInSlot(slot);
            if (!(stack.getItem() instanceof ResearchAugmentItem augment)) {
                continue;
            }
            int count = Math.max(1, stack.getCount());
            switch (augment.augmentType()) {
                case SPEED -> {
                    if (augment.isDungeon()) {
                        dungeonSpeed += count;
                    } else {
                        normalSpeed += count;
                    }
                }
                case PRODUCTIVITY -> {
                    if (augment.isDungeon()) {
                        dungeonProductivity += count;
                    } else {
                        normalProductivity += count;
                    }
                }
                case STABILIZER -> {
                    if (augment.isDungeon()) {
                        dungeonStabilizer += count;
                    } else {
                        normalStabilizer += count;
                    }
                }
                case SPECIALIZER -> {
                    if (augment.isDungeon()) {
                        matchingSpecializer += count;
                    }
                }
            }
        }

        double speedMultiplier = Math.pow(0.85D, normalSpeed) * Math.pow(0.70D, dungeonSpeed);
        double powerMultiplier = Math.pow(1.35D, normalSpeed)
                * Math.pow(1.30D, normalProductivity)
                * Math.pow(1.20D, normalStabilizer)
                * Math.pow(1.75D, dungeonSpeed)
                * Math.pow(1.60D, dungeonProductivity)
                * Math.pow(1.35D, dungeonStabilizer);
        double bonusRunChance = Math.min(0.90D, (normalProductivity * 0.10D) + (dungeonProductivity * 0.20D));
        double corruptionMultiplier = Math.pow(0.80D, normalStabilizer) * Math.pow(0.50D, dungeonStabilizer);

        if (matchingSpecializer > 0) {
            speedMultiplier *= Math.pow(0.80D, matchingSpecializer);
            corruptionMultiplier *= Math.pow(0.75D, matchingSpecializer);
            powerMultiplier *= Math.pow(1.50D, matchingSpecializer);
        }

        return new ResearchStationAugmentSummary(
                Math.max(0.0D, speedMultiplier),
                Math.max(1.0D, powerMultiplier),
                Math.max(0.0D, bonusRunChance),
                Math.max(0.0D, corruptionMultiplier)
        );
    }

    public static ResearchStationAugmentSummary computeAugmentSummary(Level level, ResearchControllerBlockEntity controller, ResourceLocation categoryId) {
        if (level == null || controller == null || controller.augmenterPos() == null) {
            return ResearchStationAugmentSummary.DEFAULT;
        }
        if (!(level.getBlockEntity(controller.augmenterPos()) instanceof AugmenterBlockEntity augmenter)) {
            return ResearchStationAugmentSummary.DEFAULT;
        }

        int normalSpeed = 0;
        int normalProductivity = 0;
        int normalStabilizer = 0;
        int dungeonSpeed = 0;
        int dungeonProductivity = 0;
        int dungeonStabilizer = 0;
        int matchingSpecializer = 0;

        for (int slot = 0; slot < augmenter.activeSlotCount(); slot++) {
            ItemStack stack = augmenter.rawItemHandler().getStackInSlot(slot);
            if (!(stack.getItem() instanceof ResearchAugmentItem augment)) {
                continue;
            }
            int count = Math.max(1, stack.getCount());
            switch (augment.augmentType()) {
                case SPEED -> {
                    if (augment.isDungeon()) {
                        dungeonSpeed += count;
                    } else {
                        normalSpeed += count;
                    }
                }
                case PRODUCTIVITY -> {
                    if (augment.isDungeon()) {
                        dungeonProductivity += count;
                    } else {
                        normalProductivity += count;
                    }
                }
                case STABILIZER -> {
                    if (augment.isDungeon()) {
                        dungeonStabilizer += count;
                    } else {
                        normalStabilizer += count;
                    }
                }
                case SPECIALIZER -> {
                    if (augment.isDungeon() && augment.categoryId() != null && augment.categoryId().equals(categoryId)) {
                        matchingSpecializer += count;
                    }
                }
            }
        }

        double speedMultiplier = Math.pow(0.85D, normalSpeed) * Math.pow(0.70D, dungeonSpeed) * Math.pow(0.80D, matchingSpecializer);
        double powerMultiplier = Math.pow(1.35D, normalSpeed)
                * Math.pow(1.30D, normalProductivity)
                * Math.pow(1.20D, normalStabilizer)
                * Math.pow(1.75D, dungeonSpeed)
                * Math.pow(1.60D, dungeonProductivity)
                * Math.pow(1.35D, dungeonStabilizer)
                * Math.pow(1.50D, matchingSpecializer);
        double bonusRunChance = Math.min(0.90D, (normalProductivity * 0.10D) + (dungeonProductivity * 0.20D));
        double corruptionMultiplier = Math.pow(0.80D, normalStabilizer) * Math.pow(0.50D, dungeonStabilizer) * Math.pow(0.75D, matchingSpecializer);
        return new ResearchStationAugmentSummary(speedMultiplier, Math.max(1.0D, powerMultiplier), bonusRunChance, corruptionMultiplier);
    }

    public static Map<String, Integer> countMaterials(MaterialStorageBlockEntity storage) {
        Map<String, Integer> counts = new HashMap<>();
        if (storage == null) {
            return counts;
        }
        for (int slot = 0; slot < storage.activeSlotCount(); slot++) {
            ItemStack stack = storage.rawItemHandler().getStackInSlot(slot);
            if (stack.isEmpty()) {
                continue;
            }
            ResourceLocation itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
            io.github.ozokuz.incore.features.research.ResearchMaterialManager.all().forEach((materialId, definition) -> {
                if (definition.itemId().equals(itemId)) {
                    counts.merge(materialId.toString(), stack.getCount(), Integer::sum);
                }
            });
        }
        return counts;
    }
}
