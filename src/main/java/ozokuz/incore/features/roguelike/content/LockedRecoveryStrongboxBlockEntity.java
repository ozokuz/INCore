package ozokuz.incore.features.roguelike.content;

import ozokuz.incore.Registration;
import ozokuz.incore.features.roguelike.state.RoguelikeSavedData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class LockedRecoveryStrongboxBlockEntity extends BlockEntity {
    private UUID recoveryId;

    public LockedRecoveryStrongboxBlockEntity(BlockPos pos, BlockState blockState) {
        super(Registration.LOCKED_RECOVERY_STRONGBOX_BE.get(), pos, blockState);
    }

    public void setRecoveryId(String rawRecoveryId) {
        this.recoveryId = parseRecoveryId(rawRecoveryId);
        setChanged();
    }

    public UUID recoveryId() {
        return recoveryId;
    }

    public boolean tryUnlock(ServerPlayer player, ItemStack keyStack) {
        if (player.getServer() == null || recoveryId == null) {
            player.sendSystemMessage(Component.translatable("incore.roguelike.recovery.invalid").withStyle(ChatFormatting.RED));
            return false;
        }

        UUID keyId = parseRecoveryId(keyStack.get(Registration.RECOVERY_STRONGBOX_ID.get()));
        if (keyId == null || !recoveryId.equals(keyId)) {
            player.sendSystemMessage(Component.translatable("incore.roguelike.recovery.wrong_key").withStyle(ChatFormatting.RED));
            return false;
        }

        RoguelikeSavedData data = RoguelikeSavedData.get(player.getServer());
        RoguelikeSavedData.RecoveryStrongboxRecord record = data.recoveryStrongbox(recoveryId);
        if (record == null) {
            player.sendSystemMessage(Component.translatable("incore.roguelike.recovery.invalid").withStyle(ChatFormatting.RED));
            return false;
        }

        for (RoguelikeSavedData.ItemStackRecord itemRecord : record.contents()) {
            ItemStack stack = itemRecord.toStack(player.registryAccess());
            if (stack.isEmpty()) {
                continue;
            }
            if (!player.addItem(stack)) {
                player.drop(stack, false);
            }
        }

        if (!player.isCreative()) {
            keyStack.shrink(1);
        }
        data.removeRecoveryStrongbox(recoveryId);
        player.sendSystemMessage(Component.translatable("incore.roguelike.recovery.opened").withStyle(ChatFormatting.GOLD));
        if (this.level != null) {
            this.level.setBlockAndUpdate(this.worldPosition, Blocks.AIR.defaultBlockState());
        }
        return true;
    }

    public void showStatus(ServerPlayer player) {
        player.sendSystemMessage(Component.translatable("incore.roguelike.recovery.use_key").withStyle(ChatFormatting.GRAY));
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.loadAdditional(tag, registries);
        this.recoveryId = parseRecoveryId(tag.getString("recoveryId"));
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.saveAdditional(tag, registries);
        if (recoveryId != null) {
            tag.putString("recoveryId", recoveryId.toString());
        }
    }

    private static UUID parseRecoveryId(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
