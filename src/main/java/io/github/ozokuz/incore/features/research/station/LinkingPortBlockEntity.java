package io.github.ozokuz.incore.features.research.station;

import io.github.ozokuz.incore.Registration;
import io.github.ozokuz.incore.features.research.station.network.LinkingPortRegistry;
import io.github.ozokuz.incore.features.research.station.network.StationNetworkService;
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
    private LinkOwnerKind ownerKind = LinkOwnerKind.NONE;
    private String ownerId = "";
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
        return ownerKind == LinkOwnerKind.STATION ? ownerId : "";
    }

    public LinkOwnerKind ownerKind() {
        return ownerKind;
    }

    public String ownerId() {
        return ownerId;
    }

    public String attachedTeamId() {
        return attachedTeamId;
    }

    public void setAttachment(LinkOwnerKind ownerKind, @Nullable String ownerId, @Nullable String teamId) {
        LinkOwnerKind nextOwnerKind = ownerKind == null ? LinkOwnerKind.NONE : ownerKind;
        String nextOwnerId = ownerId == null ? "" : ownerId.strip();
        String nextTeamId = teamId == null ? "" : teamId.strip();
        if (this.ownerKind == nextOwnerKind && this.ownerId.equals(nextOwnerId) && attachedTeamId.equals(nextTeamId)) {
            return;
        }
        this.ownerKind = nextOwnerKind;
        this.ownerId = nextOwnerId;
        attachedTeamId = nextTeamId;
        setChanged();
    }

    public void clearAttachment() {
        setAttachment(LinkOwnerKind.NONE, "", "");
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
        try {
            ownerKind = LinkOwnerKind.valueOf(tag.getString("ownerKind"));
        } catch (IllegalArgumentException ignored) {
            ownerKind = LinkOwnerKind.NONE;
        }
        ownerId = tag.getString("ownerId");
        attachedTeamId = tag.getString("attachedTeamId");
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString("ownerKind", ownerKind.name());
        if (!ownerId.isBlank()) {
            tag.putString("ownerId", ownerId);
        }
        if (!attachedTeamId.isBlank()) {
            tag.putString("attachedTeamId", attachedTeamId);
        }
    }
}
