package io.github.ozokuz.incore.features.researchv2.station;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractResearchStationPartBlockEntity extends BlockEntity {
    private BlockPos controllerPos;
    private String stationId = "";
    private String teamId = "";

    protected AbstractResearchStationPartBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public final void bindToController(ResearchControllerBlockEntity controller) {
        if (controller == null || !controller.isFormed()) {
            clearBinding();
            return;
        }
        controllerPos = controller.getBlockPos().immutable();
        stationId = controller.stationId();
        teamId = controller.teamId();
        setChanged();
    }

    public final void clearBinding() {
        boolean changed = controllerPos != null || !stationId.isBlank() || !teamId.isBlank();
        controllerPos = null;
        stationId = "";
        teamId = "";
        if (changed) {
            setChanged();
        }
    }

    public final BlockPos controllerPos() {
        return controllerPos == null ? null : controllerPos.immutable();
    }

    public final String stationId() {
        return stationId;
    }

    public final String teamId() {
        return teamId;
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (level != null && !level.isClientSide) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
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

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("controllerPos")) {
            controllerPos = BlockPos.of(tag.getLong("controllerPos"));
        } else {
            controllerPos = null;
        }
        stationId = tag.getString("stationId");
        teamId = tag.getString("teamId");
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.saveAdditional(tag, registries);
        if (controllerPos != null) {
            tag.putLong("controllerPos", controllerPos.asLong());
        }
        if (!stationId.isBlank()) {
            tag.putString("stationId", stationId);
        }
        if (!teamId.isBlank()) {
            tag.putString("teamId", teamId);
        }
    }
}
