package io.github.ozokuz.incore.features.surfaceore;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CompassItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.neoforged.fml.loading.FMLEnvironment;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class SurfaceStoneDebugCompassItem extends CompassItem {
    private static final String PLAYER_DATA_ROOT = "incore_surface_stone_debug";
    private static final String PLAYER_DATA_FOUND_PATCHES = "found_patch_chunks";

    public SurfaceStoneDebugCompassItem(Item.Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);
        if (FMLEnvironment.production) {
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.displayClientMessage(Component.translatable("incore.surface_stone.debug_compass.dev_only"), true);
            }
            return InteractionResultHolder.fail(stack);
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }

        SurfaceStonePatchSavedData savedData = SurfaceStonePatchSavedData.get(serverPlayer.serverLevel());
        Set<Long> foundPatches = readFoundPatchChunks(serverPlayer);
        Optional<SurfaceStonePatchSavedData.PatchTarget> nearest = savedData.findNearestUnfound(serverPlayer.blockPosition(), foundPatches);
        if (nearest.isEmpty()) {
            serverPlayer.displayClientMessage(Component.translatable("incore.surface_stone.debug_compass.none_left"), true);
            return InteractionResultHolder.fail(stack);
        }

        SurfaceStonePatchSavedData.PatchTarget target = nearest.get();
        markFoundPatch(serverPlayer, target.chunkKey());
        stack.set(
                DataComponents.LODESTONE_TRACKER,
                new LodestoneTracker(Optional.of(GlobalPos.of(level.dimension(), target.pos())), false)
        );
        serverPlayer.setItemInHand(usedHand, stack);
        serverPlayer.getInventory().setChanged();
        serverPlayer.containerMenu.broadcastChanges();

        BlockPos pos = target.pos();
        ChunkPos chunkPos = new ChunkPos(pos);
        level.playSound(null, serverPlayer.blockPosition(), SoundEvents.LODESTONE_COMPASS_LOCK, SoundSource.PLAYERS, 1.0F, 1.0F);
        serverPlayer.displayClientMessage(
                Component.translatable("incore.surface_stone.debug_compass.locked", pos.getX(), pos.getY(), pos.getZ(), chunkPos.x, chunkPos.z),
                false
        );
        return InteractionResultHolder.success(stack);
    }

    private static Set<Long> readFoundPatchChunks(ServerPlayer player) {
        CompoundTag root = player.getPersistentData().getCompound(PLAYER_DATA_ROOT);
        long[] values = root.getLongArray(PLAYER_DATA_FOUND_PATCHES);
        Set<Long> found = new HashSet<>();
        for (long value : values) {
            found.add(value);
        }
        return found;
    }

    private static void markFoundPatch(ServerPlayer player, long chunkKey) {
        CompoundTag persistent = player.getPersistentData();
        CompoundTag root = persistent.getCompound(PLAYER_DATA_ROOT);
        Set<Long> found = new HashSet<>();
        for (long value : root.getLongArray(PLAYER_DATA_FOUND_PATCHES)) {
            found.add(value);
        }
        if (found.add(chunkKey)) {
            long[] packed = new long[found.size()];
            int i = 0;
            for (long value : found) {
                packed[i++] = value;
            }
            root.putLongArray(PLAYER_DATA_FOUND_PATCHES, packed);
            persistent.put(PLAYER_DATA_ROOT, root);
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        LodestoneTracker tracker = stack.get(DataComponents.LODESTONE_TRACKER);
        if (tracker != null && tracker.target().isPresent()) {
            return;
        }
        super.inventoryTick(stack, level, entity, slotId, isSelected);
    }
}
