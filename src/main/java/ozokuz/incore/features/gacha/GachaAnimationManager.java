package ozokuz.incore.features.gacha;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.core.component.DataComponents;
import ozokuz.incore.INCore;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.component.Fireworks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

@EventBusSubscriber(modid = INCore.MODID)
public final class GachaAnimationManager {
    private static final int REVEAL_INTERVAL_TICKS = 8;
    private static final int FIREWORK_DELAY_TICKS = 12;
    private static final int REWARD_DROP_DELAY_TICKS = 12;
    private static final Map<ServerLevel, List<AnimationState>> ACTIVE = new HashMap<>();

    private GachaAnimationManager() {
    }

    public static void start(
            ServerLevel level,
            double x,
            double y,
            double z,
            List<Integer> rarities,
            int bestRarity,
            List<ItemStack> rewards,
            List<GachaService.HighRarityReward> highRarityRewards,
            String playerName,
            String bannerName
    ) {
        if (rarities.isEmpty()) {
            completeAnimation(level, x, y, z, rewards, highRarityRewards, playerName, bannerName);
            return;
        }

        List<Integer> sequence = new ArrayList<>(rarities);
        Collections.shuffle(sequence, new Random(level.random.nextLong()));
        List<ItemStack> queuedRewards = rewards.stream().map(ItemStack::copy).toList();
        List<GachaService.HighRarityReward> queuedAnnouncements = highRarityRewards.stream()
                .map(reward -> new GachaService.HighRarityReward(reward.stack().copy(), reward.rarity()))
                .toList();

        ACTIVE.computeIfAbsent(level, ignored -> new ArrayList<>())
                .add(new AnimationState(x, y, z, List.copyOf(sequence), bestRarity, queuedRewards, queuedAnnouncements, playerName, bannerName));
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        List<AnimationState> animations = ACTIVE.get(level);
        if (animations == null || animations.isEmpty()) {
            return;
        }

        Iterator<AnimationState> iterator = animations.iterator();
        while (iterator.hasNext()) {
            AnimationState state = iterator.next();
            state.age++;

            if (!state.fireworkSpawned && state.age % REVEAL_INTERVAL_TICKS == 0 && state.revealIndex < state.rarities.size()) {
                int rarity = state.rarities.get(state.revealIndex++);
                emitRarityParticles(level, state, rarity);
            }

            int revealEndTick = state.rarities.size() * REVEAL_INTERVAL_TICKS;
            if (!state.fireworkSpawned && state.age >= revealEndTick + FIREWORK_DELAY_TICKS) {
                emitFinishFirework(level, state);
                state.fireworkSpawned = true;
                state.fireworkTick = state.age;
            }

            if (state.fireworkSpawned && state.age >= state.fireworkTick + REWARD_DROP_DELAY_TICKS) {
                completeAnimation(level, state.x, state.y, state.z, state.rewards, state.highRarityRewards, state.playerName, state.bannerName);
                iterator.remove();
            }
        }

        if (animations.isEmpty()) {
            ACTIVE.remove(level);
        }
    }

    private static void emitRarityParticles(ServerLevel level, AnimationState state, int rarityValue) {
        GachaRarity rarity = GachaRarity.fromStars(rarityValue);
        DustParticleOptions dust = new DustParticleOptions(rarity.dustColor(), 1.25F);
        level.sendParticles(dust, state.x, state.y + 1.0D, state.z, 20, 0.30D, 0.25D, 0.30D, 0.01D);
        level.playSound(
                null,
                state.x,
                state.y,
                state.z,
                SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.BLOCKS,
                0.65F,
                0.9F + (rarity.stars() * 0.05F)
        );
    }

    private static void emitFinishFirework(ServerLevel level, AnimationState state) {
        GachaRarity best = GachaRarity.fromStars(state.bestRarity);
        DustParticleOptions burst = new DustParticleOptions(best.dustColor(), 1.9F);
        level.sendParticles(burst, state.x, state.y + 1.2D, state.z, 40, 0.35D, 0.30D, 0.35D, 0.02D);

        ItemStack rocketStack = new ItemStack(Items.FIREWORK_ROCKET);
        FireworkExplosion explosion = new FireworkExplosion(
                FireworkExplosion.Shape.BURST,
                new IntArrayList(new int[]{best.rgb()}),
                new IntArrayList(),
                true,
                true
        );
        rocketStack.set(DataComponents.FIREWORKS, new Fireworks(0, List.of(explosion)));

        FireworkRocketEntity firework = new FireworkRocketEntity(level, state.x, state.y + 1.0D, state.z, rocketStack);
        level.addFreshEntity(firework);
    }

    private static void dropRewards(ServerLevel level, double x, double y, double z, List<ItemStack> rewards) {
        for (ItemStack reward : rewards) {
            if (reward.isEmpty()) {
                continue;
            }
            ItemEntity itemEntity = new ItemEntity(level, x, y + 0.2D, z, reward.copy());
            itemEntity.setDefaultPickUpDelay();
            level.addFreshEntity(itemEntity);
        }
    }

    private static void completeAnimation(
            ServerLevel level,
            double x,
            double y,
            double z,
            List<ItemStack> rewards,
            List<GachaService.HighRarityReward> highRarityRewards,
            String playerName,
            String bannerName
    ) {
        dropRewards(level, x, y, z, rewards);
        broadcastHighRarity(level, highRarityRewards, playerName, bannerName);
    }

    private static void broadcastHighRarity(
            ServerLevel level,
            List<GachaService.HighRarityReward> rewards,
            String playerName,
            String bannerName
    ) {
        if (rewards.isEmpty() || level.getServer() == null) {
            return;
        }

        for (GachaService.HighRarityReward reward : rewards) {
            if (reward.stack().isEmpty()) {
                continue;
            }

            ChatFormatting rarityColor = reward.rarity() >= 6 ? ChatFormatting.RED : ChatFormatting.GOLD;
            MutableComponent message = Component.empty()
                    .append(Component.literal(playerName).withStyle(ChatFormatting.AQUA))
                    .append(Component.literal(" pulled ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(reward.rarity() + "★").withStyle(rarityColor))
                    .append(Component.literal(" ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(reward.stack().getHoverName().getString()).withStyle(rarityColor))
                    .append(Component.literal(" x").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(String.valueOf(reward.stack().getCount())).withStyle(ChatFormatting.WHITE))
                    .append(Component.literal(" from ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(bannerName).withStyle(ChatFormatting.YELLOW))
                    .append(Component.literal(" crate!").withStyle(ChatFormatting.GRAY));

            level.getServer().getPlayerList().broadcastSystemMessage(message, false);
        }
    }

    private static final class AnimationState {
        private final double x;
        private final double y;
        private final double z;
        private final List<Integer> rarities;
        private final int bestRarity;
        private final List<ItemStack> rewards;
        private final List<GachaService.HighRarityReward> highRarityRewards;
        private final String playerName;
        private final String bannerName;
        private int revealIndex;
        private boolean fireworkSpawned;
        private int fireworkTick;
        private int age;

        private AnimationState(
                double x,
                double y,
                double z,
                List<Integer> rarities,
                int bestRarity,
                List<ItemStack> rewards,
                List<GachaService.HighRarityReward> highRarityRewards,
                String playerName,
                String bannerName
        ) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.rarities = rarities;
            this.bestRarity = bestRarity;
            this.rewards = rewards;
            this.highRarityRewards = highRarityRewards;
            this.playerName = playerName;
            this.bannerName = bannerName;
            this.revealIndex = 0;
            this.fireworkSpawned = false;
            this.fireworkTick = 0;
            this.age = 0;
        }
    }
}
