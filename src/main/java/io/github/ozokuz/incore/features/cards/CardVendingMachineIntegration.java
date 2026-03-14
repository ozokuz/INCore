package io.github.ozokuz.incore.features.cards;

import com.google.gson.JsonObject;
import io.github.ozokuz.incore.Registration;
import io.github.ozokuz.incore.features.vendingmachine.VendingMachineCurrencyRegistry;
import io.github.ozokuz.incore.features.vendingmachine.VendingMachineCurrencySpec;
import io.github.ozokuz.incore.features.vendingmachine.VendingMachineCurrencyType;
import io.github.ozokuz.incore.features.vendingmachine.VendingMachineCurrencyUtil;
import io.github.ozokuz.incore.features.vendingmachine.VendingMachineCurrencyView;
import io.github.ozokuz.incore.features.vendingmachine.VendingMachineItemUtil;
import io.github.ozokuz.incore.features.vendingmachine.VendingMachineProductRegistry;
import io.github.ozokuz.incore.features.vendingmachine.VendingMachineProductSpec;
import io.github.ozokuz.incore.features.vendingmachine.VendingMachineProductType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class CardVendingMachineIntegration {
    private static boolean initialized;

    private CardVendingMachineIntegration() {
    }

    public static synchronized void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;

        VendingMachineProductRegistry.register(new CardBoosterProductType());
        VendingMachineProductRegistry.register(new CardBoosterBoxProductType());
        VendingMachineCurrencyRegistry.register(new CardTokenCurrencyType());
    }

    private static final class CardBoosterProductType implements VendingMachineProductType {
        private static final ResourceLocation TYPE_ID = ResourceLocation.parse("incore:card_booster");

        @Override
        public ResourceLocation id() {
            return TYPE_ID;
        }

        @Override
        public @Nullable VendingMachineProductSpec parse(JsonObject json) {
            ResourceLocation setId = ResourceLocation.tryParse(GsonHelper.getAsString(json, "set", ""));
            if (setId == null) {
                return null;
            }

            int count = Math.max(1, GsonHelper.getAsInt(json, "count", 1));
            return new Spec(setId, count);
        }

        @Override
        public boolean isAvailable(VendingMachineProductSpec spec) {
            return spec instanceof Spec boosterSpec && CardBoosterManager.get(boosterSpec.setId()) != null;
        }

        @Override
        public ItemStack previewStack(VendingMachineProductSpec spec) {
            if (!(spec instanceof Spec boosterSpec)) {
                return ItemStack.EMPTY;
            }
            return CardItemFactory.booster(boosterSpec.setId(), 1);
        }

        @Override
        public String productId(VendingMachineProductSpec spec) {
            return spec instanceof Spec boosterSpec ? boosterSpec.setId().toString() : "";
        }

        @Override
        public boolean grant(ServerPlayer player, VendingMachineProductSpec spec, int quantity) {
            if (!(spec instanceof Spec boosterSpec) || quantity <= 0) {
                return false;
            }
            if (CardBoosterManager.get(boosterSpec.setId()) == null) {
                return false;
            }

            long total = (long) boosterSpec.count() * quantity;
            if (total <= 0L || total > Integer.MAX_VALUE) {
                return false;
            }

            VendingMachineItemUtil.giveOrDropStacked(player, CardItemFactory.booster(boosterSpec.setId(), (int) total));
            return true;
        }

        private record Spec(ResourceLocation setId, int count) implements VendingMachineProductSpec {
            @Override
            public ResourceLocation typeId() {
                return TYPE_ID;
            }

            @Override
            public int unitCount() {
                return count;
            }
        }
    }

    private static final class CardBoosterBoxProductType implements VendingMachineProductType {
        private static final ResourceLocation TYPE_ID = ResourceLocation.parse("incore:card_booster_box");

        @Override
        public ResourceLocation id() {
            return TYPE_ID;
        }

        @Override
        public @Nullable VendingMachineProductSpec parse(JsonObject json) {
            ResourceLocation boxId = ResourceLocation.tryParse(GsonHelper.getAsString(json, "box", ""));
            if (boxId == null) {
                return null;
            }

            int count = Math.max(1, GsonHelper.getAsInt(json, "count", 1));
            return new Spec(boxId, count);
        }

        @Override
        public boolean isAvailable(VendingMachineProductSpec spec) {
            return spec instanceof Spec boxSpec && CardBoosterBoxManager.get(boxSpec.boxId()) != null;
        }

        @Override
        public ItemStack previewStack(VendingMachineProductSpec spec) {
            if (!(spec instanceof Spec boxSpec)) {
                return ItemStack.EMPTY;
            }
            return CardItemFactory.boosterBox(boxSpec.boxId(), 1);
        }

        @Override
        public String productId(VendingMachineProductSpec spec) {
            return spec instanceof Spec boxSpec ? boxSpec.boxId().toString() : "";
        }

        @Override
        public boolean grant(ServerPlayer player, VendingMachineProductSpec spec, int quantity) {
            if (!(spec instanceof Spec boxSpec) || quantity <= 0) {
                return false;
            }
            if (CardBoosterBoxManager.get(boxSpec.boxId()) == null) {
                return false;
            }

            long total = (long) boxSpec.count() * quantity;
            if (total <= 0L || total > Integer.MAX_VALUE) {
                return false;
            }

            VendingMachineItemUtil.giveOrDropStacked(player, CardItemFactory.boosterBox(boxSpec.boxId(), (int) total));
            return true;
        }

        private record Spec(ResourceLocation boxId, int count) implements VendingMachineProductSpec {
            @Override
            public ResourceLocation typeId() {
                return TYPE_ID;
            }

            @Override
            public int unitCount() {
                return count;
            }
        }
    }

    private static final class CardTokenCurrencyType implements VendingMachineCurrencyType {
        private static final ResourceLocation TYPE_ID = ResourceLocation.parse("incore:card_token");
        private static final ResourceLocation TOKEN_ITEM_ID = ResourceLocation.parse("incore:card_token");
        private static final ResourceLocation SPUR_TYPE_ID = ResourceLocation.parse("incore:bank_spur");
        private static final ResourceLocation SPUR_ICON_ITEM = ResourceLocation.parse("numismatics:spur");

        @Override
        public ResourceLocation id() {
            return TYPE_ID;
        }

        @Override
        public @Nullable VendingMachineCurrencySpec parse(JsonObject json) {
            int amount = Math.max(1, GsonHelper.getAsInt(json, "amount", 1));
            int spurConversionRate = GsonHelper.getAsInt(json, "spur_conversion_rate", 0);
            if (spurConversionRate <= 0) {
                return null;
            }

            return new Spec(amount, spurConversionRate);
        }

        @Override
        public VendingMachineCurrencyView buildView(ServerPlayer player, VendingMachineCurrencySpec spec) {
            if (!(spec instanceof Spec tokenSpec)) {
                return new VendingMachineCurrencyView(TYPE_ID.toString(), TOKEN_ITEM_ID.toString(), "TOKEN", 1, 0, SPUR_TYPE_ID.toString(), SPUR_ICON_ITEM.toString(), "SPUR", 1, 0);
            }

            Item tokenItem = Registration.CARD_TOKEN_ITEM.get();
            String label = tokenItem.getDescription().getString();
            int availableTokens = VendingMachineCurrencyUtil.countItem(player, TOKEN_ITEM_ID);

            return new VendingMachineCurrencyView(
                    TYPE_ID.toString(),
                    TOKEN_ITEM_ID.toString(),
                    label,
                    tokenSpec.amount(),
                    availableTokens,
                    SPUR_TYPE_ID.toString(),
                    SPUR_ICON_ITEM.toString(),
                    "SPUR",
                    tokenSpec.spurConversionRate(),
                    VendingMachineCurrencyUtil.getBankSpurBalance(player)
            );
        }

        @Override
        public boolean consume(ServerPlayer player, VendingMachineCurrencySpec spec, int quantity, boolean allowConversion) {
            if (!(spec instanceof Spec tokenSpec) || quantity <= 0) {
                return false;
            }

            long requiredLong = (long) tokenSpec.amount() * quantity;
            if (requiredLong <= 0L || requiredLong > Integer.MAX_VALUE) {
                return false;
            }
            int requiredTokens = (int) requiredLong;

            int availableTokens = VendingMachineCurrencyUtil.countItem(player, TOKEN_ITEM_ID);
            int missingTokens = Math.max(0, requiredTokens - availableTokens);
            int tokensToConsume = Math.min(requiredTokens, availableTokens);

            if (missingTokens > 0) {
                if (!allowConversion) {
                    return false;
                }

                long requiredSpurLong = (long) missingTokens * tokenSpec.spurConversionRate();
                if (requiredSpurLong <= 0L || requiredSpurLong > Integer.MAX_VALUE) {
                    return false;
                }

                if (!VendingMachineCurrencyUtil.deductBankSpur(player, (int) requiredSpurLong)) {
                    return false;
                }
            }

            if (tokensToConsume > 0) {
                VendingMachineCurrencyUtil.consumeItem(player, TOKEN_ITEM_ID, tokensToConsume);
            }
            return true;
        }

        @Override
        public VendingMachineCurrencySpec withUnitAmount(VendingMachineCurrencySpec spec, int unitAmount) {
            if (!(spec instanceof Spec tokenSpec)) {
                return spec;
            }

            return new Spec(Math.max(1, unitAmount), tokenSpec.spurConversionRate());
        }

        private record Spec(int amount, int spurConversionRate) implements VendingMachineCurrencySpec {
            @Override
            public ResourceLocation typeId() {
                return TYPE_ID;
            }

            @Override
            public int unitAmount() {
                return amount;
            }

            @Override
            public int spurConversionRate() {
                return spurConversionRate;
            }
        }
    }
}
