package ozokuz.incore.features.research.discovery;

import ozokuz.incore.INCore;
import ozokuz.incore.Registration;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = INCore.MODID)
public final class FieldResearchEvents {
    private FieldResearchEvents() {
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide || event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ItemStack pen = player.getMainHandItem();
        ItemStack paper = player.getOffhandItem();
        if (!pen.is(Registration.FIELD_PEN_ITEM.get()) || !paper.is(net.minecraft.world.item.Items.PAPER)) {
            return;
        }

        FieldResearchRegistry.FieldResearchDefinition mapping = FieldResearchRegistry.match(event.getLevel().getBlockState(event.getPos()));
        if (mapping == null) {
            return;
        }

        ItemStack note = mapping.createNote(Registration.FIELD_RESEARCH_NOTE_ITEM.get());
        paper.shrink(1);
        damagePen(player, pen);
        if (!player.addItem(note)) {
            player.drop(note, false);
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    private static void damagePen(ServerPlayer player, ItemStack stack) {
        if (!stack.isDamageableItem()) {
            return;
        }
        int nextDamage = stack.getDamageValue() + 1;
        if (nextDamage >= stack.getMaxDamage()) {
            stack.shrink(1);
            return;
        }
        stack.setDamageValue(nextDamage);
    }
}
