package io.github.ozokuz.incore.mixin.walljump;

import com.jahirtrap.walljump.logic.WallJumpLogic;
import io.github.ozokuz.incore.features.stamina.WallJumpStaminaCompat;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = WallJumpLogic.class, remap = false)
public abstract class WallJumpLogicMixin {
    @Inject(method = "canWallCling", at = @At("HEAD"), cancellable = true)
    private static void incore$preventWallClingWithoutStamina(LocalPlayer player, CallbackInfoReturnable<Boolean> cir) {
        if (!WallJumpStaminaCompat.canUseWallJump(player)) {
            cir.setReturnValue(false);
        }
    }
}
