package io.github.ozokuz.incore.features.assembly.content;

import io.github.ozokuz.incore.Registration;
import io.github.ozokuz.incore.features.assembly.recipe.AssemblyRecipe;
import io.github.ozokuz.incore.features.researchv2.team.ResearchTeamResolver;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class AssemblyStationBlockEntity extends BlockEntity implements MenuProvider {
    public static final int INPUT_START = 0;
    public static final int INPUT_COUNT = 9;
    public static final int OUTPUT_SLOT = 9;
    public static final int SLOT_COUNT = 10;

    private final ItemStackHandler items = new ItemStackHandler(SLOT_COUNT) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return slot >= 0 && slot < SLOT_COUNT;
        }
    };

    public AssemblyStationBlockEntity(BlockPos pos, BlockState state) {
        super(Registration.ASSEMBLY_STATION_BE.get(), pos, state);
    }

    public ItemStackHandler itemHandler() {
        return items;
    }

    public boolean canAccess(Player player) {
        return level != null
                && level.getBlockEntity(worldPosition) == this
                && player.distanceToSqr(worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D, worldPosition.getZ() + 0.5D) <= 64.0D;
    }

    public boolean tryCraft(Player player, @Nullable net.minecraft.resources.ResourceLocation recipeId) {
        if (level == null || level.isClientSide || recipeId == null || player.getServer() == null) {
            return false;
        }
        if (!(player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)) {
            return false;
        }
        String teamId = ResearchTeamResolver.resolveTeamId(serverPlayer);
        if (teamId == null || teamId.isBlank()) {
            return false;
        }
        var holder = AssemblyRecipeUtil.findRecipeHolder(level.getRecipeManager(), recipeId);
        if (holder == null || !AssemblyRecipeUtil.isUnlocked(player.getServer(), teamId, recipeId)) {
            return false;
        }
        AssemblyRecipe recipe = holder.value();
        var input = AssemblyRecipeUtil.craftingInput(items, INPUT_START);
        if (!recipe.matches(input, level)) {
            return false;
        }
        List<Integer> consumedSlots = AssemblyRecipeUtil.consumedSlots(recipe, items, INPUT_START);
        if (consumedSlots.isEmpty()) {
            return false;
        }
        ItemStack result = recipe.getResultItem(level.registryAccess());
        if (!AssemblyRecipeUtil.canFitOutputs(items, OUTPUT_SLOT, 1, List.of(result))) {
            return false;
        }
        AssemblyRecipeUtil.consumeSlots(items, consumedSlots);
        AssemblyRecipeUtil.insertOutputs(items, OUTPUT_SLOT, 1, List.of(result));
        setChanged();
        return true;
    }

    public void dropContents() {
        if (level == null) {
            return;
        }
        for (int slot = 0; slot < items.getSlots(); slot++) {
            ItemStack stack = items.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                net.minecraft.world.Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), stack.copy());
            }
        }
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("items", items.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.loadAdditional(tag, registries);
        items.deserializeNBT(registries, tag.getCompound("items"));
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("block.incore.assembly_station");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, @NotNull Inventory inventory, @NotNull Player player) {
        return new AssemblyStationMenu(containerId, inventory, this);
    }
}
