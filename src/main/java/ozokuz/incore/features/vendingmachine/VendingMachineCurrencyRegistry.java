package ozokuz.incore.features.vendingmachine;

import com.google.gson.JsonObject;
import ozokuz.incore.INCore;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class VendingMachineCurrencyRegistry {
    private static final Map<ResourceLocation, VendingMachineCurrencyType> TYPES = new ConcurrentHashMap<>();

    private VendingMachineCurrencyRegistry() {
    }

    public static void register(VendingMachineCurrencyType type) {
        VendingMachineCurrencyType previous = TYPES.putIfAbsent(type.id(), type);
        if (previous != null && previous != type) {
            INCore.LOGGER.warn("VendingMachine currency type '{}' is already registered; keeping first registration.", type.id());
        }
    }

    public static @Nullable ParsedCurrency parse(JsonObject json) {
        ResourceLocation typeId = ResourceLocation.tryParse(GsonHelper.getAsString(json, "type", ""));
        if (typeId == null) {
            return null;
        }

        VendingMachineCurrencyType type = TYPES.get(typeId);
        if (type == null) {
            return null;
        }

        VendingMachineCurrencySpec spec = type.parse(json);
        if (spec == null) {
            return null;
        }

        return new ParsedCurrency(type, spec);
    }

    public record ParsedCurrency(VendingMachineCurrencyType type, VendingMachineCurrencySpec spec) {
    }
}
