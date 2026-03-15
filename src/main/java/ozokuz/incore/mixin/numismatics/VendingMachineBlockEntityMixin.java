package ozokuz.incore.mixin.numismatics;

import dev.ithundxr.createnumismatics.content.vendor.VendorBlockEntity;
import ozokuz.incore.features.tasks.DailyTaskEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(VendorBlockEntity.class)
public abstract class VendingMachineBlockEntityMixin {
    @Shadow
    protected UUID owner;

    @Shadow(remap = false)
    public abstract boolean isTrustedInternal(Player player);

    @Inject(
            method = "trySellTo",
            at = @At(value = "INVOKE", target = "Ldev/ithundxr/createnumismatics/content/vendor/VendorBlockEntity;notifyUpdate()V"),
            remap = false
    )
    private void onTrySellTo(Player player, InteractionHand hand, CallbackInfo ci) {
        if (!(player instanceof ServerPlayer serverBuyer)) {
            return;
        }
        if (isTrustedInternal(serverBuyer)) {
            return;
        }
        DailyTaskEvents.onBuyFromPlayer(serverBuyer);
        MinecraftServer server = serverBuyer.getServer();
        if (server != null) {
            DailyTaskEvents.onSellToPlayer(server, owner);
        }
    }

    @Inject(
            method = "tryBuyFrom",
            at = @At(value = "INVOKE", target = "Ldev/ithundxr/createnumismatics/content/vendor/VendorBlockEntity;notifyUpdate()V"),
            remap = false
    )
    private void onTryBuyFrom(Player player, InteractionHand hand, CallbackInfo ci) {
        if (!(player instanceof ServerPlayer serverSeller)) {
            return;
        }
        if (isTrustedInternal(serverSeller)) {
            return;
        }
        DailyTaskEvents.onSellToPlayer(serverSeller);
        MinecraftServer server = serverSeller.getServer();
        if (server != null) {
            DailyTaskEvents.onBuyFromPlayer(server, owner);
        }
    }
}
