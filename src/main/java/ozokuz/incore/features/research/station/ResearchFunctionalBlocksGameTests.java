package ozokuz.incore.features.research.station;

import ozokuz.incore.INCore;
import ozokuz.incore.Registration;
import ozokuz.incore.features.machines.multiblock.AugmenterBlockEntity;
import ozokuz.incore.features.machines.multiblock.ElectricPowerInputBlockEntity;
import ozokuz.incore.features.machines.multiblock.OutputPortBlockEntity;
import ozokuz.incore.features.machines.multiblock.OutputPortMode;
import ozokuz.incore.features.research.ResearchDeterministicRng;
import ozokuz.incore.features.research.ResearchManager;
import ozokuz.incore.features.research.model.ResearchCostDefinition;
import ozokuz.incore.features.research.state.ResearchNetworkSavedData;
import ozokuz.incore.features.research.state.ResearchQueueEntry;
import ozokuz.incore.features.research.state.ResearchQueueStatus;
import ozokuz.incore.features.research.state.TeamResearchState;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("incore")
@PrefixGameTestTemplate(false)
public final class ResearchFunctionalBlocksGameTests {
    private static final ResourceLocation SIGNAL_CALIBRATION = ResourceLocation.fromNamespaceAndPath(INCore.MODID, "signal_calibration");

    private ResearchFunctionalBlocksGameTests() {
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void station_requires_core_functional_blocks_only(GameTestHelper helper) {
        FunctionalStation station = buildFunctionalStation(helper);
        ResearchControllerBlockEntity controller = bindController(helper, station.controllerPos(), teamId(helper, "phase7_shape", station.controllerPos()));
        helper.assertTrue(controller.isFormed(), "expected station with all functional blocks to form");

        helper.setBlock(station.outputPortPos(), Registration.RESEARCH_STATION_CASING_BLOCK.get());
        controller.revalidateStructure();
        helper.assertTrue(controller.isFormed(), "station should still form without an output port");

        helper.setBlock(station.augmenterPos(), Registration.RESEARCH_STATION_CASING_BLOCK.get());
        controller.revalidateStructure();
        helper.assertTrue(controller.isFormed(), "station should still form without an augmenter");

        helper.setBlock(station.outputPortPos(), Registration.LOGIC_HOUSING_BLOCK.get());
        controller.revalidateStructure();
        helper.assertFalse(controller.isFormed(), "station should fail with duplicate logic housing");

        helper.setBlock(station.outputPortPos(), Registration.RESEARCH_STATION_CASING_BLOCK.get());
        helper.setBlock(station.secondOutputPortPos(), Registration.OUTPUT_PORT_BLOCK.get().defaultBlockState().setValue(BlockStateProperties.FACING, Direction.NORTH));
        helper.setBlock(station.augmenterPos(), Registration.AUGMENTER_BLOCK.get().defaultBlockState().setValue(BlockStateProperties.FACING, Direction.NORTH));
        controller = bindController(helper, station.controllerPos(), controller.teamId());
        helper.assertTrue(controller.isFormed(), "expected station to reform after restoring the core shape");

        helper.setBlock(station.outputPortPos(), Registration.OUTPUT_PORT_BLOCK.get().defaultBlockState().setValue(BlockStateProperties.FACING, Direction.NORTH));
        helper.setBlock(station.augmenterPos(), Registration.OUTPUT_PORT_BLOCK.get().defaultBlockState().setValue(BlockStateProperties.FACING, Direction.NORTH));
        controller.revalidateStructure();
        helper.assertFalse(controller.isFormed(), "station should fail with more than two output ports");

        helper.setBlock(station.augmenterPos(), Registration.AUGMENTER_BLOCK.get().defaultBlockState().setValue(BlockStateProperties.FACING, Direction.NORTH));
        controller = bindController(helper, station.controllerPos(), controller.teamId());
        helper.assertTrue(controller.isFormed(), "station should reform after restoring the augmenter");

        helper.setBlock(station.logicHousingPos(), Registration.RESEARCH_STATION_CASING_BLOCK.get());
        controller.revalidateStructure();
        helper.assertFalse(controller.isFormed(), "station should fail without logic housing");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void stations_cannot_share_walls(GameTestHelper helper) {
        FunctionalStation first = buildFunctionalStation(helper, 1, 1, 1);
        FunctionalStation second = buildFunctionalStation(helper, 3, 1, 1);

        ResearchControllerBlockEntity firstController = bindController(helper, first.controllerPos(), teamId(helper, "phase8_wall_share_a", first.controllerPos()));
        ResearchControllerBlockEntity secondController = bindController(helper, second.controllerPos(), teamId(helper, "phase8_wall_share_b", second.controllerPos()));

        helper.assertFalse(firstController.isFormed() && secondController.isFormed(), "wall-sharing stations must not both form");
        firstController.revalidateStructure();
        helper.assertFalse(firstController.isFormed() && secondController.isFormed(), "revalidation must continue rejecting wall-sharing stations");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void material_storage_counts_registered_materials(GameTestHelper helper) {
        FunctionalStation station = buildFunctionalStation(helper);
        ResearchControllerBlockEntity controller = bindController(helper, station.controllerPos(), teamId(helper, "phase7_materials", station.controllerPos()));
        MaterialStorageBlockEntity storage = requireBlockEntity(helper, station.materialStoragePos(), MaterialStorageBlockEntity.class);
        storage.rawItemHandler().setStackInSlot(0, new ItemStack(Registration.STARTER_DATA_ITEM.get(), 3));

        helper.runAfterDelay(5, () -> {
            helper.assertTrue(ResearchStationRuntime.hasRequiredMaterials(controller, List.of(new ResearchCostDefinition.ResearchMaterialRequirement("incore:starter_data", 3))), "expected material storage to satisfy starter data");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void tiered_housing_and_storage_increase_slot_counts(GameTestHelper helper) {
        FunctionalStation station = buildFunctionalStation(helper);
        helper.setBlock(station.logicHousingPos(), Registration.LOGIC_HOUSING_T3_BLOCK.get().defaultBlockState().setValue(BlockStateProperties.FACING, Direction.NORTH));
        helper.setBlock(station.materialStoragePos(), Registration.MATERIAL_STORAGE_T4_BLOCK.get().defaultBlockState().setValue(BlockStateProperties.FACING, Direction.NORTH));

        ResearchControllerBlockEntity controller = bindController(helper, station.controllerPos(), teamId(helper, "phase7_tiered_parts", station.controllerPos()));
        helper.assertTrue(controller.isFormed(), "expected station with tiered housing and storage to form");

        LogicHousingBlockEntity logicHousing = requireBlockEntity(helper, station.logicHousingPos(), LogicHousingBlockEntity.class);
        MaterialStorageBlockEntity storage = requireBlockEntity(helper, station.materialStoragePos(), MaterialStorageBlockEntity.class);
        helper.assertValueEqual(3, logicHousing.activeSlotCount(), "tier 3 logic housing should expose 3 active slots");
        helper.assertValueEqual(36, storage.activeSlotCount(), "tier 4 material storage should expose 36 active slots");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void logic_module_exhaustion_returns_expected_items(GameTestHelper helper) {
        FunctionalStation station = buildFunctionalStation(helper);
        ResearchControllerBlockEntity controller = bindController(helper, station.controllerPos(), teamId(helper, "phase7_logic", station.controllerPos()));
        LogicHousingBlockEntity logicHousing = requireBlockEntity(helper, station.logicHousingPos(), LogicHousingBlockEntity.class);
        OutputPortBlockEntity outputPort = requireBlockEntity(helper, station.outputPortPos(), OutputPortBlockEntity.class);
        outputPort.setMode(OutputPortMode.LOGIC);

        ItemStack t1 = new ItemStack(Registration.BASIC_LOGIC_MODULE_ITEM.get());
        t1.setDamageValue(t1.getMaxDamage() - 1);
        logicHousing.rawItemHandler().setStackInSlot(0, t1);
        helper.assertTrue(ResearchStationRuntime.consumeRequiredModules(controller, SIGNAL_CALIBRATION, 0, List.of(new ResearchCostDefinition.LogicModuleRequirement("basic", 1))), "t1 consume should succeed");
        helper.assertTrue(logicHousing.rawItemHandler().getStackInSlot(0).isEmpty(), "t1 should be consumed");
        helper.assertTrue(outputPort.rawItemHandler().getStackInSlot(0).isEmpty(), "t1 should not emit a returned item");

        logicHousing.rawItemHandler().setStackInSlot(0, new ItemStack(Registration.LOGIC_MODULE_T2_ITEM.get()));
        ItemStack t2 = logicHousing.rawItemHandler().getStackInSlot(0);
        t2.setDamageValue(t2.getMaxDamage() - 1);
        logicHousing.rawItemHandler().setStackInSlot(0, t2);
        helper.assertTrue(ResearchStationRuntime.consumeRequiredModules(controller, SIGNAL_CALIBRATION, 1, List.of(new ResearchCostDefinition.LogicModuleRequirement("t2", 1))), "t2 consume should succeed");
        helper.assertTrue(logicHousing.rawItemHandler().getStackInSlot(0).isEmpty(), "t2 source slot should be emptied");
        helper.assertTrue(outputPort.rawItemHandler().getStackInSlot(0).is(Registration.BROKEN_LOGIC_MODULE_T2_ITEM.get()), "t2 should output broken");
        outputPort.rawItemHandler().setStackInSlot(0, ItemStack.EMPTY);

        logicHousing.rawItemHandler().setStackInSlot(0, new ItemStack(Registration.LOGIC_MODULE_T3_ITEM.get()));
        ItemStack t3 = logicHousing.rawItemHandler().getStackInSlot(0);
        t3.setDamageValue(t3.getMaxDamage() - 1);
        logicHousing.rawItemHandler().setStackInSlot(0, t3);
        helper.assertTrue(ResearchStationRuntime.consumeRequiredModules(controller, SIGNAL_CALIBRATION, 2, List.of(new ResearchCostDefinition.LogicModuleRequirement("t3", 1))), "t3 consume should succeed");
        ItemStack t3Result = outputPort.rawItemHandler().getStackInSlot(0);
        boolean expectedUsed = ResearchDeterministicRng.rollChance(
                controller.teamId(),
                controller.stationId(),
                SIGNAL_CALIBRATION,
                2,
                "logic_t3_exhaust_slot_0",
                0.50D
        );
        helper.assertTrue(
                expectedUsed ? t3Result.is(Registration.USED_LOGIC_MODULE_T3_ITEM.get()) : t3Result.is(Registration.BROKEN_LOGIC_MODULE_T3_ITEM.get()),
                "t3 should return the deterministic seed result"
        );
        outputPort.rawItemHandler().setStackInSlot(0, ItemStack.EMPTY);

        logicHousing.rawItemHandler().setStackInSlot(0, new ItemStack(Registration.LOGIC_MODULE_T4_ITEM.get()));
        ItemStack t4 = logicHousing.rawItemHandler().getStackInSlot(0);
        t4.setDamageValue(t4.getMaxDamage() - 1);
        logicHousing.rawItemHandler().setStackInSlot(0, t4);
        helper.assertTrue(ResearchStationRuntime.consumeRequiredModules(controller, SIGNAL_CALIBRATION, 3, List.of(new ResearchCostDefinition.LogicModuleRequirement("t4", 1))), "t4 consume should succeed");
        helper.assertTrue(outputPort.rawItemHandler().getStackInSlot(0).is(Registration.USED_LOGIC_MODULE_T4_ITEM.get()), "t4 should output used");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void output_port_switches_between_logic_and_drive(GameTestHelper helper) {
        FunctionalStation station = buildFunctionalStation(helper);
        ResearchControllerBlockEntity controller = bindController(helper, station.controllerPos(), teamId(helper, "phase7_output", station.controllerPos()));
        ResearchDriveBlockEntity drive = requireBlockEntity(helper, station.researchDrivePos(), ResearchDriveBlockEntity.class);
        OutputPortBlockEntity outputPort = requireBlockEntity(helper, station.outputPortPos(), OutputPortBlockEntity.class);
        outputPort.setMode(OutputPortMode.LOGIC);

        outputPort.insertOutput(new ItemStack(Registration.USED_LOGIC_MODULE_T4_ITEM.get(), 1));
        drive.rawItemHandler().setStackInSlot(0, Registration.RESEARCH_DISK_T1_ITEM.get().getDefaultInstance());

        var front = outputPort.getBlockState().getValue(BlockStateProperties.FACING);
        helper.assertTrue(outputPort.frontExtractView(front) != null, "expected output port capability");
        helper.assertTrue(outputPort.frontExtractView(front).extractItem(0, 1, true).is(Registration.USED_LOGIC_MODULE_T4_ITEM.get()), "logic mode should expose output inventory");

        outputPort.rawItemHandler().setStackInSlot(0, ItemStack.EMPTY);
        outputPort.toggleMode();
        ResearchStationRuntime.flushDriveOutput(controller);
        helper.assertTrue(outputPort.frontExtractView(front).extractItem(0, 1, true).isEmpty(), "clean disks should remain mounted and not route to drive output");
        helper.assertTrue(drive.mountedDisk().is(Registration.RESEARCH_DISK_T1_ITEM.get()), "clean disk should stay in the drive");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void two_output_ports_can_target_both_output_types(GameTestHelper helper) {
        FunctionalStation station = buildFunctionalStation(helper);
        helper.setBlock(station.secondOutputPortPos(), Registration.OUTPUT_PORT_BLOCK.get().defaultBlockState().setValue(BlockStateProperties.FACING, Direction.NORTH));

        ResearchControllerBlockEntity controller = bindController(helper, station.controllerPos(), teamId(helper, "phase7_dual_output", station.controllerPos()));
        helper.assertValueEqual(2, controller.outputPortPositions().size(), "station should allow two output ports");

        ResearchDriveBlockEntity drive = requireBlockEntity(helper, station.researchDrivePos(), ResearchDriveBlockEntity.class);
        OutputPortBlockEntity logicPort = requireBlockEntity(helper, station.outputPortPos(), OutputPortBlockEntity.class);
        OutputPortBlockEntity drivePort = requireBlockEntity(helper, station.secondOutputPortPos(), OutputPortBlockEntity.class);
        logicPort.setMode(OutputPortMode.LOGIC);

        logicPort.insertOutput(new ItemStack(Registration.USED_LOGIC_MODULE_T4_ITEM.get()));
        drive.rawItemHandler().setStackInSlot(0, Registration.RESEARCH_DISK_T1_ITEM.get().getDefaultInstance());
        int ordinal = findCorruptionWindowOrdinal(controller, SIGNAL_CALIBRATION, 1, ResearchDiskTier.T1.corruptionChance(), ResearchDiskTier.T4.corruptionChance());
        ResearchStationRuntime.writeDiskSnapshot(controller, SIGNAL_CALIBRATION, 1, 3, ordinal, false, 1.0D);
        drivePort.setMode(OutputPortMode.DRIVE);
        ResearchStationRuntime.flushDriveOutput(controller);

        Direction logicFront = logicPort.getBlockState().getValue(BlockStateProperties.FACING);
        Direction driveFront = drivePort.getBlockState().getValue(BlockStateProperties.FACING);
        helper.assertTrue(logicPort.frontExtractView(logicFront).extractItem(0, 1, true).is(Registration.USED_LOGIC_MODULE_T4_ITEM.get()), "first output port should expose logic items");
        helper.assertTrue(drivePort.frontExtractView(driveFront).extractItem(0, 1, true).is(Registration.RESEARCH_DISK_T1_ITEM.get()), "corrupted disk should route through the drive output port");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 120)
    public static void disk_writes_snapshots_for_completed_runs(GameTestHelper helper) {
        FunctionalStation station = buildFunctionalStation(helper);
        ResearchControllerBlockEntity controller = bindController(helper, station.controllerPos(), teamId(helper, "phase7_disk", station.controllerPos()));
        ResearchDriveBlockEntity drive = requireBlockEntity(helper, station.researchDrivePos(), ResearchDriveBlockEntity.class);
        drive.rawItemHandler().setStackInSlot(0, Registration.RESEARCH_DISK_T1_ITEM.get().getDefaultInstance());

        ResearchStationRuntime.writeDiskSnapshot(controller, SIGNAL_CALIBRATION, 1, 3, 1, false, 1.0D);
        ResearchStationRuntime.writeDiskSnapshot(controller, SIGNAL_CALIBRATION, 3, 3, 2, true, 1.0D);
        helper.assertValueEqual(0, ResearchDiskData.readSnapshots(drive.mountedDisk()).size(), "completed research should be erased from the disk");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 400)
    public static void disk_corruption_is_tiered_and_deterministic(GameTestHelper helper) {
        FunctionalStation station = buildFunctionalStation(helper);
        ResearchControllerBlockEntity controller = bindController(helper, station.controllerPos(), teamId(helper, "phase7_corruption", station.controllerPos()));
        ResearchDriveBlockEntity drive = requireBlockEntity(helper, station.researchDrivePos(), ResearchDriveBlockEntity.class);
        int ordinal = findCorruptionWindowOrdinal(controller, SIGNAL_CALIBRATION, 1, ResearchDiskTier.T1.corruptionChance(), ResearchDiskTier.T4.corruptionChance());

        drive.rawItemHandler().setStackInSlot(0, Registration.RESEARCH_DISK_T1_ITEM.get().getDefaultInstance());
        ResearchStationRuntime.writeDiskSnapshot(controller, SIGNAL_CALIBRATION, 1, 3, ordinal, false, 1.0D);
        helper.assertValueEqual(1, ResearchDiskData.readSnapshots(drive.mountedDisk()).get(0).corruptedSegments().size(), "tier 1 disk should corrupt for the selected deterministic write");

        drive.rawItemHandler().setStackInSlot(0, Registration.RESEARCH_DISK_T4_ITEM.get().getDefaultInstance());
        ResearchStationRuntime.writeDiskSnapshot(controller, SIGNAL_CALIBRATION, 1, 3, ordinal, false, 1.0D);
        helper.assertValueEqual(0, ResearchDiskData.readSnapshots(drive.mountedDisk()).get(0).corruptedSegments().size(), "tier 4 disk should avoid corruption for the same deterministic write");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 240)
    public static void mounted_disk_can_be_extracted_while_station_is_active(GameTestHelper helper) {
        FunctionalStation station = buildFunctionalStation(helper);
        ResearchControllerBlockEntity controller = bindController(helper, station.controllerPos(), teamId(helper, "phase7_lock", station.controllerPos()));
        ResearchDriveBlockEntity drive = requireBlockEntity(helper, station.researchDrivePos(), ResearchDriveBlockEntity.class);
        LogicHousingBlockEntity logicHousing = requireBlockEntity(helper, station.logicHousingPos(), LogicHousingBlockEntity.class);
        MaterialStorageBlockEntity storage = requireBlockEntity(helper, station.materialStoragePos(), MaterialStorageBlockEntity.class);
        OutputPortBlockEntity outputPort = requireBlockEntity(helper, station.outputPortPos(), OutputPortBlockEntity.class);
        ElectricPowerInputBlockEntity input = requireBlockEntity(helper, station.inputPos(), ElectricPowerInputBlockEntity.class);
        chargeElectricInput(input, 60_000);
        outputPort.setMode(OutputPortMode.LOGIC);

        drive.rawItemHandler().setStackInSlot(0, Registration.RESEARCH_DISK_T1_ITEM.get().getDefaultInstance());
        logicHousing.rawItemHandler().setStackInSlot(0, new ItemStack(Registration.BASIC_LOGIC_MODULE_ITEM.get()));
        storage.rawItemHandler().setStackInSlot(0, new ItemStack(Registration.STARTER_DATA_ITEM.get(), 3));

        MinecraftServer server = helper.getLevel().getServer();
        helper.assertTrue(server != null, "expected test server");
        TeamResearchState state = ResearchManager.ensureTeamState(server, controller.teamId());
        state.discoveredNodes().add(SIGNAL_CALIBRATION);
        state.researchQueue().add(new ResearchQueueEntry(
                SIGNAL_CALIBRATION,
                0,
                200,
                0,
                3,
                false,
                ResearchQueueStatus.QUEUED,
                10_000,
                0,
                10_000,
                List.of(controller.stationId()),
                List.of()
        ));
        ResearchNetworkSavedData.get(server).setDirty();

        helper.runAfterDelay(5, () -> {
            helper.assertTrue(
                    ResearchManager.tickResearch(server, controller.teamId()),
                    "tick should activate queued research; queueSize="
                            + state.researchQueue().size()
                            + " completed="
                            + state.completedNodes().contains(SIGNAL_CALIBRATION)
            );
            helper.assertTrue(!state.researchQueue().isEmpty(), "expected queued research to still be active");
            helper.assertTrue(drive.itemHandler().extractItem(0, 1, true).is(Registration.RESEARCH_DISK_T1_ITEM.get()), "manual extraction should remain available during active research");

            outputPort.setMode(OutputPortMode.DRIVE);
            Direction front = outputPort.getBlockState().getValue(BlockStateProperties.FACING);
            helper.assertTrue(outputPort.frontExtractView(front) != null, "expected output port capability");
            helper.assertTrue(outputPort.frontExtractView(front).extractItem(0, 1, true).isEmpty(), "drive output should remain empty until a corrupted disk is flushed");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void multiblock_capabilities_only_expose_front_face_and_support_vertical_facing(GameTestHelper helper) {
        FunctionalStation station = buildFunctionalStation(helper);
        helper.setBlock(station.logicHousingPos(), Registration.LOGIC_HOUSING_BLOCK.get().defaultBlockState().setValue(BlockStateProperties.FACING, Direction.UP));
        helper.setBlock(station.researchDrivePos(), Registration.RESEARCH_DRIVE_BLOCK.get().defaultBlockState().setValue(BlockStateProperties.FACING, Direction.UP));
        helper.setBlock(station.materialStoragePos(), Registration.MATERIAL_STORAGE_BLOCK.get().defaultBlockState().setValue(BlockStateProperties.FACING, Direction.UP));
        helper.setBlock(station.outputPortPos(), Registration.OUTPUT_PORT_BLOCK.get().defaultBlockState().setValue(BlockStateProperties.FACING, Direction.UP));
        helper.setBlock(station.augmenterPos(), Registration.AUGMENTER_BLOCK.get().defaultBlockState().setValue(BlockStateProperties.FACING, Direction.UP));
        helper.setBlock(station.inputPos(), Registration.ELECTRIC_POWER_INPUT_BLOCK.get().defaultBlockState().setValue(BlockStateProperties.FACING, Direction.UP));
        helper.setBlock(station.secondOutputPortPos(), Registration.LINKING_PORT_BLOCK.get().defaultBlockState().setValue(BlockStateProperties.FACING, Direction.UP));

        ResearchControllerBlockEntity controller = bindController(helper, station.controllerPos(), teamId(helper, "phase8_vertical_fronts", station.controllerPos()));
        helper.assertTrue(controller.isFormed(), "station should still form when capability blocks face up");

        helper.assertTrue(itemCapability(helper, station.logicHousingPos(), Direction.UP) != null, "logic housing should expose items on its front");
        helper.assertTrue(itemCapability(helper, station.logicHousingPos(), Direction.NORTH) == null, "logic housing should reject side access");
        helper.assertTrue(itemCapability(helper, station.researchDrivePos(), Direction.UP) != null, "research drive should expose items on its front");
        helper.assertTrue(itemCapability(helper, station.researchDrivePos(), Direction.NORTH) == null, "research drive should reject side access");
        helper.assertTrue(itemCapability(helper, station.materialStoragePos(), Direction.UP) != null, "material storage should expose items on its front");
        helper.assertTrue(itemCapability(helper, station.materialStoragePos(), Direction.NORTH) == null, "material storage should reject side access");
        helper.assertTrue(itemCapability(helper, station.augmenterPos(), Direction.UP) != null, "augmenter should expose items on its front");
        helper.assertTrue(itemCapability(helper, station.augmenterPos(), Direction.NORTH) == null, "augmenter should reject side access");
        helper.assertTrue(itemCapability(helper, station.outputPortPos(), Direction.UP) != null, "output port should expose output on its front");
        helper.assertTrue(itemCapability(helper, station.outputPortPos(), Direction.NORTH) == null, "output port should reject side access");
        helper.assertTrue(energyCapability(helper, station.inputPos(), Direction.UP) != null, "electric input should expose energy on its front");
        helper.assertTrue(energyCapability(helper, station.inputPos(), Direction.NORTH) == null, "electric input should reject side access");
        helper.assertValueEqual(Direction.UP, helper.getBlockState(station.secondOutputPortPos()).getValue(BlockStateProperties.FACING), "linking port should support vertical front facing");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 400)
    public static void augments_change_research_runtime(GameTestHelper helper) {
        FunctionalStation station = buildFunctionalStation(helper);
        ResearchControllerBlockEntity controller = bindController(helper, station.controllerPos(), teamId(helper, "phase7_augments", station.controllerPos()));
        ResearchDriveBlockEntity drive = requireBlockEntity(helper, station.researchDrivePos(), ResearchDriveBlockEntity.class);
        LogicHousingBlockEntity logicHousing = requireBlockEntity(helper, station.logicHousingPos(), LogicHousingBlockEntity.class);
        MaterialStorageBlockEntity storage = requireBlockEntity(helper, station.materialStoragePos(), MaterialStorageBlockEntity.class);
        AugmenterBlockEntity augmenter = requireBlockEntity(helper, station.augmenterPos(), AugmenterBlockEntity.class);
        ElectricPowerInputBlockEntity input = requireBlockEntity(helper, station.inputPos(), ElectricPowerInputBlockEntity.class);
        chargeElectricInput(input, 60_000);

        drive.rawItemHandler().setStackInSlot(0, Registration.RESEARCH_DISK_T1_ITEM.get().getDefaultInstance());
        logicHousing.rawItemHandler().setStackInSlot(0, new ItemStack(Registration.BASIC_LOGIC_MODULE_ITEM.get()));
        storage.rawItemHandler().setStackInSlot(0, new ItemStack(Registration.STARTER_DATA_ITEM.get(), 3));
        augmenter.rawItemHandler().setStackInSlot(0, new ItemStack(Registration.SPEED_AUGMENT_ITEM.get(), 1));
        augmenter.rawItemHandler().setStackInSlot(1, new ItemStack(Registration.DUNGEON_PRODUCTIVITY_AUGMENT_ITEM.get(), 1));

        MinecraftServer server = helper.getLevel().getServer();
        helper.assertTrue(server != null, "expected test server");
        TeamResearchState state = ResearchManager.ensureTeamState(server, controller.teamId());
        state.discoveredNodes().add(SIGNAL_CALIBRATION);
        ResearchNetworkSavedData.get(server).setDirty();

        helper.runAfterDelay(5, () -> helper.assertTrue(ResearchManager.queueResearch(server, controller.teamId(), SIGNAL_CALIBRATION), "queue should succeed"));
        helper.runAfterDelay(20, () -> {
            var head = state.researchQueue().get(0);
            helper.assertTrue(head.runTickRequired() < 200, "speed augment should reduce runtime");
            helper.assertTrue(head.runPowerMultiplierBps() > 10_000, "augment should increase power multiplier");
        });
        helper.succeedWhen(() -> helper.assertTrue(!state.researchQueue().isEmpty() || ResearchManager.isResearched(server, controller.teamId(), SIGNAL_CALIBRATION), "expected queue to progress"));
    }

    private static FunctionalStation buildFunctionalStation(GameTestHelper helper) {
        return buildFunctionalStation(helper, 1, 1, 1);
    }

    private static FunctionalStation buildFunctionalStation(GameTestHelper helper, int minX, int minY, int minZ) {
        FunctionalStation station = new FunctionalStation(
                new BlockPos(minX + 1, minY + 1, minZ),
                new BlockPos(minX, minY + 1, minZ),
                new BlockPos(minX, minY, minZ),
                new BlockPos(minX + 1, minY, minZ),
                new BlockPos(minX + 2, minY, minZ),
                new BlockPos(minX + 1, minY, minZ + 1),
                new BlockPos(minX + 2, minY + 1, minZ + 1),
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
        helper.setBlock(station.inputPos(), Registration.ELECTRIC_POWER_INPUT_BLOCK.get().defaultBlockState().setValue(BlockStateProperties.FACING, Direction.NORTH));
        placeController(helper, station.controllerPos(), Direction.NORTH);
        return station;
    }

    private static void placeController(GameTestHelper helper, BlockPos pos, Direction facing) {
        helper.setBlock(pos, Registration.RESEARCH_CONTROLLER_T1_BLOCK.get().defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, facing));
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

    private static String teamId(GameTestHelper helper, String base, BlockPos controllerPos) {
        return base + "_" + helper.absolutePos(controllerPos).asLong();
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

    private static int findCorruptionWindowOrdinal(ResearchControllerBlockEntity controller, ResourceLocation nodeId, int completedRuns, double higherChance, double lowerChance) {
        for (int ordinal = 1; ordinal <= 64; ordinal++) {
            String eventKey = "disk_write_" + ordinal;
            boolean higher = ResearchDeterministicRng.rollChance(controller.teamId(), controller.stationId(), nodeId, completedRuns, eventKey, higherChance);
            boolean lower = ResearchDeterministicRng.rollChance(controller.teamId(), controller.stationId(), nodeId, completedRuns, eventKey, lowerChance);
            if (higher && !lower) {
                return ordinal;
            }
        }
        throw new IllegalStateException("expected deterministic corruption window for test seed");
    }

    private static net.neoforged.neoforge.items.IItemHandler itemCapability(GameTestHelper helper, BlockPos pos, Direction side) {
        return BlockCapabilityCache.create(Capabilities.ItemHandler.BLOCK, helper.getLevel(), helper.absolutePos(pos), side).getCapability();
    }

    private static net.neoforged.neoforge.energy.IEnergyStorage energyCapability(GameTestHelper helper, BlockPos pos, Direction side) {
        return BlockCapabilityCache.create(Capabilities.EnergyStorage.BLOCK, helper.getLevel(), helper.absolutePos(pos), side).getCapability();
    }

    private record FunctionalStation(
            BlockPos controllerPos,
            BlockPos inputPos,
            BlockPos logicHousingPos,
            BlockPos researchDrivePos,
            BlockPos materialStoragePos,
            BlockPos outputPortPos,
            BlockPos secondOutputPortPos,
            BlockPos augmenterPos
    ) {
    }
}
