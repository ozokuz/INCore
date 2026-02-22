package io.github.ozokuz.incore.features.cards;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class CardSleeveItem extends Item {
    private static final int[] GRADE_WEIGHTS = {8, 10, 12, 12, 14, 14, 12, 9, 6, 3};

    public CardSleeveItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack sleeveStack = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.success(sleeveStack);
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.fail(sleeveStack);
        }

        ItemStack target = hand == InteractionHand.MAIN_HAND ? player.getOffhandItem() : player.getMainHandItem();
        if (target.getItem() != io.github.ozokuz.incore.Registration.CARD_MODULE_ITEM.get()) {
            serverPlayer.sendSystemMessage(Component.translatable("incore.cards.sleeve.no_target"));
            return InteractionResultHolder.fail(sleeveStack);
        }

        CardItemData.CardInstance instance = CardItemData.readCardInstance(target);
        if (instance == null) {
            return InteractionResultHolder.fail(sleeveStack);
        }

        if (instance.graded()) {
            serverPlayer.sendSystemMessage(Component.translatable("incore.cards.sleeve.already_graded"));
            return InteractionResultHolder.fail(sleeveStack);
        }

        int grade = rollGrade(serverPlayer.getRandom());
        CardItemData.setCardGrade(target, grade);
        if (!serverPlayer.isCreative()) {
            sleeveStack.shrink(1);
        }

        serverPlayer.sendSystemMessage(Component.translatable("incore.cards.sleeve.graded", grade));
        return InteractionResultHolder.consume(sleeveStack);
    }

    private static int rollGrade(RandomSource random) {
        int totalWeight = 0;
        for (int weight : GRADE_WEIGHTS) {
            totalWeight += weight;
        }

        int roll = random.nextInt(totalWeight);
        int cumulative = 0;
        for (int grade = 0; grade < GRADE_WEIGHTS.length; grade++) {
            cumulative += GRADE_WEIGHTS[grade];
            if (roll < cumulative) {
                return grade + 1;
            }
        }

        return 1;
    }
}
