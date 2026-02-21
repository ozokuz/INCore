package io.github.ozokuz.incore.features.arena.content;

import io.github.ozokuz.incore.Registration;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ArenaRewardCrateBlockEntity extends BlockEntity {
    private ArenaRewardCrateData.CrateContents contents;

    public ArenaRewardCrateBlockEntity(BlockPos pos, BlockState blockState) {
        super(Registration.ARENA_REWARD_CRATE_BE.get(), pos, blockState);
    }

    public @Nullable ArenaRewardCrateData.CrateContents getContents() {
        return contents;
    }

    public void setContents(@Nullable ArenaRewardCrateData.CrateContents contents) {
        this.contents = contents;
        setChanged();
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("crate")) {
            this.contents = ArenaRewardCrateData.CrateContents.fromTag(tag.getCompound("crate"));
        } else {
            this.contents = null;
        }
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.saveAdditional(tag, registries);
        if (contents != null) {
            tag.put("crate", contents.toTag());
        }
    }
}
