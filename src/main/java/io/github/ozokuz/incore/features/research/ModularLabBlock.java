package io.github.ozokuz.incore.features.research;

import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;

public class ModularLabBlock extends BurnerLabBlock {
    public ModularLabBlock() {
        super(LabTier.MODULAR, Properties.of().mapColor(MapColor.METAL).strength(4.0F).sound(SoundType.NETHERITE_BLOCK));
    }
}
