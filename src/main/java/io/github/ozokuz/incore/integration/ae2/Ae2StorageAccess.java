package io.github.ozokuz.incore.integration.ae2;

import appeng.api.config.Actionable;
import appeng.api.networking.IGrid;
import appeng.api.networking.crafting.CalculationStrategy;
import appeng.api.networking.crafting.ICraftingLink;
import appeng.api.networking.crafting.ICraftingPlan;
import appeng.api.networking.crafting.ICraftingRequester;
import appeng.api.networking.crafting.ICraftingSimulationRequester;
import appeng.api.networking.crafting.ICraftingSubmitResult;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.storage.MEStorage;
import appeng.api.storage.StorageHelper;
import io.github.ozokuz.incore.INCore;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public final class Ae2StorageAccess {
    private Ae2StorageAccess() {
    }

    public static long count(@Nullable IGrid grid, ItemStack stack) {
        if (grid == null || stack.isEmpty()) {
            return 0L;
        }

        AEItemKey key = AEItemKey.of(stack);
        if (key == null) {
            return 0L;
        }
        return grid.getStorageService().getInventory().getAvailableStacks().get(key);
    }

    public static long count(@Nullable IGrid grid, Item item) {
        return count(grid, new ItemStack(item));
    }

    public static long count(@Nullable IGrid grid, ResourceLocation ignored, ItemStack stack) {
        return count(grid, stack);
    }

    public static boolean isCraftable(@Nullable IGrid grid, ItemStack stack) {
        if (grid == null || stack.isEmpty()) {
            return false;
        }
        AEItemKey key = AEItemKey.of(stack);
        return key != null && grid.getCraftingService().isCraftable(key);
    }

    public static boolean isRequesting(@Nullable IGrid grid, ItemStack stack) {
        if (grid == null || stack.isEmpty()) {
            return false;
        }
        AEItemKey key = AEItemKey.of(stack);
        return key != null && grid.getCraftingService().isRequesting(key);
    }

    public static InsertResult insert(@Nullable IGrid grid, @Nullable IActionSource actionSource, ItemStack stack) {
        if (grid == null || actionSource == null || stack.isEmpty()) {
            return new InsertResult(stack.copy(), 0L);
        }

        MEStorage storage = grid.getStorageService().getInventory();
        AEItemKey key = AEItemKey.of(stack);
        if (key == null) {
            return new InsertResult(stack.copy(), 0L);
        }

        long inserted = StorageHelper.poweredInsert(grid.getEnergyService(), storage, key, stack.getCount(), actionSource);
        ItemStack remainder = stack.copy();
        remainder.shrink((int) Math.min(Integer.MAX_VALUE, inserted));
        return new InsertResult(remainder, inserted);
    }

    public static long extract(@Nullable IGrid grid, @Nullable IActionSource actionSource, ItemStack stack, int amount) {
        if (grid == null || actionSource == null || stack.isEmpty() || amount <= 0) {
            return 0L;
        }

        MEStorage storage = grid.getStorageService().getInventory();
        AEItemKey key = AEItemKey.of(stack);
        if (key == null) {
            return 0L;
        }

        return StorageHelper.poweredExtraction(grid.getEnergyService(), storage, key, amount, actionSource, Actionable.MODULATE);
    }

    public static @Nullable ICraftingLink loadCraftingLink(CompoundTag tag, ICraftingRequester requester) {
        return StorageHelper.loadCraftingLink(tag, requester);
    }

    public static @Nullable ICraftingLink requestAutocrafting(
            @Nullable IGrid grid,
            @Nullable Level level,
            @Nullable IActionSource actionSource,
            @Nullable ICraftingRequester requester,
            ItemStack stack
    ) {
        if (grid == null || level == null || actionSource == null || requester == null || stack.isEmpty()) {
            return null;
        }

        AEItemKey key = AEItemKey.of(stack);
        if (key == null) {
            return null;
        }

        try {
            Future<ICraftingPlan> planFuture = grid.getCraftingService().beginCraftingCalculation(
                    level,
                    new SimpleSimulationRequester(actionSource),
                    key,
                    stack.getCount(),
                    CalculationStrategy.REPORT_MISSING_ITEMS
            );
            ICraftingPlan plan = planFuture.get();
            if (plan == null) {
                return null;
            }
            ICraftingSubmitResult submit = grid.getCraftingService().submitJob(plan, requester, null, false, actionSource);
            return submit.successful() ? submit.link() : null;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return null;
        } catch (ExecutionException exception) {
            INCore.LOGGER.warn("Failed to submit AE2 crafting job", exception);
            return null;
        }
    }

    public record InsertResult(ItemStack remainder, long inserted) {
    }

    private record SimpleSimulationRequester(IActionSource actionSource) implements ICraftingSimulationRequester {
        @Override
        public IActionSource getActionSource() {
            return actionSource;
        }
    }
}
