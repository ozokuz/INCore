package ozokuz.incore.features.cards;

import ozokuz.incore.INCore;
import ozokuz.incore.Registration;
import ozokuz.incore.features.roguelike.RoguelikeConstants;
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
        if (block != Registration.CARD_DECRYPTOR_BLOCK.get()) {
            return;
        }

        if (player.isShiftKeyDown()) {
            CardDecryptorService.decryptAll(player);
        } else {
            CardDecryptorService.decryptHeld(player, player.getMainHandItem());
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }
}
