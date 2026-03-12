package io.github.ozokuz.incore.features.assembly.content;

import io.github.ozokuz.incore.Config;
import io.github.ozokuz.incore.INCore;
import io.github.ozokuz.incore.features.assembly.recipe.AssemblyRecipe;
import io.github.ozokuz.incore.features.researchv2.team.ResearchTeamResolver;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;

public final class AutoAssemblerMachineLogic {
    private AutoAssemblerMachineLogic() {
    }

    public static void tick(Level level, net.minecraft.world.level.block.entity.BlockEntity blockEntity, AutoAssemblerSharedState state, int machineTier, boolean hasPower, Runnable consumePower) {
        if (level.isClientSide) {
            return;
        }
        state.setMachineTier(machineTier);
        ResourceLocation selectedRecipeId = state.selectedRecipeId();
        if (selectedRecipeId == null) {
            state.setProgressTicks(0);
            state.setStatus(AutoAssemblerSharedState.STATUS_NO_RECIPE);
            blockEntity.setChanged();
            return;
        }
        if (!hasPower) {
            state.setProgressTicks(0);
            state.setStatus(AutoAssemblerSharedState.STATUS_NO_POWER);
            blockEntity.setChanged();
            return;
        }
        var holder = AssemblyRecipeUtil.findRecipeHolder(level.getRecipeManager(), selectedRecipeId);
        if (holder == null) {
            state.setProgressTicks(0);
            state.setStatus(AutoAssemblerSharedState.STATUS_NO_RECIPE);
            blockEntity.setChanged();
            return;
        }
        AssemblyRecipe recipe = holder.value();
        if (recipe.tier() > machineTier) {
            state.setProgressTicks(0);
            state.setStatus(AutoAssemblerSharedState.STATUS_TIER_BLOCKED);
            blockEntity.setChanged();
            return;
        }
        String teamId = state.teamId();
        if (teamId == null || teamId.isBlank()) {
            teamId = resolveTeamId(level, blockEntity.getBlockPos());
        }
        if (teamId == null || !AssemblyRecipeUtil.isUnlocked(level.getServer(), teamId, holder.id())) {
            state.setProgressTicks(0);
            state.setStatus(AutoAssemblerSharedState.STATUS_LOCKED);
            blockEntity.setChanged();
            return;
        }
        var input = AssemblyRecipeUtil.craftingInput(state.items(), AutoAssemblerSharedState.INPUT_START);
        if (!recipe.matches(input, level)) {
            state.setProgressTicks(0);
            state.setStatus(AutoAssemblerSharedState.STATUS_NO_INPUT);
            blockEntity.setChanged();
            return;
        }
        List<Integer> consumedSlots = AssemblyRecipeUtil.consumedSlots(recipe, state.items(), AutoAssemblerSharedState.INPUT_START);
        if (consumedSlots.isEmpty()) {
            state.setProgressTicks(0);
            state.setStatus(AutoAssemblerSharedState.STATUS_NO_INPUT);
            blockEntity.setChanged();
            return;
        }
        state.setMaxProgressTicks(recipe.craftTimeTicks());
        int nextProgress = state.progressTicks() + 1;
        if (nextProgress < state.maxProgressTicks()) {
            consumePower.run();
            state.setProgressTicks(nextProgress);
            state.setStatus(AutoAssemblerSharedState.STATUS_IDLE);
            blockEntity.setChanged();
            return;
        }
        RandomSource random = RandomSource.create(((long) blockEntity.getBlockPos().asLong() << 32) ^ state.attempts());
        var outcome = AssemblyCraftingLogic.resolveAutoOutcome(recipe, machineTier, level.registryAccess(), random);
        if (!AssemblyRecipeUtil.canFitOutputs(state.items(), AutoAssemblerSharedState.OUTPUT_START, AutoAssemblerSharedState.OUTPUT_COUNT, outcome.outputs())) {
            state.setProgressTicks(0);
            state.setStatus(AutoAssemblerSharedState.STATUS_OUTPUT_FULL);
            blockEntity.setChanged();
            return;
        }
        consumePower.run();
        state.incrementAttempts();
        AssemblyRecipeUtil.consumeSlots(state.items(), consumedSlots);
        AssemblyRecipeUtil.insertOutputs(state.items(), AutoAssemblerSharedState.OUTPUT_START, AutoAssemblerSharedState.OUTPUT_COUNT, outcome.outputs());
        if (outcome.success()) {
            state.incrementSuccesses();
            if (machineTier >= 3) {
                int leftovers = Math.max(0, outcome.outputs().size() - 1);
                state.addLeftoverEmits(leftovers);
            }
        } else if (machineTier == 1) {
            state.incrementTier1Failures();
        } else if (machineTier == 2) {
            state.incrementTier2Failures();
        }
        state.setProgressTicks(0);
        state.setStatus(AutoAssemblerSharedState.STATUS_IDLE);
        if (Config.ASSEMBLY_DEBUG_LOGGING.get() && state.attempts() % 20 == 0) {
            INCore.LOGGER.info(
                    "[Assembly] pos={} tier={} recipe={} attempts={} successes={} t1fail={} t2fail={} leftovers={} outputs={}",
                    blockEntity.getBlockPos(),
                    machineTier,
                    holder.id(),
                    state.attempts(),
                    state.successes(),
                    state.tier1Failures(),
                    state.tier2Failures(),
                    state.leftoverEmits(),
                    outcome.outputs().stream().map(stack -> stack.getItem().toString()).toList()
            );
        }
        blockEntity.setChanged();
    }

    private static String resolveTeamId(Level level, net.minecraft.core.BlockPos pos) {
        if (level.getServer() == null) {
            return null;
        }
        Player player = level.getNearestPlayer(pos.getX(), pos.getY(), pos.getZ(), 16.0D, false);
        if (player instanceof ServerPlayer serverPlayer) {
            return ResearchTeamResolver.resolveTeamId(serverPlayer);
        }
        return level.players().stream()
                .filter(ServerPlayer.class::isInstance)
                .map(ServerPlayer.class::cast)
                .findFirst()
                .map(ResearchTeamResolver::resolveTeamId)
                .orElse(null);
    }
}
