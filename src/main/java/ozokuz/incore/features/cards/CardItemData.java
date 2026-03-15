package ozokuz.incore.features.cards;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class CardItemData {
    public static final String KEY_BOOSTER_ID = "incore:booster_id";
    public static final String KEY_BOOSTER_BOX_ID = "incore:booster_box_id";
    public static final String KEY_CORE_ID = "incore:deck_core_id";
    public static final String KEY_BOX_ID = "incore:deck_box_id";

    public static final String KEY_CARD_ID = "incore:card_id";
    public static final String KEY_CARD_FOIL = "incore:foil";
    public static final String KEY_CARD_GRADE = "incore:grade";
    public static final String KEY_CARD_GRADED = "incore:graded";
    public static final String KEY_CARD_REVEALED = "incore:revealed";
    public static final String KEY_CARD_CHAOTIC_MULTIPLIER = "incore:chaotic_multiplier";
    public static final String KEY_CARD_CHAOTIC_EFFECTS = "incore:chaotic_effects";
    public static final String KEY_CARD_CHAOTIC_DOWNSIDES = "incore:chaotic_downsides";

    public static final String KEY_DECK_CORE_ID = "incore:deck_core";
    public static final String KEY_DECK_BOX_ID = "incore:deck_box";
    public static final String KEY_DECK_INTEGRITY = "incore:deck_integrity";
    public static final String KEY_DECK_MAX_INTEGRITY = "incore:deck_max_integrity";
    public static final String KEY_DECK_BRICKED = "incore:deck_bricked";
    public static final String KEY_DECK_MODULES = "incore:deck_modules";
    public static final String KEY_DECK_MODIFIERS = "incore:deck_modifiers";
    public static final String KEY_DECK_MODIFIER_LINES = "incore:deck_modifier_lines";
    public static final String KEY_DECK_MODIFIER_TEXT = "incore:deck_modifier_text";
    public static final String KEY_DECK_PREVIEW = "incore:deck_preview";

    private CardItemData() {
    }

    public static void writeBoosterId(ItemStack stack, ResourceLocation boosterId) {
        writeString(stack, KEY_BOOSTER_ID, boosterId.toString());
    }

    public static @Nullable ResourceLocation readBoosterId(ItemStack stack) {
        return readResourceLocation(stack, KEY_BOOSTER_ID);
    }

    public static void writeBoosterBoxId(ItemStack stack, ResourceLocation boxId) {
        writeString(stack, KEY_BOOSTER_BOX_ID, boxId.toString());
    }

    public static @Nullable ResourceLocation readBoosterBoxId(ItemStack stack) {
        return readResourceLocation(stack, KEY_BOOSTER_BOX_ID);
    }

    public static void writeDeckCoreId(ItemStack stack, ResourceLocation coreId) {
        writeString(stack, KEY_CORE_ID, coreId.toString());
    }

    public static @Nullable ResourceLocation readDeckCoreId(ItemStack stack) {
        return readResourceLocation(stack, KEY_CORE_ID);
    }

    public static void writeDeckBoxId(ItemStack stack, ResourceLocation boxId) {
        writeString(stack, KEY_BOX_ID, boxId.toString());
    }

    public static @Nullable ResourceLocation readDeckBoxId(ItemStack stack) {
        return readResourceLocation(stack, KEY_BOX_ID);
    }

    public static void writeCardInstance(
            ItemStack stack,
            ResourceLocation cardId,
            boolean foil,
            int grade,
            boolean graded,
            boolean revealed,
            double chaoticMultiplier,
            List<CardAttributeEffect> chaoticEffects,
            List<CardAttributeEffect> chaoticDownsides
    ) {
        CompoundTag tag = new CompoundTag();
        tag.putString(KEY_CARD_ID, cardId.toString());
        tag.putBoolean(KEY_CARD_FOIL, foil);
        tag.putInt(KEY_CARD_GRADE, Math.clamp(grade, 0, 10));
        tag.putBoolean(KEY_CARD_GRADED, graded);
        tag.putBoolean(KEY_CARD_REVEALED, revealed);
        tag.putDouble(KEY_CARD_CHAOTIC_MULTIPLIER, chaoticMultiplier);
        tag.put(KEY_CARD_CHAOTIC_EFFECTS, writeEffectList(chaoticEffects));
        tag.put(KEY_CARD_CHAOTIC_DOWNSIDES, writeEffectList(chaoticDownsides));
        writeCustomData(stack, tag);
    }

    public static @Nullable CardInstance readCardInstance(ItemStack stack) {
        CompoundTag tag = readCustomTag(stack);
        if (tag == null || !tag.contains(KEY_CARD_ID, Tag.TAG_STRING)) {
            return null;
        }

        ResourceLocation cardId = ResourceLocation.tryParse(tag.getString(KEY_CARD_ID));
        if (cardId == null) {
            return null;
        }

        return new CardInstance(
                cardId,
                tag.getBoolean(KEY_CARD_FOIL),
                Math.clamp(tag.getInt(KEY_CARD_GRADE), 0, 10),
                tag.getBoolean(KEY_CARD_GRADED),
                tag.getBoolean(KEY_CARD_REVEALED),
                tag.contains(KEY_CARD_CHAOTIC_MULTIPLIER, Tag.TAG_DOUBLE) ? tag.getDouble(KEY_CARD_CHAOTIC_MULTIPLIER) : 1.0D,
                readEffectList(tag, KEY_CARD_CHAOTIC_EFFECTS),
                readEffectList(tag, KEY_CARD_CHAOTIC_DOWNSIDES)
        );
    }

    public static void setCardGrade(ItemStack stack, int grade) {
        CompoundTag tag = readCustomTag(stack);
        if (tag == null) {
            return;
        }

        tag.putInt(KEY_CARD_GRADE, Math.clamp(grade, 1, 10));
        tag.putBoolean(KEY_CARD_GRADED, true);
        writeCustomData(stack, tag);
    }

    public static void setCardRevealed(ItemStack stack, boolean revealed) {
        CompoundTag tag = readCustomTag(stack);
        if (tag == null) {
            return;
        }

        tag.putBoolean(KEY_CARD_REVEALED, revealed);
        writeCustomData(stack, tag);
    }

    public static void setCardChaoticMultiplier(ItemStack stack, double value) {
        CompoundTag tag = readCustomTag(stack);
        if (tag == null) {
            return;
        }

        tag.putDouble(KEY_CARD_CHAOTIC_MULTIPLIER, value);
        writeCustomData(stack, tag);
    }

    public static void writeDeckData(
            ItemStack stack,
            ResourceLocation coreId,
            ResourceLocation boxId,
            int integrity,
            int maxIntegrity,
            boolean bricked,
            List<CardInstance> modules
    ) {
        CompoundTag tag = new CompoundTag();
        tag.putString(KEY_DECK_CORE_ID, coreId.toString());
        tag.putString(KEY_DECK_BOX_ID, boxId.toString());
        tag.putInt(KEY_DECK_INTEGRITY, Math.max(0, integrity));
        tag.putInt(KEY_DECK_MAX_INTEGRITY, Math.max(1, maxIntegrity));
        tag.putBoolean(KEY_DECK_BRICKED, bricked);

        ListTag moduleTags = new ListTag();
        for (CardInstance module : modules) {
            CompoundTag row = new CompoundTag();
            row.putString(KEY_CARD_ID, module.cardId().toString());
            row.putBoolean(KEY_CARD_FOIL, module.foil());
            row.putInt(KEY_CARD_GRADE, Math.clamp(module.grade(), 0, 10));
            row.putBoolean(KEY_CARD_GRADED, module.graded());
            row.putBoolean(KEY_CARD_REVEALED, module.revealed());
            row.putDouble(KEY_CARD_CHAOTIC_MULTIPLIER, module.chaoticMultiplier());
            row.put(KEY_CARD_CHAOTIC_EFFECTS, writeEffectList(module.chaoticEffects()));
            row.put(KEY_CARD_CHAOTIC_DOWNSIDES, writeEffectList(module.chaoticDownsides()));
            moduleTags.add(row);
        }
        tag.put(KEY_DECK_MODULES, moduleTags);
        writeCustomData(stack, tag);
    }

    public static @Nullable DeckData readDeckData(ItemStack stack) {
        CompoundTag tag = readCustomTag(stack);
        if (tag == null || !tag.contains(KEY_DECK_CORE_ID, Tag.TAG_STRING) || !tag.contains(KEY_DECK_BOX_ID, Tag.TAG_STRING)) {
            return null;
        }

        ResourceLocation coreId = ResourceLocation.tryParse(tag.getString(KEY_DECK_CORE_ID));
        ResourceLocation boxId = ResourceLocation.tryParse(tag.getString(KEY_DECK_BOX_ID));
        if (coreId == null || boxId == null) {
            return null;
        }

        List<CardInstance> modules = new ArrayList<>();
        ListTag moduleTags = tag.getList(KEY_DECK_MODULES, Tag.TAG_COMPOUND);
        for (Tag raw : moduleTags) {
            CompoundTag row = (CompoundTag) raw;
            ResourceLocation cardId = ResourceLocation.tryParse(row.getString(KEY_CARD_ID));
            if (cardId == null) {
                continue;
            }

            modules.add(new CardInstance(
                    cardId,
                    row.getBoolean(KEY_CARD_FOIL),
                    Math.clamp(row.getInt(KEY_CARD_GRADE), 0, 10),
                    row.getBoolean(KEY_CARD_GRADED),
                    row.getBoolean(KEY_CARD_REVEALED),
                    row.contains(KEY_CARD_CHAOTIC_MULTIPLIER, Tag.TAG_DOUBLE) ? row.getDouble(KEY_CARD_CHAOTIC_MULTIPLIER) : 1.0D,
                    readEffectList(row, KEY_CARD_CHAOTIC_EFFECTS),
                    readEffectList(row, KEY_CARD_CHAOTIC_DOWNSIDES)
            ));
        }

        int maxIntegrity = Math.max(1, tag.getInt(KEY_DECK_MAX_INTEGRITY));
        int integrity = Math.clamp(tag.getInt(KEY_DECK_INTEGRITY), 0, maxIntegrity);
        boolean bricked = tag.getBoolean(KEY_DECK_BRICKED) || integrity <= 0;

        return new DeckData(coreId, boxId, integrity, maxIntegrity, bricked, List.copyOf(modules));
    }

    public static void writeDeckBack(ItemStack stack, DeckData data) {
        writeDeckData(stack, data.coreId(), data.boxId(), data.integrity(), data.maxIntegrity(), data.bricked(), data.modules());
    }

    public static void writeDeckModifierSnapshot(ItemStack stack, List<DeckModifierEntry> modifiers) {
        CompoundTag tag = readCustomTag(stack);
        if (tag == null) {
            tag = new CompoundTag();
        }

        ListTag modifierTags = new ListTag();
        for (DeckModifierEntry entry : modifiers) {
            CompoundTag row = new CompoundTag();
            row.putString("attribute", entry.attributeId().toString());
            row.putDouble("amount", entry.amount());
            row.putString("operation", operationKey(entry.operation()));
            modifierTags.add(row);
        }
        tag.put(KEY_DECK_MODIFIERS, modifierTags);
        writeCustomData(stack, tag);
    }

    public static List<DeckModifierEntry> readDeckModifierSnapshot(ItemStack stack) {
        CompoundTag tag = readCustomTag(stack);
        if (tag == null || !tag.contains(KEY_DECK_MODIFIERS, Tag.TAG_LIST)) {
            return List.of();
        }

        List<DeckModifierEntry> result = new ArrayList<>();
        ListTag modifierTags = tag.getList(KEY_DECK_MODIFIERS, Tag.TAG_COMPOUND);
        for (Tag raw : modifierTags) {
            CompoundTag row = (CompoundTag) raw;

            ResourceLocation attributeId = ResourceLocation.tryParse(row.getString("attribute"));
            if (attributeId == null) {
                continue;
            }

            AttributeModifier.Operation operation = parseOperation(row.getString("operation"));
            double amount = row.getDouble("amount");
            result.add(new DeckModifierEntry(attributeId, amount, operation));
        }
        return List.copyOf(result);
    }

    public static void writeDeckModifierLineSnapshot(ItemStack stack, List<String> lines) {
        CompoundTag tag = readCustomTag(stack);
        if (tag == null) {
            tag = new CompoundTag();
        }

        ListTag lineTags = new ListTag();
        for (String line : lines) {
            lineTags.add(net.minecraft.nbt.StringTag.valueOf(line));
        }
        tag.put(KEY_DECK_MODIFIER_LINES, lineTags);
        tag.putString(KEY_DECK_MODIFIER_TEXT, String.join("\n", lines));
        writeCustomData(stack, tag);
    }

    public static List<String> readDeckModifierLineSnapshot(ItemStack stack) {
        CompoundTag tag = readCustomTag(stack);
        if (tag == null) {
            return List.of();
        }

        if (tag.contains(KEY_DECK_MODIFIER_TEXT, Tag.TAG_STRING)) {
            String raw = tag.getString(KEY_DECK_MODIFIER_TEXT).trim();
            if (!raw.isEmpty()) {
                return List.of(raw.split("\\R"));
            }
        }

        if (!tag.contains(KEY_DECK_MODIFIER_LINES, Tag.TAG_LIST)) {
            return List.of();
        }

        List<String> result = new ArrayList<>();
        ListTag lineTags = tag.getList(KEY_DECK_MODIFIER_LINES, Tag.TAG_STRING);
        for (Tag raw : lineTags) {
            result.add(raw.getAsString());
        }
        return List.copyOf(result);
    }

    public static void writeDeckPreview(ItemStack stack, boolean preview) {
        if (stack.isEmpty()) {
            return;
        }
        CompoundTag tag = readCustomTag(stack);
        if (tag == null) {
            tag = new CompoundTag();
        }
        tag.putBoolean(KEY_DECK_PREVIEW, preview);
        writeCustomData(stack, tag);
    }

    public static boolean readDeckPreview(ItemStack stack) {
        CompoundTag tag = readCustomTag(stack);
        return tag != null && tag.getBoolean(KEY_DECK_PREVIEW);
    }

    private static ListTag writeEffectList(List<CardAttributeEffect> effects) {
        ListTag listTag = new ListTag();
        for (CardAttributeEffect effect : effects) {
            CompoundTag row = new CompoundTag();
            row.putString("attribute", effect.attributeId().toString());
            row.putDouble("amount", effect.amount());
            row.putString("operation", operationKey(effect.operation()));
            listTag.add(row);
        }
        return listTag;
    }

    private static List<CardAttributeEffect> readEffectList(CompoundTag tag, String key) {
        if (!tag.contains(key, Tag.TAG_LIST)) {
            return List.of();
        }

        List<CardAttributeEffect> result = new ArrayList<>();
        ListTag listTag = tag.getList(key, Tag.TAG_COMPOUND);
        for (Tag raw : listTag) {
            CompoundTag row = (CompoundTag) raw;
            ResourceLocation attributeId = ResourceLocation.tryParse(row.getString("attribute"));
            if (attributeId == null) {
                continue;
            }

            result.add(new CardAttributeEffect(
                    attributeId,
                    row.getDouble("amount"),
                    parseOperation(row.getString("operation"))
            ));
        }
        return List.copyOf(result);
    }

    private static String operationKey(AttributeModifier.Operation operation) {
        return switch (operation) {
            case ADD_MULTIPLIED_BASE -> "add_multiplied_base";
            case ADD_MULTIPLIED_TOTAL -> "add_multiplied_total";
            case ADD_VALUE -> "add_value";
        };
    }

    private static AttributeModifier.Operation parseOperation(String raw) {
        return switch (raw) {
            case "add_multiplied_base" -> AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
            case "add_multiplied_total" -> AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;
            default -> AttributeModifier.Operation.ADD_VALUE;
        };
    }

    private static void writeString(ItemStack stack, String key, String value) {
        CompoundTag tag = readCustomTag(stack);
        if (tag == null) {
            tag = new CompoundTag();
        }
        tag.putString(key, value);
        writeCustomData(stack, tag);
    }

    private static @Nullable ResourceLocation readResourceLocation(ItemStack stack, String key) {
        CompoundTag tag = readCustomTag(stack);
        if (tag == null || !tag.contains(key, Tag.TAG_STRING)) {
            return null;
        }
        return ResourceLocation.tryParse(tag.getString(key));
    }

    private static @Nullable CompoundTag readCustomTag(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return null;
        }
        return data.copyTag();
    }

    private static void writeCustomData(ItemStack stack, CompoundTag tag) {
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public record CardInstance(
            ResourceLocation cardId,
            boolean foil,
            int grade,
            boolean graded,
            boolean revealed,
            double chaoticMultiplier,
            List<CardAttributeEffect> chaoticEffects,
            List<CardAttributeEffect> chaoticDownsides
    ) {
    }

    public record DeckData(
            ResourceLocation coreId,
            ResourceLocation boxId,
            int integrity,
            int maxIntegrity,
            boolean bricked,
            List<CardInstance> modules
    ) {
    }

    public record DeckModifierEntry(
            ResourceLocation attributeId,
            double amount,
            AttributeModifier.Operation operation
    ) {
    }
}
