package ozokuz.incore.features.machines.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractMultiblockPartBlockEntity extends BlockEntity {
    private BlockPos controllerPos;
    private MultiblockOwnerKind ownerKind = MultiblockOwnerKind.NONE;
    private String ownerId = "";
    private String stationId = "";
    private String teamId = "";

    protected AbstractMultiblockPartBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public final void bindToController(BlockPos controllerPos, String stationId, String teamId) {
        if (controllerPos == null || stationId == null || stationId.isBlank()) {
            clearBinding();
            return;
        }
        this.controllerPos = controllerPos.immutable();
        ownerKind = MultiblockOwnerKind.STATION;
        ownerId = stationId.strip();
        this.stationId = stationId.strip();
        this.teamId = teamId == null ? "" : teamId.strip();
        onBindingChanged();
        setChanged();
    }

    public final void bindToOrchestrator(BlockPos controllerPos, String orchestratorId, String teamId) {
        if (controllerPos == null || orchestratorId == null || orchestratorId.isBlank()) {
            clearBinding();
            return;
        }
        this.controllerPos = controllerPos.immutable();
        ownerKind = MultiblockOwnerKind.ORCHESTRATOR;
        ownerId = orchestratorId.strip();
        stationId = "";
        this.teamId = teamId == null ? "" : teamId.strip();
        onBindingChanged();
        setChanged();
    }

    public final void clearBinding() {
        boolean changed = controllerPos != null || ownerKind != MultiblockOwnerKind.NONE || !ownerId.isBlank() || !stationId.isBlank() || !teamId.isBlank();
        controllerPos = null;
        ownerKind = MultiblockOwnerKind.NONE;
        ownerId = "";
        stationId = "";
        teamId = "";
        if (changed) {
            onBindingChanged();
            setChanged();
        }
    }

    public final BlockPos controllerPos() {
        return controllerPos == null ? null : controllerPos.immutable();
    }

    public final String stationId() {
        return stationId;
    }

    public final MultiblockOwnerKind ownerKind() {
        return ownerKind;
    }

    public final String ownerId() {
        return ownerId;
    }

    public final String teamId() {
        return teamId;
    }

    protected void onBindingChanged() {
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
        try {
            ownerKind = MultiblockOwnerKind.valueOf(tag.getString("ownerKind"));
        } catch (IllegalArgumentException ignored) {
            ownerKind = MultiblockOwnerKind.NONE;
        }
        ownerId = tag.getString("ownerId");
        stationId = tag.getString("stationId");
        teamId = tag.getString("teamId");
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.saveAdditional(tag, registries);
        if (controllerPos != null) {
            tag.putLong("controllerPos", controllerPos.asLong());
        }
        tag.putString("ownerKind", ownerKind.name());
        if (!ownerId.isBlank()) {
            tag.putString("ownerId", ownerId);
        }
        if (!stationId.isBlank()) {
            tag.putString("stationId", stationId);
        }
        if (!teamId.isBlank()) {
            tag.putString("teamId", teamId);
        }
    }
}
