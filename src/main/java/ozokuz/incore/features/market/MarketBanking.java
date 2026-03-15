package ozokuz.incore.features.market;

import dev.ithundxr.createnumismatics.Numismatics;
import dev.ithundxr.createnumismatics.content.backend.BankAccount;
import dev.ithundxr.createnumismatics.content.backend.Coin;
import dev.ithundxr.createnumismatics.content.bank.CardItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class MarketBanking {
    private MarketBanking() {
    }

    public static BankAccount resolveManualAccount(ServerPlayer player, ItemStack optionalCard) {
        BankAccount cardAccount = resolveCardAccount(player, optionalCard, false);
        if (cardAccount != null) {
            return cardAccount;
        }
        return Numismatics.BANK.getAccount(player);
    }

    public static BankAccount resolveCardAccount(@Nullable Player player, ItemStack cardStack, boolean requireAuthorization) {
        if (cardStack.isEmpty() || !CardItem.isBound(cardStack)) {
            return null;
        }

        UUID accountId = CardItem.get(cardStack);
        if (accountId == null) {
            return null;
        }

        BankAccount account = Numismatics.BANK.getAccount(accountId);
        if (account == null) {
            return null;
        }

        if (requireAuthorization && (player == null || !account.isAuthorized(player))) {
            return null;
        }

        return account;
    }

    public static boolean withdraw(BankAccount account, int spurs) {
        if (account == null || spurs <= 0) {
            return false;
        }
        return account.deduct(Coin.SPUR, spurs);
    }

    public static void deposit(BankAccount account, int spurs) {
        if (account == null || spurs <= 0) {
            return;
        }
        account.deposit(Coin.SPUR, spurs);
    }

    public static int balanceSpur(BankAccount account) {
        return account == null ? 0 : Math.max(0, account.getBalance());
    }
}
