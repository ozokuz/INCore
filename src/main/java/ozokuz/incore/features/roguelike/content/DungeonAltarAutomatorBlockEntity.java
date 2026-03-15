package ozokuz.incore.features.roguelike.content;

import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.networking.crafting.ICraftingLink;
import appeng.api.networking.crafting.ICraftingRequester;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import com.google.common.collect.ImmutableSet;
import ozokuz.incore.Registration;
import ozokuz.incore.features.roguelike.RoguelikeService;
import ozokuz.incore.features.roguelike.data.AltarOfferingData;
import ozokuz.incore.features.roguelike.data.AltarOfferingManager;
import ozokuz.incore.features.roguelike.state.RoguelikeSavedData;
import ozokuz.incore.features.roguelike.state.RoguelikeSavedData.AltarRequirement;
import ozokuz.incore.integration.ae2.Ae2GridNodeComponent;
import ozokuz.incore.integration.ae2.Ae2StorageAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.wrapper.InvWrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class DungeonAltarAutomatorBlockEntity extends BlockEntity implements Container, IInWorldGridNodeHost, IActionHost, ICraftingRequester {
    public static final int CRYSTAL_SLOT = 0;
    public static final int SLOT_COUNT = 1;

    public static final int STATUS_NO_ALTAR_ABOVE = 0;
    public static final int STATUS_AE2_OFFLINE = 1;
    public static final int STATUS_NO_CRYSTAL = 2;
    public static final int STATUS_AWAITING_ITEMS = 3;
    public static final int STATUS_REQUESTING = 4;
    public static final int STATUS_ALTAR_COMPLETE = 5;

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private final Map<AltarRequirement, Integer> offeringBuffer = new LinkedHashMap<>();
    private final Map<AltarRequirement, ICraftingLink> activeLinks = new LinkedHashMap<>();
    private final Map<AltarRequirement, CompoundTag> serializedLinks = new LinkedHashMap<>();
    private final Ae2GridNodeComponent<DungeonAltarAutomatorBlockEntity> gridNode = new Ae2GridNodeComponent<>(this, "mainGridNode");
    private final IActionSource actionSource = IActionSource.ofMachine(this);

    private @Nullable UUID ownerId;
    private int status = STATUS_NO_ALTAR_ABOVE;
    private List<RequestView> requestViews = List.of();

    public DungeonAltarAutomatorBlockEntity(BlockPos pos, BlockState blockState) {
        super(Registration.DUNGEON_ALTAR_AUTOMATOR_BE.get(), pos, blockState);
        gridNode.setVisualRepresentation(Registration.DUNGEON_ALTAR_AUTOMATOR_BLOCK.get());
        gridNode.managedNode().addService(ICraftingRequester.class, this);
    }

    public @Nullable UUID ownerId() {
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

    public @Nullable BlockPos boundAltarPos() {
        return altar() != null ? worldPosition.above() : null;
    }

    public int statusForDisplay() {
        return status;
    }

    public List<RequestView> requestViews() {
        return requestViews;
    }

    public boolean ae2Linked() {
        return gridNode.isReady();
    }

    public boolean ae2Online() {
        return gridNode.isOnline() && gridNode.hasGridBooted() && gridNode.isPowered();
    }

    public @Nullable IGrid grid() {
        return gridNode.managedNode().getGrid();
    }

    public IActionSource actionSource() {
        return actionSource;
    }

    public Map<AltarRequirement, Integer> offeringBuffer() {
        return Map.copyOf(offeringBuffer);
    }

    public InvWrapper itemHandler() {
        return new InvWrapper(this);
    }

    public boolean canServeRequestsFor(@Nullable IGrid terminalGrid) {
        return ae2Online() && terminalGrid != null && terminalGrid == grid();
    }

    public int addBufferedItems(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        if (stack.is(Registration.EMPTY_DUNGEON_CRYSTAL_ITEM.get())) {
            if (!getItem(CRYSTAL_SLOT).isEmpty()) {
                return 0;
            }
            setItem(CRYSTAL_SLOT, stack.copyWithCount(1));
            return 1;
        }

        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (itemId == null) {
            return 0;
        }
        int inserted = stack.getCount();

        AltarRequirement targetRequirement = findRequirementForItem(itemId);
        if (targetRequirement == null) {
            return 0;
        }

        offeringBuffer.merge(targetRequirement, inserted, Integer::sum);
        setChanged();
        syncToClient();
        return inserted;
    }

    private @Nullable AltarRequirement findRequirementForItem(ResourceLocation itemId) {
        if (!(level instanceof ServerLevel serverLevel) || ownerId == null) {
            return null;
        }
        List<AltarRequirement> requirements = RoguelikeService.altarRequirementsForOwner(serverLevel.getServer(), ownerId);
        for (AltarRequirement req : requirements) {
            AltarOfferingData offering = AltarOfferingManager.OFFERINGS.get(req.offeringId());
            if (offering != null) {
                ResourceLocation reqItemId = BuiltInRegistries.ITEM.getKey(offering.item());
                if (itemId.equals(reqItemId) && !req.isComplete()) {
                    return req;
                }
            }
        }
        return null;
    }

    public void requestMissingItems() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        DungeonAltarBlockEntity altar = altar();
        UUID owner = validateAltarAndOwner(serverLevel, altar);
        if (altar == null || owner == null) {
            status = STATUS_NO_ALTAR_ABOVE;
            requestViews = List.of();
            cancelAllLinks();
            syncToClient();
            return;
        }

        if (!ae2Online()) {
            updateViews(serverLevel, owner);
            return;
        }

        for (RequestView view : computeRequestViews(serverLevel, owner)) {
            if (view.missing() <= 0) {
                continue;
            }

            var item = BuiltInRegistries.ITEM.get(view.itemId());
            if (item == null || item == net.minecraft.world.item.Items.AIR) {
                continue;
            }

            int remaining = view.missing();
            long extracted = Ae2StorageAccess.extract(grid(), actionSource, new ItemStack(item), remaining);
            if (extracted > 0) {
                int inserted = addBufferedItems(new ItemStack(item, (int) Math.min(Integer.MAX_VALUE, extracted)));
                remaining -= inserted;
            }

            if (remaining > 0 && !activeLinks.containsKey(view.requirement()) && Ae2StorageAccess.isCraftable(grid(), new ItemStack(item))) {
                ICraftingLink link = Ae2StorageAccess.requestAutocrafting(
                        grid(),
                        serverLevel,
                        actionSource,
                        this,
                        new ItemStack(item, remaining)
                );
                if (link != null) {
                    activeLinks.put(view.requirement(), link);
                }
            }
        }

        updateViews(serverLevel, owner);
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

        items.set(CRYSTAL_SLOT, ItemStack.EMPTY);
        ListTag itemList = tag.getList("items", Tag.TAG_COMPOUND);
        for (Tag itemTag : itemList) {
            CompoundTag row = (CompoundTag) itemTag;
            int slot = row.getInt("slot");
            if (slot >= 0 && slot < SLOT_COUNT) {
                items.set(slot, ItemStack.parseOptional(registries, row.getCompound("stack")));
            }
        }

        offeringBuffer.clear();
        ListTag bufferList = tag.getList("offeringBuffer", Tag.TAG_COMPOUND);
        for (Tag bufferTag : bufferList) {
            CompoundTag row = (CompoundTag) bufferTag;
            if (row.hasUUID("reqId")) {
                UUID reqId = row.getUUID("reqId");
                int count = Math.max(0, row.getInt("count"));
                AltarRequirement req = new AltarRequirement(reqId, ResourceLocation.tryParse(row.getString("item")), count, 0);
                offeringBuffer.put(req, count);
            }
        }

        ownerId = tag.hasUUID("ownerId") ? tag.getUUID("ownerId") : null;
        status = tag.getInt("status");
        requestViews = readViews(tag.getList("requestViews", Tag.TAG_COMPOUND));

        activeLinks.clear();
        serializedLinks.clear();
        ListTag linkList = tag.getList("activeLinks", Tag.TAG_COMPOUND);
        for (Tag linkTag : linkList) {
            CompoundTag row = (CompoundTag) linkTag;
            ResourceLocation itemId = ResourceLocation.tryParse(row.getString("item"));
            if (row.hasUUID("reqId") && itemId != null) {
                UUID reqId = row.getUUID("reqId");
                AltarRequirement req = new AltarRequirement(reqId, itemId, 1, 0);
                serializedLinks.put(req, row.getCompound("link"));
            }
        }
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.saveAdditional(tag, registries);
        gridNode.saveToTag(tag);

        ListTag itemList = new ListTag();
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            ItemStack stack = items.get(slot);
            if (stack.isEmpty()) {
                continue;
            }
            CompoundTag row = new CompoundTag();
            row.putInt("slot", slot);
            row.put("stack", stack.save(registries));
            itemList.add(row);
        }
        tag.put("items", itemList);

        ListTag bufferList = new ListTag();
        for (Map.Entry<AltarRequirement, Integer> entry : offeringBuffer.entrySet()) {
            CompoundTag row = new CompoundTag();
            row.putUUID("reqId", entry.getKey().id());
            row.putString("item", entry.getKey().offeringId().toString());
            row.putInt("count", entry.getValue());
            bufferList.add(row);
        }
        tag.put("offeringBuffer", bufferList);

        if (ownerId != null) {
            tag.putUUID("ownerId", ownerId);
        }
        tag.putInt("status", status);

        ListTag viewsTag = new ListTag();
        for (RequestView view : requestViews) {
            CompoundTag row = new CompoundTag();
            if (view.requirement() != null) {
                row.putUUID("reqId", view.requirement().id());
            }
            row.putString("item", view.itemId().toString());
            row.putInt("submitted", view.submitted());
            row.putInt("required", view.required());
            row.putInt("buffered", view.buffered());
            row.putLong("meAvailable", view.meAvailable());
            row.putBoolean("craftable", view.craftable());
            row.putBoolean("requesting", view.requesting());
            viewsTag.add(row);
        }
        tag.put("requestViews", viewsTag);

        ListTag linkList = new ListTag();
        for (Map.Entry<AltarRequirement, ICraftingLink> entry : activeLinks.entrySet()) {
            CompoundTag row = new CompoundTag();
            row.putUUID("reqId", entry.getKey().id());
            row.putString("item", entry.getKey().offeringId().toString());
            CompoundTag linkTag = new CompoundTag();
            entry.getValue().writeToNBT(linkTag);
            row.put("link", linkTag);
            linkList.add(row);
        }
        for (Map.Entry<AltarRequirement, CompoundTag> entry : serializedLinks.entrySet()) {
            if (activeLinks.containsKey(entry.getKey())) {
                continue;
            }
            CompoundTag row = new CompoundTag();
            row.putUUID("reqId", entry.getKey().id());
            row.putString("item", entry.getKey().offeringId().toString());
            row.put("link", entry.getValue().copy());
            linkList.add(row);
        }
        tag.put("activeLinks", linkList);
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
        if (!(blockEntity instanceof DungeonAltarAutomatorBlockEntity automator) || level.isClientSide || !(level instanceof ServerLevel serverLevel)) {
            return;
        }
        if (serverLevel.getGameTime() % 5L != 0L) {
            return;
        }
        automator.serverTick(serverLevel);
    }

    private void serverTick(ServerLevel level) {
        restoreCraftingLinks();

        DungeonAltarBlockEntity altar = altar();
        UUID owner = validateAltarAndOwner(level, altar);
        if (altar == null || owner == null) {
            status = STATUS_NO_ALTAR_ABOVE;
            requestViews = List.of();
            cancelAllLinks();
            syncToClient();
            return;
        }

        RoguelikeService.ensureAltarRequirements(level.getServer(), owner);
        ensureCrystalReady(level, owner);

        if (RoguelikeService.isAltarComplete(level.getServer(), owner)) {
            updateViews(level, owner);
            status = STATUS_ALTAR_COMPLETE;
            setChanged();
            syncToClient();
            return;
        }

        submitBufferedOfferings(level, owner);
        updateViews(level, owner);
        setChanged();
        syncToClient();
    }

    private void ensureCrystalReady(ServerLevel level, UUID owner) {
        if (RoguelikeService.isCrystalPlaced(level.getServer(), owner)) {
            return;
        }

        if (getItem(CRYSTAL_SLOT).isEmpty() && ae2Online()) {
            long extracted = Ae2StorageAccess.extract(grid(), actionSource, new ItemStack(Registration.EMPTY_DUNGEON_CRYSTAL_ITEM.get()), 1);
            if (extracted > 0) {
                setItem(CRYSTAL_SLOT, new ItemStack(Registration.EMPTY_DUNGEON_CRYSTAL_ITEM.get(), 1));
            }
        }

        ItemStack crystal = getItem(CRYSTAL_SLOT);
        if (crystal.isEmpty()) {
            return;
        }

        BlockPos altarPos = worldPosition.above();
        if (RoguelikeService.placeCrystalForAutomation(level, altarPos, owner)) {
            crystal.shrink(1);
            if (crystal.isEmpty()) {
                items.set(CRYSTAL_SLOT, ItemStack.EMPTY);
            }
        }
    }

    private void submitBufferedOfferings(ServerLevel level, UUID owner) {
        BlockPos altarPos = worldPosition.above();
        for (RoguelikeSavedData.AltarRequirement requirement : RoguelikeService.altarRequirementsForOwner(level.getServer(), owner)) {
            AltarOfferingData offering = AltarOfferingManager.OFFERINGS.get(requirement.offeringId());
            if (offering == null || requirement.isComplete()) {
                continue;
            }

            int buffered = offeringBuffer.getOrDefault(requirement, 0);
            if (buffered <= 0) {
                continue;
            }

            int consumed = RoguelikeService.submitOfferingForAutomation(level, altarPos, owner, requirement.offeringId(), Math.min(buffered, requirement.remaining()));
            if (consumed <= 0) {
                continue;
            }

            int remaining = buffered - consumed;
            if (remaining <= 0) {
                offeringBuffer.remove(requirement);
            } else {
                offeringBuffer.put(requirement, remaining);
            }
        }
    }

    private void updateViews(ServerLevel level, UUID owner) {
        requestViews = computeRequestViews(level, owner);
        Set<ResourceLocation> pendingItems = new HashSet<>();
        for (RequestView view : requestViews) {
            if (view.missing() > 0) {
                pendingItems.add(view.itemId());
            }
        }

        activeLinks.entrySet().removeIf(entry -> {
            ICraftingLink link = entry.getValue();
            if (link == null || link.isCanceled() || link.isDone()) {
                return true;
            }
            if (!pendingItems.contains(entry.getKey())) {
                link.cancel();
                return true;
            }
            return false;
        });

        if (RoguelikeService.isAltarComplete(level.getServer(), owner)) {
            status = STATUS_ALTAR_COMPLETE;
            return;
        }

        boolean crystalMissing = requestViews.stream()
                .anyMatch(view -> view.itemId().equals(BuiltInRegistries.ITEM.getKey(Registration.EMPTY_DUNGEON_CRYSTAL_ITEM.get())) && view.missing() > 0);
        if (crystalMissing && !ae2Online() && getItem(CRYSTAL_SLOT).isEmpty()) {
            status = STATUS_NO_CRYSTAL;
            return;
        }
        if (crystalMissing && getItem(CRYSTAL_SLOT).isEmpty() && requestViews.stream().noneMatch(view -> view.itemId().equals(BuiltInRegistries.ITEM.getKey(Registration.EMPTY_DUNGEON_CRYSTAL_ITEM.get())) && (view.meAvailable() > 0 || view.requesting()))) {
            status = STATUS_NO_CRYSTAL;
            return;
        }
        if (!ae2Online()) {
            status = STATUS_AE2_OFFLINE;
            return;
        }
        status = activeLinks.isEmpty() ? STATUS_AWAITING_ITEMS : STATUS_REQUESTING;
    }

    private List<RequestView> computeRequestViews(ServerLevel level, UUID owner) {
        List<RequestView> views = new ArrayList<>();
        boolean crystalPlaced = RoguelikeService.isCrystalPlaced(level.getServer(), owner);
        if (!crystalPlaced) {
            ItemStack crystalStack = new ItemStack(Registration.EMPTY_DUNGEON_CRYSTAL_ITEM.get());
            int buffered = getItem(CRYSTAL_SLOT).isEmpty() ? 0 : 1;
            ResourceLocation crystalItemId = BuiltInRegistries.ITEM.getKey(Registration.EMPTY_DUNGEON_CRYSTAL_ITEM.get());
            AltarRequirement crystalReq = new AltarRequirement(crystalItemId, 1, 0);
            views.add(new RequestView(
                    crystalReq,
                    crystalItemId,
                    0,
                    1,
                    buffered,
                    ae2Online() ? Ae2StorageAccess.count(grid(), crystalStack) : 0L,
                    ae2Online() && Ae2StorageAccess.isCraftable(grid(), crystalStack),
                    activeLinks.containsKey(crystalReq)
            ));
        }

        for (RoguelikeSavedData.AltarRequirement requirement : RoguelikeService.altarRequirementsForOwner(level.getServer(), owner)) {
            AltarOfferingData offering = AltarOfferingManager.OFFERINGS.get(requirement.offeringId());
            if (offering == null) {
                continue;
            }
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(offering.item());
            ItemStack stack = new ItemStack(offering.item());
            views.add(new RequestView(
                    requirement,
                    itemId,
                    requirement.submittedAmount(),
                    requirement.requiredAmount(),
                    offeringBuffer.getOrDefault(requirement, 0),
                    ae2Online() ? Ae2StorageAccess.count(grid(), stack) : 0L,
                    ae2Online() && Ae2StorageAccess.isCraftable(grid(), stack),
                    activeLinks.containsKey(requirement) || (ae2Online() && Ae2StorageAccess.isRequesting(grid(), stack))
            ));
        }
        return List.copyOf(views);
    }

    private @Nullable UUID validateAltarAndOwner(ServerLevel level, @Nullable DungeonAltarBlockEntity altar) {
        if (altar == null) {
            return null;
        }

        UUID altarOwner = altar.ownerId();
        if (ownerId == null) {
            ownerId = altarOwner;
        }
        if (ownerId == null) {
            return null;
        }
        if (altarOwner == null) {
            altar.setOwner(ownerId);
            return ownerId;
        }
        return altarOwner.equals(ownerId) ? ownerId : null;
    }

    private @Nullable DungeonAltarBlockEntity altar() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return level != null && level.getBlockEntity(worldPosition.above()) instanceof DungeonAltarBlockEntity altar ? altar : null;
        }
        return serverLevel.getBlockEntity(worldPosition.above()) instanceof DungeonAltarBlockEntity altar ? altar : null;
    }

    private void restoreCraftingLinks() {
        if (serializedLinks.isEmpty()) {
            return;
        }
        for (Map.Entry<AltarRequirement, CompoundTag> entry : List.copyOf(serializedLinks.entrySet())) {
            if (activeLinks.containsKey(entry.getKey())) {
                serializedLinks.remove(entry.getKey());
                continue;
            }
            ICraftingLink link = Ae2StorageAccess.loadCraftingLink(entry.getValue(), this);
            if (link != null) {
                activeLinks.put(entry.getKey(), link);
            }
            serializedLinks.remove(entry.getKey());
        }
    }

    private void cancelAllLinks() {
        activeLinks.values().forEach(ICraftingLink::cancel);
        activeLinks.clear();
        serializedLinks.clear();
    }

    private void syncToClient() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        BlockState state = getBlockState();
        serverLevel.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
    }

    private List<RequestView> readViews(ListTag viewsTag) {
        List<RequestView> views = new ArrayList<>();
        for (Tag viewTag : viewsTag) {
            CompoundTag row = (CompoundTag) viewTag;
            ResourceLocation itemId = ResourceLocation.tryParse(row.getString("item"));
            if (itemId == null) {
                continue;
            }
            AltarRequirement requirement = row.hasUUID("reqId") 
                ? new AltarRequirement(row.getUUID("reqId"), itemId, row.getInt("required"), row.getInt("submitted"))
                : null;
            views.add(new RequestView(
                    requirement,
                    itemId,
                    row.getInt("submitted"),
                    row.getInt("required"),
                    row.getInt("buffered"),
                    row.getLong("meAvailable"),
                    row.getBoolean("craftable"),
                    row.getBoolean("requesting")
            ));
        }
        return List.copyOf(views);
    }

    @Override
    public @Nullable IGridNode getGridNode(Direction dir) {
        return gridNode.getGridNode(dir);
    }

    @Override
    public @Nullable IGridNode getActionableNode() {
        return gridNode.managedNode().getNode();
    }

    @Override
    public ImmutableSet<ICraftingLink> getRequestedJobs() {
        return ImmutableSet.copyOf(activeLinks.values());
    }

    @Override
    public long insertCraftedItems(ICraftingLink link, appeng.api.stacks.AEKey what, long amount, appeng.api.config.Actionable mode) {
        if (!(what instanceof AEItemKey itemKey) || amount <= 0) {
            return 0;
        }
        ItemStack stack = itemKey.toStack((int) Math.min(Integer.MAX_VALUE, amount));
        if (mode == appeng.api.config.Actionable.SIMULATE) {
            if (stack.is(Registration.EMPTY_DUNGEON_CRYSTAL_ITEM.get())) {
                return getItem(CRYSTAL_SLOT).isEmpty() ? Math.min(1L, amount) : 0L;
            }
            return amount;
        }
        return addBufferedItems(stack);
    }

    @Override
    public void jobStateChange(ICraftingLink link) {
        activeLinks.entrySet().removeIf(entry -> entry.getValue() == link);
        setChanged();
        syncToClient();
    }

    @Override
    public int getContainerSize() {
        return SLOT_COUNT;
    }

    @Override
    public boolean isEmpty() {
        return items.get(CRYSTAL_SLOT).isEmpty();
    }

    @Override
    public @NotNull ItemStack getItem(int slot) {
        if (slot != CRYSTAL_SLOT) {
            return ItemStack.EMPTY;
        }
        return items.get(CRYSTAL_SLOT);
    }

    @Override
    public @NotNull ItemStack removeItem(int slot, int amount) {
        if (slot != CRYSTAL_SLOT) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = items.get(CRYSTAL_SLOT);
        ItemStack removed = stack.split(amount);
        if (stack.isEmpty()) {
            items.set(CRYSTAL_SLOT, ItemStack.EMPTY);
        }
        setChanged();
        syncToClient();
        return removed;
    }

    @Override
    public @NotNull ItemStack removeItemNoUpdate(int slot) {
        if (slot != CRYSTAL_SLOT) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = items.get(CRYSTAL_SLOT);
        items.set(CRYSTAL_SLOT, ItemStack.EMPTY);
        return stack;
    }

    @Override
    public void setItem(int slot, @NotNull ItemStack stack) {
        if (slot != CRYSTAL_SLOT) {
            return;
        }
        items.set(CRYSTAL_SLOT, stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1));
        setChanged();
        syncToClient();
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return level != null && level.getBlockEntity(worldPosition) == this && player.distanceToSqr(worldPosition.getCenter()) <= 64.0D;
    }

    @Override
    public boolean canPlaceItem(int slot, @NotNull ItemStack stack) {
        return slot == CRYSTAL_SLOT && stack.is(Registration.EMPTY_DUNGEON_CRYSTAL_ITEM.get());
    }

    @Override
    public void clearContent() {
        items.set(CRYSTAL_SLOT, ItemStack.EMPTY);
        setChanged();
        syncToClient();
    }

    public record RequestView(AltarRequirement requirement, ResourceLocation itemId, int submitted, int required, int buffered, long meAvailable, boolean craftable, boolean requesting) {
        public int missing() {
            return Math.max(0, required - submitted - buffered);
        }
    }
}
