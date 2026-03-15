package ozokuz.incore.features.vendingmachine;

import ozokuz.incore.Config;
import ozokuz.incore.Registration;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import top.theillusivec4.curios.api.CuriosApi;

public final class VendingMachineDiscountService {
    private static final int DARK_MARKET_DISCOUNT_MIN_PERCENT = 50;
    private static final int DARK_MARKET_DISCOUNT_MAX_PERCENT = 75;

    private VendingMachineDiscountService() {
    }

    public static int rollNormalOfferDiscountPercent(RandomSource random) {
        DiscountRollProfile profile = baseProfile();
        if (profile.chancePercent() <= 0 || profile.maxPercent() <= 0) {
            return 0;
        }

        if (random.nextInt(100) >= profile.chancePercent()) {
            return 0;
        }

        return rollNormalDiscountAmountPercent(random);
    }

    public static int rollNormalDiscountAmountPercent(RandomSource random) {
        DiscountRollProfile profile = baseProfile();
        if (profile.maxPercent() <= 0) {
            return 0;
        }
        return profile.minPercent() + random.nextInt(profile.maxPercent() - profile.minPercent() + 1);
    }

    public static boolean hasDiscountCharmEquipped(ServerPlayer player) {
        return CuriosApi.getCuriosInventory(player)
                .map(handler -> handler.findFirstCurio(stack -> stack.is(Registration.VENDING_MACHINE_DISCOUNT_CHARM_ITEM.get())).isPresent())
                .orElse(false);
    }

    public static int curioBonusChancePercent() {
        return Math.clamp(Config.VENDING_MACHINE_DISCOUNT_CURIO_BONUS_CHANCE_PERCENT.get(), 0, 100);
    }

    public static int curioBonusAmountPercent() {
        return Math.clamp(Config.VENDING_MACHINE_DISCOUNT_CURIO_BONUS_AMOUNT_PERCENT.get(), 0, 100);
    }

    public static int baseDiscountMinPercent() {
        return Math.clamp(Config.VENDING_MACHINE_OFFER_DISCOUNT_MIN_PERCENT.get(), 0, 100);
    }

    public static int baseDiscountMaxPercent() {
        return Math.clamp(Config.VENDING_MACHINE_OFFER_DISCOUNT_MAX_PERCENT.get(), 0, 100);
    }

    private static DiscountRollProfile baseProfile() {
        int chance = Math.clamp(Config.VENDING_MACHINE_OFFER_DISCOUNT_CHANCE_PERCENT.get(), 0, 100);
        int chanceCap = Math.clamp(Config.VENDING_MACHINE_OFFER_DISCOUNT_CHANCE_CAP_PERCENT.get(), 0, 100);
        int minPercent = Math.clamp(Config.VENDING_MACHINE_OFFER_DISCOUNT_MIN_PERCENT.get(), 0, 100);
        int maxPercent = Math.clamp(Config.VENDING_MACHINE_OFFER_DISCOUNT_MAX_PERCENT.get(), 0, 100);

        chance = Math.clamp(chance, 0, chanceCap);
        minPercent = Math.clamp(minPercent, 0, 100);
        maxPercent = Math.clamp(maxPercent, 0, 100);
        if (maxPercent < minPercent) {
            minPercent = maxPercent;
        }

        return new DiscountRollProfile(chance, minPercent, maxPercent);
    }

    public static int rollDarkMarketOfferDiscountPercent(RandomSource random) {
        return DARK_MARKET_DISCOUNT_MIN_PERCENT + random.nextInt(DARK_MARKET_DISCOUNT_MAX_PERCENT - DARK_MARKET_DISCOUNT_MIN_PERCENT + 1);
    }

    public static int applyDiscountedUnitAmount(int baseAmount, int discountPercent) {
        int sanitizedBase = Math.max(1, baseAmount);
        int sanitizedDiscount = Math.clamp(discountPercent, 0, 100);
        long scaled = Math.round(sanitizedBase * (100.0D - sanitizedDiscount) / 100.0D);
        if (scaled <= 0L) {
            return 1;
        }
        if (scaled > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) scaled;
    }

    private record DiscountRollProfile(int chancePercent, int minPercent, int maxPercent) {
    }
}
