package io.github.ozokuz.incore.features.assembly.content;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

public interface AutoAssemblerBlockEntity {
    ItemStackHandler itemHandler();

    @Nullable IItemHandler automationView(@Nullable Direction side);

    boolean canAccess(Player player);

    void setSelectedRecipeId(@Nullable ResourceLocation recipeId, @Nullable Player player);

    @Nullable ResourceLocation selectedRecipeId();

    int machineTier();

    ContainerData data();

    net.minecraft.core.BlockPos getBlockPos();

    @Nullable Level getLevel();
}
