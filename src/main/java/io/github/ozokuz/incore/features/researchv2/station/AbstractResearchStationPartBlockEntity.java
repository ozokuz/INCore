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
    private LinkOwnerKind ownerKind = LinkOwnerKind.NONE;
    private String ownerId = "";
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
        ownerKind = LinkOwnerKind.STATION;
        ownerId = controller.stationId();
        stationId = controller.stationId();
        teamId = controller.teamId();
        onBindingChanged();
        setChanged();
    }

    public final void bindToOrchestrator(ResearchOrchestratorControllerBlockEntity orchestrator) {
        if (orchestrator == null || !orchestrator.isFormed()) {
            clearBinding();
            return;
        }
        controllerPos = orchestrator.getBlockPos().immutable();
        ownerKind = LinkOwnerKind.ORCHESTRATOR;
        ownerId = orchestrator.orchestratorId();
        stationId = "";
        teamId = orchestrator.teamId();
        onBindingChanged();
        setChanged();
    }

    public final void clearBinding() {
        boolean changed = controllerPos != null || ownerKind != LinkOwnerKind.NONE || !ownerId.isBlank() || !stationId.isBlank() || !teamId.isBlank();
        controllerPos = null;
        ownerKind = LinkOwnerKind.NONE;
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

    public final LinkOwnerKind ownerKind() {
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
            ownerKind = LinkOwnerKind.valueOf(tag.getString("ownerKind"));
        } catch (IllegalArgumentException ignored) {
            ownerKind = LinkOwnerKind.NONE;
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
