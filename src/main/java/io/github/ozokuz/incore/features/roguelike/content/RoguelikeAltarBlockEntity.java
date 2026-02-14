package io.github.ozokuz.incore.features.roguelike.content;

import io.github.ozokuz.incore.Registration;
import io.github.ozokuz.incore.features.roguelike.RoguelikeService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class RoguelikeAltarBlockEntity extends BlockEntity {
    private List<DisplayEntry> displayEntries = List.of();
    @Nullable
    private UUID ownerId;

    public RoguelikeAltarBlockEntity(BlockPos pos, BlockState blockState) {
        super(Registration.ROGUELIKE_ALTAR_BE.get(), pos, blockState);
    }

    public List<DisplayEntry> displayEntries() {
        return displayEntries;
    }

    @Nullable
    public UUID ownerId() {
        return ownerId;
    }

    public void setOwner(@Nullable UUID ownerId) {
        if (Objects.equals(this.ownerId, ownerId)) {
            return;
        }

        this.ownerId = ownerId;
        setChanged();
        syncToClient();
    }

    public void setDisplayEntries(List<DisplayEntry> entries) {
        List<DisplayEntry> normalized = List.copyOf(entries);
        if (displayEntries.equals(normalized)) {
            return;
        }

        displayEntries = normalized;
        setChanged();
        syncToClient();
    }

    private void syncToClient() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        BlockState state = getBlockState();
        serverLevel.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.loadAdditional(tag, registries);

        List<DisplayEntry> loaded = new ArrayList<>();
        ListTag displayTag = tag.getList("altarDisplay", Tag.TAG_COMPOUND);
        for (Tag entryTag : displayTag) {
            DisplayEntry entry = DisplayEntry.fromTag((CompoundTag) entryTag);
            if (entry != null) {
                loaded.add(entry);
            }
        }

        displayEntries = List.copyOf(loaded);
        ownerId = tag.hasUUID("ownerId") ? tag.getUUID("ownerId") : null;
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.saveAdditional(tag, registries);

        ListTag displayTag = new ListTag();
        for (DisplayEntry entry : displayEntries) {
            displayTag.add(entry.toTag());
        }

        tag.put("altarDisplay", displayTag);
        if (ownerId != null) {
            tag.putUUID("ownerId", ownerId);
        }
    }

    @Override
    public @NotNull CompoundTag getUpdateTag(HolderLookup.@NotNull Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public static <T extends BlockEntity> void tick(Level level, BlockPos pos, BlockState state, T blockEntity) {
        if (!(blockEntity instanceof RoguelikeAltarBlockEntity altar) || level.isClientSide) {
            return;
        }

        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        if (serverLevel.getGameTime() % 5 != 0L) {
            return;
        }

        RoguelikeService.tickAltar(serverLevel, pos, altar);
    }

    public record DisplayEntry(ResourceLocation itemId, int submittedAmount, int requiredAmount) {
        public boolean isComplete() {
            return submittedAmount >= requiredAmount;
        }

        public CompoundTag toTag() {
            CompoundTag tag = new CompoundTag();
            tag.putString("item", itemId.toString());
            tag.putInt("submitted", submittedAmount);
            tag.putInt("required", requiredAmount);
            return tag;
        }

        public static DisplayEntry fromTag(CompoundTag tag) {
            ResourceLocation itemId = ResourceLocation.tryParse(tag.getString("item"));
            if (itemId == null) {
                return null;
            }

            int required = Math.max(1, tag.getInt("required"));
            int submitted = Math.max(0, Math.min(required, tag.getInt("submitted")));
            return new DisplayEntry(itemId, submitted, required);
        }
    }
}
