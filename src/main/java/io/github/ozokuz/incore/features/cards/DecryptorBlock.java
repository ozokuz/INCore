package io.github.ozokuz.incore.features.cards;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public class DecryptorBlock extends Block {
    public DecryptorBlock() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(3.2F)
                .sound(SoundType.METAL));
    }
}
