package io.github.ozokuz.incore.features.cards;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import io.github.ozokuz.incore.INCore;
import io.github.ozokuz.incore.Registration;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class CardDeckService {
    private CardDeckService() {
    }

    public static boolean assembleDeckFromInventory(ServerPlayer player) {
        CoreSelection coreSelection = findCore(player);
        if (coreSelection == null) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("incore.cards.deck.missing_core"));
            return false;
        }

        BoxSelection boxSelection = findBox(player);
        if (boxSelection == null) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("incore.cards.deck.missing_box"));
            return false;
        }

        CardDeckCoreData core = CardDeckCoreManager.get(coreSelection.coreId());
        if (core == null) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("incore.cards.deck.invalid_core", coreSelection.coreId().toString()));
            return false;
        }

        CardDeckBoxData box = CardDeckBoxManager.get(boxSelection.boxId());
        if (box == null) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("incore.cards.deck.invalid_box", boxSelection.boxId().toString()));
            return false;
        }

        int capacity = Math.max(1, core.capacityPoints() + box.capacityBonus());
        List<CardCandidate> candidates = collectCards(player);
        if (candidates.isEmpty()) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("incore.cards.deck.missing_modules"));
            return false;
        }

        candidates.sort((a, b) -> {
            int rarityCompare = Integer.compare(b.module().rarity(), a.module().rarity());
            if (rarityCompare != 0) {
                return rarityCompare;
            }
            return Integer.compare(b.instance().grade(), a.instance().grade());
        });

        int used = 0;
        List<CardCandidate> selected = new ArrayList<>();
        for (CardCandidate candidate : candidates) {
            int next = used + candidate.module().deckPoints();
            if (next > capacity) {
                continue;
            }
            selected.add(candidate);
            used = next;
        }

        if (selected.isEmpty()) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("incore.cards.deck.no_capacity"));
            return false;
        }

        coreSelection.stack().shrink(1);
        boxSelection.stack().shrink(1);

        Map<Integer, Integer> consumeBySlot = new HashMap<>();
        for (CardCandidate candidate : selected) {
            consumeBySlot.merge(candidate.slot(), 1, Integer::sum);
        }

        for (Map.Entry<Integer, Integer> entry : consumeBySlot.entrySet()) {
            ItemStack stack = player.getInventory().getItem(entry.getKey());
            if (!stack.isEmpty()) {
                stack.shrink(entry.getValue());
                if (stack.isEmpty()) {
                    player.getInventory().setItem(entry.getKey(), ItemStack.EMPTY);
                }
            }
        }

        int maxIntegrity = Math.max(1, core.baseIntegrity() + box.integrityBonus());
        List<CardItemData.CardInstance> modules = selected.stream().map(CardCandidate::instance).toList();

        ItemStack deck = new ItemStack(Registration.CARD_DECK_ITEM.get());
        CardItemData.writeDeckData(
                deck,
                core.id(),
                box.id(),
                maxIntegrity,
                maxIntegrity,
                false,
                modules
        );
        List<ResolvedModule> assembledResolvedModules = selected.stream()
                .map(candidate -> new ResolvedModule(candidate.instance(), candidate.module()))
                .toList();
        Multimap<Holder<Attribute>, AttributeModifier> resolvedModifiers = resolveModifiersFromModules(assembledResolvedModules, maxIntegrity, maxIntegrity);
        CardItemData.writeDeckModifierSnapshot(deck, snapshotModifiers(resolvedModifiers));
        CardItemData.writeDeckModifierLineSnapshot(deck, snapshotModifierLines(resolvedModifiers));

        if (!player.addItem(deck)) {
            player.drop(deck, false);
        }

        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
        player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                "incore.cards.deck.assembled",
                modules.size(),
                used,
                capacity,
                maxIntegrity
        ));
        return true;
    }

    public static Multimap<Holder<Attribute>, AttributeModifier> resolveDeckModifiers(ItemStack deckStack, @Nullable ServerPlayer wearer) {
        CardItemData.DeckData deck = CardItemData.readDeckData(deckStack);
        if (deck == null || deck.bricked() || deck.integrity() <= 0) {
            return HashMultimap.create();
        }

        List<ResolvedModule> resolvedModules = new ArrayList<>();
        for (CardItemData.CardInstance instance : deck.modules()) {
            CardModuleData module = CardModuleManager.get(instance.cardId());
            if (module == null) {
                continue;
            }
            resolvedModules.add(new ResolvedModule(instance, module));
        }

        if (resolvedModules.isEmpty()) {
            return HashMultimap.create();
        }
        return resolveModifiersFromModules(resolvedModules, deck.integrity(), deck.maxIntegrity());
    }

    public static void onDungeonTransition(ServerPlayer player) {
        mutateEquippedDeck(player, deck -> {
            if (deck.bricked() || deck.integrity() <= 0) {
                return deck;
            }

            List<CardItemData.CardInstance> modules = new ArrayList<>(deck.modules().size());
            int integrityLoss = 0;

            for (CardItemData.CardInstance instance : deck.modules()) {
                CardModuleData module = CardModuleManager.get(instance.cardId());
                if (module == null) {
                    modules.add(instance);
                    continue;
                }

                CardChaoticService.ChaoticRoll chaoticRoll = new CardChaoticService.ChaoticRoll(
                        instance.chaoticEffects(),
                        instance.chaoticDownsides()
                );
                if (module.moduleType() == CardModuleType.CHAOTIC) {
                    chaoticRoll = CardChaoticService.roll(module, player.getRandom());
                }

                if (module.moduleType() == CardModuleType.CORRUPTED) {
                    integrityLoss += Math.max(1, module.corruptedIntegrityCost());
                }

                modules.add(new CardItemData.CardInstance(
                        instance.cardId(),
                        instance.foil(),
                        instance.grade(),
                        instance.graded(),
                        instance.revealed(),
                        1.0D,
                        chaoticRoll.effects(),
                        chaoticRoll.downsides()
                ));
            }

            int nextIntegrity = Math.max(0, deck.integrity() - integrityLoss);
            boolean bricked = deck.bricked() || nextIntegrity <= 0;
            if (bricked && !deck.bricked()) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("incore.cards.deck.bricked"));
            } else if (integrityLoss > 0) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                        "incore.cards.deck.corruption",
                        integrityLoss,
                        nextIntegrity,
                        deck.maxIntegrity()
                ));
            }

            return new CardItemData.DeckData(
                    deck.coreId(),
                    deck.boxId(),
                    nextIntegrity,
                    deck.maxIntegrity(),
                    bricked,
                    List.copyOf(modules)
            );
        });
    }

    public static void setIntegrity(ServerPlayer player, int value) {
        mutateEquippedDeck(player, deck -> {
            int next = Math.clamp(value, 0, deck.maxIntegrity());
            return new CardItemData.DeckData(
                    deck.coreId(),
                    deck.boxId(),
                    next,
                    deck.maxIntegrity(),
                    next <= 0,
                    deck.modules()
            );
        });
    }

    public static void setBricked(ServerPlayer player, boolean bricked) {
        mutateEquippedDeck(player, deck -> new CardItemData.DeckData(
                deck.coreId(),
                deck.boxId(),
                bricked ? 0 : Math.max(1, deck.integrity()),
                deck.maxIntegrity(),
                bricked,
                deck.modules()
        ));
    }

    public static @Nullable ItemStack findEquippedDeck(ServerPlayer player) {
        Optional<IDynamicStackHandler> stacks = CuriosApi.getCuriosInventory(player)
                .flatMap(handler -> handler.getStacksHandler("deck"))
                .map(handler -> handler.getStacks());
        if (stacks.isEmpty()) {
            return null;
        }

        IDynamicStackHandler stackHandler = stacks.get();
        for (int i = 0; i < stackHandler.getSlots(); i++) {
            ItemStack stack = stackHandler.getStackInSlot(i);
            if (stack.getItem() == Registration.CARD_DECK_ITEM.get()) {
                return stack;
            }
        }
        return null;
    }

    private static void mutateEquippedDeck(ServerPlayer player, java.util.function.UnaryOperator<CardItemData.DeckData> mutator) {
        CuriosApi.getCuriosInventory(player).flatMap(handler -> handler.getStacksHandler("deck")).ifPresent(handler -> {
            IDynamicStackHandler stacks = handler.getStacks();
            for (int i = 0; i < stacks.getSlots(); i++) {
                ItemStack stack = stacks.getStackInSlot(i);
                if (stack.getItem() != Registration.CARD_DECK_ITEM.get()) {
                    continue;
                }

                CardItemData.DeckData current = CardItemData.readDeckData(stack);
                if (current == null) {
                    continue;
                }

                CardItemData.DeckData updated = mutator.apply(current);
                CardItemData.writeDeckBack(stack, updated);
                Multimap<Holder<Attribute>, AttributeModifier> resolvedModifiers = resolveDeckModifiers(stack, player);
                CardItemData.writeDeckModifierSnapshot(stack, snapshotModifiers(resolvedModifiers));
                CardItemData.writeDeckModifierLineSnapshot(stack, snapshotModifierLines(resolvedModifiers));
                stacks.setStackInSlot(i, stack);
            }
        });
    }

    private static List<CardItemData.DeckModifierEntry> snapshotModifiers(Multimap<Holder<Attribute>, AttributeModifier> modifiers) {
        Map<ModifierKey, Double> totals = new LinkedHashMap<>();
        modifiers.entries().stream()
                .sorted(Comparator
                        .comparing((Map.Entry<Holder<Attribute>, AttributeModifier> entry) -> {
                            ResourceLocation id = BuiltInRegistries.ATTRIBUTE.getKey(entry.getKey().value());
                            return id == null ? "" : id.toString();
                        })
                        .thenComparing(entry -> entry.getValue().operation().name()))
                .forEach(entry -> {
                    ModifierKey key = new ModifierKey(entry.getKey(), entry.getValue().operation());
                    totals.merge(key, entry.getValue().amount(), Double::sum);
                });

        List<CardItemData.DeckModifierEntry> result = new ArrayList<>();
        for (Map.Entry<ModifierKey, Double> entry : totals.entrySet()) {
            ResourceLocation attributeId = BuiltInRegistries.ATTRIBUTE.getKey(entry.getKey().attribute().value());
            if (attributeId == null) {
                continue;
            }

            result.add(new CardItemData.DeckModifierEntry(
                    attributeId,
                    entry.getValue(),
                    entry.getKey().operation()
            ));
        }
        return List.copyOf(result);
    }

    private static List<String> snapshotModifierLines(Multimap<Holder<Attribute>, AttributeModifier> modifiers) {
        Map<ModifierKey, Double> totals = new LinkedHashMap<>();
        modifiers.entries().forEach(entry -> {
            ModifierKey key = new ModifierKey(entry.getKey(), entry.getValue().operation());
            totals.merge(key, entry.getValue().amount(), Double::sum);
        });

        List<String> lines = new ArrayList<>();
        for (Map.Entry<ModifierKey, Double> entry : totals.entrySet()) {
            double amount = entry.getValue();
            if (Math.abs(amount) < 1.0E-9D) {
                continue;
            }

            ResourceLocation attributeId = BuiltInRegistries.ATTRIBUTE.getKey(entry.getKey().attribute().value());
            String attributeName = attributeId == null
                    ? entry.getKey().attribute().value().getDescriptionId()
                    : CardAttributeResolver.displayName(attributeId);
            String label = switch (entry.getKey().operation()) {
                case ADD_VALUE -> CardNumberFormat.signed(amount) + " " + attributeName;
                case ADD_MULTIPLIED_BASE -> CardNumberFormat.signed(amount * 100.0D) + "% base " + attributeName;
                case ADD_MULTIPLIED_TOTAL -> CardNumberFormat.signed(amount * 100.0D) + "% total " + attributeName;
            };
            lines.add(label);
        }
        return List.copyOf(lines);
    }

    private static Multimap<Holder<Attribute>, AttributeModifier> resolveModifiersFromModules(
            List<ResolvedModule> resolvedModules,
            int integrity,
            int maxIntegrity
    ) {
        Multimap<Holder<Attribute>, AttributeModifier> modifiers = HashMultimap.create();
        int modifierIndex = 0;

        Map<CardModuleType, Integer> typeCounts = new EnumMap<>(CardModuleType.class);
        Map<String, Integer> tagCounts = new HashMap<>();

        for (ResolvedModule resolved : resolvedModules) {
            typeCounts.merge(resolved.module.moduleType(), 1, Integer::sum);
            for (String tag : resolved.module.tags()) {
                tagCounts.merge(tag, 1, Integer::sum);
            }

            double multiplier = baseMultiplier(resolved.instance, resolved.module);

            List<CardAttributeEffect> effects = resolved.module.moduleType() == CardModuleType.CHAOTIC
                    ? (resolved.instance.chaoticEffects().isEmpty() ? resolved.module.effects() : resolved.instance.chaoticEffects())
                    : resolved.module.effects();
            List<CardAttributeEffect> downsides = resolved.module.moduleType() == CardModuleType.CHAOTIC
                    ? (resolved.instance.chaoticDownsides().isEmpty() ? resolved.module.downsides() : resolved.instance.chaoticDownsides())
                    : resolved.module.downsides();
            boolean capToTwoDecimals = resolved.module.moduleType() == CardModuleType.CHAOTIC;

            for (CardAttributeEffect effect : effects) {
                addModifier(modifiers, effect, multiplier, modifierIndex++, capToTwoDecimals);
            }
            for (CardAttributeEffect downside : downsides) {
                addModifier(modifiers, downside, multiplier, modifierIndex++, capToTwoDecimals);
            }
        }

        for (CardSynergyData synergy : CardSynergyManager.all()) {
            if (resolvedModules.size() < synergy.minCards()) {
                continue;
            }

            boolean tagsOk = synergy.requiredTags().stream().allMatch(tag -> tagCounts.getOrDefault(tag, 0) > 0);
            if (!tagsOk) {
                continue;
            }

            boolean typesOk = true;
            for (Map.Entry<CardModuleType, Integer> entry : synergy.requiredTypeCounts().entrySet()) {
                if (typeCounts.getOrDefault(entry.getKey(), 0) < entry.getValue()) {
                    typesOk = false;
                    break;
                }
            }

            if (!typesOk) {
                continue;
            }

            for (CardAttributeEffect effect : synergy.effects()) {
                addModifier(modifiers, effect, 1.0D, modifierIndex++, false);
            }
        }

        long corruptedCount = resolvedModules.stream().filter(module -> module.module.moduleType() == CardModuleType.CORRUPTED).count();
        if (corruptedCount > 0L) {
            double ratio = (double) integrity / (double) Math.max(1, maxIntegrity);
            if (ratio <= 0.75D) {
                addPenalty(modifiers, "minecraft:movement_speed", -0.05D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, modifierIndex++);
            }
            if (ratio <= 0.50D) {
                addPenalty(modifiers, "minecraft:attack_damage", -0.10D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, modifierIndex++);
            }
            if (ratio <= 0.25D) {
                addPenalty(modifiers, "minecraft:max_health", -4.0D, AttributeModifier.Operation.ADD_VALUE, modifierIndex++);
            }
        }

        return modifiers;
    }

    private static void addPenalty(
            Multimap<Holder<Attribute>, AttributeModifier> modifiers,
            String attributeId,
            double amount,
            AttributeModifier.Operation operation,
            int index
    ) {
        ResourceLocation id = ResourceLocation.tryParse(attributeId);
        if (id == null) {
            return;
        }

        CardAttributeResolver.resolveHolder(id).ifPresent(attribute -> modifiers.put(
                attribute,
                new AttributeModifier(ResourceLocation.fromNamespaceAndPath(INCore.MODID, "deck_penalty_" + index), amount, operation)
        ));
    }

    private static void addModifier(
            Multimap<Holder<Attribute>, AttributeModifier> modifiers,
            CardAttributeEffect effect,
            double multiplier,
            int index,
            boolean capToTwoDecimals
    ) {
        double amount = effect.amount() * multiplier;
        if (capToTwoDecimals) {
            amount = CardNumberFormat.round(amount, 2);
        }
        final double resolvedAmount = amount;

        CardAttributeResolver.resolveHolder(effect.attributeId()).ifPresent(attribute -> modifiers.put(
                attribute,
                new AttributeModifier(
                        ResourceLocation.fromNamespaceAndPath(INCore.MODID, "deck_mod_" + index),
                        resolvedAmount,
                        effect.operation()
                )
        ));
    }

    private static double baseMultiplier(CardItemData.CardInstance instance, CardModuleData module) {
        double multiplier = 1.0D;

        if (instance.foil()) {
            multiplier *= 1.10D;
        }

        if (instance.graded()) {
            multiplier *= (1.0D + Math.clamp(instance.grade(), 1, 10) * 0.02D);
        }

        if (module.moduleType() == CardModuleType.CRYPTIC && !instance.revealed()) {
            multiplier *= 2.0D;
        }

        return multiplier;
    }

    private static @Nullable CoreSelection findCore(ServerPlayer player) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.getItem() != Registration.CARD_DECK_CORE_ITEM.get() || stack.isEmpty()) {
                continue;
            }

            ResourceLocation id = CardItemData.readDeckCoreId(stack);
            if (id == null) {
                id = CardDeckCoreManager.getDefaultCoreId();
            }
            return new CoreSelection(slot, stack, id);
        }
        return null;
    }

    private static @Nullable BoxSelection findBox(ServerPlayer player) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.getItem() != Registration.CARD_DECK_BOX_ITEM.get() || stack.isEmpty()) {
                continue;
            }

            ResourceLocation id = CardItemData.readDeckBoxId(stack);
            if (id == null) {
                id = CardDeckBoxManager.getDefaultBoxId();
            }
            return new BoxSelection(slot, stack, id);
        }
        return null;
    }

    private static List<CardCandidate> collectCards(ServerPlayer player) {
        List<CardCandidate> result = new ArrayList<>();
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.getItem() != Registration.CARD_MODULE_ITEM.get() || stack.isEmpty()) {
                continue;
            }

            CardItemData.CardInstance instance = CardItemData.readCardInstance(stack);
            if (instance == null) {
                continue;
            }

            CardModuleData module = CardModuleManager.get(instance.cardId());
            if (module == null) {
                continue;
            }

            for (int i = 0; i < stack.getCount(); i++) {
                result.add(new CardCandidate(slot, instance, module));
            }
        }
        return result;
    }

    private record CardCandidate(int slot, CardItemData.CardInstance instance, CardModuleData module) {
    }

    private record CoreSelection(int slot, ItemStack stack, ResourceLocation coreId) {
    }

    private record BoxSelection(int slot, ItemStack stack, ResourceLocation boxId) {
    }

    private record ResolvedModule(CardItemData.CardInstance instance, CardModuleData module) {
    }

    private record ModifierKey(Holder<Attribute> attribute, AttributeModifier.Operation operation) {
    }
}
