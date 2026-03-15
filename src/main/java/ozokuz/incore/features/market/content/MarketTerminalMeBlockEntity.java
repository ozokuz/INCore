package ozokuz.incore.features.market.content;

import appeng.api.networking.IGridNode;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.IActionSource;
import ozokuz.incore.Registration;
import ozokuz.incore.integration.ae2.Ae2GridNodeComponent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MarketTerminalMeBlockEntity extends AbstractMarketTerminalBlockEntity implements IInWorldGridNodeHost, IActionHost {
    private final Ae2GridNodeComponent<MarketTerminalMeBlockEntity> gridNode = new Ae2GridNodeComponent<>(this, "mainGridNode");
    private final IActionSource actionSource = IActionSource.ofMachine(this);

    public MarketTerminalMeBlockEntity(BlockPos pos, BlockState state) {
        super(Registration.MARKET_TERMINAL_ME_BE.get(), pos, state);
        gridNode.setVisualRepresentation(Registration.MARKET_TERMINAL_ME_BLOCK.get());
    }

    public IActionSource actionSource() {
        return actionSource;
    }

    public boolean ae2Linked() {
        return gridNode.isReady();
    }

    public boolean ae2Online() {
        return gridNode.isOnline() && gridNode.hasGridBooted() && gridNode.isPowered();
    }

    public @Nullable appeng.api.networking.IGrid grid() {
        return gridNode.managedNode().getGrid();
    }

    @Override
    public void clearRemoved() {
        super.clearRemoved();
        gridNode.clearRemoved();
    }

    @Override
    public void setRemoved() {
        gridNode.setRemoved();
        super.setRemoved();
    }

    @Override
    public void onChunkUnloaded() {
        gridNode.onChunkUnloaded();
        super.onChunkUnloaded();
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.loadAdditional(tag, registries);
        gridNode.loadFromTag(tag);
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.saveAdditional(tag, registries);
        gridNode.saveToTag(tag);
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, @NotNull Inventory playerInventory, @NotNull Player player) {
        return new MarketTerminalMeCardMenu(containerId, playerInventory, this);
    }

    @Override
    public @Nullable IGridNode getGridNode(Direction dir) {
        return gridNode.getGridNode(dir);
    }

    @Override
    public @Nullable IGridNode getActionableNode() {
        return gridNode.managedNode().getNode();
    }
}
