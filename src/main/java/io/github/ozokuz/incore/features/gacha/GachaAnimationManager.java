package io.github.ozokuz.incore.features.gacha;

import io.github.ozokuz.incore.INCore;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@EventBusSubscriber(modid = INCore.MODID)
public final class GachaAnimationManager {
    private static final int FRAMES_PER_PULL = 4;
    private static final int FINISH_DELAY_TICKS = 10;
    private static final Map<ServerLevel, List<AnimationState>> ACTIVE = new HashMap<>();

    private GachaAnimationManager() {
    }

    public static void start(ServerLevel level, double x, double y, double z, List<Integer> rarities, int bestRarity) {
        if (rarities.isEmpty()) {
            return;
        }

        ACTIVE.computeIfAbsent(level, ignored -> new ArrayList<>())
                .add(new AnimationState(x, y, z, List.copyOf(rarities), bestRarity));
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

            int revealIndex = state.age / FRAMES_PER_PULL;
            if (state.age % FRAMES_PER_PULL == 0 && revealIndex >= 1 && revealIndex <= state.rarities.size()) {
                int rarity = state.rarities.get(revealIndex - 1);
                emitRarityParticles(level, state, rarity);
            }

            int finishTick = state.rarities.size() * FRAMES_PER_PULL + FINISH_DELAY_TICKS;
            if (state.age >= finishTick) {
                emitFinishFirework(level, state);
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

        FireworkRocketEntity firework = new FireworkRocketEntity(level, state.x, state.y + 1.0D, state.z, new ItemStack(Items.FIREWORK_ROCKET));
        level.addFreshEntity(firework);
    }

    private static final class AnimationState {
        private final double x;
        private final double y;
        private final double z;
        private final List<Integer> rarities;
        private final int bestRarity;
        private int age;

        private AnimationState(double x, double y, double z, List<Integer> rarities, int bestRarity) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.rarities = rarities;
            this.bestRarity = bestRarity;
            this.age = 0;
        }
    }
}
