package io.github.ozokuz.incore.features.vendor;

import com.google.gson.JsonObject;
import io.github.ozokuz.incore.INCore;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class VendorProductRegistry {
    private static final Map<ResourceLocation, VendorProductType> TYPES = new ConcurrentHashMap<>();

    private VendorProductRegistry() {
    }

    public static void register(VendorProductType type) {
        VendorProductType previous = TYPES.putIfAbsent(type.id(), type);
        if (previous != null && previous != type) {
            INCore.LOGGER.warn("Vendor product type '{}' is already registered; keeping first registration.", type.id());
        }
    }

    public static @Nullable ParsedProduct parse(JsonObject json) {
        ResourceLocation typeId = ResourceLocation.tryParse(GsonHelper.getAsString(json, "type", ""));
        if (typeId == null) {
            return null;
        }

        VendorProductType type = TYPES.get(typeId);
        if (type == null) {
            return null;
        }

        VendorProductSpec spec = type.parse(json);
        if (spec == null) {
            return null;
        }

        return new ParsedProduct(type, spec);
    }

    public record ParsedProduct(VendorProductType type, VendorProductSpec spec) {
    }
}
