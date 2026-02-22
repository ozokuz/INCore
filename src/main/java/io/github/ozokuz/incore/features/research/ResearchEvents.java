package io.github.ozokuz.incore.features.research;

import com.simibubi.create.content.equipment.blueprint.BlueprintEntity;
import io.github.ozokuz.incore.INCore;
import io.github.ozokuz.incore.features.research.network.ResearchNetworking;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = INCore.MODID)
public class ResearchEvents {
    private static final long LOCKED_CRAFT_MESSAGE_INTERVAL_TICKS = 20L;
    private static final ResourceLocation CREATE_WRENCH_ID = ResourceLocation.fromNamespaceAndPath("create", "wrench");
    private static final ResourceLocation CREATE_MECHANICAL_CRAFTER_ID = ResourceLocation.fromNamespaceAndPath("create", "mechanical_crafter");
    private static final Map<UUID, Long> nextLockedCraftMessageTick = new HashMap<>();

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!(event.getEntity() instanceof ServerPlayer newPlayer)) {
            return;
        }

        if (!(event.getOriginal() instanceof ServerPlayer oldPlayer)) {
            return;
        }

        ResearchProgressService.copyData(oldPlayer, newPlayer);
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            nextLockedCraftMessageTick.remove(player.getUUID());
            ResearchNetworking.syncLockState(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        enforceLockedCrafting(player);

        if (player.serverLevel().getGameTime() % 20L != 0L) {
            return;
        }

        boolean changed = ResearchProgressService.tickResearch(player);
        if (changed) {
            ResearchNetworking.openFor(player);
        } else {
            ResearchNetworking.syncLockState(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            nextLockedCraftMessageTick.remove(player.getUUID());
        }
    }

    @SubscribeEvent
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        ItemStack crafted = event.getCrafting();
        if (crafted.isEmpty()) {
            return;
        }
        if (!ResearchRecipeLockService.isOutputLocked(player, crafted)) {
            return;
        }

        notifyLockedCraft(player);
        ResearchNetworking.syncLockState(player);
    }

    private static void enforceLockedCrafting(ServerPlayer player) {
        AbstractContainerMenu menu = player.containerMenu;
        if (menu.slots.isEmpty()) {
            return;
        }

        boolean changed = false;
        for (Slot slot : menu.slots) {
            if (!isCraftingResultSlot(menu, slot)) {
                continue;
            }

            ItemStack result = slot.getItem();
            if (result.isEmpty() || !ResearchRecipeLockService.isOutputLocked(player, result)) {
                continue;
            }

            slot.set(ItemStack.EMPTY);
            changed = true;
        }

        if (!changed) {
            return;
        }

        menu.broadcastChanges();
        notifyLockedCraft(player);
    }

    @SubscribeEvent
    public static void onBlueprintCraftAttempt(PlayerInteractEvent.EntityInteractSpecific event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!(event.getTarget() instanceof BlueprintEntity blueprint)) {
            return;
        }
        if (isCreateWrench(player.getItemInHand(event.getHand()))) {
            return;
        }

        ItemStack result = resolveBlueprintOutput(blueprint, event.getLocalPos());
        if (result.isEmpty() || !ResearchRecipeLockService.isOutputLocked(player, result)) {
            return;
        }

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        notifyLockedCraft(player);
        ResearchNetworking.syncLockState(player);
    }

    @SubscribeEvent
    public static void onMechanicalCrafterPlaced(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!(event.getLevel() instanceof net.minecraft.server.level.ServerLevel level) || !isMechanicalCrafter(event.getPlacedBlock())) {
            return;
        }
        claimMechanicalCrafterPlacement(level, event.getPos(), player);
    }

    @SubscribeEvent
    public static void onMechanicalCrafterInteract(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!(event.getLevel() instanceof net.minecraft.server.level.ServerLevel level)) {
            return;
        }

        if (!isMechanicalCrafter(level.getBlockState(event.getPos()))) {
            return;
        }
        claimMechanicalCrafterUser(level, event.getPos(), player);
    }

    private static void claimMechanicalCrafterPlacement(net.minecraft.server.level.ServerLevel level, net.minecraft.core.BlockPos pos, ServerPlayer player) {
        var blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof MechanicalCrafterOwnershipAccess ownership)) {
            return;
        }
        ownership.incore$setOwnerIfAbsent(player);
        blockEntity.setChanged();
    }

    private static void claimMechanicalCrafterUser(net.minecraft.server.level.ServerLevel level, net.minecraft.core.BlockPos pos, ServerPlayer player) {
        var blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof MechanicalCrafterOwnershipAccess ownership)) {
            return;
        }
        ownership.incore$setOwnerIfAbsent(player);
        blockEntity.setChanged();
    }

    private static boolean isMechanicalCrafter(BlockState state) {
        return CREATE_MECHANICAL_CRAFTER_ID.equals(BuiltInRegistries.BLOCK.getKey(state.getBlock()));
    }

    private static boolean isCraftingResultSlot(AbstractContainerMenu menu, Slot slot) {
        if (slot instanceof ResultSlot) {
            return true;
        }

        String menuClass = menu.getClass().getName().toLowerCase(Locale.ROOT);
        if (!menuClass.contains("craft")) {
            return false;
        }

        String slotClass = slot.getClass().getName().toLowerCase(Locale.ROOT);
        return slotClass.contains("result") || slotClass.contains("output");
    }

    private static boolean isCreateWrench(ItemStack stack) {
        return !stack.isEmpty() && CREATE_WRENCH_ID.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }

    private static ItemStack resolveBlueprintOutput(BlueprintEntity blueprint, net.minecraft.world.phys.Vec3 localPos) {
        Object section = blueprint.getSectionAt(localPos);
        if (section == null) {
            return ItemStack.EMPTY;
        }

        try {
            var getItems = section.getClass().getDeclaredMethod("getItems");
            getItems.setAccessible(true);
            Object items = getItems.invoke(section);
            if (items instanceof IItemHandler handler) {
                return handler.getStackInSlot(9);
            }
        } catch (ReflectiveOperationException ignored) {
            return ItemStack.EMPTY;
        }

        return ItemStack.EMPTY;
    }

    public static void notifyLockedCraft(ServerPlayer player) {
        long now = player.serverLevel().getGameTime();
        UUID playerId = player.getUUID();
        long nextAllowed = nextLockedCraftMessageTick.getOrDefault(playerId, 0L);
        if (now < nextAllowed) {
            return;
        }
        nextLockedCraftMessageTick.put(playerId, now + LOCKED_CRAFT_MESSAGE_INTERVAL_TICKS);
        player.displayClientMessage(net.minecraft.network.chat.Component.translatable("incore.research.recipe_locked"), true);
    }
}
