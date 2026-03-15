package ozokuz.incore.features.research.station;

import com.mojang.serialization.MapCodec;
import ozokuz.incore.features.machines.multiblock.AbstractMachinePartBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class OrchestrationDriveBlock extends AbstractMachinePartBlock {
    public static final MapCodec<OrchestrationDriveBlock> CODEC = simpleCodec(OrchestrationDriveBlock::new);

    public OrchestrationDriveBlock() {
        super();
    }

    public OrchestrationDriveBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected @NotNull MapCodec<? extends AbstractMachinePartBlock> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new OrchestrationDriveBlockEntity(pos, state);
    }

    @Override
    protected @NotNull InteractionResult useWithoutItem(@NotNull BlockState state, Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull BlockHitResult hitResult) {
        openMenuIfServer(level, pos, player);
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
        openMenuIfServer(level, pos, player);
        return ItemInteractionResult.CONSUME;
    }

    private static void openMenuIfServer(Level level, BlockPos pos, Player player) {
        if (level.isClientSide) {
            return;
        }
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof MenuProvider provider && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(provider, pos);
        }
    }
}
