package io.github.ozokuz.incore.features.research;

import io.github.ozokuz.incore.INCore;
import io.github.ozokuz.incore.features.research.network.ResearchNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = INCore.MODID)
public class ResearchEvents {
    private static final long LOCKED_CRAFT_MESSAGE_INTERVAL_TICKS = 20L;
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

        player.displayClientMessage(net.minecraft.network.chat.Component.translatable("incore.research.recipe_locked"), true);
        ResearchNetworking.syncLockState(player);
    }

    private static void enforceLockedCrafting(ServerPlayer player) {
        AbstractContainerMenu menu = player.containerMenu;
        if (!(menu instanceof CraftingMenu) && !(menu instanceof InventoryMenu)) {
            return;
        }
        if (menu.slots.isEmpty()) {
            return;
        }

        Slot resultSlot = menu.slots.getFirst();
        ItemStack result = resultSlot.getItem();
        if (result.isEmpty() || !ResearchRecipeLockService.isOutputLocked(player, result)) {
            return;
        }

        resultSlot.set(ItemStack.EMPTY);
        menu.broadcastChanges();
        sendLockedCraftMessage(player);
    }

    private static void sendLockedCraftMessage(ServerPlayer player) {
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
