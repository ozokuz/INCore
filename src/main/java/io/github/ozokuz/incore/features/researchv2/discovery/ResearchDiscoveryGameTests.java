package io.github.ozokuz.incore.features.researchv2.discovery;

import io.github.ozokuz.incore.INCore;
import io.github.ozokuz.incore.Registration;
import io.github.ozokuz.incore.features.researchv2.ResearchManager;
import io.github.ozokuz.incore.features.researchv2.state.ResearchNetworkSavedData;
import io.github.ozokuz.incore.features.researchv2.state.TeamResearchState;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("incore")
@PrefixGameTestTemplate(false)
public final class ResearchDiscoveryGameTests {
    private static final ResourceLocation SIGNAL_CALIBRATION = ResourceLocation.fromNamespaceAndPath(INCore.MODID, "signal_calibration");
    private static final ResourceLocation TERRAIN_SCANNING = ResourceLocation.fromNamespaceAndPath(INCore.MODID, "terrain_scanning");
    private static final ResourceLocation CONTINUUM_SIGNAL_THEORY = ResourceLocation.fromNamespaceAndPath(INCore.MODID, "continuum_signal_theory");

    private ResearchDiscoveryGameTests() {
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void field_research_note_grants_discovery(GameTestHelper helper) {
        helper.setBlock(new BlockPos(1, 1, 1), Registration.STONE_SURFACE_STONE_SPOT_BLOCK.get());
        FieldResearchRegistry.FieldResearchDefinition mapping = FieldResearchRegistry.match(helper.getBlockState(new BlockPos(1, 1, 1)));
        helper.assertTrue(mapping != null, "expected stone surface block to map to field research");

        ItemStack note = mapping.createNote(Registration.FIELD_RESEARCH_NOTE_ITEM.get());
        DiscoveryPayload payload = DiscoveryPayloadData.read(note);
        helper.assertTrue(payload.nodeIds().contains(TERRAIN_SCANNING), "expected note payload to include terrain scanning");

        MinecraftServer server = helper.getLevel().getServer();
        helper.assertTrue(server != null, "expected test server");
        String teamId = teamId(helper, "field");
        helper.assertTrue(DiscoveryGrantService.grantFromPayload(server, teamId, payload), "expected note payload to grant discovery");
        helper.assertTrue(ResearchManager.isDiscovered(server, teamId, TERRAIN_SCANNING), "expected terrain scanning to be discovered");
        helper.assertTrue(ResearchManager.snapshotJson(server, teamId).contains("incore:terrain_scanning"), "expected snapshot json to include discovered node");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 260)
    public static void datalogger_generates_report_for_matching_environment(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, Registration.DATALOGGER_BLOCK.get());
        DataloggerBlockEntity datalogger = requireBlockEntity(helper, pos, DataloggerBlockEntity.class);

        helper.runAfterDelay(220, () -> {
            helper.assertTrue(datalogger.hasBufferedReport(), "expected datalogger to generate a report");
            DiscoveryPayload payload = DiscoveryPayloadData.read(datalogger.bufferedReport());
            helper.assertTrue(payload.nodeIds().contains(SIGNAL_CALIBRATION), "expected report payload to include signal calibration");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 140)
    public static void translator_decodes_continuum_reports(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, Registration.TRANSLATOR_BLOCK.get());
        TranslatorBlockEntity translator = requireBlockEntity(helper, pos, TranslatorBlockEntity.class);

        ItemStack raw = new ItemStack(Registration.CONTINUUM_DATA_REPORT_ITEM.get());
        ContinuumDataReportData.writeReportId(raw, ResourceLocation.fromNamespaceAndPath(INCore.MODID, "ancient_signal_fragment"));
        helper.assertTrue(translator.tryInsertInput(raw), "expected translator input insertion to succeed");

        helper.runAfterDelay(110, () -> {
            ItemStack output = translator.output();
            helper.assertTrue(output.is(Registration.DECODED_CONTINUUM_REPORT_ITEM.get()), "expected translator to output decoded report");
            DiscoveryPayload payload = DiscoveryPayloadData.read(output);
            helper.assertTrue(payload.nodeIds().contains(CONTINUUM_SIGNAL_THEORY), "expected decoded report to unlock continuum signal theory");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 160)
    public static void research_samples_transfer_discovery_between_teams(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, Registration.RESEARCH_SAMPLE_FABRICATOR_BLOCK.get());
        ResearchSampleFabricatorBlockEntity fabricator = requireBlockEntity(helper, pos, ResearchSampleFabricatorBlockEntity.class);
        fabricator.itemHandler().setStackInSlot(0, new ItemStack(Registration.BLANK_RESEARCH_SAMPLE_ITEM.get()));

        MinecraftServer server = helper.getLevel().getServer();
        helper.assertTrue(server != null, "expected test server");
        String sourceTeam = teamId(helper, "sample_source");
        String targetTeam = teamId(helper, "sample_target");

        TeamResearchState sourceState = ResearchManager.ensureTeamState(server, sourceTeam);
        sourceState.discoveredNodes().add(SIGNAL_CALIBRATION);
        sourceState.completedNodes().add(SIGNAL_CALIBRATION);
        ResearchNetworkSavedData.get(server).setDirty();

        helper.assertTrue(fabricator.fabricate(sourceTeam, SIGNAL_CALIBRATION), "expected fabricator to start creating a research sample");
        helper.runAfterDelay(110, () -> {
            ItemStack sample = fabricator.itemHandler().getStackInSlot(1);
            helper.assertTrue(sample.is(Registration.RESEARCH_SAMPLE_ITEM.get()), "expected fabricated output to be a research sample");

            DiscoveryPayload payload = DiscoveryPayloadData.read(sample);
            helper.assertTrue(DiscoveryGrantService.grantFromPayload(server, targetTeam, payload), "expected sample to grant discovery to another team");
            helper.assertTrue(ResearchManager.isDiscovered(server, targetTeam, SIGNAL_CALIBRATION), "expected target team to discover the node");
            helper.succeed();
        });
    }

    private static <T> T requireBlockEntity(GameTestHelper helper, BlockPos pos, Class<T> type) {
        Object blockEntity = helper.getBlockEntity(pos);
        helper.assertTrue(type.isInstance(blockEntity), "expected block entity " + type.getSimpleName() + " at " + pos);
        return type.cast(blockEntity);
    }

    private static String teamId(GameTestHelper helper, String base) {
        return base + "_" + helper.absolutePos(new BlockPos(1, 1, 1)).asLong();
    }
}
