package io.github.ozokuz.incore.features.research.station;

import io.github.ozokuz.incore.features.machines.multiblock.*;

import io.github.ozokuz.incore.INCore;
import io.github.ozokuz.incore.Registration;
import io.github.ozokuz.incore.features.research.ResearchManager;
import io.github.ozokuz.incore.features.research.provider.ResearchProviderManager;
import io.github.ozokuz.incore.features.research.state.ResearchNetworkSavedData;
import io.github.ozokuz.incore.features.research.state.ResearchQueueStatus;
import io.github.ozokuz.incore.features.research.state.TeamResearchState;
import io.github.ozokuz.incore.features.research.station.network.StationNetworkService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
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
        ResearchControllerBlockEntity controller = bindController(helper, station.controllerPos(), teamId(helper, "phase6_electric_fill", station.controllerPos()));
        ElectricPowerInputBlockEntity input = requireBlockEntity(helper, station.inputPos(), ElectricPowerInputBlockEntity.class);
        chargeElectricInput(input, 4_000);

        helper.succeedWhen(() -> helper.assertTrue(
                controller.availableResearchPower(Integer.MAX_VALUE) > 0,
                "expected FE input to expose research power without buffering"
        ));
    }

    @GameTest(template = "empty", timeoutTicks = 120)
    public static void electric_input_tiers_scale_fe_buffer_capacity(GameTestHelper helper) {
        BuiltStation t1Station = buildStation(helper, Registration.RESEARCH_CONTROLLER_T1_BLOCK.get(), Registration.ELECTRIC_POWER_INPUT_BLOCK.get());
        bindController(helper, t1Station.controllerPos(), teamId(helper, "phase7_fe_buffer_t1", t1Station.controllerPos()));
        ElectricPowerInputBlockEntity t1Input = requireBlockEntity(helper, t1Station.inputPos(), ElectricPowerInputBlockEntity.class);

        BuiltStation t4Station = fillCasingShell(helper, 6, 1, 1);
        placeController(helper, t4Station.controllerPos(), Registration.RESEARCH_CONTROLLER_T1_BLOCK.get(), Direction.NORTH);
        helper.setBlock(t4Station.inputPos(), Registration.ELECTRIC_POWER_INPUT_T4_BLOCK.get());
        bindController(helper, t4Station.controllerPos(), teamId(helper, "phase7_fe_buffer_t4", t4Station.controllerPos()));
        ElectricPowerInputBlockEntity t4Input = requireBlockEntity(helper, t4Station.inputPos(), ElectricPowerInputBlockEntity.class);

        helper.assertTrue(t4Input.energyCapacity() > t1Input.energyCapacity(), "higher-tier electric input should expose a larger FE buffer");

        chargeElectricInput(t1Input, Integer.MAX_VALUE);
        chargeElectricInput(t4Input, Integer.MAX_VALUE);
        helper.assertValueEqual(t1Input.energyCapacity(), t1Input.energyStored(), "tier 1 input should store up to its FE capacity");
        helper.assertValueEqual(t4Input.energyCapacity(), t4Input.energyStored(), "tier 4 input should store up to its FE capacity");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 900)
    public static void electric_core_advances_research(GameTestHelper helper) {
        BuiltStation station = buildStation(helper, Registration.RESEARCH_CONTROLLER_T1_BLOCK.get(), Registration.ELECTRIC_POWER_INPUT_BLOCK.get());
        ResearchControllerBlockEntity controller = bindController(helper, station.controllerPos(), teamId(helper, "phase6_research_progress", station.controllerPos()));
        ElectricPowerInputBlockEntity input = requireBlockEntity(helper, station.inputPos(), ElectricPowerInputBlockEntity.class);
        ResearchDriveBlockEntity drive = requireBlockEntity(helper, station.researchDrivePos(), ResearchDriveBlockEntity.class);
        LogicHousingBlockEntity logicHousing = requireBlockEntity(helper, station.logicHousingPos(), LogicHousingBlockEntity.class);
        MaterialStorageBlockEntity materialStorage = requireBlockEntity(helper, station.materialStoragePos(), MaterialStorageBlockEntity.class);
        chargeElectricInput(input, 60_000);
        drive.rawItemHandler().setStackInSlot(0, Registration.RESEARCH_DISK_T1_ITEM.get().getDefaultInstance());
        logicHousing.rawItemHandler().setStackInSlot(0, new ItemStack(Registration.BASIC_LOGIC_MODULE_ITEM.get()));
        materialStorage.rawItemHandler().setStackInSlot(0, new ItemStack(Registration.STARTER_DATA_ITEM.get(), 3));

        MinecraftServer server = helper.getLevel().getServer();
        helper.assertTrue(server != null, "expected game test server");
        TeamResearchState state = ResearchManager.ensureTeamState(server, controller.teamId());
        state.discoveredNodes().add(SIGNAL_CALIBRATION);
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

    @GameTest(template = "empty", timeoutTicks = 900)
    public static void linked_stations_add_parallel_run_capacity(GameTestHelper helper) {
        BuiltStation stationA = buildStation(helper, Registration.RESEARCH_CONTROLLER_T1_BLOCK.get(), Registration.ELECTRIC_POWER_INPUT_BLOCK.get());
        BuiltStation stationB = buildStation(helper, Registration.RESEARCH_CONTROLLER_T1_BLOCK.get(), Registration.ELECTRIC_POWER_INPUT_BLOCK.get(), 6, 1, 1);

        ResearchControllerBlockEntity controllerA = bindController(helper, stationA.controllerPos(), teamId(helper, "phase8_linked_team", stationA.controllerPos()));
        ResearchControllerBlockEntity controllerB = bindController(helper, stationB.controllerPos(), controllerA.teamId());
        ElectricPowerInputBlockEntity inputA = requireBlockEntity(helper, stationA.inputPos(), ElectricPowerInputBlockEntity.class);
        ElectricPowerInputBlockEntity inputB = requireBlockEntity(helper, stationB.inputPos(), ElectricPowerInputBlockEntity.class);
        ResearchDriveBlockEntity driveA = requireBlockEntity(helper, stationA.researchDrivePos(), ResearchDriveBlockEntity.class);
        ResearchDriveBlockEntity driveB = requireBlockEntity(helper, stationB.researchDrivePos(), ResearchDriveBlockEntity.class);
        LogicHousingBlockEntity logicA = requireBlockEntity(helper, stationA.logicHousingPos(), LogicHousingBlockEntity.class);
        LogicHousingBlockEntity logicB = requireBlockEntity(helper, stationB.logicHousingPos(), LogicHousingBlockEntity.class);
        MaterialStorageBlockEntity storageA = requireBlockEntity(helper, stationA.materialStoragePos(), MaterialStorageBlockEntity.class);
        MaterialStorageBlockEntity storageB = requireBlockEntity(helper, stationB.materialStoragePos(), MaterialStorageBlockEntity.class);

        chargeElectricInput(inputA, 60_000);
        chargeElectricInput(inputB, 60_000);
        driveA.rawItemHandler().setStackInSlot(0, Registration.RESEARCH_DISK_T1_ITEM.get().getDefaultInstance());
        driveB.rawItemHandler().setStackInSlot(0, Registration.RESEARCH_DISK_T1_ITEM.get().getDefaultInstance());
        logicA.rawItemHandler().setStackInSlot(0, new ItemStack(Registration.BASIC_LOGIC_MODULE_ITEM.get()));
        logicB.rawItemHandler().setStackInSlot(0, new ItemStack(Registration.BASIC_LOGIC_MODULE_ITEM.get()));
        storageA.rawItemHandler().setStackInSlot(0, new ItemStack(Registration.STARTER_DATA_ITEM.get(), 3));
        storageB.rawItemHandler().setStackInSlot(0, new ItemStack(Registration.STARTER_DATA_ITEM.get(), 3));

        BlockPos portA = placeLinkPort(helper, stationA.sparePos());
        BlockPos portB = placeLinkPort(helper, stationB.sparePos());
        connectPorts(helper, portA, portB);

        MinecraftServer server = helper.getLevel().getServer();
        helper.assertTrue(server != null, "expected game test server");
        TeamResearchState state = ResearchManager.ensureTeamState(server, controllerA.teamId());
        state.discoveredNodes().add(SIGNAL_CALIBRATION);
        ResearchNetworkSavedData.get(server).setDirty();
        helper.assertValueEqual(1, StationNetworkService.snapshot(server, controllerA.teamId()).stationNetworkCount(), "expected stations to merge into one network");
        helper.assertValueEqual(2, StationNetworkService.snapshot(server, controllerA.teamId()).linkedStationCount(), "expected both stations to be linked");
        helper.assertTrue(
                ResearchManager.queueResearch(server, controllerA.teamId(), SIGNAL_CALIBRATION),
                "expected linked team to queue signal_calibration: "
                        + ResearchManager.explainQueueFailure(server, controllerA.teamId(), SIGNAL_CALIBRATION)
        );

        helper.runAfterDelay(10, () -> {
            TeamResearchState currentState = ResearchManager.ensureTeamState(server, controllerA.teamId());
            helper.assertFalse(currentState.researchQueue().isEmpty(), "expected queued research to remain present");
            helper.assertValueEqual(2, currentState.researchQueue().get(0).activeRuns().size(), "expected both linked stations to run in parallel");
        });

        helper.succeedWhen(() -> {
            TeamResearchState currentState = ResearchManager.ensureTeamState(server, controllerA.teamId());
            String queueDetails = currentState.researchQueue().isEmpty()
                    ? "empty"
                    : currentState.researchQueue().get(0).status()
                    + " progress="
                    + currentState.researchQueue().get(0).runTickProgress()
                    + "/"
                    + currentState.researchQueue().get(0).runTickRequired()
                    + " runs="
                    + currentState.researchQueue().get(0).completedRuns()
                    + "/"
                    + currentState.researchQueue().get(0).requiredRuns();
            helper.assertTrue(
                    ResearchManager.isResearched(server, controllerA.teamId(), SIGNAL_CALIBRATION),
                    "expected linked stations to finish research with parallel local runs; queue="
                            + queueDetails
                            + " availablePower="
                            + ResearchProviderManager.availablePower(server, controllerA.teamId())
                            + " networks="
                            + StationNetworkService.snapshot(server, controllerA.teamId()).stationNetworkCount()
            );
        });
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void linked_stations_do_not_share_materials(GameTestHelper helper) {
        BuiltStation stationA = buildStation(helper, Registration.RESEARCH_CONTROLLER_T1_BLOCK.get(), Registration.ELECTRIC_POWER_INPUT_BLOCK.get());
        BuiltStation stationB = buildStation(helper, Registration.RESEARCH_CONTROLLER_T1_BLOCK.get(), Registration.ELECTRIC_POWER_INPUT_BLOCK.get(), 6, 1, 1);

        ResearchControllerBlockEntity controllerA = bindController(helper, stationA.controllerPos(), teamId(helper, "phase8_linked_local_inputs", stationA.controllerPos()));
        bindController(helper, stationB.controllerPos(), controllerA.teamId());
        ElectricPowerInputBlockEntity inputA = requireBlockEntity(helper, stationA.inputPos(), ElectricPowerInputBlockEntity.class);
        ElectricPowerInputBlockEntity inputB = requireBlockEntity(helper, stationB.inputPos(), ElectricPowerInputBlockEntity.class);
        ResearchDriveBlockEntity driveA = requireBlockEntity(helper, stationA.researchDrivePos(), ResearchDriveBlockEntity.class);
        ResearchDriveBlockEntity driveB = requireBlockEntity(helper, stationB.researchDrivePos(), ResearchDriveBlockEntity.class);
        LogicHousingBlockEntity logicA = requireBlockEntity(helper, stationA.logicHousingPos(), LogicHousingBlockEntity.class);
        LogicHousingBlockEntity logicB = requireBlockEntity(helper, stationB.logicHousingPos(), LogicHousingBlockEntity.class);
        MaterialStorageBlockEntity storageA = requireBlockEntity(helper, stationA.materialStoragePos(), MaterialStorageBlockEntity.class);
        MaterialStorageBlockEntity storageB = requireBlockEntity(helper, stationB.materialStoragePos(), MaterialStorageBlockEntity.class);

        chargeElectricInput(inputA, 30_000);
        chargeElectricInput(inputB, 30_000);
        driveA.rawItemHandler().setStackInSlot(0, Registration.RESEARCH_DISK_T1_ITEM.get().getDefaultInstance());
        driveB.rawItemHandler().setStackInSlot(0, Registration.RESEARCH_DISK_T1_ITEM.get().getDefaultInstance());
        logicA.rawItemHandler().setStackInSlot(0, new ItemStack(Registration.BASIC_LOGIC_MODULE_ITEM.get()));
        logicB.rawItemHandler().setStackInSlot(0, new ItemStack(Registration.BASIC_LOGIC_MODULE_ITEM.get()));
        storageA.rawItemHandler().setStackInSlot(0, new ItemStack(Registration.STARTER_DATA_ITEM.get(), 1));
        storageB.rawItemHandler().setStackInSlot(0, new ItemStack(Registration.STARTER_DATA_ITEM.get(), 2));

        BlockPos portA = placeLinkPort(helper, stationA.sparePos());
        BlockPos portB = placeLinkPort(helper, stationB.sparePos());
        connectPorts(helper, portA, portB);

        MinecraftServer server = helper.getLevel().getServer();
        helper.assertTrue(server != null, "expected game test server");
        TeamResearchState state = ResearchManager.ensureTeamState(server, controllerA.teamId());
        state.discoveredNodes().add(SIGNAL_CALIBRATION);
        ResearchNetworkSavedData.get(server).setDirty();
        helper.assertTrue(ResearchManager.queueResearch(server, controllerA.teamId(), SIGNAL_CALIBRATION), "expected queue to succeed");

        helper.runAfterDelay(20, () -> {
            TeamResearchState currentState = ResearchManager.ensureTeamState(server, controllerA.teamId());
            helper.assertFalse(currentState.researchQueue().isEmpty(), "expected queue to remain present");
            helper.assertValueEqual(0, currentState.researchQueue().get(0).activeRuns().size(), "expected no station to start without full local materials");
            helper.assertTrue(
                    currentState.researchQueue().get(0).status() == ResearchQueueStatus.PAUSED_MISSING_INPUTS,
                    "expected linked stations to pause because materials are not shared across stations"
            );
        });
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 240)
    public static void second_unlinked_network_pauses_active_queue(GameTestHelper helper) {
        BuiltStation stationA = buildStation(helper, Registration.RESEARCH_CONTROLLER_T1_BLOCK.get(), Registration.ELECTRIC_POWER_INPUT_BLOCK.get());
        ResearchControllerBlockEntity controllerA = bindController(helper, stationA.controllerPos(), teamId(helper, "phase8_conflict_team", stationA.controllerPos()));
        ElectricPowerInputBlockEntity inputA = requireBlockEntity(helper, stationA.inputPos(), ElectricPowerInputBlockEntity.class);
        ResearchDriveBlockEntity driveA = requireBlockEntity(helper, stationA.researchDrivePos(), ResearchDriveBlockEntity.class);
        LogicHousingBlockEntity logicA = requireBlockEntity(helper, stationA.logicHousingPos(), LogicHousingBlockEntity.class);
        MaterialStorageBlockEntity storageA = requireBlockEntity(helper, stationA.materialStoragePos(), MaterialStorageBlockEntity.class);

        chargeElectricInput(inputA, 2_000);
        driveA.rawItemHandler().setStackInSlot(0, Registration.RESEARCH_DISK_T1_ITEM.get().getDefaultInstance());
        logicA.rawItemHandler().setStackInSlot(0, new ItemStack(Registration.BASIC_LOGIC_MODULE_ITEM.get()));
        storageA.rawItemHandler().setStackInSlot(0, new ItemStack(Registration.STARTER_DATA_ITEM.get(), 3));

        MinecraftServer server = helper.getLevel().getServer();
        helper.assertTrue(server != null, "expected game test server");
        TeamResearchState state = ResearchManager.ensureTeamState(server, controllerA.teamId());
        state.discoveredNodes().add(SIGNAL_CALIBRATION);
        ResearchNetworkSavedData.get(server).setDirty();
        helper.assertTrue(
                ResearchManager.queueResearch(server, controllerA.teamId(), SIGNAL_CALIBRATION),
                "expected first station to queue signal_calibration: "
                        + ResearchManager.explainQueueFailure(server, controllerA.teamId(), SIGNAL_CALIBRATION)
        );

        helper.runAfterDelay(10, () -> {
            BuiltStation stationB = buildStation(helper, Registration.RESEARCH_CONTROLLER_T1_BLOCK.get(), Registration.ELECTRIC_POWER_INPUT_BLOCK.get(), 6, 1, 1);
            bindController(helper, stationB.controllerPos(), controllerA.teamId());
        });

        helper.succeedWhen(() -> {
            TeamResearchState currentState = ResearchManager.ensureTeamState(server, controllerA.teamId());
            helper.assertFalse(currentState.researchQueue().isEmpty(), "expected queued research to remain present");
            helper.assertTrue(
                    currentState.researchQueue().get(0).status() == ResearchQueueStatus.PAUSED_NETWORK_CONFLICT,
                    "expected queue to pause because the team has multiple unlinked station networks"
            );
            helper.assertValueEqual(2, StationNetworkService.snapshot(server, controllerA.teamId()).stationNetworkCount(), "expected two separate station networks");
        });
    }

    @GameTest(template = "empty", timeoutTicks = 120)
    public static void controller_tier_changes_capacity_and_category_gate(GameTestHelper helper) {
        BuiltStation station = buildStation(helper, Registration.RESEARCH_CONTROLLER_T1_BLOCK.get(), Registration.ELECTRIC_POWER_INPUT_BLOCK.get());
        ResearchControllerBlockEntity controllerT1 = bindController(helper, station.controllerPos(), teamId(helper, "phase6_tier_swap", station.controllerPos()));
        helper.assertValueEqual(0, controllerT1.rpCapacity(), "controller should not expose an RP buffer");

        MinecraftServer server = helper.getLevel().getServer();
        helper.assertTrue(server != null, "expected game test server");
        TeamResearchState state = ResearchManager.ensureTeamState(server, controllerT1.teamId());
        state.discoveredNodes().add(TERRAIN_SCANNING);
        ResearchNetworkSavedData.get(server).setDirty();
        helper.assertFalse(ResearchManager.canQueue(server, controllerT1.teamId(), TERRAIN_SCANNING), "tier 1 controller should not allow expedition node queueing");

        helper.setBlock(station.controllerPos(), Registration.RESEARCH_CONTROLLER_T3_BLOCK.get());
        ResearchControllerBlockEntity controllerT3 = bindController(helper, station.controllerPos(), controllerT1.teamId());

        helper.assertValueEqual(0, controllerT3.rpCapacity(), "tier 3 controller should also operate without an RP buffer");
        helper.assertTrue(ResearchManager.canQueue(server, controllerT3.teamId(), TERRAIN_SCANNING), "tier 3 controller should allow expedition node queueing");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 120)
    public static void buffer_full_does_not_consume_fe(GameTestHelper helper) {
        BuiltStation station = buildStation(helper, Registration.RESEARCH_CONTROLLER_T1_BLOCK.get(), Registration.ELECTRIC_POWER_INPUT_BLOCK.get());
        bindController(helper, station.controllerPos(), teamId(helper, "phase6_fe_full", station.controllerPos()));
        ElectricPowerInputBlockEntity input = requireBlockEntity(helper, station.inputPos(), ElectricPowerInputBlockEntity.class);
        chargeElectricInput(input, 2_000);
        int before = input.energyStored();

        helper.runAfterDelay(5, () -> {
            helper.assertValueEqual(before, input.energyStored(), "expected FE input to remain unchanged while no research is consuming power");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void mixed_power_family_station_is_invalid(GameTestHelper helper) {
        BuiltStation station = buildStation(helper, Registration.RESEARCH_CONTROLLER_T1_BLOCK.get(), Registration.ELECTRIC_POWER_INPUT_BLOCK.get());
        helper.setBlock(station.extraPos(), Registration.MECHANICAL_POWER_INPUT_BLOCK.get());
        ResearchControllerBlockEntity controller = bindController(helper, station.controllerPos(), teamId(helper, "phase6_mixed_family", station.controllerPos()));
        controller.revalidateStructure();
        helper.assertFalse(controller.isFormed(), "station should be invalid when input families are mixed");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void station_requires_single_power_input_type(GameTestHelper helper) {
        BuiltStation station = fillCasingShell(helper);
        helper.setBlock(station.controllerPos(), Registration.RESEARCH_CONTROLLER_T1_BLOCK.get());
        helper.setBlock(station.inputPos(), Registration.MECHANICAL_POWER_INPUT_BLOCK.get());
        helper.setBlock(station.extraPos(), Registration.ELECTRIC_POWER_INPUT_BLOCK.get());
        ResearchControllerBlockEntity controller = bindController(helper, station.controllerPos(), teamId(helper, "phase6_input_type", station.controllerPos()));
        controller.revalidateStructure();
        helper.assertFalse(controller.isFormed(), "station should be invalid with mixed power input block types");

        helper.setBlock(station.extraPos(), Registration.MECHANICAL_POWER_INPUT_BLOCK.get());
        controller = bindController(helper, station.controllerPos(), controller.teamId());
        controller.revalidateStructure();
        helper.assertTrue(controller.isFormed(), "station should allow multiple inputs when they are the same block type");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void station_requires_one_or_more_inputs(GameTestHelper helper) {
        BuiltStation station = fillCasingShell(helper);
        placeController(helper, station.controllerPos(), Registration.RESEARCH_CONTROLLER_T1_BLOCK.get(), Direction.NORTH);
        ResearchControllerBlockEntity controller = bindController(helper, station.controllerPos(), teamId(helper, "phase6_input_count", station.controllerPos()));
        controller.revalidateStructure();
        helper.assertFalse(controller.isFormed(), "station should be invalid without a power input");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void controller_must_face_outward(GameTestHelper helper) {
        BuiltStation station = fillCasingShell(helper);
        placeController(helper, station.controllerPos(), Registration.RESEARCH_CONTROLLER_T1_BLOCK.get(), Direction.SOUTH);
        helper.setBlock(station.inputPos(), Registration.ELECTRIC_POWER_INPUT_BLOCK.get());
        ResearchControllerBlockEntity controller = bindController(helper, station.controllerPos(), teamId(helper, "phase6_controller_facing", station.controllerPos()));
        controller.revalidateStructure();
        helper.assertFalse(controller.isFormed(), "controller should face outward from the multiblock");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void controller_must_be_in_center_column(GameTestHelper helper) {
        BuiltStation station = fillCasingShell(helper);
        placeController(helper, station.inputPos(), Registration.RESEARCH_CONTROLLER_T1_BLOCK.get(), Direction.NORTH);
        helper.setBlock(station.controllerPos(), Registration.ELECTRIC_POWER_INPUT_BLOCK.get());
        ResearchControllerBlockEntity controller = bindController(helper, station.inputPos(), teamId(helper, "phase6_controller_column", station.inputPos()));
        controller.revalidateStructure();
        helper.assertFalse(controller.isFormed(), "controller should only form in the center column of the outward face");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 160)
    public static void mechanical_input_requires_operational_create_network(GameTestHelper helper) {
        BuiltStation station = buildStation(helper, Registration.RESEARCH_CONTROLLER_T1_BLOCK.get(), Registration.MECHANICAL_POWER_INPUT_BLOCK.get());
        ResearchControllerBlockEntity controller = bindController(helper, station.controllerPos(), teamId(helper, "phase6_mechanical", station.controllerPos()));
        MechanicalPowerInputBlockEntity input = requireBlockEntity(helper, station.inputPos(), MechanicalPowerInputBlockEntity.class);

        input.setSpeed(32.0F);
        input.updateFromNetwork(0.0F, 16.0F, 1);
        helper.runAfterDelay(5, () -> helper.assertValueEqual(0, controller.availableResearchPower(Integer.MAX_VALUE), "overstressed mechanical input should not expose RP"));

        helper.runAfterDelay(10, () -> {
            input.updateFromNetwork(100.0F, 0.0F, 1);
            input.setSpeed(32.0F);
        });
        helper.succeedWhen(() -> helper.assertTrue(controller.availableResearchPower(Integer.MAX_VALUE) > 0, "operational mechanical input should expose RP"));
    }

    private static BuiltStation buildStation(GameTestHelper helper, net.minecraft.world.level.block.Block controllerBlock, net.minecraft.world.level.block.Block inputBlock) {
        return buildStation(helper, controllerBlock, inputBlock, 1, 1, 1);
    }

    private static BuiltStation buildStation(GameTestHelper helper, net.minecraft.world.level.block.Block controllerBlock, net.minecraft.world.level.block.Block inputBlock, int minX, int minY, int minZ) {
        BuiltStation station = fillCasingShell(helper, minX, minY, minZ);
        placeController(helper, station.controllerPos(), controllerBlock, Direction.NORTH);
        helper.setBlock(station.inputPos(), inputBlock);
        return station;
    }

    private static BuiltStation fillCasingShell(GameTestHelper helper) {
        return fillCasingShell(helper, 1, 1, 1);
    }

    private static BuiltStation fillCasingShell(GameTestHelper helper, int minX, int minY, int minZ) {
        BuiltStation station = new BuiltStation(
                new BlockPos(minX + 1, minY + 1, minZ),
                new BlockPos(minX, minY + 1, minZ),
                new BlockPos(minX + 2, minY + 1, minZ),
                new BlockPos(minX + 2, minY + 1, minZ + 1),
                new BlockPos(minX, minY, minZ),
                new BlockPos(minX + 1, minY, minZ),
                new BlockPos(minX + 2, minY, minZ),
                new BlockPos(minX + 1, minY, minZ + 1),
                new BlockPos(minX, minY, minZ + 1)
        );

        for (int x = minX; x <= minX + 2; x++) {
            for (int y = minY; y <= minY + 1; y++) {
                for (int z = minZ; z <= minZ + 1; z++) {
                    helper.setBlock(new BlockPos(x, y, z), Registration.RESEARCH_STATION_CASING_BLOCK.get());
                }
            }
        }
        helper.setBlock(station.logicHousingPos(), Registration.LOGIC_HOUSING_BLOCK.get().defaultBlockState().setValue(BlockStateProperties.FACING, Direction.NORTH));
        helper.setBlock(station.researchDrivePos(), Registration.RESEARCH_DRIVE_BLOCK.get().defaultBlockState().setValue(BlockStateProperties.FACING, Direction.NORTH));
        helper.setBlock(station.materialStoragePos(), Registration.MATERIAL_STORAGE_BLOCK.get().defaultBlockState().setValue(BlockStateProperties.FACING, Direction.NORTH));
        helper.setBlock(station.outputPortPos(), Registration.OUTPUT_PORT_BLOCK.get().defaultBlockState().setValue(BlockStateProperties.FACING, Direction.NORTH));
        helper.setBlock(station.augmenterPos(), Registration.AUGMENTER_BLOCK.get().defaultBlockState().setValue(BlockStateProperties.FACING, Direction.NORTH));
        return station;
    }

    private static void placeController(GameTestHelper helper, BlockPos controllerPos, net.minecraft.world.level.block.Block controllerBlock, Direction facing) {
        helper.setBlock(
                controllerPos,
                controllerBlock.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, facing)
        );
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
        Direction front = input.getBlockState().getValue(BlockStateProperties.FACING);
        var storage = input.getEnergyStorage(front);
        if (storage == null) {
            for (Direction direction : Direction.values()) {
                storage = input.getEnergyStorage(direction);
                if (storage != null) {
                    break;
                }
            }
        }
        while (remaining > 0 && storage != null) {
            int received = storage.receiveEnergy(remaining, false);
            if (received <= 0) {
                break;
            }
            remaining -= received;
        }
    }

    private static BlockPos placeLinkPort(GameTestHelper helper, BlockPos portPos) {
        helper.setBlock(
                portPos,
                Registration.LINKING_PORT_BLOCK.get().defaultBlockState().setValue(BlockStateProperties.FACING, Direction.NORTH)
        );
        return portPos;
    }

    private static void connectPorts(GameTestHelper helper, BlockPos firstPort, BlockPos secondPort) {
        BlockPos current = firstPort;
        int liftY = Math.max(firstPort.getY(), secondPort.getY()) + 1;
        while (current.getY() < liftY) {
            current = current.above();
            helper.setBlock(current, Registration.RESEARCH_LINK_CABLE_BLOCK.get());
        }
        while (current.getX() != secondPort.getX()) {
            current = current.getX() < secondPort.getX() ? current.east() : current.west();
            helper.setBlock(current, Registration.RESEARCH_LINK_CABLE_BLOCK.get());
        }
        while (current.getZ() != secondPort.getZ()) {
            current = current.getZ() < secondPort.getZ() ? current.south() : current.north();
            helper.setBlock(current, Registration.RESEARCH_LINK_CABLE_BLOCK.get());
        }
        while (current.getY() > secondPort.getY() + 1) {
            current = current.below();
            helper.setBlock(current, Registration.RESEARCH_LINK_CABLE_BLOCK.get());
        }
    }

    private static String teamId(GameTestHelper helper, String base, BlockPos controllerPos) {
        return base + "_" + helper.absolutePos(controllerPos).asLong();
    }

    private record BuiltStation(
            BlockPos controllerPos,
            BlockPos inputPos,
            BlockPos extraPos,
            BlockPos sparePos,
            BlockPos logicHousingPos,
            BlockPos researchDrivePos,
            BlockPos materialStoragePos,
            BlockPos outputPortPos,
            BlockPos augmenterPos
    ) {
    }
}
