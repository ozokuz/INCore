package io.github.ozokuz.incore.features.market.content;

import com.mojang.serialization.MapCodec;
import io.github.ozokuz.incore.Registration;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MarketAutoBuyerMk2Block extends MarketAutoBuyerBlock {
    public static final MapCodec<MarketAutoBuyerMk2Block> CODEC = simpleCodec(MarketAutoBuyerMk2Block::new);

    public MarketAutoBuyerMk2Block() {
        this(BlockBehaviour.Properties.ofFullCopy(Registration.MARKET_AUTOBUYER_BLOCK.get()));
    }

    public MarketAutoBuyerMk2Block(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected @NotNull MapCodec<? extends MarketAutoBuyerBlock> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new MarketAutoBuyerMk2BlockEntity(pos, state);
    }

    @Override
    @SuppressWarnings("unchecked")
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> blockEntityType) {
        if (blockEntityType != Registration.MARKET_AUTOBUYER_MK2_BE.get()) {
            return null;
        }
        return (lvl, pos, blockState, blockEntity) -> MarketAutoBuyerMk2BlockEntity.tick(lvl, pos, blockState, (MarketAutoBuyerMk2BlockEntity) blockEntity);
    }
}
