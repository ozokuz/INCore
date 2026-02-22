package io.github.ozokuz.incore.features.research;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
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
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.MapColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import io.github.ozokuz.incore.Registration;

public class BurnerLabBlock extends BaseEntityBlock implements LabTierProvider {
    public static final MapCodec<BurnerLabBlock> CODEC = simpleCodec(BurnerLabBlock::new);
    private final LabTier tier;

    public BurnerLabBlock() {
        this(LabTier.BURNER, Properties.of().mapColor(MapColor.METAL).strength(3.0F).sound(SoundType.METAL));
    }

    public BurnerLabBlock(Properties properties) {
        this(LabTier.BURNER, properties);
    }

    protected BurnerLabBlock(LabTier tier, Properties properties) {
        super(properties);
        this.tier = tier;
    }

    @Override
    protected @NotNull MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new LabBlockEntity(pos, state);
    }

    @Override
    protected @NotNull InteractionResult useWithoutItem(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull net.minecraft.world.phys.BlockHitResult hitResult) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof MenuProvider provider && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(provider, pos);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void setPlacedBy(Level level, @NotNull BlockPos pos, @NotNull BlockState state, @Nullable LivingEntity placer, @NotNull ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide || !(placer instanceof Player player)) {
            return;
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof LabBlockEntity labBlockEntity) {
            if (player instanceof ServerPlayer serverPlayer) {
                labBlockEntity.setOwner(serverPlayer);
            } else {
                labBlockEntity.setOwner(player.getUUID());
            }
        }
    }

    @Override
    protected void onRemove(@NotNull BlockState state, Level level, @NotNull BlockPos pos, @NotNull BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof LabBlockEntity labBlockEntity) {
                for (int slot = 0; slot < labBlockEntity.slotCount(); slot++) {
                    ItemStack stack = labBlockEntity.getSlotItem(slot);
                    if (!stack.isEmpty()) {
                        popResource(level, pos, stack.copy());
                    }
                }
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> blockEntityType) {
        return createTickerHelper(blockEntityType, Registration.LAB_BLOCK_ENTITY.get(), LabBlockEntity::tick);
    }

    @Override
    public LabTier incore$getLabTier() {
        return tier;
    }
}
