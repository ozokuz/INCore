package io.github.ozokuz.incore.features.vendingmachine;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public interface VendingMachineProductType {
    ResourceLocation id();

    @Nullable
    VendingMachineProductSpec parse(JsonObject json);

    boolean isAvailable(VendingMachineProductSpec spec);

    ItemStack previewStack(VendingMachineProductSpec spec);

    String productId(VendingMachineProductSpec spec);

    boolean grant(ServerPlayer player, VendingMachineProductSpec spec, int quantity);
}
