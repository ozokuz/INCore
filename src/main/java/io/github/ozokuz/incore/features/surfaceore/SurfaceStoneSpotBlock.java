package io.github.ozokuz.incore.features.surfaceore;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public class SurfaceStoneSpotBlock extends Block {
    private final SurfaceStoneType stoneType;

    public SurfaceStoneSpotBlock(SurfaceStoneType stoneType) {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.STONE)
                .requiresCorrectToolForDrops()
                .strength(1.8F, 6.0F)
                .sound(SoundType.STONE));
        this.stoneType = stoneType;
    }

    public SurfaceStoneType stoneType() {
        return stoneType;
    }
}
