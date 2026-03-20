package ozokuz.incore.integration.ldlib.ui;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public record ShopUiRouteContext(
        @Nullable ResourceLocation selectedCategoryId,
        @Nullable ResourceLocation selectedOfferId
) implements INCoreUiRouteContext {
}
