package ozokuz.incore.features.roguelike.content;

import ozokuz.incore.Registration;
import ozokuz.incore.features.roguelike.RoguelikeService;
import ozokuz.incore.features.roguelike.data.DungeonModifierManager;
import ozokuz.incore.features.roguelike.data.DungeonObjectiveManager;
import ozokuz.incore.features.roguelike.data.DungeonThemeManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class DungeonCrystalModificationStationBlockEntity extends BlockEntity implements MenuProvider, Container {
    public static final int INPUT_SLOT = 0;
    public static final int THEME_SLOT = 1;
    public static final int OBJECTIVE_SLOT = 2;
    public static final int MODIFIER_START = 3;
    public static final int MODIFIER_COUNT = 6;
    public static final int OUTPUT_SLOT = MODIFIER_START + MODIFIER_COUNT;
    public static final int SLOT_COUNT = OUTPUT_SLOT + 1;

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private boolean validPreview;
    private boolean updatingOutput;

    public DungeonCrystalModificationStationBlockEntity(BlockPos pos, BlockState blockState) {
        super(Registration.DUNGEON_CRYSTAL_MODIFICATION_STATION_BE.get(), pos, blockState);
    }

    public boolean validPreview() {
        return validPreview;
    }

    public List<ItemStack> inputContents() {
        List<ItemStack> drops = new ArrayList<>();
        for (int slot = 0; slot < OUTPUT_SLOT; slot++) {
            ItemStack stack = items.get(slot);
            if (!stack.isEmpty()) {
                drops.add(stack.copy());
            }
        }
        return List.copyOf(drops);
    }

    public void refreshPreview() {
        ItemStack input = items.get(INPUT_SLOT);
        ResourceLocation themeId = parseSelectionId(items.get(THEME_SLOT));
        ResourceLocation objectiveId = parseSelectionId(items.get(OBJECTIVE_SLOT));
        List<ResourceLocation> modifiers = parseModifierSelection();

        boolean valid = input.is(Registration.EMPTY_DUNGEON_CRYSTAL_ITEM.get())
                && themeId != null
                && objectiveId != null
                && DungeonThemeManager.THEMES.containsKey(themeId)
                && DungeonObjectiveManager.OBJECTIVES.containsKey(objectiveId);

        validPreview = valid;
        if (!valid) {
            setOutputInternal(ItemStack.EMPTY);
            setChanged();
            return;
        }

        ItemStack output = RoguelikeService.createDungeonCrystal(1, themeId, objectiveId, modifiers, true);
        setOutputInternal(output);
        setChanged();
    }

    public void consumeInputAfterOutputTaken() {
        if (!validPreview) {
            return;
        }

        ItemStack input = items.get(INPUT_SLOT);
        if (!input.isEmpty()) {
            input.shrink(1);
            if (input.isEmpty()) {
                items.set(INPUT_SLOT, ItemStack.EMPTY);
            }
        }
        refreshPreview();
    }

    @Nullable
    private static ResourceLocation parseSelectionId(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }

        String raw = stack.getHoverName().getString().trim().toLowerCase(Locale.ROOT);
        if (raw.isBlank()) {
            return null;
        }

        ResourceLocation parsed = ResourceLocation.tryParse(raw);
        if (parsed != null) {
            return parsed;
        }

        if (!raw.contains(":")) {
            return ResourceLocation.fromNamespaceAndPath("incore", raw);
        }
        return null;
    }

    private List<ResourceLocation> parseModifierSelection() {
        Set<ResourceLocation> selected = new LinkedHashSet<>();
        for (int i = 0; i < MODIFIER_COUNT; i++) {
            ResourceLocation id = parseSelectionId(items.get(MODIFIER_START + i));
            if (id != null && DungeonModifierManager.MODIFIERS.containsKey(id)) {
                selected.add(id);
            }
        }
        return List.copyOf(selected);
    }

    private void setOutputInternal(ItemStack stack) {
        updatingOutput = true;
        items.set(OUTPUT_SLOT, stack);
        updatingOutput = false;
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.loadAdditional(tag, registries);
        clearContent();

        if (tag.contains("items", Tag.TAG_LIST)) {
            ListTag list = tag.getList("items", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag row = list.getCompound(i);
                int slot = row.getInt("slot");
                if (slot < 0 || slot >= OUTPUT_SLOT) {
                    continue;
                }
                items.set(slot, ItemStack.parseOptional(registries, row.getCompound("stack")));
            }
        }
        refreshPreview();
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.saveAdditional(tag, registries);
        ListTag list = new ListTag();
        for (int slot = 0; slot < OUTPUT_SLOT; slot++) {
            ItemStack stack = items.get(slot);
            if (stack.isEmpty()) {
                continue;
            }

            CompoundTag row = new CompoundTag();
            row.putInt("slot", slot);
            row.put("stack", stack.save(registries));
            list.add(row);
        }
        tag.put("items", list);
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("block.incore.dungeon_crystal_modification_station");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, @NotNull Inventory inventory, @NotNull Player player) {
        refreshPreview();
        return new DungeonCrystalModificationStationMenu(containerId, inventory, this);
    }

    @Override
    public int getContainerSize() {
        return SLOT_COUNT;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public @NotNull ItemStack getItem(int slot) {
        if (slot < 0 || slot >= items.size()) {
            return ItemStack.EMPTY;
        }
        return items.get(slot);
    }

    @Override
    public @NotNull ItemStack removeItem(int slot, int amount) {
        if (slot < 0 || slot >= items.size()) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = items.get(slot);
        ItemStack removed = stack.split(amount);
        if (stack.isEmpty()) {
            items.set(slot, ItemStack.EMPTY);
        }
        if (slot != OUTPUT_SLOT && !updatingOutput) {
            refreshPreview();
        }
        return removed;
    }

    @Override
    public @NotNull ItemStack removeItemNoUpdate(int slot) {
        if (slot < 0 || slot >= items.size()) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = items.get(slot);
        items.set(slot, ItemStack.EMPTY);
        if (slot != OUTPUT_SLOT && !updatingOutput) {
            refreshPreview();
        }
        return stack;
    }

    @Override
    public void setItem(int slot, @NotNull ItemStack stack) {
        if (slot < 0 || slot >= items.size()) {
            return;
        }

        ItemStack next = stack.copy();
        int maxStack = getMaxStackSize(next);
        if (!next.isEmpty() && next.getCount() > maxStack) {
            next.setCount(maxStack);
        }
        items.set(slot, next);
        if (slot != OUTPUT_SLOT && !updatingOutput) {
            refreshPreview();
        } else {
            setChanged();
        }
    }

    @Override
    public boolean canPlaceItem(int slot, @NotNull ItemStack stack) {
        if (slot == OUTPUT_SLOT) {
            return false;
        }
        return true;
    }

    @Override
    public void setChanged() {
        super.setChanged();
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        if (level == null) {
            return false;
        }
        return player.distanceToSqr(worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D, worldPosition.getZ() + 0.5D) <= 64.0D;
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < items.size(); i++) {
            items.set(i, ItemStack.EMPTY);
        }
    }
}
