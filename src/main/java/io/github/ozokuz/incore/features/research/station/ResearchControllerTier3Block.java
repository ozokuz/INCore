package io.github.ozokuz.incore.features.research.station;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.jetbrains.annotations.NotNull;

public class ResearchControllerTier3Block extends AbstractResearchControllerBlock {
    public static final MapCodec<ResearchControllerTier3Block> CODEC = simpleCodec(ResearchControllerTier3Block::new);

    public ResearchControllerTier3Block() {
        super(3);
    }

    public ResearchControllerTier3Block(BlockBehaviour.Properties properties) {
        super(3, properties);
    }

    @Override
    protected @NotNull MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }
}
