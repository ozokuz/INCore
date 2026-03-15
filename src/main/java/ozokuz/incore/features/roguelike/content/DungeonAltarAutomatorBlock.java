package ozokuz.incore.features.roguelike.content;

import appeng.api.implementations.items.IMemoryCard;
import ozokuz.incore.Registration;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DungeonAltarAutomatorBlock extends Block implements EntityBlock {
    public DungeonAltarAutomatorBlock() {
        super(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(4.0F).sound(SoundType.METAL));
    }

    @Override
    protected @NotNull ItemInteractionResult useItemOn(ItemStack stack, @NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos,
                                                       @NotNull Player player, @NotNull InteractionHand hand, @NotNull BlockHitResult hitResult) {
        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof DungeonAltarAutomatorBlockEntity automator)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (stack.getItem() instanceof IMemoryCard) {
            AutomatorMemoryCardLink.write(stack, level.dimension(), pos, automator.ownerId());
            player.sendSystemMessage(Component.translatable("incore.roguelike.automator.memory_card.saved"));
            return ItemInteractionResult.SUCCESS;
        }

        if (stack.is(Registration.EMPTY_DUNGEON_CRYSTAL_ITEM.get()) && automator.getItem(DungeonAltarAutomatorBlockEntity.CRYSTAL_SLOT).isEmpty()) {
            automator.setItem(DungeonAltarAutomatorBlockEntity.CRYSTAL_SLOT, stack.copyWithCount(1));
            if (!player.isCreative()) {
                stack.shrink(1);
            }
            return ItemInteractionResult.SUCCESS;
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected @NotNull InteractionResult useWithoutItem(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos,
                                                        @NotNull Player player, @NotNull BlockHitResult hitResult) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof DungeonAltarAutomatorBlockEntity automator) {
            player.sendSystemMessage(statusText(automator.statusForDisplay()));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void setPlacedBy(@NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState state, @Nullable LivingEntity placer, @NotNull ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide || !(placer instanceof Player player)) {
            return;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof DungeonAltarAutomatorBlockEntity automator)) {
            return;
        }

        automator.setOwner(player.getUUID());
        if (level.getBlockEntity(pos.above()) instanceof DungeonAltarBlockEntity altar && altar.ownerId() == null) {
            altar.setOwner(player.getUUID());
        }
    }

    @Override
    public void onRemove(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof DungeonAltarAutomatorBlockEntity automator) {
                ItemStack stack = automator.getItem(DungeonAltarAutomatorBlockEntity.CRYSTAL_SLOT);
                if (!stack.isEmpty() && !level.isClientSide) {
                    level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D, stack.copy()));
                    automator.setItem(DungeonAltarAutomatorBlockEntity.CRYSTAL_SLOT, ItemStack.EMPTY);
                    automator.setChanged();
                }
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new DungeonAltarAutomatorBlockEntity(pos, state);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> type) {
        return type == Registration.DUNGEON_ALTAR_AUTOMATOR_BE.get() ? DungeonAltarAutomatorBlockEntity::tick : null;
    }

    private static Component statusText(int status) {
        return switch (status) {
            case DungeonAltarAutomatorBlockEntity.STATUS_AE2_OFFLINE -> Component.translatable("incore.roguelike.automator.status.ae2_offline");
            case DungeonAltarAutomatorBlockEntity.STATUS_NO_CRYSTAL -> Component.translatable("incore.roguelike.automator.status.no_crystal");
            case DungeonAltarAutomatorBlockEntity.STATUS_REQUESTING -> Component.translatable("incore.roguelike.automator.status.requesting");
            case DungeonAltarAutomatorBlockEntity.STATUS_ALTAR_COMPLETE -> Component.translatable("incore.roguelike.automator.status.altar_complete");
            case DungeonAltarAutomatorBlockEntity.STATUS_AWAITING_ITEMS -> Component.translatable("incore.roguelike.automator.status.awaiting_items");
            default -> Component.translatable("incore.roguelike.automator.status.no_altar_above");
        };
    }
}
