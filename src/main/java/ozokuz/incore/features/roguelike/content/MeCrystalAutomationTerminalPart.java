package ozokuz.incore.features.roguelike.content;

import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.parts.IPartItem;
import appeng.api.parts.IPartModel;
import appeng.core.AppEng;
import appeng.parts.PartModel;
import appeng.parts.reporting.AbstractDisplayPart;
import appeng.items.parts.PartModels;
import ozokuz.incore.features.market.MarketTeamAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ozokuz.incore.features.roguelike.state.RoguelikeSavedData;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class MeCrystalAutomationTerminalPart extends AbstractDisplayPart implements IGridTickable {
    public static final int STATUS_NO_LINK = 0;
    public static final int STATUS_AE2_OFFLINE = 1;
    public static final int STATUS_AUTOMATOR_OFFLINE = 2;
    public static final int STATUS_NETWORK_MISMATCH = 3;
    public static final int STATUS_READY = 4;
    public static final int STATUS_REQUESTING = 5;

    @PartModels
    public static final ResourceLocation MODEL_OFF = AppEng.makeId("part/terminal_off");
    @PartModels
    public static final ResourceLocation MODEL_ON = AppEng.makeId("part/terminal_on");

    public static final IPartModel MODELS_OFF = new PartModel(MODEL_BASE, MODEL_OFF, MODEL_STATUS_OFF);
    public static final IPartModel MODELS_ON = new PartModel(MODEL_BASE, MODEL_ON, MODEL_STATUS_ON);
    public static final IPartModel MODELS_HAS_CHANNEL = new PartModel(MODEL_BASE, MODEL_ON, MODEL_STATUS_HAS_CHANNEL);

    private static final TickingRequest TICKING_REQUEST = new TickingRequest(5, 20, false);

    private final IActionSource actionSource = IActionSource.ofMachine(this);

    private @Nullable UUID ownerId;
    private @Nullable BlockPos boundAutomatorPos;
    private @Nullable BlockPos boundAltarPos;
    private int status = STATUS_NO_LINK;
    private List<DungeonAltarAutomatorBlockEntity.RequestView> requestViews = List.of();

    public MeCrystalAutomationTerminalPart(IPartItem<?> partItem) {
        super(partItem, true);
        getMainNode().addService(IGridTickable.class, this);
    }

    @Override
    public boolean useStandardMemoryCard() {
        return false;
    }

    @Override
    public boolean onUseWithoutItem(Player player, Vec3 pos) {
        if (ownerId == null && !player.level().isClientSide()) {
            ownerId = player.getUUID();
            markDirty();
        }
        if (player.isShiftKeyDown()) {
            if (!player.level().isClientSide()) {
                player.sendSystemMessage(statusText(status));
            }
            return true;
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return true;
        }

        serverPlayer.openMenu(
                new SimpleMenuProvider(this::createMenu, getPartItem().asItem().getDescription()),
                buffer -> {
                    buffer.writeBlockPos(hostPos());
                    buffer.writeByte(getSide().get3DDataValue());
                }
        );
        return true;
    }

    @Override
    public boolean onUseItemOn(ItemStack heldItem, Player player, net.minecraft.world.InteractionHand hand, Vec3 pos) {
        if (!AutomatorMemoryCardLink.isMemoryCard(heldItem)) {
            return false;
        }

        AutomatorMemoryCardLink link = AutomatorMemoryCardLink.read(heldItem);
        if (link == null) {
            if (!player.level().isClientSide()) {
                player.sendSystemMessage(Component.translatable("incore.roguelike.terminal.memory_card.invalid"));
            }
            return true;
        }

        BindResult result = bindToAutomator(player, link);
        if (!player.level().isClientSide()) {
            player.sendSystemMessage(switch (result) {
                case SUCCESS -> Component.translatable("incore.roguelike.terminal.memory_card.linked");
                case WRONG_OWNER -> Component.translatable("incore.roguelike.terminal.memory_card.wrong_owner");
                case TARGET_MISSING -> Component.translatable("incore.roguelike.terminal.memory_card.target_missing");
                case INVALID_TARGET -> Component.translatable("incore.roguelike.terminal.memory_card.invalid_target");
                case INVALID_CARD -> Component.translatable("incore.roguelike.terminal.memory_card.invalid");
            });
        }
        return true;
    }

    public @Nullable UUID ownerId() {
        return ownerId;
    }

    public @Nullable BlockPos boundAutomatorPos() {
        return boundAutomatorPos;
    }

    public @Nullable BlockPos boundAltarPos() {
        return boundAltarPos;
    }

    public int statusForDisplay() {
        return status;
    }

    public List<DungeonAltarAutomatorBlockEntity.RequestView> requestViews() {
        return requestViews;
    }

    public boolean ae2Linked() {
        return getMainNode().isReady();
    }

    public boolean ae2Online() {
        IGridNode node = getGridNode();
        return node != null && node.isPowered() && node.hasGridBooted();
    }

    public @Nullable IGrid grid() {
        return getMainNode().getGrid();
    }

    public IActionSource actionSource() {
        return actionSource;
    }

    public BlockPos hostPos() {
        return getBlockEntity().getBlockPos();
    }

    public BindResult bindToAutomator(Player player, @Nullable AutomatorMemoryCardLink link) {
        if (link == null || getLevel() == null) {
            return BindResult.INVALID_CARD;
        }
        if (!getLevel().dimension().location().equals(link.dimensionId())) {
            return BindResult.TARGET_MISSING;
        }

        if (!(getLevel() instanceof ServerLevel serverLevel)) {
            return BindResult.INVALID_TARGET;
        }

        if (!(serverLevel.getBlockEntity(link.automatorPos()) instanceof DungeonAltarAutomatorBlockEntity automator)) {
            return BindResult.TARGET_MISSING;
        }
        if (link.ownerId() != null && !MarketTeamAccess.canAccess(link.ownerId(), player)) {
            return BindResult.WRONG_OWNER;
        }
        if (!MarketTeamAccess.canAccess(automator.ownerId(), player)) {
            return BindResult.WRONG_OWNER;
        }
        if (automator.boundAltarPos() == null) {
            return BindResult.INVALID_TARGET;
        }

        boolean changed = !Objects.equals(boundAutomatorPos, link.automatorPos());
        boundAutomatorPos = link.automatorPos();
        if (ownerId == null) {
            ownerId = player.getUUID();
            changed = true;
        }
        refreshSnapshot();
        if (changed) {
            markDirty();
        }
        return BindResult.SUCCESS;
    }

    public void requestMissingItems() {
        if (!(getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }

        refreshSnapshot();
        DungeonAltarAutomatorBlockEntity automator = boundAutomator(serverLevel);
        if (automator == null) {
            updateState(boundAltarPos, STATUS_NO_LINK, requestViews);
            return;
        }
        if (!ae2Online()) {
            updateState(boundAltarPos, STATUS_AE2_OFFLINE, requestViews);
            return;
        }
        if (!automator.ae2Online()) {
            updateState(boundAltarPos, STATUS_AUTOMATOR_OFFLINE, requestViews);
            return;
        }
        if (grid() != automator.grid()) {
            updateState(boundAltarPos, STATUS_NETWORK_MISMATCH, requestViews);
            return;
        }

        automator.requestMissingItems();
        refreshSnapshot();
    }

    public void refreshSnapshot() {
        if (!(getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }

        DungeonAltarAutomatorBlockEntity automator = boundAutomator(serverLevel);
        if (automator == null || automator.boundAltarPos() == null) {
            updateState(null, STATUS_NO_LINK, List.of());
            return;
        }

        BlockPos newBoundAltarPos = automator.boundAltarPos();
        List<DungeonAltarAutomatorBlockEntity.RequestView> newRequestViews = List.copyOf(automator.requestViews());
        int newStatus;
        if (!ae2Online()) {
            newStatus = STATUS_AE2_OFFLINE;
        } else if (!automator.ae2Online()) {
            newStatus = STATUS_AUTOMATOR_OFFLINE;
        } else if (grid() != automator.grid()) {
            newStatus = STATUS_NETWORK_MISMATCH;
        } else if (automator.statusForDisplay() == DungeonAltarAutomatorBlockEntity.STATUS_REQUESTING) {
            newStatus = STATUS_REQUESTING;
        } else {
            newStatus = STATUS_READY;
        }
        updateState(newBoundAltarPos, newStatus, newRequestViews);
    }

    @Override
    public void readFromNBT(CompoundTag data, HolderLookup.Provider registries) {
        super.readFromNBT(data, registries);
        ownerId = data.hasUUID("ownerId") ? data.getUUID("ownerId") : null;
        boundAutomatorPos = data.contains("boundAutomator") ? BlockPos.of(data.getLong("boundAutomator")) : null;
        boundAltarPos = data.contains("boundAltar") ? BlockPos.of(data.getLong("boundAltar")) : null;
        status = data.getInt("status");
        requestViews = readViews(data.getList("requestViews", Tag.TAG_COMPOUND));
    }

    @Override
    public void writeToNBT(CompoundTag data, HolderLookup.Provider registries) {
        super.writeToNBT(data, registries);
        if (ownerId != null) {
            data.putUUID("ownerId", ownerId);
        }
        if (boundAutomatorPos != null) {
            data.putLong("boundAutomator", boundAutomatorPos.asLong());
        }
        if (boundAltarPos != null) {
            data.putLong("boundAltar", boundAltarPos.asLong());
        }
        data.putInt("status", status);
        data.put("requestViews", writeViewsToTag());
    }

    @Override
    public void writeToStream(RegistryFriendlyByteBuf data) {
        super.writeToStream(data);
        writeNullablePos(data, boundAutomatorPos);
        writeNullablePos(data, boundAltarPos);
        data.writeVarInt(status);
        data.writeVarInt(requestViews.size());
        for (DungeonAltarAutomatorBlockEntity.RequestView view : requestViews) {
            if (view.requirement() != null) {
                data.writeUUID(view.requirement().id());
            } else {
                data.writeUUID(java.util.UUID.randomUUID());
            }
            data.writeResourceLocation(view.itemId());
            data.writeVarInt(view.submitted());
            data.writeVarInt(view.required());
            data.writeVarInt(view.buffered());
            data.writeVarLong(view.meAvailable());
            data.writeBoolean(view.craftable());
            data.writeBoolean(view.requesting());
        }
    }

    @Override
    public boolean readFromStream(RegistryFriendlyByteBuf data) {
        super.readFromStream(data);
        boundAutomatorPos = readNullablePos(data);
        boundAltarPos = readNullablePos(data);
        status = data.readVarInt();
        int size = data.readVarInt();
        List<DungeonAltarAutomatorBlockEntity.RequestView> views = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            UUID reqId = data.readUUID();
            ResourceLocation itemId = data.readResourceLocation();
            int submitted = data.readVarInt();
            int required = data.readVarInt();
            int buffered = data.readVarInt();
            long meAvailable = data.readVarLong();
            boolean craftable = data.readBoolean();
            boolean requesting = data.readBoolean();
            RoguelikeSavedData.AltarRequirement req =
                new RoguelikeSavedData.AltarRequirement(reqId, itemId, required, submitted);
            views.add(new DungeonAltarAutomatorBlockEntity.RequestView(
                    req,
                    itemId,
                    submitted,
                    required,
                    buffered,
                    meAvailable,
                    craftable,
                    requesting
            ));
        }
        requestViews = List.copyOf(views);
        return true;
    }

    @Override
    public TickingRequest getTickingRequest(IGridNode node) {
        return TICKING_REQUEST;
    }

    @Override
    public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {
        if (!isClientSide()) {
            refreshSnapshot();
        }
        return status == STATUS_REQUESTING ? TickRateModulation.FASTER : TickRateModulation.SLOWER;
    }

    @Override
    public IPartModel getStaticModels() {
        return selectModel(MODELS_OFF, MODELS_ON, MODELS_HAS_CHANNEL);
    }

    public static @Nullable MeCrystalAutomationTerminalPart resolve(Level level, BlockPos hostPos, net.minecraft.core.Direction side) {
        if (level == null) {
            return null;
        }
        if (appeng.api.parts.PartHelper.getPart(level, hostPos, side) instanceof MeCrystalAutomationTerminalPart terminal) {
            return terminal;
        }
        return null;
    }

    public static Component statusText(int status) {
        return switch (status) {
            case STATUS_AE2_OFFLINE -> Component.translatable("screen.incore.me_crystal_automation_terminal.status.ae2_offline");
            case STATUS_AUTOMATOR_OFFLINE -> Component.translatable("screen.incore.me_crystal_automation_terminal.status.automator_offline");
            case STATUS_NETWORK_MISMATCH -> Component.translatable("screen.incore.me_crystal_automation_terminal.status.network_mismatch");
            case STATUS_REQUESTING -> Component.translatable("screen.incore.me_crystal_automation_terminal.status.requesting");
            case STATUS_READY -> Component.translatable("screen.incore.me_crystal_automation_terminal.status.ready");
            default -> Component.translatable("screen.incore.me_crystal_automation_terminal.status.no_link");
        };
    }

    private @Nullable DungeonAltarAutomatorBlockEntity boundAutomator(ServerLevel level) {
        if (boundAutomatorPos == null) {
            return null;
        }
        return level.getBlockEntity(boundAutomatorPos) instanceof DungeonAltarAutomatorBlockEntity automator ? automator : null;
    }

    private ListTag writeViewsToTag() {
        ListTag viewsTag = new ListTag();
        for (DungeonAltarAutomatorBlockEntity.RequestView view : requestViews) {
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
        return viewsTag;
    }

    private List<DungeonAltarAutomatorBlockEntity.RequestView> readViews(ListTag viewsTag) {
        List<DungeonAltarAutomatorBlockEntity.RequestView> views = new ArrayList<>();
        for (Tag viewTag : viewsTag) {
            CompoundTag row = (CompoundTag) viewTag;
            ResourceLocation itemId = ResourceLocation.tryParse(row.getString("item"));
            if (itemId == null) {
                continue;
            }
            RoguelikeSavedData.AltarRequirement requirement = row.hasUUID("reqId")
                ? new RoguelikeSavedData.AltarRequirement(row.getUUID("reqId"), itemId, row.getInt("required"), row.getInt("submitted"))
                : null;
            views.add(new DungeonAltarAutomatorBlockEntity.RequestView(
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

    private void markDirty() {
        if (getHost() != null) {
            getHost().markForSave();
            getHost().markForUpdate();
        }
    }

    private void updateState(@Nullable BlockPos newBoundAltarPos, int newStatus, List<DungeonAltarAutomatorBlockEntity.RequestView> newRequestViews) {
        if (Objects.equals(boundAltarPos, newBoundAltarPos) && status == newStatus && Objects.equals(requestViews, newRequestViews)) {
            return;
        }
        boundAltarPos = newBoundAltarPos;
        status = newStatus;
        requestViews = List.copyOf(newRequestViews);
        markDirty();
    }

    private static void writeNullablePos(RegistryFriendlyByteBuf data, @Nullable BlockPos pos) {
        data.writeBoolean(pos != null);
        if (pos != null) {
            data.writeBlockPos(pos);
        }
    }

    private static @Nullable BlockPos readNullablePos(RegistryFriendlyByteBuf data) {
        return data.readBoolean() ? data.readBlockPos() : null;
    }

    private @NotNull AbstractContainerMenu createMenu(int containerId, @NotNull Inventory inventory, @NotNull Player player) {
        return new MeCrystalAutomationTerminalMenu(containerId, inventory, hostPos(), getSide(), this);
    }

    public enum BindResult {
        SUCCESS,
        INVALID_CARD,
        WRONG_OWNER,
        TARGET_MISSING,
        INVALID_TARGET
    }
}
