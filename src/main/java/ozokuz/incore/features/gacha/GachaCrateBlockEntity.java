package ozokuz.incore.features.gacha;

import ozokuz.incore.Registration;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class GachaCrateBlockEntity extends BlockEntity {
    private static final String KEY_BANNER = "banner";
    private ResourceLocation bannerId;

    public GachaCrateBlockEntity(BlockPos pos, BlockState blockState) {
        super(Registration.GACHA_RIFT_BE.get(), pos, blockState);
    }

    public @Nullable ResourceLocation getBannerId() {
        return bannerId;
    }

    public void setBannerId(@Nullable ResourceLocation bannerId) {
        this.bannerId = bannerId;
        setChanged();
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains(KEY_BANNER, Tag.TAG_STRING)) {
            bannerId = ResourceLocation.tryParse(tag.getString(KEY_BANNER));
        } else {
            bannerId = null;
        }
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.saveAdditional(tag, registries);
        if (bannerId != null) {
            tag.putString(KEY_BANNER, bannerId.toString());
        }
    }
}
