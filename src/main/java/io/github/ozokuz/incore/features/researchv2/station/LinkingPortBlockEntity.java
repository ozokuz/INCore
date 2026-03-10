package io.github.ozokuz.incore.features.researchv2.station;

import io.github.ozokuz.incore.Registration;
import io.github.ozokuz.incore.features.researchv2.station.network.LinkingPortRegistry;
import io.github.ozokuz.incore.features.researchv2.station.network.StationNetworkService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LinkingPortBlockEntity extends BlockEntity {
    private String attachedStationId = "";
    private String attachedTeamId = "";

    public LinkingPortBlockEntity(BlockPos pos, BlockState state) {
        super(Registration.LINKING_PORT_BE.get(), pos, state);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) {
            LinkingPortRegistry.register(this);
            StationNetworkService.onTopologyChanged(level);
        }
    }

    @Override
    public void setRemoved() {
        if (level != null && !level.isClientSide) {
            LinkingPortRegistry.unregister(this);
            StationNetworkService.onTopologyChanged(level);
        }
        super.setRemoved();
    }

    public String attachedStationId() {
        return attachedStationId;
    }

    public String attachedTeamId() {
        return attachedTeamId;
    }

    public void setAttachment(@Nullable String stationId, @Nullable String teamId) {
        String nextStationId = stationId == null ? "" : stationId.strip();
        String nextTeamId = teamId == null ? "" : teamId.strip();
        if (attachedStationId.equals(nextStationId) && attachedTeamId.equals(nextTeamId)) {
            return;
        }
        attachedStationId = nextStationId;
        attachedTeamId = nextTeamId;
        setChanged();
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (level != null && !level.isClientSide) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, 3);
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
        attachedStationId = tag.getString("attachedStationId");
        attachedTeamId = tag.getString("attachedTeamId");
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.saveAdditional(tag, registries);
        if (!attachedStationId.isBlank()) {
            tag.putString("attachedStationId", attachedStationId);
        }
        if (!attachedTeamId.isBlank()) {
            tag.putString("attachedTeamId", attachedTeamId);
        }
    }
}
