package ozokuz.incore.features.gacha;

import ozokuz.incore.INCore;
import ozokuz.incore.Registration;
import ozokuz.incore.features.playerlevel.PlayerFeatureUnlockService;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = INCore.MODID)
public final class GachaEvents {
    private GachaEvents() {
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide || event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }

        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (event.getLevel().getBlockState(event.getPos()).getBlock() != Registration.GACHA_RIFT_BLOCK.get()) {
            return;
        }

        if (!player.isShiftKeyDown()) {
            return;
        }

        if (!player.getMainHandItem().isEmpty() || !player.getOffhandItem().isEmpty()) {
            player.sendSystemMessage(Component.translatable("incore.gacha.crate.empty_hand_required"));
            event.setCancellationResult(InteractionResult.FAIL);
            event.setCanceled(true);
            return;
        }

        if (!(event.getLevel().getBlockEntity(event.getPos()) instanceof GachaCrateBlockEntity crateBlockEntity)) {
            event.setCancellationResult(InteractionResult.FAIL);
            event.setCanceled(true);
            return;
        }

        ResourceLocation bannerId = crateBlockEntity.getBannerId();
        if (bannerId == null) {
            player.sendSystemMessage(Component.translatable("incore.gacha.crate.invalid"));
            event.setCancellationResult(InteractionResult.FAIL);
            event.setCanceled(true);
            return;
        }
        GachaBannerData banner = GachaBannerManager.get(bannerId);
        if (banner == null) {
            player.sendSystemMessage(Component.translatable("incore.gacha.banner.invalid", bannerId.toString()));
            event.setCancellationResult(InteractionResult.FAIL);
            event.setCanceled(true);
            return;
        }
        ResourceLocation requiredUnlock = GachaService.requiredUnlockForBanner(banner);
        if (!PlayerFeatureUnlockService.hasUnlocked(player, requiredUnlock)) {
            player.sendSystemMessage(PlayerFeatureUnlockService.lockedMessage(requiredUnlock));
            event.setCancellationResult(InteractionResult.FAIL);
            event.setCanceled(true);
            return;
        }

        boolean opened = GachaService.pullCrateForBanner(
                player,
                bannerId,
                event.getPos().getX() + 0.5D,
                event.getPos().getY() + 0.5D,
                event.getPos().getZ() + 0.5D
        );

        if (opened) {
            event.getLevel().removeBlock(event.getPos(), false);
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        ResourceLocation selected = GachaPityManager.getLastBanner(player);
        if (selected != null && GachaBannerManager.get(selected) != null) {
            return;
        }

        ResourceLocation defaultBanner = GachaBannerManager.getDefaultBannerId();
        if (defaultBanner != null) {
            GachaPityManager.setLastBanner(player, defaultBanner);
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!(event.getEntity() instanceof ServerPlayer newPlayer)) {
            return;
        }

        if (!(event.getOriginal() instanceof ServerPlayer oldPlayer)) {
            return;
        }

        GachaPityManager.copyData(oldPlayer, newPlayer);
    }
}
