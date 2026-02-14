package io.github.ozokuz.incore.features.roguelike.content;

import io.github.ozokuz.incore.Registration;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class RoguelikePortalBlockEntity extends BlockEntity {
    private long dungeonId;

    public RoguelikePortalBlockEntity(BlockPos pos, BlockState blockState) {
        super(Registration.ROGUELIKE_PORTAL_BE.get(), pos, blockState);
    }

    public long dungeonId() {
        return dungeonId;
    }

    public boolean isActivated() {
        return dungeonId > 0;
    }

    public void setDungeonId(long dungeonId) {
        this.dungeonId = Math.max(0, dungeonId);
        setChanged();
    }

    public void clearDungeonId() {
        setDungeonId(0L);
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.loadAdditional(tag, registries);
        dungeonId = Math.max(0L, tag.getLong("dungeonId"));
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.saveAdditional(tag, registries);
        if (dungeonId > 0) {
            tag.putLong("dungeonId", dungeonId);
        }
    }
}
