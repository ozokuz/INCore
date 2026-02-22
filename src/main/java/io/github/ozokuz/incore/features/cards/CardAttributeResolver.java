package io.github.ozokuz.incore.features.cards;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;

import java.util.Optional;

public final class CardAttributeResolver {
    private CardAttributeResolver() {
    }

    public static Optional<Holder.Reference<Attribute>> resolveHolder(ResourceLocation attributeId) {
        Optional<Holder.Reference<Attribute>> direct = BuiltInRegistries.ATTRIBUTE.getHolder(attributeId);
        if (direct.isPresent()) {
            return direct;
        }

        if (!"minecraft".equals(attributeId.getNamespace())) {
            return Optional.empty();
        }

        String path = attributeId.getPath();
        if (path.startsWith("generic.")) {
            ResourceLocation stripped = ResourceLocation.fromNamespaceAndPath("minecraft", path.substring("generic.".length()));
            return BuiltInRegistries.ATTRIBUTE.getHolder(stripped);
        }

        ResourceLocation generic = ResourceLocation.fromNamespaceAndPath("minecraft", "generic." + path);
        return BuiltInRegistries.ATTRIBUTE.getHolder(generic);
    }

    public static String displayName(ResourceLocation attributeId) {
        return resolveHolder(attributeId)
                .map(holder -> Component.translatable(holder.value().getDescriptionId()).getString())
                .orElse(attributeId.toString());
    }
}
