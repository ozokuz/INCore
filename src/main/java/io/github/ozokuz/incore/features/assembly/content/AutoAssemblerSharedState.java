package io.github.ozokuz.incore.features.assembly.content;

import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class AutoAssemblerSharedState {
    public static final int INPUT_START = 0;
    public static final int INPUT_COUNT = 9;
    public static final int OUTPUT_START = 9;
    public static final int OUTPUT_COUNT = 4;
    public static final int SLOT_COUNT = 13;

    public static final int STATUS_IDLE = 0;
    public static final int STATUS_NO_RECIPE = 1;
    public static final int STATUS_LOCKED = 2;
    public static final int STATUS_TIER_BLOCKED = 3;
    public static final int STATUS_NO_INPUT = 4;
    public static final int STATUS_OUTPUT_FULL = 5;
    public static final int STATUS_NO_POWER = 6;

    private final ItemStackHandler items = new ItemStackHandler(SLOT_COUNT) {
        @Override
        protected void onContentsChanged(int slot) {
            if (onDirty != null) {
                onDirty.run();
            }
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return slot >= 0 && slot < SLOT_COUNT;
        }
    };
    private final IItemHandler frontExtractView = new IItemHandler() {
        @Override
        public int getSlots() {
            return OUTPUT_COUNT;
        }

        @Override
        public @NotNull ItemStack getStackInSlot(int slot) {
            return items.getStackInSlot(OUTPUT_START + slot);
        }

        @Override
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            return stack;
        }

        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            return items.extractItem(OUTPUT_START + slot, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return items.getSlotLimit(OUTPUT_START + slot);
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return false;
        }
    };
    private final IItemHandler sideInsertView = new IItemHandler() {
        @Override
        public int getSlots() {
            return INPUT_COUNT;
        }

        @Override
        public @NotNull ItemStack getStackInSlot(int slot) {
            return items.getStackInSlot(slot);
        }

        @Override
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            return items.insertItem(slot, stack, simulate);
        }

        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return items.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return slot >= 0 && slot < INPUT_COUNT;
        }
    };
    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> progressTicks;
                case 1 -> maxProgressTicks;
                case 2 -> status;
                case 3 -> machineTier;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (index == 0) {
                progressTicks = Math.max(0, value);
            } else if (index == 1) {
                maxProgressTicks = Math.max(1, value);
            } else if (index == 2) {
                status = value;
            } else if (index == 3) {
                machineTier = value;
            }
        }

        @Override
        public int getCount() {
            return 4;
        }
    };

    private @Nullable Runnable onDirty;
    private @Nullable ResourceLocation selectedRecipeId;
    private @Nullable String teamId;
    private int progressTicks;
    private int maxProgressTicks = 100;
    private int status = STATUS_NO_RECIPE;
    private int machineTier = 1;
    private int attempts;
    private int successes;
    private int tier1Failures;
    private int tier2Failures;
    private int leftoverEmits;

    public void setOnDirty(@Nullable Runnable onDirty) {
        this.onDirty = onDirty;
    }

    public ItemStackHandler items() {
        return items;
    }

    public ContainerData data() {
        return data;
    }

    public @Nullable IItemHandler automationView(Direction front, @Nullable Direction side) {
        if (side == null) {
            return items;
        }
        return side == front ? frontExtractView : sideInsertView;
    }

    public @Nullable ResourceLocation selectedRecipeId() {
        return selectedRecipeId;
    }

    public void setSelectedRecipeId(@Nullable ResourceLocation selectedRecipeId) {
        this.selectedRecipeId = selectedRecipeId;
        if (onDirty != null) {
            onDirty.run();
        }
    }

    public @Nullable String teamId() {
        return teamId;
    }

    public void setTeamId(@Nullable String teamId) {
        this.teamId = teamId == null || teamId.isBlank() ? null : teamId;
        if (onDirty != null) {
            onDirty.run();
        }
    }

    public int progressTicks() {
        return progressTicks;
    }

    public void setProgressTicks(int progressTicks) {
        this.progressTicks = Math.max(0, progressTicks);
    }

    public int maxProgressTicks() {
        return maxProgressTicks;
    }

    public void setMaxProgressTicks(int maxProgressTicks) {
        this.maxProgressTicks = Math.max(1, maxProgressTicks);
    }

    public int status() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public int machineTier() {
        return machineTier;
    }

    public void setMachineTier(int machineTier) {
        this.machineTier = machineTier;
    }

    public void incrementAttempts() {
        attempts++;
    }

    public void incrementSuccesses() {
        successes++;
    }

    public void incrementTier1Failures() {
        tier1Failures++;
    }

    public void incrementTier2Failures() {
        tier2Failures++;
    }

    public void addLeftoverEmits(int amount) {
        leftoverEmits += Math.max(0, amount);
    }

    public int attempts() {
        return attempts;
    }

    public int successes() {
        return successes;
    }

    public int tier1Failures() {
        return tier1Failures;
    }

    public int tier2Failures() {
        return tier2Failures;
    }

    public int leftoverEmits() {
        return leftoverEmits;
    }

    public void save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.put("items", items.serializeNBT(registries));
        if (selectedRecipeId != null) {
            tag.putString("selectedRecipeId", selectedRecipeId.toString());
        }
        if (teamId != null) {
            tag.putString("teamId", teamId);
        }
        tag.putInt("progressTicks", progressTicks);
        tag.putInt("maxProgressTicks", maxProgressTicks);
        tag.putInt("status", status);
        tag.putInt("machineTier", machineTier);
        tag.putInt("attempts", attempts);
        tag.putInt("successes", successes);
        tag.putInt("tier1Failures", tier1Failures);
        tag.putInt("tier2Failures", tier2Failures);
        tag.putInt("leftoverEmits", leftoverEmits);
    }

    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        items.deserializeNBT(registries, tag.getCompound("items"));
        selectedRecipeId = ResourceLocation.tryParse(tag.getString("selectedRecipeId"));
        teamId = tag.contains("teamId") ? tag.getString("teamId") : null;
        progressTicks = Math.max(0, tag.getInt("progressTicks"));
        maxProgressTicks = Math.max(1, tag.getInt("maxProgressTicks"));
        status = tag.getInt("status");
        machineTier = Math.max(1, tag.getInt("machineTier"));
        attempts = Math.max(0, tag.getInt("attempts"));
        successes = Math.max(0, tag.getInt("successes"));
        tier1Failures = Math.max(0, tag.getInt("tier1Failures"));
        tier2Failures = Math.max(0, tag.getInt("tier2Failures"));
        leftoverEmits = Math.max(0, tag.getInt("leftoverEmits"));
    }

    public void dropContents(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos) {
        for (int slot = 0; slot < items.getSlots(); slot++) {
            ItemStack stack = items.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                net.minecraft.world.Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack.copy());
            }
        }
    }
}
