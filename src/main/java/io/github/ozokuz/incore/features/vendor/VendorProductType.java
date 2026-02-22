package io.github.ozokuz.incore.features.vendor;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public interface VendorProductType {
    ResourceLocation id();

    @Nullable
    VendorProductSpec parse(JsonObject json);

    boolean isAvailable(VendorProductSpec spec);

    ItemStack previewStack(VendorProductSpec spec);

    String productId(VendorProductSpec spec);

    boolean grant(ServerPlayer player, VendorProductSpec spec, int quantity);
}
