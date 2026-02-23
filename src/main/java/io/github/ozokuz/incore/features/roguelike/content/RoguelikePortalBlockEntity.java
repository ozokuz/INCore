package io.github.ozokuz.incore.features.roguelike.content;

import io.github.ozokuz.incore.Registration;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class RoguelikePortalBlockEntity extends BlockEntity {
    private long instanceId;

    public RoguelikePortalBlockEntity(BlockPos pos, BlockState blockState) {
        super(Registration.ROGUELIKE_PORTAL_BE.get(), pos, blockState);
    }

    public long instanceId() {
        return instanceId;
    }

    public long dungeonId() {
        return instanceId;
    }

    public boolean isActivated() {
        return instanceId > 0;
    }

    public void setInstanceId(long instanceId) {
        this.instanceId = Math.max(0, instanceId);
        setChanged();
    }

    public void setDungeonId(long dungeonId) {
        setInstanceId(dungeonId);
    }

    public void clearInstanceId() {
        setInstanceId(0L);
    }

    public void clearDungeonId() {
        clearInstanceId();
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("instanceId")) {
            instanceId = Math.max(0L, tag.getLong("instanceId"));
        } else {
            instanceId = Math.max(0L, tag.getLong("dungeonId"));
        }
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.saveAdditional(tag, registries);
        if (instanceId > 0L) {
            tag.putLong("instanceId", instanceId);
        }
    }
}
