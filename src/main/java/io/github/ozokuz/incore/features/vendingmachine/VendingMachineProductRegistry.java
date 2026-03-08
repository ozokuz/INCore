package io.github.ozokuz.incore.features.vendingmachine;

import com.google.gson.JsonObject;
import io.github.ozokuz.incore.INCore;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class VendingMachineProductRegistry {
    private static final Map<ResourceLocation, VendingMachineProductType> TYPES = new ConcurrentHashMap<>();

    private VendingMachineProductRegistry() {
    }

    public static void register(VendingMachineProductType type) {
        VendingMachineProductType previous = TYPES.putIfAbsent(type.id(), type);
        if (previous != null && previous != type) {
            INCore.LOGGER.warn("VendingMachine product type '{}' is already registered; keeping first registration.", type.id());
        }
    }

    public static @Nullable ParsedProduct parse(JsonObject json) {
        ResourceLocation typeId = ResourceLocation.tryParse(GsonHelper.getAsString(json, "type", ""));
        if (typeId == null) {
            return null;
        }

        VendingMachineProductType type = TYPES.get(typeId);
        if (type == null) {
            return null;
        }

        VendingMachineProductSpec spec = type.parse(json);
        if (spec == null) {
            return null;
        }

        return new ParsedProduct(type, spec);
    }

    public record ParsedProduct(VendingMachineProductType type, VendingMachineProductSpec spec) {
    }
}
