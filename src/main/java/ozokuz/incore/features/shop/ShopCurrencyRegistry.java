package ozokuz.incore.features.shop;

import com.google.gson.JsonObject;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import org.jetbrains.annotations.Nullable;
import ozokuz.incore.INCore;

public final class ShopCurrencyRegistry {
    private static final Map<ResourceLocation, ShopCurrencyType> TYPES = new ConcurrentHashMap<>();

    private ShopCurrencyRegistry() {
    }

    public static void register(ShopCurrencyType type) {
        ShopCurrencyType previous = TYPES.putIfAbsent(type.id(), type);
        if (previous != null && previous != type) {
            INCore.LOGGER.warn("Shop currency type '{}' is already registered; keeping first registration.", type.id());
        }
    }

    public static @Nullable ParsedCurrency parse(JsonObject json) {
        ResourceLocation typeId = ResourceLocation.tryParse(GsonHelper.getAsString(json, "type", ""));
        if (typeId == null) {
            return null;
        }
        ShopCurrencyType type = TYPES.get(typeId);
        if (type == null) {
            return null;
        }
        ShopCurrencySpec spec = type.parse(json);
        if (spec == null) {
            return null;
        }
        return new ParsedCurrency(type, spec);
    }

    public static @Nullable ShopCurrencyType get(ResourceLocation id) {
        return TYPES.get(id);
    }

    public record ParsedCurrency(ShopCurrencyType type, ShopCurrencySpec spec) {
    }
}
