package ozokuz.incore.features.research.station;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.jetbrains.annotations.NotNull;

public class ResearchControllerTier4Block extends AbstractResearchControllerBlock {
    public static final MapCodec<ResearchControllerTier4Block> CODEC = simpleCodec(ResearchControllerTier4Block::new);

    public ResearchControllerTier4Block() {
        super(4);
    }

    public ResearchControllerTier4Block(BlockBehaviour.Properties properties) {
        super(4, properties);
    }

    @Override
    protected @NotNull MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }
}
