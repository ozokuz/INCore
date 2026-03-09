package io.github.ozokuz.incore.client.features.market;

import com.google.gson.Gson;
import io.github.ozokuz.incore.features.market.MarketService;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

final class MarketScreenDataUtil {
    private static final Gson GSON = new Gson();

    private MarketScreenDataUtil() {
    }

    static MarketService.ScreenData parse(String json) {
        MarketService.ScreenData parsed = GSON.fromJson(json, MarketService.ScreenData.class);
        if (parsed == null || parsed.items() == null) {
            return new MarketService.ScreenData(false, null, List.of(), 0, false, false);
        }
        return new MarketService.ScreenData(
                parsed.canTrade(),
                parsed.terminalPos(),
                parsed.items(),
                parsed.balanceSpur(),
                parsed.ae2Linked(),
                parsed.ae2Online()
        );
    }

    static List<MarketService.ItemView> orderedItems(MarketService.ScreenData data) {
        if (data == null || data.items() == null || data.items().isEmpty()) {
            return List.of();
        }

        List<MarketService.ItemView> copy = new ArrayList<>(data.items());
        copy.sort(Comparator
                .comparingInt(MarketService.ItemView::availableCount).reversed()
                .thenComparing(MarketService.ItemView::displayName, String.CASE_INSENSITIVE_ORDER));
        return copy;
    }

    static @Nullable MarketService.ItemView findItem(MarketService.ScreenData data, @Nullable String selectedItemId) {
        if (selectedItemId == null || data == null) {
            return null;
        }

        for (MarketService.ItemView item : orderedItems(data)) {
            if (selectedItemId.equals(item.itemId())) {
                return item;
            }
        }
        return null;
    }

    static @Nullable ResourceLocation parseItemId(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return ResourceLocation.tryParse(raw);
    }
}
