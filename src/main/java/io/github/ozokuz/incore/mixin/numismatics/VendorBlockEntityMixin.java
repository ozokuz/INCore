package io.github.ozokuz.incore.mixin.numismatics;

import dev.ithundxr.createnumismatics.content.vendor.VendorBlockEntity;
import io.github.ozokuz.incore.features.tasks.DailyTaskEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(VendorBlockEntity.class)
public abstract class VendorBlockEntityMixin {
    @Shadow(remap = false)
    public abstract UUID getOwner();

    @Inject(
            method = "trySellTo",
            at = @At("RETURN"),
            remap = false
    )
    private void onTrySellTo(net.minecraft.world.entity.player.Player buyer, int index, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ()) {
            return;
        }
        if (!(buyer instanceof ServerPlayer serverBuyer)) {
            return;
        }
        UUID owner = getOwner();
        if (owner == null) {
            return;
        }
        if (serverBuyer.getUUID().equals(owner)) {
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
            at = @At("RETURN"),
            remap = false
    )
    private void onTryBuyFrom(net.minecraft.world.entity.player.Player seller, int index, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ()) {
            return;
        }
        if (!(seller instanceof ServerPlayer serverSeller)) {
            return;
        }
        UUID owner = getOwner();
        if (owner == null) {
            return;
        }
        if (serverSeller.getUUID().equals(owner)) {
            return;
        }
        DailyTaskEvents.onSellToPlayer(serverSeller);
        MinecraftServer server = serverSeller.getServer();
        if (server != null) {
            DailyTaskEvents.onBuyFromPlayer(server, owner);
        }
    }
}
