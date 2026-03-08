package io.github.ozokuz.incore.features.playerlevel;

import io.github.ozokuz.incore.features.entropy.EntropyManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public interface PlayerLevelReward {
    void grant(ServerPlayer player);

    String previewText();

    record ItemReward(Item item, int count) implements PlayerLevelReward {
        @Override
        public void grant(ServerPlayer player) {
            if (count <= 0) {
                return;
            }

            int remaining = count;
            while (remaining > 0) {
                int stackSize = Math.min(item.getDefaultMaxStackSize(), remaining);
                ItemStack stack = new ItemStack(item, stackSize);
                boolean added = player.getInventory().add(stack);
                if (!added || !stack.isEmpty()) {
                    player.drop(stack, false);
                }
                remaining -= stackSize;
            }
        }

        @Override
        public String previewText() {
            return count + "x " + BuiltInRegistries.ITEM.getKey(item);
        }
    }

    record EntropyCapBonusReward(int amount) implements PlayerLevelReward {
        @Override
        public void grant(ServerPlayer player) {
            if (amount <= 0) {
                return;
            }

            EntropyManager.addEntropyCapBonus(player, amount);
        }

        @Override
        public String previewText() {
            return "+" + amount + " max entropy";
        }
    }

    record CommandReward(String command, String preview) implements PlayerLevelReward {
        @Override
        public void grant(ServerPlayer player) {
            if (command == null || command.isBlank()) {
                return;
            }

            if (player.getServer() == null) {
                return;
            }

            String commandToRun = command.startsWith("/") ? command.substring(1) : command;
            String resolved = commandToRun.replace("%player%", player.getGameProfile().getName());
            CommandSourceStack source = player.createCommandSourceStack().withPermission(2).withSuppressedOutput();
            player.getServer().getCommands().performPrefixedCommand(source, resolved);
        }

        @Override
        public String previewText() {
            if (preview != null && !preview.isBlank()) {
                return preview;
            }

            return "Custom command reward";
        }
    }
}
