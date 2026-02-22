package io.github.ozokuz.incore.features.cards;

import com.google.gson.JsonObject;
import io.github.ozokuz.incore.Registration;
import io.github.ozokuz.incore.features.vendor.VendorCurrencyRegistry;
import io.github.ozokuz.incore.features.vendor.VendorCurrencySpec;
import io.github.ozokuz.incore.features.vendor.VendorCurrencyType;
import io.github.ozokuz.incore.features.vendor.VendorCurrencyUtil;
import io.github.ozokuz.incore.features.vendor.VendorCurrencyView;
import io.github.ozokuz.incore.features.vendor.VendorItemUtil;
import io.github.ozokuz.incore.features.vendor.VendorProductRegistry;
import io.github.ozokuz.incore.features.vendor.VendorProductSpec;
import io.github.ozokuz.incore.features.vendor.VendorProductType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class CardVendorIntegration {
    private static boolean initialized;

    private CardVendorIntegration() {
    }

    public static synchronized void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;

        VendorProductRegistry.register(new CardBoosterProductType());
        VendorProductRegistry.register(new CardBoosterBoxProductType());
        VendorCurrencyRegistry.register(new CardTokenCurrencyType());
    }

    private static final class CardBoosterProductType implements VendorProductType {
        private static final ResourceLocation TYPE_ID = ResourceLocation.parse("incore:card_booster");

        @Override
        public ResourceLocation id() {
            return TYPE_ID;
        }

        @Override
        public @Nullable VendorProductSpec parse(JsonObject json) {
            ResourceLocation setId = ResourceLocation.tryParse(GsonHelper.getAsString(json, "set", ""));
            if (setId == null) {
                return null;
            }

            int count = Math.max(1, GsonHelper.getAsInt(json, "count", 1));
            return new Spec(setId, count);
        }

        @Override
        public boolean isAvailable(VendorProductSpec spec) {
            return spec instanceof Spec boosterSpec && CardBoosterManager.get(boosterSpec.setId()) != null;
        }

        @Override
        public ItemStack previewStack(VendorProductSpec spec) {
            if (!(spec instanceof Spec boosterSpec)) {
                return ItemStack.EMPTY;
            }
            return CardItemFactory.booster(boosterSpec.setId(), 1);
        }

        @Override
        public String productId(VendorProductSpec spec) {
            return spec instanceof Spec boosterSpec ? boosterSpec.setId().toString() : "";
        }

        @Override
        public boolean grant(ServerPlayer player, VendorProductSpec spec, int quantity) {
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

            VendorItemUtil.giveOrDropStacked(player, CardItemFactory.booster(boosterSpec.setId(), (int) total));
            return true;
        }

        private record Spec(ResourceLocation setId, int count) implements VendorProductSpec {
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

    private static final class CardBoosterBoxProductType implements VendorProductType {
        private static final ResourceLocation TYPE_ID = ResourceLocation.parse("incore:card_booster_box");

        @Override
        public ResourceLocation id() {
            return TYPE_ID;
        }

        @Override
        public @Nullable VendorProductSpec parse(JsonObject json) {
            ResourceLocation boxId = ResourceLocation.tryParse(GsonHelper.getAsString(json, "box", ""));
            if (boxId == null) {
                return null;
            }

            int count = Math.max(1, GsonHelper.getAsInt(json, "count", 1));
            return new Spec(boxId, count);
        }

        @Override
        public boolean isAvailable(VendorProductSpec spec) {
            return spec instanceof Spec boxSpec && CardBoosterBoxManager.get(boxSpec.boxId()) != null;
        }

        @Override
        public ItemStack previewStack(VendorProductSpec spec) {
            if (!(spec instanceof Spec boxSpec)) {
                return ItemStack.EMPTY;
            }
            return CardItemFactory.boosterBox(boxSpec.boxId(), 1);
        }

        @Override
        public String productId(VendorProductSpec spec) {
            return spec instanceof Spec boxSpec ? boxSpec.boxId().toString() : "";
        }

        @Override
        public boolean grant(ServerPlayer player, VendorProductSpec spec, int quantity) {
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

            VendorItemUtil.giveOrDropStacked(player, CardItemFactory.boosterBox(boxSpec.boxId(), (int) total));
            return true;
        }

        private record Spec(ResourceLocation boxId, int count) implements VendorProductSpec {
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

    private static final class CardTokenCurrencyType implements VendorCurrencyType {
        private static final ResourceLocation TYPE_ID = ResourceLocation.parse("incore:card_token");
        private static final ResourceLocation TOKEN_ITEM_ID = ResourceLocation.parse("incore:card_token");
        private static final ResourceLocation SPUR_TYPE_ID = ResourceLocation.parse("incore:bank_spur");
        private static final ResourceLocation SPUR_ICON_ITEM = ResourceLocation.parse("numismatics:spur");

        @Override
        public ResourceLocation id() {
            return TYPE_ID;
        }

        @Override
        public @Nullable VendorCurrencySpec parse(JsonObject json) {
            int amount = Math.max(1, GsonHelper.getAsInt(json, "amount", 1));
            int spurConversionRate = GsonHelper.getAsInt(json, "spur_conversion_rate", 0);
            if (spurConversionRate <= 0) {
                return null;
            }

            return new Spec(amount, spurConversionRate);
        }

        @Override
        public VendorCurrencyView buildView(ServerPlayer player, VendorCurrencySpec spec) {
            if (!(spec instanceof Spec tokenSpec)) {
                return new VendorCurrencyView(TYPE_ID.toString(), TOKEN_ITEM_ID.toString(), "TOKEN", 1, 0, SPUR_TYPE_ID.toString(), SPUR_ICON_ITEM.toString(), "SPUR", 1, 0);
            }

            Item tokenItem = Registration.CARD_TOKEN_ITEM.get();
            String label = tokenItem.getDescription().getString();
            int availableTokens = VendorCurrencyUtil.countItem(player, TOKEN_ITEM_ID);

            return new VendorCurrencyView(
                    TYPE_ID.toString(),
                    TOKEN_ITEM_ID.toString(),
                    label,
                    tokenSpec.amount(),
                    availableTokens,
                    SPUR_TYPE_ID.toString(),
                    SPUR_ICON_ITEM.toString(),
                    "SPUR",
                    tokenSpec.spurConversionRate(),
                    VendorCurrencyUtil.getBankSpurBalance(player)
            );
        }

        @Override
        public boolean consume(ServerPlayer player, VendorCurrencySpec spec, int quantity, boolean allowConversion) {
            if (!(spec instanceof Spec tokenSpec) || quantity <= 0) {
                return false;
            }

            long requiredLong = (long) tokenSpec.amount() * quantity;
            if (requiredLong <= 0L || requiredLong > Integer.MAX_VALUE) {
                return false;
            }
            int requiredTokens = (int) requiredLong;

            int availableTokens = VendorCurrencyUtil.countItem(player, TOKEN_ITEM_ID);
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

                if (!VendorCurrencyUtil.deductBankSpur(player, (int) requiredSpurLong)) {
                    return false;
                }
            }

            if (tokensToConsume > 0) {
                VendorCurrencyUtil.consumeItem(player, TOKEN_ITEM_ID, tokensToConsume);
            }
            return true;
        }

        @Override
        public VendorCurrencySpec withUnitAmount(VendorCurrencySpec spec, int unitAmount) {
            if (!(spec instanceof Spec tokenSpec)) {
                return spec;
            }

            return new Spec(Math.max(1, unitAmount), tokenSpec.spurConversionRate());
        }

        private record Spec(int amount, int spurConversionRate) implements VendorCurrencySpec {
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
