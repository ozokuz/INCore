package io.github.ozokuz.incore.features.cards;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class CardBoosterBoxItem extends Item {
    public CardBoosterBoxItem(Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        ResourceLocation boxId = CardItemData.readBoosterBoxId(stack);
        if (boxId == null) {
            return super.getName(stack);
        }

        CardBoosterBoxData box = CardBoosterBoxManager.get(boxId);
        if (box == null) {
            return super.getName(stack);
        }

        return Component.literal(box.name());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.fail(stack);
        }

        ResourceLocation boxId = CardItemData.readBoosterBoxId(stack);
        if (boxId == null) {
            boxId = CardBoosterBoxManager.getDefaultBoxId();
        }

        CardBoosterBoxData box = CardBoosterBoxManager.get(boxId);
        if (box == null) {
            serverPlayer.sendSystemMessage(Component.translatable("incore.cards.booster_box.invalid", boxId.toString()));
            return InteractionResultHolder.fail(stack);
        }

        if (CardBoosterManager.get(box.setId()) == null) {
            serverPlayer.sendSystemMessage(Component.translatable("incore.cards.booster.invalid", box.setId().toString()));
            return InteractionResultHolder.fail(stack);
        }

        ItemStack boosters = CardItemFactory.booster(box.setId(), box.boosterCount());
        if (!serverPlayer.addItem(boosters)) {
            serverPlayer.drop(boosters, false);
        }

        if (!serverPlayer.isCreative()) {
            stack.shrink(1);
        }

        serverPlayer.sendSystemMessage(Component.translatable("incore.cards.booster_box.opened", box.name(), box.boosterCount()));
        return InteractionResultHolder.consume(stack);
    }
}
