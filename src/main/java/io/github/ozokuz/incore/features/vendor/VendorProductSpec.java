package io.github.ozokuz.incore.features.vendor;

import net.minecraft.resources.ResourceLocation;

public interface VendorProductSpec {
    ResourceLocation typeId();

    int unitCount();
}
