package io.github.ozokuz.incore.features.assembly.content;

import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import io.github.ozokuz.incore.INCore;
import io.github.ozokuz.incore.Registration;
import io.github.ozokuz.incore.features.researchv2.ResearchManager;
import io.github.ozokuz.incore.features.researchv2.state.ResearchNetworkSavedData;
import io.github.ozokuz.incore.features.researchv2.state.TeamResearchState;
import io.github.ozokuz.incore.features.researchv2.team.ResearchTeamResolver;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.UUID;

@GameTestHolder("incore")
@PrefixGameTestTemplate(false)
public final class AssemblyGameTests {
    private static final ResourceLocation SIGNAL_CALIBRATION = ResourceLocation.fromNamespaceAndPath(INCore.MODID, "signal_calibration");
    private static final ResourceLocation RELAY_PROTOCOLS = ResourceLocation.fromNamespaceAndPath(INCore.MODID, "relay_protocols");
    private static final ResourceLocation TERRAIN_SCANNING = ResourceLocation.fromNamespaceAndPath(INCore.MODID, "terrain_scanning");

    private static final ResourceLocation T1_RECIPE = ResourceLocation.fromNamespaceAndPath(INCore.MODID, "assembly_field_sensor");
    private static final ResourceLocation T2_RECIPE = ResourceLocation.fromNamespaceAndPath(INCore.MODID, "assembly_signal_router");
    private static final ResourceLocation T3_RECIPE = ResourceLocation.fromNamespaceAndPath(INCore.MODID, "assembly_precision_latch");

    private AssemblyGameTests() {
    }

    @GameTest(template = "empty", timeoutTicks = 60)
    public static void station_gates_visibility_and_manual_crafting(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, Registration.ASSEMBLY_STATION_BLOCK.get());
        AssemblyStationBlockEntity station = requireBlockEntity(helper, pos, AssemblyStationBlockEntity.class);
        setT1Inputs(station.itemHandler(), 1);

        ServerPlayer player = fakePlayer(helper, "assembly_station");
        movePlayer(helper, player, pos);
        MinecraftServer server = helper.getLevel().getServer();
        helper.assertTrue(server != null, "expected test server");
        String teamId = ResearchTeamResolver.resolveTeamId(player);
        helper.assertTrue(teamId != null, "expected team id");
        ResearchManager.clearResearch(server, teamId);

        helper.assertFalse(
                AssemblyRecipeUtil.unlockedRecipeIds(server, teamId, helper.getLevel().getRecipeManager()).contains(T1_RECIPE.toString()),
                "locked assembly recipe should not be visible before research"
        );
        helper.assertFalse(station.tryCraft(player, T1_RECIPE), "station should reject locked assembly recipes");

        markResearched(server, teamId, SIGNAL_CALIBRATION);
        helper.assertTrue(
                AssemblyRecipeUtil.unlockedRecipeIds(server, teamId, helper.getLevel().getRecipeManager()).contains(T1_RECIPE.toString()),
                "researched assembly recipe should become visible"
        );
        helper.assertTrue(station.tryCraft(player, T1_RECIPE), "station should craft unlocked assembly recipes");
        helper.assertTrue(station.itemHandler().getStackInSlot(AssemblyStationBlockEntity.OUTPUT_SLOT).is(Items.OBSERVER), "expected observer output");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 60)
    public static void auto_assembler_tiers_and_outcomes_are_distinct(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        helper.assertTrue(server != null, "expected test server");
        String teamId = ResearchTeamResolver.resolveTeamId(fakePlayer(helper, "auto_assembler"));
        helper.assertTrue(teamId != null, "expected team id");
        markResearched(server, teamId, SIGNAL_CALIBRATION);
        markResearched(server, teamId, RELAY_PROTOCOLS);
        markResearched(server, teamId, TERRAIN_SCANNING);

        BlockEntity anchorA = placeAnchor(helper, new BlockPos(1, 1, 1));
        BlockEntity anchorB = placeAnchor(helper, new BlockPos(2, 1, 1));
        BlockEntity anchorC = placeAnchor(helper, new BlockPos(3, 1, 1));
        BlockEntity anchorD = placeAnchor(helper, new BlockPos(4, 1, 1));
        BlockEntity anchorE = placeAnchor(helper, new BlockPos(5, 1, 1));

        AutoAssemblerSharedState tierBlockedT1 = new AutoAssemblerSharedState();
        tierBlockedT1.setSelectedRecipeId(T2_RECIPE);
        tierBlockedT1.setTeamId(teamId);
        setT2Inputs(tierBlockedT1.items(), 1);
        AutoAssemblerMachineLogic.tick(helper.getLevel(), anchorA, tierBlockedT1, 1, true, () -> {});
        helper.assertValueEqual(AutoAssemblerSharedState.STATUS_TIER_BLOCKED, tierBlockedT1.status(), "tier 1 machine should block tier 2 recipes");

        AutoAssemblerSharedState tierBlockedT2 = new AutoAssemblerSharedState();
        tierBlockedT2.setSelectedRecipeId(T3_RECIPE);
        tierBlockedT2.setTeamId(teamId);
        setT3Inputs(tierBlockedT2.items(), 1);
        AutoAssemblerMachineLogic.tick(helper.getLevel(), anchorB, tierBlockedT2, 2, true, () -> {});
        helper.assertValueEqual(AutoAssemblerSharedState.STATUS_TIER_BLOCKED, tierBlockedT2.status(), "tier 2 machine should block tier 3 recipes");

        AutoAssemblerSharedState t1State = new AutoAssemblerSharedState();
        t1State.setSelectedRecipeId(T1_RECIPE);
        t1State.setTeamId(teamId);
        setT1Inputs(t1State.items(), 64);
        runMachineTicks(helper, anchorC, t1State, 1, 100);
        helper.assertValueEqual(20, t1State.attempts(), "tier 1 machine should complete 20 attempts");
        helper.assertValueEqual(20, t1State.tier1Failures(), "tier 1 machine should log failure attempts");
        helper.assertValueEqual(0, t1State.successes(), "tier 1 recipe is configured to fail in tests");
        helper.assertValueEqual(20, countOutputs(t1State, Items.GRAVEL), "tier 1 failures should emit gravel");

        AutoAssemblerSharedState t2State = new AutoAssemblerSharedState();
        t2State.setSelectedRecipeId(T2_RECIPE);
        t2State.setTeamId(teamId);
        setT2Inputs(t2State.items(), 64);
        runMachineTicks(helper, anchorD, t2State, 2, 100);
        helper.assertValueEqual(20, t2State.attempts(), "tier 2 machine should complete 20 attempts");
        helper.assertValueEqual(20, t2State.tier2Failures(), "tier 2 machine should log failure attempts");
        helper.assertValueEqual(0, t2State.successes(), "tier 2 recipe is configured to fail in tests");
        helper.assertValueEqual(40, countOutputs(t2State, Items.IRON_NUGGET), "tier 2 failures should emit faulty output");
        helper.assertValueEqual(20, countOutputs(t2State, Items.REDSTONE), "tier 2 failures should emit recyclables");

        AutoAssemblerSharedState t3State = new AutoAssemblerSharedState();
        t3State.setSelectedRecipeId(T3_RECIPE);
        t3State.setTeamId(teamId);
        setT3Inputs(t3State.items(), 64);
        runMachineTicks(helper, anchorE, t3State, 3, 100);
        helper.assertValueEqual(20, t3State.attempts(), "tier 3 machine should complete 20 attempts");
        helper.assertValueEqual(20, t3State.successes(), "tier 3 machine should always succeed");
        helper.assertValueEqual(0, t3State.tier1Failures(), "tier 3 machine should not log tier 1 failures");
        helper.assertValueEqual(0, t3State.tier2Failures(), "tier 3 machine should not log tier 2 failures");
        helper.assertValueEqual(20, countOutputs(t3State, Items.REPEATER), "tier 3 machine should produce the main result");
        helper.assertValueEqual(40, countOutputs(t3State, Items.GOLD_NUGGET), "tier 3 machine should emit leftovers");
        helper.assertValueEqual(20, t3State.leftoverEmits(), "tier 3 machine should count leftover emissions");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void output_full_stalls_without_consuming_inputs(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        helper.assertTrue(server != null, "expected test server");
        String teamId = ResearchTeamResolver.resolveTeamId(fakePlayer(helper, "output_full"));
        helper.assertTrue(teamId != null, "expected team id");
        markResearched(server, teamId, SIGNAL_CALIBRATION);

        BlockEntity anchor = placeAnchor(helper, new BlockPos(1, 1, 1));
        AutoAssemblerSharedState state = new AutoAssemblerSharedState();
        state.setSelectedRecipeId(T1_RECIPE);
        state.setTeamId(teamId);
        setT1Inputs(state.items(), 5);
        for (int slot = AutoAssemblerSharedState.OUTPUT_START; slot < AutoAssemblerSharedState.OUTPUT_START + AutoAssemblerSharedState.OUTPUT_COUNT; slot++) {
            state.items().setStackInSlot(slot, new ItemStack(Items.COBBLESTONE, 64));
        }

        int redstoneBefore = state.items().getStackInSlot(0).getCount();
        int ironBefore = state.items().getStackInSlot(4).getCount();
        runMachineTicks(helper, anchor, state, 1, 5);
        helper.assertValueEqual(AutoAssemblerSharedState.STATUS_OUTPUT_FULL, state.status(), "full outputs should stall assembly");
        helper.assertValueEqual(0, state.attempts(), "full outputs should prevent craft completion");
        helper.assertValueEqual(redstoneBefore, state.items().getStackInSlot(0).getCount(), "stalled craft should not consume redstone");
        helper.assertValueEqual(ironBefore, state.items().getStackInSlot(4).getCount(), "stalled craft should not consume iron");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void capabilities_follow_side_and_power_rules(GameTestHelper helper) {
        BlockPos t1Pos = new BlockPos(1, 1, 1);
        BlockPos t2Pos = new BlockPos(3, 1, 1);
        BlockPos t3Pos = new BlockPos(5, 1, 1);

        helper.setBlock(t1Pos, Registration.AUTO_ASSEMBLER_T1_BLOCK.get().defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH));
        helper.setBlock(t2Pos, Registration.AUTO_ASSEMBLER_T2_BLOCK.get().defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH));
        helper.setBlock(t3Pos, Registration.AUTO_ASSEMBLER_T3_BLOCK.get().defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH));

        AutoAssemblerT1BlockEntity t1 = requireBlockEntity(helper, t1Pos, AutoAssemblerT1BlockEntity.class);
        t1.itemHandler().setStackInSlot(AutoAssemblerSharedState.OUTPUT_START, new ItemStack(Items.GRAVEL, 4));

        IItemHandler frontItems = itemCapability(helper, t1Pos, Direction.NORTH);
        IItemHandler sideItems = itemCapability(helper, t1Pos, Direction.EAST);
        helper.assertTrue(frontItems != null, "expected front item capability on t1");
        helper.assertTrue(sideItems != null, "expected side item capability on t1");
        helper.assertTrue(frontItems.insertItem(0, new ItemStack(Items.IRON_INGOT), true).is(Items.IRON_INGOT), "front should reject inserts");
        helper.assertTrue(frontItems.extractItem(0, 1, true).is(Items.GRAVEL), "front should extract outputs");
        helper.assertTrue(sideItems.insertItem(0, new ItemStack(Items.IRON_INGOT), false).isEmpty(), "side should insert inputs");
        helper.assertTrue(sideItems.extractItem(0, 1, true).isEmpty(), "side should reject extraction");

        IEnergyStorage t1Energy = energyCapability(helper, t1Pos, Direction.NORTH);
        IEnergyStorage t2FrontEnergy = energyCapability(helper, t2Pos, Direction.NORTH);
        IEnergyStorage t2SideEnergy = energyCapability(helper, t2Pos, Direction.EAST);
        IEnergyStorage t3FrontEnergy = energyCapability(helper, t3Pos, Direction.NORTH);
        IEnergyStorage t3SideEnergy = energyCapability(helper, t3Pos, Direction.EAST);
        helper.assertTrue(t1Energy == null, "tier 1 assembler should not expose FE");
        helper.assertTrue(t2FrontEnergy != null, "tier 2 assembler should expose FE on the front");
        helper.assertTrue(t2SideEnergy == null, "tier 2 assembler should not expose FE on the sides");
        helper.assertTrue(t3FrontEnergy != null, "tier 3 assembler should expose FE on the front");
        helper.assertTrue(t3SideEnergy == null, "tier 3 assembler should not expose FE on the sides");

        helper.assertTrue(
                Registration.AUTO_ASSEMBLER_T1_BLOCK.get() instanceof HorizontalKineticBlock kineticBlock
                        && kineticBlock.hasShaftTowards(helper.getLevel(), helper.absolutePos(t1Pos), helper.getBlockState(t1Pos), Direction.SOUTH),
                "tier 1 assembler should expose a rear shaft"
        );
        helper.assertFalse(Registration.AUTO_ASSEMBLER_T2_BLOCK.get() instanceof HorizontalKineticBlock, "tier 2 assembler should not expose kinetic behavior");
        helper.assertFalse(Registration.AUTO_ASSEMBLER_T3_BLOCK.get() instanceof HorizontalKineticBlock, "tier 3 assembler should not expose kinetic behavior");
        helper.succeed();
    }

    private static BlockEntity placeAnchor(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, Registration.ASSEMBLY_STATION_BLOCK.get());
        return helper.getBlockEntity(pos);
    }

    private static void runMachineTicks(GameTestHelper helper, BlockEntity anchor, AutoAssemblerSharedState state, int machineTier, int ticks) {
        for (int i = 0; i < ticks; i++) {
            AutoAssemblerMachineLogic.tick(helper.getLevel(), anchor, state, machineTier, true, () -> {});
        }
    }

    private static int countOutputs(AutoAssemblerSharedState state, net.minecraft.world.item.Item item) {
        int count = 0;
        for (int slot = AutoAssemblerSharedState.OUTPUT_START; slot < AutoAssemblerSharedState.OUTPUT_START + AutoAssemblerSharedState.OUTPUT_COUNT; slot++) {
            ItemStack stack = state.items().getStackInSlot(slot);
            if (stack.is(item)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static void setT1Inputs(net.neoforged.neoforge.items.ItemStackHandler handler, int countPerSlot) {
        handler.setStackInSlot(0, new ItemStack(Items.REDSTONE, countPerSlot));
        handler.setStackInSlot(1, new ItemStack(Items.GLASS, countPerSlot));
        handler.setStackInSlot(3, new ItemStack(Items.GLASS, countPerSlot));
        handler.setStackInSlot(4, new ItemStack(Items.IRON_INGOT, countPerSlot));
    }

    private static void setT2Inputs(net.neoforged.neoforge.items.ItemStackHandler handler, int countPerSlot) {
        handler.setStackInSlot(0, new ItemStack(Items.REDSTONE_TORCH, countPerSlot));
        handler.setStackInSlot(1, new ItemStack(Items.QUARTZ, countPerSlot));
        handler.setStackInSlot(2, new ItemStack(Items.STONE, countPerSlot));
    }

    private static void setT3Inputs(net.neoforged.neoforge.items.ItemStackHandler handler, int countPerSlot) {
        handler.setStackInSlot(0, new ItemStack(Items.IRON_INGOT, countPerSlot));
        handler.setStackInSlot(1, new ItemStack(Items.GOLD_INGOT, countPerSlot));
        handler.setStackInSlot(2, new ItemStack(Items.IRON_INGOT, countPerSlot));
        handler.setStackInSlot(3, new ItemStack(Items.REDSTONE, countPerSlot));
        handler.setStackInSlot(4, new ItemStack(Items.QUARTZ, countPerSlot));
        handler.setStackInSlot(5, new ItemStack(Items.REDSTONE, countPerSlot));
        handler.setStackInSlot(6, new ItemStack(Items.IRON_INGOT, countPerSlot));
        handler.setStackInSlot(7, new ItemStack(Items.SMOOTH_STONE, countPerSlot));
        handler.setStackInSlot(8, new ItemStack(Items.IRON_INGOT, countPerSlot));
    }

    private static void markResearched(MinecraftServer server, String teamId, ResourceLocation nodeId) {
        TeamResearchState state = ResearchManager.ensureTeamState(server, teamId);
        state.discoveredNodes().add(nodeId);
        state.completedNodes().add(nodeId);
        ResearchNetworkSavedData.get(server).setDirty();
    }

    private static void movePlayer(GameTestHelper helper, ServerPlayer player, BlockPos pos) {
        BlockPos absolute = helper.absolutePos(pos);
        player.teleportTo(helper.getLevel(), absolute.getX() + 0.5D, absolute.getY() + 1.0D, absolute.getZ() + 0.5D, 0.0F, 0.0F);
    }

    private static ServerPlayer fakePlayer(GameTestHelper helper, String name) {
        return FakePlayerFactory.get(
                helper.getLevel(),
                new com.mojang.authlib.GameProfile(
                        UUID.randomUUID(),
                        name
                )
        );
    }

    private static IItemHandler itemCapability(GameTestHelper helper, BlockPos pos, Direction side) {
        return BlockCapabilityCache.create(Capabilities.ItemHandler.BLOCK, helper.getLevel(), helper.absolutePos(pos), side).getCapability();
    }

    private static IEnergyStorage energyCapability(GameTestHelper helper, BlockPos pos, Direction side) {
        return BlockCapabilityCache.create(Capabilities.EnergyStorage.BLOCK, helper.getLevel(), helper.absolutePos(pos), side).getCapability();
    }

    private static <T> T requireBlockEntity(GameTestHelper helper, BlockPos pos, Class<T> type) {
        Object blockEntity = helper.getBlockEntity(pos);
        helper.assertTrue(type.isInstance(blockEntity), "expected block entity " + type.getSimpleName() + " at " + pos);
        return type.cast(blockEntity);
    }
}
