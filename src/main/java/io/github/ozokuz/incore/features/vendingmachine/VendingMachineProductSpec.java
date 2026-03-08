package io.github.ozokuz.incore.features.vendingmachine;

import net.minecraft.resources.ResourceLocation;

public interface VendingMachineProductSpec {
    ResourceLocation typeId();

    int unitCount();
}
