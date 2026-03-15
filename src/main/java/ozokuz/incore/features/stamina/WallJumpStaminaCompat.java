package ozokuz.incore.features.stamina;

import ozokuz.incore.Config;
import net.minecraft.world.entity.player.Player;
import tictim.paraglider.api.stamina.Stamina;

public final class WallJumpStaminaCompat {
    private static final double EPSILON = 1.0E-6D;

    private WallJumpStaminaCompat() {
    }

    public static double wallJumpCost() {
        return Config.WALL_JUMP_STAMINA_COST.get();
    }

    public static boolean canUseWallJump(Player player) {
        return !Stamina.get(player).isDepleted();
    }

    public static boolean consumeForWallJump(Player player) {
        Stamina stamina = Stamina.get(player);
        if (stamina.isDepleted()) {
            return false;
        }

        double cost = wallJumpCost();
        if (cost > 0.0D) {
            // Intentionally allow partial consumption so the jump can still execute and drain to zero.
            stamina.takeStamina(cost, false, false);
        }

        if (stamina.stamina() <= EPSILON && stamina.extraStamina() <= EPSILON) {
            stamina.setDepleted(true, false);
        }

        return true;
    }
}
