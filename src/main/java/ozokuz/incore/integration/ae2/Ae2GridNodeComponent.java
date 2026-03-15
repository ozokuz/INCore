package ozokuz.incore.integration.ae2;

import appeng.api.networking.GridFlags;
import appeng.api.networking.GridHelper;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IGridNodeListener;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.networking.IManagedGridNode;
import appeng.api.util.AECableType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.Set;

public final class Ae2GridNodeComponent<T extends BlockEntity & IInWorldGridNodeHost> {
    @SuppressWarnings("rawtypes")
    private static final IGridNodeListener LISTENER = new IGridNodeListener<BlockEntity>() {
        @Override
        public void onSaveChanges(BlockEntity nodeOwner, IGridNode node) {
            nodeOwner.setChanged();
        }

        @Override
        public void onStateChanged(BlockEntity nodeOwner, IGridNode node, State state) {
            nodeOwner.setChanged();
            if (nodeOwner.getLevel() != null && !nodeOwner.getLevel().isClientSide) {
                BlockState blockState = nodeOwner.getBlockState();
                nodeOwner.getLevel().sendBlockUpdated(nodeOwner.getBlockPos(), blockState, blockState, 3);
            }
        }
    };

    private final T owner;
    private final IManagedGridNode managedNode;
    private final String tagName;

    public Ae2GridNodeComponent(T owner, String tagName) {
        this.owner = owner;
        this.tagName = tagName;
        @SuppressWarnings("unchecked")
        IGridNodeListener<T> typedListener = (IGridNodeListener<T>) (IGridNodeListener<?>) LISTENER;
        this.managedNode = GridHelper.createManagedNode(owner, typedListener)
                .setTagName(tagName)
                .setInWorldNode(true)
                .setExposedOnSides(allDirections())
                .setFlags(GridFlags.REQUIRE_CHANNEL)
                .setIdlePowerUsage(1.0D);
    }

    public IManagedGridNode managedNode() {
        return managedNode;
    }

    public void setVisualRepresentation(net.minecraft.world.level.ItemLike itemLike) {
        managedNode.setVisualRepresentation(itemLike);
    }

    public void loadFromTag(CompoundTag tag) {
        if (tag.contains(tagName, CompoundTag.TAG_COMPOUND)) {
            managedNode.loadFromNBT(tag.getCompound(tagName));
        }
    }

    public void saveToTag(CompoundTag tag) {
        CompoundTag nodeTag = new CompoundTag();
        managedNode.saveToNBT(nodeTag);
        tag.put(tagName, nodeTag);
    }

    public void onPlacedBy(net.minecraft.world.entity.player.Player player) {
        managedNode.setOwningPlayer(player);
    }

    public void onReady() {
        Level level = owner.getLevel();
        BlockPos pos = owner.getBlockPos();
        if (level != null && !level.isClientSide && !managedNode.isReady()) {
            managedNode.create(level, pos);
        }
    }

    public void clearRemoved() {
        GridHelper.onFirstTick(owner, ignored -> onReady());
    }

    public void setRemoved() {
        managedNode.destroy();
    }

    public void onChunkUnloaded() {
        managedNode.destroy();
    }

    public boolean isReady() {
        return managedNode.isReady();
    }

    public boolean isOnline() {
        return managedNode.isOnline();
    }

    public boolean hasGridBooted() {
        return managedNode.hasGridBooted();
    }

    public boolean isPowered() {
        return managedNode.isPowered();
    }

    public @Nullable IGridNode getGridNode(Direction direction) {
        return managedNode.getNode();
    }

    public AECableType getCableConnectionType(Direction direction) {
        return AECableType.SMART;
    }

    private static Set<Direction> allDirections() {
        return EnumSet.allOf(Direction.class);
    }
}
