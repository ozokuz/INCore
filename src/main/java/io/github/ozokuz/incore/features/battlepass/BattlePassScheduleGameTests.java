package io.github.ozokuz.incore.features.battlepass;

import io.github.ozokuz.incore.INCore;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.time.Instant;

@GameTestHolder("incore")
@PrefixGameTestTemplate(false)
public final class BattlePassScheduleGameTests {
    private static final ResourceLocation SEASON_ALPHA = ResourceLocation.fromNamespaceAndPath(INCore.MODID, "season_alpha");
    private static final ResourceLocation SEASON_BRAVO = ResourceLocation.fromNamespaceAndPath(INCore.MODID, "season_bravo");

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void schedule_bootstraps_first_ordered_set(GameTestHelper helper) {
        MinecraftServer server = requireServer(helper);
        Instant weekStart = BattlePassWeekTime.weekStart(BattlePassWeekTime.now(server)).toInstant();
        Instant now = weekStart.plusSeconds(3 * 24 * 60 * 60L);

        BattlePassDefinition active = BattlePassManager.getActiveSet(server, now).orElseThrow();
        BattlePassScheduleSavedData data = BattlePassScheduleSavedData.get(server);

        helper.assertValueEqual(SEASON_ALPHA.toString(), active.id().toString(), "expected the lowest ordered battle pass to bootstrap first");
        helper.assertValueEqual(weekStart.toEpochMilli(), active.startsAt().toEpochMilli(), "expected bootstrap start to align to the current week start");
        helper.assertValueEqual(SEASON_ALPHA.toString(), data.activeSetId(), "expected saved schedule to persist the bootstrapped set");
        helper.assertValueEqual(weekStart.toEpochMilli(), data.activeStartEpochMillis(), "expected saved schedule to persist the aligned start");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void schedule_advances_and_wraps_by_duration(GameTestHelper helper) {
        MinecraftServer server = requireServer(helper);
        BattlePassScheduleSavedData data = BattlePassScheduleSavedData.get(server);
        Instant weekStart = BattlePassWeekTime.weekStart(BattlePassWeekTime.now(server)).toInstant();
        // season_alpha and season_bravo are both configured as two-week battle passes in test data.
        long seasonDurationSeconds = 14L * 24L * 60L * 60L;
        data.setActiveSet(SEASON_ALPHA.toString(), weekStart.toEpochMilli());

        BattlePassDefinition advanced = BattlePassManager.getActiveSet(server, weekStart.plusSeconds(seasonDurationSeconds + 3600L)).orElseThrow();
        helper.assertValueEqual(SEASON_BRAVO.toString(), advanced.id().toString(), "expected schedule to advance to the next ordered set after the first duration elapses");
        helper.assertValueEqual(weekStart.plusSeconds(seasonDurationSeconds).toEpochMilli(), advanced.startsAt().toEpochMilli(), "expected next set to start exactly when the prior set ends");

        BattlePassDefinition wrapped = BattlePassManager.getActiveSet(server, weekStart.plusSeconds((seasonDurationSeconds * 2L) + 3600L)).orElseThrow();
        helper.assertValueEqual(SEASON_ALPHA.toString(), wrapped.id().toString(), "expected schedule to wrap back to the first ordered set");
        helper.assertValueEqual(weekStart.plusSeconds(seasonDurationSeconds * 2L).toEpochMilli(), wrapped.startsAt().toEpochMilli(), "expected wrapped schedule to continue from the previous end time");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void manual_set_and_rotate_reset_to_current_week_start(GameTestHelper helper) {
        MinecraftServer server = requireServer(helper);
        Instant baseWeekStart = BattlePassWeekTime.weekStart(BattlePassWeekTime.now(server)).toInstant();
        Instant midWeek = baseWeekStart.plusSeconds(2 * 24 * 60 * 60L);

        BattlePassDefinition manual = BattlePassManager.setForcedSet(server, SEASON_BRAVO, midWeek).orElseThrow();
        helper.assertValueEqual(SEASON_BRAVO.toString(), manual.id().toString(), "expected manual set to select the requested battle pass");
        helper.assertValueEqual(baseWeekStart.toEpochMilli(), manual.startsAt().toEpochMilli(), "expected manual set to align to the current week start");

        Instant nextWeekMid = baseWeekStart.plusSeconds((7L * 24L * 60L * 60L) + (2 * 24L * 60L * 60L));
        BattlePassDefinition rotated = BattlePassManager.rotateForcedSet(server, 1, nextWeekMid).orElseThrow();
        helper.assertValueEqual(SEASON_ALPHA.toString(), rotated.id().toString(), "expected manual rotation to wrap according to configured order");
        helper.assertValueEqual(baseWeekStart.plusSeconds(7L * 24L * 60L * 60L).toEpochMilli(), rotated.startsAt().toEpochMilli(), "expected manual rotation to reset to that week's start");
        helper.succeed();
    }

    private static MinecraftServer requireServer(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        helper.assertTrue(server != null, "expected game test server");
        return server;
    }
}
