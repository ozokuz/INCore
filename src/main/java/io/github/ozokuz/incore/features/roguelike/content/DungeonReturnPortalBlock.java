package io.github.ozokuz.incore.features.roguelike.content;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public class DungeonReturnPortalBlock extends Block {
    public DungeonReturnPortalBlock() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_CYAN)
                .strength(5.0F)
                .noOcclusion());
    }
}
