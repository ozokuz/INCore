package io.github.ozokuz.incore.features.market.content;

import com.mojang.serialization.MapCodec;
import io.github.ozokuz.incore.Registration;
import io.github.ozokuz.incore.features.playerlevel.PlayerFeatureUnlockIds;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MarketAutoTraderMk2Block extends MarketAutoTraderBlock {
    public static final MapCodec<MarketAutoTraderMk2Block> CODEC = simpleCodec(MarketAutoTraderMk2Block::new);

    public MarketAutoTraderMk2Block() {
        this(BlockBehaviour.Properties.ofFullCopy(Registration.MARKET_AUTOTRADER_BLOCK.get()));
    }

    public MarketAutoTraderMk2Block(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected @NotNull MapCodec<? extends MarketAutoTraderBlock> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new MarketAutoTraderMk2BlockEntity(pos, state);
    }

    @Override
    @SuppressWarnings("unchecked")
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> blockEntityType) {
        if (blockEntityType != Registration.MARKET_AUTOTRADER_MK2_BE.get()) {
            return null;
        }
        return (lvl, pos, blockState, blockEntity) -> MarketAutoTraderMk2BlockEntity.tick(lvl, pos, blockState, (MarketAutoTraderMk2BlockEntity) blockEntity);
    }

    @Override
    protected net.minecraft.resources.ResourceLocation requiredUnlock() {
        return PlayerFeatureUnlockIds.MARKET_AUTOTRADER_MK2;
    }
}
