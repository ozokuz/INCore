package io.github.ozokuz.incore.features.researchv2.station;

import io.github.ozokuz.incore.Registration;
import io.github.ozokuz.incore.features.researchv2.station.network.StationNetworkService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class WirelessLinkBlockEntity extends AbstractInventoryStationPartBlockEntity {
    private String channelId = "";
    private String ownerTeamId = "";
    private int bindingStatus = 0;

    public WirelessLinkBlockEntity(BlockPos pos, BlockState state) {
        super(Registration.WIRELESS_LINK_BE.get(), pos, state, 1);
    }

    @Override
    public StationPartType stationPartType() {
        return StationPartType.WIRELESS_LINK;
    }

    @Override
    public int activeSlotCount() {
        return 1;
    }

    @Override
    protected boolean mayPlaceItem(int slot, ItemStack stack) {
        return StationInventoryRules.isSignalTransmitter(stack);
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory playerInventory) {
        return new WirelessLinkMenu(containerId, playerInventory, this);
    }

    @Override
    protected void onInventoryContentsChanged(int slot) {
        refreshBindingState();
    }

    @Override
    protected void onBindingChanged() {
        refreshBindingState();
    }

    public ItemStack transmitter() {
        return rawItemHandler().getStackInSlot(0);
    }

    public String channelId() {
        return channelId;
    }

    public String ownerTeamId() {
        return ownerTeamId;
    }

    public boolean hasStoredChannel() {
        return !channelId.isBlank() && !ownerTeamId.isBlank();
    }

    public int bindingStatus() {
        return bindingStatus;
    }

    public boolean hasInstalledTransmitter() {
        return !transmitter().isEmpty();
    }

    public boolean transmitterMatchesStoredBinding() {
        return SignalTransmitterData.matches(transmitter(), channelId, ownerTeamId);
    }

    public void clearStoredBinding() {
        if (channelId.isBlank() && ownerTeamId.isBlank()) {
            return;
        }
        channelId = "";
        ownerTeamId = "";
        bindingStatus = 0;
        setChanged();
        notifyTopologyChanged();
    }

    public void refreshBindingState() {
        if (level == null || level.isClientSide) {
            return;
        }

        ItemStack stack = transmitter();
        if (ownerKind() == LinkOwnerKind.ORCHESTRATOR) {
            if (!stack.isEmpty()) {
                if (!SignalTransmitterData.hasBinding(stack)) {
                    if (channelId.isBlank() || ownerTeamId.isBlank()) {
                        SignalTransmitterData.initialize(stack, teamId());
                        channelId = SignalTransmitterData.readChannelId(stack);
                        ownerTeamId = SignalTransmitterData.readOwnerTeamId(stack);
                    } else {
                        SignalTransmitterData.write(stack, channelId, ownerTeamId);
                    }
                    bindingStatus = 1;
                } else if (channelId.isBlank() || ownerTeamId.isBlank()) {
                    if (teamId().equals(SignalTransmitterData.readOwnerTeamId(stack))) {
                        channelId = SignalTransmitterData.readChannelId(stack);
                        ownerTeamId = SignalTransmitterData.readOwnerTeamId(stack);
                        bindingStatus = 1;
                    } else {
                        bindingStatus = 2;
                    }
                } else if (SignalTransmitterData.matches(stack, channelId, ownerTeamId)) {
                    bindingStatus = 1;
                } else {
                    bindingStatus = 2;
                }
            } else {
                bindingStatus = hasStoredChannel() ? 3 : 0;
            }
        } else if (ownerKind() == LinkOwnerKind.STATION) {
            bindingStatus = stack.isEmpty() ? 0 : (SignalTransmitterData.hasBinding(stack) ? 1 : 2);
        } else {
            bindingStatus = stack.isEmpty() ? 0 : 2;
        }

        setChanged();
        notifyTopologyChanged();
    }

    private void notifyTopologyChanged() {
        if (level != null && !level.isClientSide) {
            StationNetworkService.onTopologyChanged(level);
        }
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.loadAdditional(tag, registries);
        channelId = tag.getString("channelId");
        ownerTeamId = tag.getString("ownerTeamId");
        bindingStatus = Math.max(0, tag.getInt("bindingStatus"));
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.saveAdditional(tag, registries);
        if (!channelId.isBlank()) {
            tag.putString("channelId", channelId);
        }
        if (!ownerTeamId.isBlank()) {
            tag.putString("ownerTeamId", ownerTeamId);
        }
        tag.putInt("bindingStatus", bindingStatus);
    }
}
