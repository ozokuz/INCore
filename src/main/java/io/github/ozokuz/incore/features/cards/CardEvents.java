package io.github.ozokuz.incore.features.cards;

import io.github.ozokuz.incore.INCore;
import io.github.ozokuz.incore.Registration;
import io.github.ozokuz.incore.features.roguelike.RoguelikeConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = INCore.MODID)
public final class CardEvents {
    private CardEvents() {
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!(event.getEntity() instanceof ServerPlayer newPlayer) || !(event.getOriginal() instanceof ServerPlayer oldPlayer)) {
            return;
        }

        CardCollectionService.copyData(oldPlayer, newPlayer);
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        boolean roguelikeTransition = event.getFrom().equals(RoguelikeConstants.ROGUELIKE_DIMENSION)
                || event.getTo().equals(RoguelikeConstants.ROGUELIKE_DIMENSION);

        if (roguelikeTransition) {
            CardDeckService.onDungeonTransition(player);
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide || event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }

        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        var block = event.getLevel().getBlockState(event.getPos()).getBlock();
        if (block != Registration.CARD_DECRYPTOR_BLOCK.get()
                && block != Registration.CARD_VENDOR_BLOCK.get()) {
            return;
        }

        if (block == Registration.CARD_DECRYPTOR_BLOCK.get()) {
            if (player.isShiftKeyDown()) {
                CardDecryptorService.decryptAll(player);
            } else {
                CardDecryptorService.decryptHeld(player, player.getMainHandItem());
            }
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            return;
        }

        if (!player.getMainHandItem().isEmpty() || !player.getOffhandItem().isEmpty()) {
            player.sendSystemMessage(Component.translatable("incore.cards.station.empty_hand"));
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
            return;
        }

        if (block == Registration.CARD_VENDOR_BLOCK.get()) {
            CardVendorService.openVendorScreen(player, event.getPos());
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }
}
