package ozokuz.incore.features.surfaceore;

import ozokuz.incore.features.surfaceore.network.SurfaceOreNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class SurfaceOreTypeLocatorItem extends Item {
    private static final String PLAYER_DATA_ROOT = "incore_surface_ore_type_locator";
    private static final String PLAYER_DATA_FOUND_PATCHES = "found_patch_keys";

    private final String marker;
    private final int color;

    public SurfaceOreTypeLocatorItem(Item.Properties properties, String marker, int color) {
        super(properties.stacksTo(1));
        this.marker = marker;
        this.color = color;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.sidedSuccess(player.getItemInHand(usedHand), level.isClientSide());
        }

        ItemStack stack = player.getItemInHand(usedHand);
        SurfaceOrePatchSavedData savedData = SurfaceOrePatchSavedData.get(serverPlayer.serverLevel());
        Set<String> foundPatches = readFoundPatchKeys(serverPlayer);
        Optional<SurfaceOrePatchSavedData.PatchTarget> nearest = savedData.findNearestUnfound(
                serverPlayer.blockPosition(),
                level.dimension(),
                foundPatches
        );

        if (nearest.isEmpty()) {
            serverPlayer.displayClientMessage(Component.translatable("incore.surface_ore.locator.none_left", "any ore"), true);
            return InteractionResultHolder.fail(stack);
        }

        SurfaceOrePatchSavedData.PatchTarget target = nearest.get();
        markFoundPatch(serverPlayer, target.key());

        BlockPos pos = target.pos();
        String waypointName = "Ore";

        SurfaceOreNetworking.sendWaypointToPlayer(serverPlayer, waypointName, marker, pos.asLong());

        stack.shrink(1);
        level.playSound(null, serverPlayer.blockPosition(), SoundEvents.LODESTONE_COMPASS_LOCK, SoundSource.PLAYERS, 1.0F, 1.0F);
        serverPlayer.displayClientMessage(
                Component.translatable("incore.surface_ore.locator.waypoint_added", waypointName, pos.getX(), pos.getY(), pos.getZ()),
                false
        );

        return InteractionResultHolder.success(stack);
    }

    private static String getDimensionString(net.minecraft.resources.ResourceKey<Level> dimension) {
        return "Internal-" + dimension.location().getPath() + "-waypoints";
    }

    private static Set<String> readFoundPatchKeys(ServerPlayer player) {
        CompoundTag root = player.getPersistentData().getCompound(PLAYER_DATA_ROOT);
        ListTag list = root.getList(PLAYER_DATA_FOUND_PATCHES, Tag.TAG_STRING);
        Set<String> found = new HashSet<>();
        for (Tag tag : list) {
            found.add(tag.getAsString());
        }
        return found;
    }

    private static void markFoundPatch(ServerPlayer player, String key) {
        CompoundTag persistent = player.getPersistentData();
        CompoundTag root = persistent.getCompound(PLAYER_DATA_ROOT);
        Set<String> found = new HashSet<>();
        ListTag list = root.getList(PLAYER_DATA_FOUND_PATCHES, Tag.TAG_STRING);
        for (Tag tag : list) {
            found.add(tag.getAsString());
        }
        if (found.add(key)) {
            ListTag newList = new ListTag();
            for (String foundKey : found) {
                newList.add(StringTag.valueOf(foundKey));
            }
            root.put(PLAYER_DATA_FOUND_PATCHES, newList);
            persistent.put(PLAYER_DATA_ROOT, root);
        }
    }
}
