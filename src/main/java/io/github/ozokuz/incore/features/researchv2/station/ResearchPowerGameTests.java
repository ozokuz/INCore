package io.github.ozokuz.incore.features.researchv2.station;

import io.github.ozokuz.incore.INCore;
import io.github.ozokuz.incore.Registration;
import io.github.ozokuz.incore.features.researchv2.ResearchManager;
import io.github.ozokuz.incore.features.researchv2.state.ResearchNetworkSavedData;
import io.github.ozokuz.incore.features.researchv2.state.TeamResearchState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("incore")
@PrefixGameTestTemplate(false)
public final class ResearchPowerGameTests {
    private static final ResourceLocation SIGNAL_CALIBRATION = ResourceLocation.fromNamespaceAndPath(INCore.MODID, "signal_calibration");
    private static final ResourceLocation TERRAIN_SCANNING = ResourceLocation.fromNamespaceAndPath(INCore.MODID, "terrain_scanning");

    private ResearchPowerGameTests() {
    }

    @GameTest(template = "empty", timeoutTicks = 120)
    public static void electric_core_fills_controller_buffer(GameTestHelper helper) {
        BuiltStation station = buildStation(helper, Registration.RESEARCH_CONTROLLER_T1_BLOCK.get(), Registration.ELECTRIC_POWER_INPUT_BLOCK.get());
        ResearchControllerBlockEntity controller = bindController(helper, station.controllerPos(), "phase6_electric_fill");
        ElectricPowerInputBlockEntity input = requireBlockEntity(helper, station.inputPos(), ElectricPowerInputBlockEntity.class);
        chargeElectricInput(input, 4_000);

        helper.succeedWhen(() -> helper.assertTrue(controller.rpBuffer() > 0, "expected RP buffer to increase from FE input"));
    }

    @GameTest(template = "empty", timeoutTicks = 900)
    public static void electric_core_advances_research(GameTestHelper helper) {
        BuiltStation station = buildStation(helper, Registration.RESEARCH_CONTROLLER_T1_BLOCK.get(), Registration.ELECTRIC_POWER_INPUT_BLOCK.get());
        ResearchControllerBlockEntity controller = bindController(helper, station.controllerPos(), "phase6_research_progress");
        ElectricPowerInputBlockEntity input = requireBlockEntity(helper, station.inputPos(), ElectricPowerInputBlockEntity.class);
        chargeElectricInput(input, 60_000);

        MinecraftServer server = helper.getLevel().getServer();
        helper.assertTrue(server != null, "expected game test server");
        TeamResearchState state = ResearchManager.ensureTeamState(server, controller.teamId());
        state.discoveredNodes().add(SIGNAL_CALIBRATION);
        state.devLogicModules().put("basic", 3);
        state.devResearchMaterials().put("incore:starter_data", 3);
        ResearchNetworkSavedData.get(server).setDirty();
        helper.runAfterDelay(5, () -> {
            boolean alreadyQueued = state.researchQueue().stream().anyMatch(entry -> SIGNAL_CALIBRATION.equals(entry.nodeId()));
            if (!alreadyQueued) {
                helper.assertTrue(
                        ResearchManager.queueResearch(server, controller.teamId(), SIGNAL_CALIBRATION),
                        "expected signal_calibration to queue: " + ResearchManager.explainQueueFailure(server, controller.teamId(), SIGNAL_CALIBRATION)
                );
            }
        });

        helper.succeedWhen(() -> helper.assertTrue(
                ResearchManager.isResearched(server, controller.teamId(), SIGNAL_CALIBRATION),
                "expected signal_calibration to complete using electric power"
        ));
    }

    @GameTest(template = "empty", timeoutTicks = 120)
    public static void controller_tier_changes_capacity_and_category_gate(GameTestHelper helper) {
        BuiltStation station = buildStation(helper, Registration.RESEARCH_CONTROLLER_T1_BLOCK.get(), Registration.ELECTRIC_POWER_INPUT_BLOCK.get());
        ResearchControllerBlockEntity controllerT1 = bindController(helper, station.controllerPos(), "phase6_tier_swap");
        int t1Capacity = controllerT1.rpCapacity();

        MinecraftServer server = helper.getLevel().getServer();
        helper.assertTrue(server != null, "expected game test server");
        TeamResearchState state = ResearchManager.ensureTeamState(server, controllerT1.teamId());
        state.discoveredNodes().add(TERRAIN_SCANNING);
        ResearchNetworkSavedData.get(server).setDirty();
        helper.assertFalse(ResearchManager.canQueue(server, controllerT1.teamId(), TERRAIN_SCANNING), "tier 1 controller should not allow expedition node queueing");

        helper.setBlock(station.controllerPos(), Registration.RESEARCH_CONTROLLER_T3_BLOCK.get());
        ResearchControllerBlockEntity controllerT3 = bindController(helper, station.controllerPos(), controllerT1.teamId());

        helper.assertTrue(controllerT3.rpCapacity() > t1Capacity, "tier 3 controller should have a larger RP buffer");
        helper.assertTrue(ResearchManager.canQueue(server, controllerT3.teamId(), TERRAIN_SCANNING), "tier 3 controller should allow expedition node queueing");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 120)
    public static void buffer_full_does_not_consume_fe(GameTestHelper helper) {
        BuiltStation station = buildStation(helper, Registration.RESEARCH_CONTROLLER_T1_BLOCK.get(), Registration.ELECTRIC_POWER_INPUT_BLOCK.get());
        ResearchControllerBlockEntity controller = bindController(helper, station.controllerPos(), "phase6_fe_full");
        ElectricPowerInputBlockEntity input = requireBlockEntity(helper, station.inputPos(), ElectricPowerInputBlockEntity.class);
        chargeElectricInput(input, 2_000);
        controller.addResearchPower(controller.rpCapacity());
        int before = input.energyStored();

        helper.runAfterDelay(5, () -> {
            helper.assertValueEqual(before, input.energyStored(), "expected FE input to remain unchanged while controller buffer is full");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 120)
    public static void buffer_full_does_not_consume_burnables(GameTestHelper helper) {
        BuiltStation station = buildStation(helper, Registration.RESEARCH_CONTROLLER_T1_BLOCK.get(), Registration.BURNER_POWER_INPUT_BLOCK.get());
        ResearchControllerBlockEntity controller = bindController(helper, station.controllerPos(), "phase6_burner_full");
        BurnerPowerInputBlockEntity input = requireBlockEntity(helper, station.inputPos(), BurnerPowerInputBlockEntity.class);
        input.itemHandler().insertItem(0, new ItemStack(Items.COAL, 1), false);
        controller.addResearchPower(controller.rpCapacity());

        helper.runAfterDelay(5, () -> {
            helper.assertValueEqual(1, input.itemHandler().getStackInSlot(0).getCount(), "expected burnables to remain untouched while controller buffer is full");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void mixed_power_family_station_is_invalid(GameTestHelper helper) {
        BuiltStation station = buildStation(helper, Registration.RESEARCH_CONTROLLER_T1_BLOCK.get(), Registration.ELECTRIC_POWER_INPUT_BLOCK.get());
        helper.setBlock(station.extraPos(), Registration.BURNER_POWER_INPUT_BLOCK.get());
        ResearchControllerBlockEntity controller = bindController(helper, station.controllerPos(), "phase6_mixed_family");
        controller.revalidateStructure();
        helper.assertFalse(controller.isFormed(), "station should be invalid when input families are mixed");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void station_requires_single_power_input_type(GameTestHelper helper) {
        BuiltStation station = fillCasingShell(helper);
        helper.setBlock(station.controllerPos(), Registration.RESEARCH_CONTROLLER_T1_BLOCK.get());
        helper.setBlock(station.inputPos(), Registration.BURNER_POWER_INPUT_BLOCK.get());
        helper.setBlock(station.extraPos(), Registration.ELECTRIC_POWER_INPUT_BLOCK.get());
        ResearchControllerBlockEntity controller = bindController(helper, station.controllerPos(), "phase6_input_type");
        controller.revalidateStructure();
        helper.assertFalse(controller.isFormed(), "station should be invalid with mixed power input block types");

        helper.setBlock(station.extraPos(), Registration.BURNER_POWER_INPUT_BLOCK.get());
        controller = bindController(helper, station.controllerPos(), controller.teamId());
        controller.revalidateStructure();
        helper.assertTrue(controller.isFormed(), "station should allow multiple inputs when they are the same block type");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void station_requires_one_or_more_inputs(GameTestHelper helper) {
        BuiltStation station = fillCasingShell(helper);
        helper.setBlock(station.controllerPos(), Registration.RESEARCH_CONTROLLER_T1_BLOCK.get());
        ResearchControllerBlockEntity controller = bindController(helper, station.controllerPos(), "phase6_input_count");
        controller.revalidateStructure();
        helper.assertFalse(controller.isFormed(), "station should be invalid without a power input");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 160)
    public static void mechanical_input_requires_operational_create_network(GameTestHelper helper) {
        BuiltStation station = buildStation(helper, Registration.RESEARCH_CONTROLLER_T1_BLOCK.get(), Registration.MECHANICAL_POWER_INPUT_BLOCK.get());
        ResearchControllerBlockEntity controller = bindController(helper, station.controllerPos(), "phase6_mechanical");
        MechanicalPowerInputBlockEntity input = requireBlockEntity(helper, station.inputPos(), MechanicalPowerInputBlockEntity.class);

        input.setSpeed(32.0F);
        input.updateFromNetwork(0.0F, 16.0F, 1);
        helper.runAfterDelay(5, () -> helper.assertValueEqual(0, controller.rpBuffer(), "overstressed mechanical input should not generate RP"));

        helper.runAfterDelay(10, () -> {
            input.updateFromNetwork(100.0F, 0.0F, 1);
            input.setSpeed(32.0F);
        });
        helper.succeedWhen(() -> helper.assertTrue(controller.rpBuffer() > 0, "operational mechanical input should generate RP"));
    }

    private static BuiltStation buildStation(GameTestHelper helper, net.minecraft.world.level.block.Block controllerBlock, net.minecraft.world.level.block.Block inputBlock) {
        BuiltStation station = fillCasingShell(helper);
        helper.setBlock(station.controllerPos(), controllerBlock);
        helper.setBlock(station.inputPos(), inputBlock);
        return station;
    }

    private static BuiltStation fillCasingShell(GameTestHelper helper) {
        BuiltStation station = new BuiltStation(
                new BlockPos(1, 2, 1),
                new BlockPos(2, 2, 1),
                new BlockPos(3, 2, 1),
                new BlockPos(3, 2, 2)
        );

        for (int x = 1; x <= 3; x++) {
            for (int y = 1; y <= 2; y++) {
                for (int z = 1; z <= 2; z++) {
                    helper.setBlock(new BlockPos(x, y, z), Registration.RESEARCH_STATION_CASING_BLOCK.get());
                }
            }
        }
        return station;
    }

    private static ResearchControllerBlockEntity bindController(GameTestHelper helper, BlockPos controllerPos, String teamId) {
        ResearchControllerBlockEntity controller = requireBlockEntity(helper, controllerPos, ResearchControllerBlockEntity.class);
        controller.setTeamId(teamId);
        controller.revalidateStructure();
        return controller;
    }

    private static <T> T requireBlockEntity(GameTestHelper helper, BlockPos pos, Class<T> type) {
        Object blockEntity = helper.getBlockEntity(pos);
        helper.assertTrue(type.isInstance(blockEntity), "expected block entity " + type.getSimpleName() + " at " + pos);
        return type.cast(blockEntity);
    }

    private static void chargeElectricInput(ElectricPowerInputBlockEntity input, int amount) {
        int remaining = Math.max(0, amount);
        Direction front = input.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
        var storage = input.getEnergyStorage(front);
        while (remaining > 0 && storage != null) {
            int received = storage.receiveEnergy(remaining, false);
            if (received <= 0) {
                break;
            }
            remaining -= received;
        }
    }

    private record BuiltStation(BlockPos controllerPos, BlockPos corePos, BlockPos inputPos, BlockPos extraPos) {
    }
}
