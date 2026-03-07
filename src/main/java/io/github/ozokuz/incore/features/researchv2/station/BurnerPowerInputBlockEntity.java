package io.github.ozokuz.incore.features.researchv2.station;

import io.github.ozokuz.incore.Registration;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;

public class BurnerPowerInputBlockEntity extends BlockEntity implements IResearchPowerInput {
    private static final int SLOT_COUNT = 9;

    private int burnTickRemainder;
    private final ItemStackHandler items = new ItemStackHandler(SLOT_COUNT) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return isAcceptedFuel(stack);
        }
    };

    public BurnerPowerInputBlockEntity(BlockPos pos, BlockState state) {
        super(Registration.BURNER_POWER_INPUT_BE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, BurnerPowerInputBlockEntity input) {
    }

    public IItemHandler itemHandler() {
        return items;
    }

    @Override
    public int pullResearchPower(ResearchControllerBlockEntity controller, int maxRp) {
        if (maxRp <= 0) {
            return 0;
        }

        int ticksPerRp = Math.max(1, io.github.ozokuz.incore.Config.BURNER_CORE_BURN_TICKS_PER_RP.get());
        int rpLimit = Math.min(Math.max(0, maxRp), Math.max(1, io.github.ozokuz.incore.Config.BURNER_CORE_MAX_RP_PER_TICK.get()));
        if (rpLimit <= 0) {
            return 0;
        }

        int totalBurnTicks = Math.max(0, burnTickRemainder);
        int burnTicksNeeded = Math.max(0, rpLimit * ticksPerRp - totalBurnTicks);
        if (burnTicksNeeded > 0) {
            totalBurnTicks += consumeBurnTicksUpTo(burnTicksNeeded);
        }

        int produced = Math.min(rpLimit, totalBurnTicks / ticksPerRp);
        int nextRemainder = totalBurnTicks - produced * ticksPerRp;
        if (nextRemainder != burnTickRemainder) {
            burnTickRemainder = nextRemainder;
            setChanged();
        }
        return Math.max(0, produced);
    }

    @Override
    public ResearchPowerFamily family() {
        return ResearchPowerFamily.BURNER;
    }

    @Override
    public int powerTier() {
        return 1;
    }

    public int consumeBurnTicksUpTo(int burnTicksNeeded) {
        if (burnTicksNeeded <= 0) {
            return 0;
        }

        int consumed = 0;
        for (int slot = 0; slot < items.getSlots() && consumed < burnTicksNeeded; slot++) {
            ItemStack stack = items.getStackInSlot(slot);
            if (!isAcceptedFuel(stack)) {
                continue;
            }

            int fuelValue = stack.getBurnTime(null);
            if (fuelValue <= 0) {
                continue;
            }

            items.extractItem(slot, 1, false);
            consumed += fuelValue;
        }
        if (consumed > 0) {
            setChanged();
        }
        return consumed;
    }

    public void dropContents() {
        if (level == null) {
            return;
        }
        for (int slot = 0; slot < items.getSlots(); slot++) {
            ItemStack stack = items.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), stack.copy());
            }
        }
    }

    public static boolean isAcceptedFuel(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        return stack.getBurnTime(null) > 0
                && BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace().equals("minecraft")
                && !stack.is(ItemTags.NON_FLAMMABLE_WOOD);
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("items")) {
            items.deserializeNBT(registries, tag.getCompound("items"));
        }
        burnTickRemainder = Math.max(0, tag.getInt("burnTickRemainder"));
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("items", items.serializeNBT(registries));
        tag.putInt("burnTickRemainder", Math.max(0, burnTickRemainder));
    }
}
