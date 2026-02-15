package io.github.ozokuz.incore.features.battlepass;

import io.github.ozokuz.incore.features.sanity.SanityManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public interface BattlePassReward {
    void grant(ServerPlayer player);

    String previewText();

    record ItemReward(Item item, int count) implements BattlePassReward {
        @Override
        public void grant(ServerPlayer player) {
            ItemStack stack = new ItemStack(item, count);
            if (!player.addItem(stack)) {
                player.drop(stack, false);
            }
        }

        @Override
        public String previewText() {
            return count + "x " + item.getName(new ItemStack(item)).getString();
        }
    }

    record CommandReward(String command, String preview) implements BattlePassReward {
        @Override
        public void grant(ServerPlayer player) {
            CommandSourceStack source = player.server.createCommandSourceStack().withSuppressedOutput();
            String resolved = command.replace("${player}", player.getGameProfile().getName());
            player.server.getCommands().performPrefixedCommand(source, resolved);
        }

        @Override
        public String previewText() {
            return preview;
        }
    }

    record SanityCapBonusReward(int amount) implements BattlePassReward {
        @Override
        public void grant(ServerPlayer player) {
            SanityManager.addMaxSanityBonus(player, amount);
        }

        @Override
        public String previewText() {
            return "+" + amount + " Max Sanity";
        }
    }

    static Item parseItem(String itemId) {
        ResourceLocation id = ResourceLocation.tryParse(itemId);
        if (id == null) {
            throw new IllegalArgumentException("Invalid item id: " + itemId);
        }

        Item item = BuiltInRegistries.ITEM.get(id);
        if (item == Items.AIR) {
            throw new IllegalArgumentException("Unknown item id: " + itemId);
        }

        return item;
    }

    static Component grantedMessage(int levelsGranted) {
        return Component.literal("Battle pass advanced " + levelsGranted + " level(s).");
    }
}
