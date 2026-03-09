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

        helper.succeedWhen(() -> helper.assertTrue(
                controller.availableResearchPower(Integer.MAX_VALUE) > 0,
                "expected FE input to expose research power without buffering"
        ));
    }

    @GameTest(template = "empty", timeoutTicks = 120)
    public static void electric_input_tiers_scale_fe_buffer_capacity(GameTestHelper helper) {
        BuiltStation t1Station = buildStation(helper, Registration.RESEARCH_CONTROLLER_T1_BLOCK.get(), Registration.ELECTRIC_POWER_INPUT_BLOCK.get());
        bindController(helper, t1Station.controllerPos(), "phase7_fe_buffer_t1");
        ElectricPowerInputBlockEntity t1Input = requireBlockEntity(helper, t1Station.inputPos(), ElectricPowerInputBlockEntity.class);

        BuiltStation t4Station = fillCasingShell(helper, 6, 1, 1);
        placeController(helper, t4Station.controllerPos(), Registration.RESEARCH_CONTROLLER_T1_BLOCK.get(), Direction.NORTH);
        helper.setBlock(t4Station.inputPos(), Registration.ELECTRIC_POWER_INPUT_T4_BLOCK.get());
        bindController(helper, t4Station.controllerPos(), "phase7_fe_buffer_t4");
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
        ResearchControllerBlockEntity controller = bindController(helper, station.controllerPos(), "phase6_research_progress");
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

    @GameTest(template = "empty", timeoutTicks = 120)
    public static void controller_tier_changes_capacity_and_category_gate(GameTestHelper helper) {
        BuiltStation station = buildStation(helper, Registration.RESEARCH_CONTROLLER_T1_BLOCK.get(), Registration.ELECTRIC_POWER_INPUT_BLOCK.get());
        ResearchControllerBlockEntity controllerT1 = bindController(helper, station.controllerPos(), "phase6_tier_swap");
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
        bindController(helper, station.controllerPos(), "phase6_fe_full");
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
        ResearchControllerBlockEntity controller = bindController(helper, station.controllerPos(), "phase6_mixed_family");
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
        ResearchControllerBlockEntity controller = bindController(helper, station.controllerPos(), "phase6_input_type");
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
        ResearchControllerBlockEntity controller = bindController(helper, station.controllerPos(), "phase6_input_count");
        controller.revalidateStructure();
        helper.assertFalse(controller.isFormed(), "station should be invalid without a power input");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void controller_must_face_outward(GameTestHelper helper) {
        BuiltStation station = fillCasingShell(helper);
        placeController(helper, station.controllerPos(), Registration.RESEARCH_CONTROLLER_T1_BLOCK.get(), Direction.SOUTH);
        helper.setBlock(station.inputPos(), Registration.ELECTRIC_POWER_INPUT_BLOCK.get());
        ResearchControllerBlockEntity controller = bindController(helper, station.controllerPos(), "phase6_controller_facing");
        controller.revalidateStructure();
        helper.assertFalse(controller.isFormed(), "controller should face outward from the multiblock");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void controller_must_be_in_center_column(GameTestHelper helper) {
        BuiltStation station = fillCasingShell(helper);
        placeController(helper, station.inputPos(), Registration.RESEARCH_CONTROLLER_T1_BLOCK.get(), Direction.NORTH);
        helper.setBlock(station.controllerPos(), Registration.ELECTRIC_POWER_INPUT_BLOCK.get());
        ResearchControllerBlockEntity controller = bindController(helper, station.inputPos(), "phase6_controller_column");
        controller.revalidateStructure();
        helper.assertFalse(controller.isFormed(), "controller should only form in the center column of the outward face");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 160)
    public static void mechanical_input_requires_operational_create_network(GameTestHelper helper) {
        BuiltStation station = buildStation(helper, Registration.RESEARCH_CONTROLLER_T1_BLOCK.get(), Registration.MECHANICAL_POWER_INPUT_BLOCK.get());
        ResearchControllerBlockEntity controller = bindController(helper, station.controllerPos(), "phase6_mechanical");
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
        BuiltStation station = fillCasingShell(helper);
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
        helper.setBlock(station.logicHousingPos(), Registration.LOGIC_HOUSING_BLOCK.get().defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH));
        helper.setBlock(station.researchDrivePos(), Registration.RESEARCH_DRIVE_BLOCK.get().defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH));
        helper.setBlock(station.materialStoragePos(), Registration.MATERIAL_STORAGE_BLOCK.get().defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH));
        helper.setBlock(station.outputPortPos(), Registration.OUTPUT_PORT_BLOCK.get().defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH));
        helper.setBlock(station.augmenterPos(), Registration.AUGMENTER_BLOCK.get().defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH));
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
        Direction front = input.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
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
