package io.github.ozokuz.incore.features.cards;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import org.jetbrains.annotations.Nullable;

public record CardAttributeEffect(ResourceLocation attributeId, double amount, AttributeModifier.Operation operation) {
    public static @Nullable CardAttributeEffect fromJson(JsonObject json) {
        String attr = GsonHelper.getAsString(json, "attribute", "");
        ResourceLocation attributeId = ResourceLocation.tryParse(attr);
        if (attributeId == null) {
            return null;
        }

        double amount = GsonHelper.getAsDouble(json, "amount", 0.0D);
        String operationRaw = GsonHelper.getAsString(json, "operation", "add_value");
        AttributeModifier.Operation operation = switch (operationRaw.toLowerCase()) {
            case "add_multiplied_base", "multiply_base" -> AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
            case "add_multiplied_total", "multiply_total" -> AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;
            default -> AttributeModifier.Operation.ADD_VALUE;
        };
        return new CardAttributeEffect(attributeId, amount, operation);
    }
}
