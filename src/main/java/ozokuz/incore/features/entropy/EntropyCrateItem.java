package ozokuz.incore.features.entropy;

import ozokuz.incore.Config;
import ozokuz.incore.features.entropy.network.EntropyNetworking;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import java.util.List;

public class EntropyCrateItem extends Item {
    private static final ResourceKey<LootTable> ENTROPY_CRATE_LOOT_TABLE = ResourceKey.create(
            Registries.LOOT_TABLE,
            ResourceLocation.parse("incore:gameplay/entropy_crate")
    );

    public EntropyCrateItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, net.minecraft.world.entity.player.Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }

        if (!(player instanceof ServerPlayer serverPlayer) || serverPlayer.getServer() == null) {
            return InteractionResultHolder.fail(stack);
        }

        int cost = Config.ENTROPY_CRATE_COST.get();
        int currentEntropy = EntropyManager.getCurrentEntropy(serverPlayer);
        int cap = EntropyManager.getEntropyCap(serverPlayer);

        if (currentEntropy < cost || !EntropyManager.tryConsume(serverPlayer, cost)) {
            serverPlayer.sendSystemMessage(Component.translatable("incore.entropy.crate.not_enough", cost, currentEntropy, cap));
            return InteractionResultHolder.fail(stack);
        }

        if (!serverPlayer.isCreative()) {
            stack.shrink(1);
        }

        LootTable lootTable = serverPlayer.getServer().reloadableRegistries().getLootTable(ENTROPY_CRATE_LOOT_TABLE);
        LootParams params = new LootParams.Builder(serverPlayer.serverLevel())
                .withParameter(LootContextParams.ORIGIN, serverPlayer.position())
                .withLuck(serverPlayer.getLuck())
                .create(LootContextParamSets.CHEST);

        List<ItemStack> loot = lootTable.getRandomItems(params);
        for (ItemStack lootStack : loot) {
            if (!serverPlayer.addItem(lootStack)) {
                serverPlayer.drop(lootStack, false);
            }
        }

        int remaining = EntropyManager.getCurrentEntropy(serverPlayer);
        EntropyNetworking.syncToPlayer(serverPlayer);
        serverPlayer.sendSystemMessage(Component.translatable("incore.entropy.crate.opened", cost, remaining, cap));
        return InteractionResultHolder.consume(stack);
    }
}
