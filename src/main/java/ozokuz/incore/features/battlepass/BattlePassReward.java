package ozokuz.incore.features.battlepass;

import ozokuz.incore.features.entropy.EntropyManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.registries.BuiltInRegistries;
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

    record CommandReward(String command, String preview) implements BattlePassReward {
        @Override
        public void grant(ServerPlayer player) {
            if (command == null || command.isBlank()) {
                return;
            }

            if (player.getServer() == null) {
                return;
            }

            String commandToRun = command.startsWith("/") ? command.substring(1) : command;
            String resolved = commandToRun
                    .replace("${player}", player.getGameProfile().getName())
                    .replace("%player%", player.getGameProfile().getName());
            CommandSourceStack source = player.createCommandSourceStack().withPermission(2).withSuppressedOutput();
            player.getServer().getCommands().performPrefixedCommand(source, resolved);
        }

        @Override
        public String previewText() {
            if (preview != null && !preview.isBlank()) {
                return preview;
            }

            return "Battle pass command";
        }
    }

    record EntropyCapBonusReward(int amount) implements BattlePassReward {
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
}
