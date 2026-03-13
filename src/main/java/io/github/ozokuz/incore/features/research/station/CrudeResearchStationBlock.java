package io.github.ozokuz.incore.features.research.station;

import com.mojang.serialization.MapCodec;
import io.github.ozokuz.incore.Registration;
import io.github.ozokuz.incore.features.research.team.ResearchTeamResolver;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CrudeResearchStationBlock extends BaseEntityBlock {
    public static final MapCodec<CrudeResearchStationBlock> CODEC = simpleCodec(CrudeResearchStationBlock::new);

    public CrudeResearchStationBlock() {
        this(Properties.of().mapColor(MapColor.METAL).strength(3.0F).sound(SoundType.METAL));
    }

    public CrudeResearchStationBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected @NotNull MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new CrudeResearchStationBlockEntity(pos, state);
    }

    @Override
    public void setPlacedBy(Level level, @NotNull BlockPos pos, @NotNull BlockState state, @Nullable LivingEntity placer, @NotNull ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide || !(placer instanceof ServerPlayer serverPlayer)) {
            return;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof CrudeResearchStationBlockEntity station)) {
            return;
        }

        String teamId = ResearchTeamResolver.resolveTeamId(serverPlayer);
        station.setTeamId(teamId);
    }

    @Override
    protected @NotNull InteractionResult useWithoutItem(
            @NotNull BlockState state,
            @NotNull Level level,
            @NotNull BlockPos pos,
            @NotNull Player player,
            @NotNull BlockHitResult hitResult
    ) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof MenuProvider provider && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(provider, pos);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    protected @NotNull ItemInteractionResult useItemOn(
            @NotNull ItemStack stack,
            @NotNull BlockState state,
            @NotNull Level level,
            @NotNull BlockPos pos,
            @NotNull Player player,
            @NotNull InteractionHand hand,
            @NotNull BlockHitResult hitResult
    ) {
        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof MenuProvider provider && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(provider, pos);
        }
        return ItemInteractionResult.CONSUME;
    }

    @Override
    protected void onRemove(
            @NotNull BlockState state,
            Level level,
            @NotNull BlockPos pos,
            @NotNull BlockState newState,
            boolean movedByPiston
    ) {
        if (!state.is(newState.getBlock())) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof CrudeResearchStationBlockEntity station) {
                for (int slot = 0; slot < station.getContainerSize(); slot++) {
                    ItemStack stack = station.getItem(slot);
                    if (!stack.isEmpty()) {
                        popResource(level, pos, stack.copy());
                    }
                }
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level,
            @NotNull BlockState state,
            @NotNull BlockEntityType<T> type
    ) {
        return createTickerHelper(type, Registration.CRUDE_RESEARCH_STATION_BE.get(), CrudeResearchStationBlockEntity::tick);
    }
}
