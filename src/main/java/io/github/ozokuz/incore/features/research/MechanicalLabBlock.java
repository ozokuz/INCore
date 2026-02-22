package io.github.ozokuz.incore.features.research;

import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;

public class MechanicalLabBlock extends BurnerLabBlock {
    public MechanicalLabBlock() {
        super(LabTier.MECHANICAL, Properties.of().mapColor(MapColor.METAL).strength(3.5F).sound(SoundType.METAL));
    }
}
