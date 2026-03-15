package ozokuz.incore.features.research.station;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.jetbrains.annotations.NotNull;

public class ResearchControllerTier1Block extends AbstractResearchControllerBlock {
    public static final MapCodec<ResearchControllerTier1Block> CODEC = simpleCodec(ResearchControllerTier1Block::new);

    public ResearchControllerTier1Block() {
        super(1);
    }

    public ResearchControllerTier1Block(BlockBehaviour.Properties properties) {
        super(1, properties);
    }

    @Override
    protected @NotNull MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }
}
