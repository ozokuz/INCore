package ozokuz.incore.mixin.walljump;

import com.jahirtrap.walljump.network.message.MessageWallJump;
import ozokuz.incore.features.stamina.WallJumpStaminaCompat;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = MessageWallJump.class, remap = false)
public abstract class MessageWallJumpMixin {
    @Inject(method = "handle", at = @At("HEAD"), cancellable = true)
    private static void incore$consumeStamina(MessageWallJump message, IPayloadContext context, CallbackInfo ci) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            if (!message.didWallJump()) {
                return;
            }
            if (!WallJumpStaminaCompat.consumeForWallJump(player)) {
                return;
            }

            player.resetFallDistance();
        });
        ci.cancel();
    }
}
