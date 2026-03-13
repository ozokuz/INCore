package io.github.ozokuz.incore.features.research.station;

import io.github.ozokuz.incore.features.machines.multiblock.*;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.jetbrains.annotations.NotNull;

public class ResearchControllerTier2Block extends AbstractResearchControllerBlock {
    public static final MapCodec<ResearchControllerTier2Block> CODEC = simpleCodec(ResearchControllerTier2Block::new);

    public ResearchControllerTier2Block() {
        super(2);
    }

    public ResearchControllerTier2Block(BlockBehaviour.Properties properties) {
        super(2, properties);
    }

    @Override
    protected @NotNull MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }
}
