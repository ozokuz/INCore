package ozokuz.incore.client.compat.jade;

import ozokuz.incore.INCore;
import ozokuz.incore.features.market.content.MarketTerminalMeBlock;
import ozokuz.incore.features.market.content.MarketTerminalMeBlockEntity;
import ozokuz.incore.features.market.content.MarketAutoTraderBlock;
import ozokuz.incore.features.market.content.MarketAutoTraderBlockEntity;
import ozokuz.incore.features.market.content.MarketAutoTraderMk2Block;
import ozokuz.incore.features.market.content.MarketAutoTraderMk2BlockEntity;
import ozokuz.incore.features.market.content.ShipmentTerminalBlock;
import ozokuz.incore.features.market.content.ShipmentTerminalBlockEntity;
import ozokuz.incore.features.market.content.ShipmentTerminalMk2Block;
import ozokuz.incore.features.market.content.ShipmentTerminalMk2BlockEntity;
import ozokuz.incore.features.research.ResearchManager;
import ozokuz.incore.features.research.discovery.DataloggerBlock;
import ozokuz.incore.features.research.discovery.DataloggerBlockEntity;
import ozokuz.incore.features.research.discovery.ResearchSampleFabricatorBlock;
import ozokuz.incore.features.research.discovery.ResearchSampleFabricatorBlockEntity;
import ozokuz.incore.features.research.discovery.TranslatorBlock;
import ozokuz.incore.features.research.discovery.TranslatorBlockEntity;
import ozokuz.incore.features.research.model.ResearchNodeDefinition;
import ozokuz.incore.features.research.registry.ResearchRegistry;
import ozokuz.incore.features.research.state.ActiveResearchRun;
import ozokuz.incore.features.research.state.ResearchQueueStatus;
import ozokuz.incore.features.research.station.AbstractResearchControllerBlock;
import ozokuz.incore.features.research.station.CrudeResearchStationBlock;
import ozokuz.incore.features.research.station.CrudeResearchStationBlockEntity;
import ozokuz.incore.features.research.station.ResearchOrchestrationService;
import ozokuz.incore.features.research.station.ResearchOrchestratorControllerBlock;
import ozokuz.incore.features.research.station.ResearchOrchestratorControllerBlockEntity;
import ozokuz.incore.features.research.station.ResearchControllerBlockEntity;
import ozokuz.incore.features.roguelike.content.DungeonAltarAutomatorBlock;
import ozokuz.incore.features.roguelike.content.DungeonAltarAutomatorBlockEntity;
import ozokuz.incore.features.surfaceore.SurfaceOreSpotBlock;
import ozokuz.incore.features.surfaceore.SurfaceOreSpotBlockEntity;
import ozokuz.incore.features.surfaceore.SurfaceStoneSpotBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import ozokuz.incore.features.research.station.network.StationNetworkService;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.BoxStyle;
import snownee.jade.api.ui.IElementHelper;

import java.util.Locale;

@WailaPlugin
public class INCoreJadePlugin implements IWailaPlugin {
    private static final MarketAutoTraderProvider MARKET_AUTOTRADER_PROVIDER = new MarketAutoTraderProvider();
    private static final MarketAutoTraderMk2Provider MARKET_AUTOTRADER_MK2_PROVIDER = new MarketAutoTraderMk2Provider();
    private static final MarketTerminalMeProvider MARKET_TERMINAL_ME_PROVIDER = new MarketTerminalMeProvider();
    private static final ShipmentTerminalProvider SHIPMENT_TERMINAL_PROVIDER = new ShipmentTerminalProvider();
    private static final ShipmentTerminalMk2Provider SHIPMENT_TERMINAL_MK2_PROVIDER = new ShipmentTerminalMk2Provider();
    private static final DungeonAltarAutomatorProvider DUNGEON_ALTAR_AUTOMATOR_PROVIDER = new DungeonAltarAutomatorProvider();
    private static final SurfaceOreSpotProvider SURFACE_ORE_SPOT_PROVIDER = new SurfaceOreSpotProvider();
    private static final SurfaceStoneSpotProvider SURFACE_STONE_SPOT_PROVIDER = new SurfaceStoneSpotProvider();
    private static final CrudeResearchStationProvider CRUDE_RESEARCH_STATION_PROVIDER = new CrudeResearchStationProvider();
    private static final ResearchControllerProvider RESEARCH_CONTROLLER_PROVIDER = new ResearchControllerProvider();
    private static final ResearchOrchestratorProvider RESEARCH_ORCHESTRATOR_PROVIDER = new ResearchOrchestratorProvider();
    private static final DataloggerProvider DATALOGGER_PROVIDER = new DataloggerProvider();
    private static final TranslatorProvider TRANSLATOR_PROVIDER = new TranslatorProvider();
    private static final ResearchSampleFabricatorProvider RESEARCH_SAMPLE_FABRICATOR_PROVIDER = new ResearchSampleFabricatorProvider();

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(MARKET_AUTOTRADER_PROVIDER, MarketAutoTraderBlockEntity.class);
        registration.registerBlockDataProvider(MARKET_AUTOTRADER_MK2_PROVIDER, MarketAutoTraderMk2BlockEntity.class);
        registration.registerBlockDataProvider(MARKET_TERMINAL_ME_PROVIDER, MarketTerminalMeBlockEntity.class);
        registration.registerBlockDataProvider(SHIPMENT_TERMINAL_PROVIDER, ShipmentTerminalBlockEntity.class);
        registration.registerBlockDataProvider(SHIPMENT_TERMINAL_MK2_PROVIDER, ShipmentTerminalMk2BlockEntity.class);
        registration.registerBlockDataProvider(DUNGEON_ALTAR_AUTOMATOR_PROVIDER, DungeonAltarAutomatorBlockEntity.class);
        registration.registerBlockDataProvider(SURFACE_ORE_SPOT_PROVIDER, SurfaceOreSpotBlockEntity.class);
        registration.registerBlockDataProvider(CRUDE_RESEARCH_STATION_PROVIDER, CrudeResearchStationBlockEntity.class);
        registration.registerBlockDataProvider(RESEARCH_CONTROLLER_PROVIDER, ResearchControllerBlockEntity.class);
        registration.registerBlockDataProvider(RESEARCH_ORCHESTRATOR_PROVIDER, ResearchOrchestratorControllerBlockEntity.class);
        registration.registerBlockDataProvider(DATALOGGER_PROVIDER, DataloggerBlockEntity.class);
        registration.registerBlockDataProvider(TRANSLATOR_PROVIDER, TranslatorBlockEntity.class);
        registration.registerBlockDataProvider(RESEARCH_SAMPLE_FABRICATOR_PROVIDER, ResearchSampleFabricatorBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(MARKET_AUTOTRADER_PROVIDER, MarketAutoTraderBlock.class);
        registration.registerBlockComponent(MARKET_AUTOTRADER_MK2_PROVIDER, MarketAutoTraderMk2Block.class);
        registration.registerBlockComponent(MARKET_TERMINAL_ME_PROVIDER, MarketTerminalMeBlock.class);
        registration.registerBlockComponent(SHIPMENT_TERMINAL_PROVIDER, ShipmentTerminalBlock.class);
        registration.registerBlockComponent(SHIPMENT_TERMINAL_MK2_PROVIDER, ShipmentTerminalMk2Block.class);
        registration.registerBlockComponent(DUNGEON_ALTAR_AUTOMATOR_PROVIDER, DungeonAltarAutomatorBlock.class);
        registration.registerBlockComponent(SURFACE_ORE_SPOT_PROVIDER, SurfaceOreSpotBlock.class);
        registration.registerBlockComponent(SURFACE_STONE_SPOT_PROVIDER, SurfaceStoneSpotBlock.class);
        registration.registerBlockComponent(CRUDE_RESEARCH_STATION_PROVIDER, CrudeResearchStationBlock.class);
        registration.registerBlockComponent(RESEARCH_CONTROLLER_PROVIDER, AbstractResearchControllerBlock.class);
        registration.registerBlockComponent(RESEARCH_ORCHESTRATOR_PROVIDER, ResearchOrchestratorControllerBlock.class);
        registration.registerBlockComponent(DATALOGGER_PROVIDER, DataloggerBlock.class);
        registration.registerBlockComponent(TRANSLATOR_PROVIDER, TranslatorBlock.class);
        registration.registerBlockComponent(RESEARCH_SAMPLE_FABRICATOR_PROVIDER, ResearchSampleFabricatorBlock.class);
    }

    private abstract static class BaseMarketAutoTraderProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
        private final ResourceLocation uid;

        protected BaseMarketAutoTraderProvider(String uidPath) {
            this.uid = ResourceLocation.fromNamespaceAndPath(INCore.MODID, uidPath);
        }

        @Override
        public ResourceLocation getUid() {
            return uid;
        }

        @Override
        public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            MarketAutoTraderBlockEntity autoTrader = autoTraderIfSupported(accessor);
            if (autoTrader == null) {
                return;
            }

            data.putInt("status", autoTrader.statusForDisplay());
            data.putInt("progress", autoTrader.progressForDisplay());
            data.putInt("max_progress", autoTrader.maxProgressForDisplay());
            data.putInt("price_cap", autoTrader.priceCapSpurForDisplay());
            data.putInt("batch_size", autoTrader.batchSizeForDisplay());
            data.putBoolean("enabled", autoTrader.statusForDisplay() != MarketAutoTraderBlockEntity.STATUS_DISABLED);
            ResourceLocation targetItemId = autoTrader.targetItemIdForDisplay();
            if (targetItemId != null) {
                data.putString("target_item_id", targetItemId.toString());
            }
            appendPowerServerData(data, autoTrader);
        }

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            if (autoTraderIfSupported(accessor) == null) {
                return;
            }

            CompoundTag data = accessor.getServerData();
            if (data.isEmpty()) {
                return;
            }

            Component statusText = autoTraderStatusText(data.getInt("status"));
            tooltip.add(Component.translatable("jade.incore.autotrader.status", statusText));
            tooltip.add(Component.translatable(
                    "jade.incore.autotrader.progress",
                    data.getInt("progress"),
                    data.getInt("max_progress")
            ));
            String targetId = data.getString("target_item_id");
            Component targetText = targetId.isBlank()
                    ? Component.translatable("jade.incore.autotrader.target.none")
                    : Component.literal(targetId);
            tooltip.add(Component.translatable("jade.incore.autotrader.target", targetText));
            tooltip.add(Component.translatable("jade.incore.autotrader.price_cap", data.getInt("price_cap")));
            tooltip.add(Component.translatable("jade.incore.autotrader.batch_size", data.getInt("batch_size")));
            Component enabledText = data.getBoolean("enabled")
                    ? Component.translatable("jade.incore.autotrader.enabled.on")
                    : Component.translatable("jade.incore.autotrader.enabled.off");
            tooltip.add(Component.translatable("jade.incore.autotrader.enabled", enabledText));
            appendPowerTooltip(tooltip, data);
        }

        protected abstract MarketAutoTraderBlockEntity autoTraderIfSupported(BlockAccessor accessor);

        protected abstract void appendPowerServerData(CompoundTag data, MarketAutoTraderBlockEntity autoTrader);

        protected abstract void appendPowerTooltip(ITooltip tooltip, CompoundTag data);
    }

    private static class MarketAutoTraderProvider extends BaseMarketAutoTraderProvider {
        private MarketAutoTraderProvider() {
            super("market_autotrader");
        }

        @Override
        protected MarketAutoTraderBlockEntity autoTraderIfSupported(BlockAccessor accessor) {
            if (!(accessor.getBlockEntity() instanceof MarketAutoTraderBlockEntity autoTrader)) {
                return null;
            }
            return autoTrader instanceof MarketAutoTraderMk2BlockEntity ? null : autoTrader;
        }

        @Override
        protected void appendPowerServerData(CompoundTag data, MarketAutoTraderBlockEntity autoTrader) {
            data.putInt("rpm", autoTrader.rpmForDisplay());
        }

        @Override
        protected void appendPowerTooltip(ITooltip tooltip, CompoundTag data) {
            tooltip.add(Component.translatable("jade.incore.autotrader.rpm", data.getInt("rpm")));
        }
    }

    private static class MarketAutoTraderMk2Provider extends BaseMarketAutoTraderProvider {
        private MarketAutoTraderMk2Provider() {
            super("market_autotrader_mk2");
        }

        @Override
        protected MarketAutoTraderBlockEntity autoTraderIfSupported(BlockAccessor accessor) {
            if (!(accessor.getBlockEntity() instanceof MarketAutoTraderMk2BlockEntity autoTrader)) {
                return null;
            }
            return autoTrader;
        }

        @Override
        protected void appendPowerServerData(CompoundTag data, MarketAutoTraderBlockEntity autoTrader) {
            if (!(autoTrader instanceof MarketAutoTraderMk2BlockEntity mk2)) {
                return;
            }
            data.putInt("fe", mk2.energyStoredForDisplay());
            data.putInt("fe_cap", mk2.energyCapacityForDisplay());
        }

        @Override
        protected void appendPowerTooltip(ITooltip tooltip, CompoundTag data) {
            tooltip.add(Component.translatable("jade.incore.autotrader.energy", data.getInt("fe"), data.getInt("fe_cap")));
        }
    }

    private abstract static class BaseShipmentTerminalProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
        private final ResourceLocation uid;

        protected BaseShipmentTerminalProvider(String uidPath) {
            this.uid = ResourceLocation.fromNamespaceAndPath(INCore.MODID, uidPath);
        }

        @Override
        public ResourceLocation getUid() {
            return uid;
        }

        @Override
        public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            ShipmentTerminalBlockEntity terminal = terminalIfSupported(accessor);
            if (terminal == null) {
                return;
            }

            data.putInt("status", terminal.statusForDisplay());
            data.putInt("progress", terminal.progressForDisplay());
            data.putInt("max_progress", terminal.maxProgressForDisplay());
            appendPowerServerData(data, terminal);
        }

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            if (terminalIfSupported(accessor) == null) {
                return;
            }

            CompoundTag data = accessor.getServerData();
            if (data.isEmpty()) {
                return;
            }

            Component statusText = shipmentStatusText(data.getInt("status"));
            tooltip.add(Component.translatable("jade.incore.shipment.status", statusText));
            tooltip.add(Component.translatable(
                    "jade.incore.shipment.progress",
                    data.getInt("progress"),
                    data.getInt("max_progress")
            ));
            appendPowerTooltip(tooltip, data);
        }

        protected abstract ShipmentTerminalBlockEntity terminalIfSupported(BlockAccessor accessor);

        protected abstract void appendPowerServerData(CompoundTag data, ShipmentTerminalBlockEntity terminal);

        protected abstract void appendPowerTooltip(ITooltip tooltip, CompoundTag data);
    }

    private static class ShipmentTerminalProvider extends BaseShipmentTerminalProvider {
        private ShipmentTerminalProvider() {
            super("shipment_terminal");
        }

        @Override
        protected ShipmentTerminalBlockEntity terminalIfSupported(BlockAccessor accessor) {
            if (!(accessor.getBlockEntity() instanceof ShipmentTerminalBlockEntity terminal)) {
                return null;
            }
            return terminal instanceof ShipmentTerminalMk2BlockEntity ? null : terminal;
        }

        @Override
        protected void appendPowerServerData(CompoundTag data, ShipmentTerminalBlockEntity terminal) {
            data.putInt("rpm", terminal.rpmForDisplay());
        }

        @Override
        protected void appendPowerTooltip(ITooltip tooltip, CompoundTag data) {
            tooltip.add(Component.translatable("jade.incore.shipment.rpm", data.getInt("rpm")));
        }
    }

    private static class ShipmentTerminalMk2Provider extends BaseShipmentTerminalProvider {
        private ShipmentTerminalMk2Provider() {
            super("shipment_terminal_mk2");
        }

        @Override
        protected ShipmentTerminalBlockEntity terminalIfSupported(BlockAccessor accessor) {
            if (!(accessor.getBlockEntity() instanceof ShipmentTerminalMk2BlockEntity terminal)) {
                return null;
            }
            return terminal;
        }

        @Override
        protected void appendPowerServerData(CompoundTag data, ShipmentTerminalBlockEntity terminal) {
            if (!(terminal instanceof ShipmentTerminalMk2BlockEntity mk2)) {
                return;
            }
            data.putInt("fe", mk2.energyStoredForDisplay());
            data.putInt("fe_cap", mk2.energyCapacityForDisplay());
        }

        @Override
        protected void appendPowerTooltip(ITooltip tooltip, CompoundTag data) {
            tooltip.add(Component.translatable("jade.incore.shipment.energy", data.getInt("fe"), data.getInt("fe_cap")));
        }
    }

    private static class MarketTerminalMeProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
        private final ResourceLocation uid = ResourceLocation.fromNamespaceAndPath(INCore.MODID, "market_terminal_me");

        @Override
        public ResourceLocation getUid() {
            return uid;
        }

        @Override
        public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            if (!(accessor.getBlockEntity() instanceof MarketTerminalMeBlockEntity terminal)) {
                return;
            }
            data.putBoolean("has_card", !terminal.cardStack().isEmpty());
            data.putBoolean("ae2_linked", terminal.ae2Linked());
            data.putBoolean("ae2_online", terminal.ae2Online());
        }

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            CompoundTag data = accessor.getServerData();
            if (data.isEmpty()) {
                return;
            }
            Component cardText = data.getBoolean("has_card")
                    ? Component.translatable("jade.incore.market_terminal.card.inserted")
                    : Component.translatable("jade.incore.market_terminal.card.missing");
            tooltip.add(Component.translatable("jade.incore.market_terminal.card", cardText));
            tooltip.add(Component.translatable("jade.incore.market_terminal.me", marketTerminalMeStatusText(data.getBoolean("ae2_linked"), data.getBoolean("ae2_online"))));
        }
    }

    private static class DungeonAltarAutomatorProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
        private final ResourceLocation uid = ResourceLocation.fromNamespaceAndPath(INCore.MODID, "dungeon_altar_automator");

        @Override
        public ResourceLocation getUid() {
            return uid;
        }

        @Override
        public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            if (!(accessor.getBlockEntity() instanceof DungeonAltarAutomatorBlockEntity automator)) {
                return;
            }
            data.putInt("status", automator.statusForDisplay());
            data.putBoolean("ae2_linked", automator.ae2Linked());
            data.putBoolean("ae2_online", automator.ae2Online());
            data.putBoolean("crystal_loaded", !automator.getItem(DungeonAltarAutomatorBlockEntity.CRYSTAL_SLOT).isEmpty());
            if (automator.boundAltarPos() != null) {
                data.putLong("bound_altar", automator.boundAltarPos().asLong());
            }
        }

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            CompoundTag data = accessor.getServerData();
            if (data.isEmpty()) {
                return;
            }
            tooltip.add(Component.translatable("jade.incore.automator.status", automatorStatusText(data.getInt("status"))));
            tooltip.add(Component.translatable("jade.incore.automator.me", marketTerminalMeStatusText(data.getBoolean("ae2_linked"), data.getBoolean("ae2_online"))));
            Component crystalText = data.getBoolean("crystal_loaded")
                    ? Component.translatable("jade.incore.automator.crystal.loaded")
                    : Component.translatable("jade.incore.automator.crystal.empty");
            tooltip.add(Component.translatable("jade.incore.automator.crystal", crystalText));
            Component bindingText = data.contains("bound_altar")
                    ? Component.literal(posText(BlockPos.of(data.getLong("bound_altar"))))
                    : Component.translatable("jade.incore.automator.binding.none");
            tooltip.add(Component.translatable("jade.incore.automator.binding", bindingText));
        }
    }

    private static class SurfaceOreSpotProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
        private final ResourceLocation uid = ResourceLocation.fromNamespaceAndPath(INCore.MODID, "surface_ore_spot");

        @Override
        public ResourceLocation getUid() {
            return uid;
        }

        @Override
        public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            if (!(accessor.getBlockState().getBlock() instanceof SurfaceOreSpotBlock oreSpot)) {
                return;
            }
            if (!(accessor.getBlockEntity() instanceof SurfaceOreSpotBlockEntity oreSpotEntity)) {
                return;
            }

            data.putString("ore_type", oreSpot.oreType().getSerializedName());
            data.putInt("remaining_mines", oreSpotEntity.remainingMines());
            data.putInt("max_mines", oreSpotEntity.maxMines());
        }

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            CompoundTag data = accessor.getServerData();
            if (data.isEmpty()) {
                return;
            }

            tooltip.add(Component.translatable("jade.incore.surface_ore.type", humanizeName(data.getString("ore_type"))));
            int maxMines = data.getInt("max_mines");
            if (maxMines > 0) {
                tooltip.add(Component.translatable("jade.incore.surface_ore.mines", data.getInt("remaining_mines"), maxMines));
            }
        }
    }

    private static class SurfaceStoneSpotProvider implements IBlockComponentProvider {
        private final ResourceLocation uid = ResourceLocation.fromNamespaceAndPath(INCore.MODID, "surface_stone_spot");

        @Override
        public ResourceLocation getUid() {
            return uid;
        }

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            if (!(accessor.getBlockState().getBlock() instanceof SurfaceStoneSpotBlock stoneSpot)) {
                return;
            }

            tooltip.add(Component.translatable("jade.incore.surface_stone.type", humanizeName(stoneSpot.stoneType().getSerializedName())));
            tooltip.add(Component.translatable("jade.incore.surface_stone.mines"));
        }
    }

    private static class CrudeResearchStationProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
        private final ResourceLocation uid = ResourceLocation.fromNamespaceAndPath(INCore.MODID, "crude_research_station");

        @Override
        public ResourceLocation getUid() {
            return uid;
        }

        @Override
        public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            if (!(accessor.getBlockEntity() instanceof CrudeResearchStationBlockEntity station)) {
                return;
            }

            data.putBoolean("team_linked", !station.teamId().isBlank());
            data.putInt("rp_buffer", station.researchPowerBuffer());
            data.putInt("burn_time", station.burnTimeRemainingForDisplay());
            data.putInt("burn_total", station.burnTimeTotalForDisplay());
            data.putInt("queue_status", station.queueStatusForDisplay());
            data.putInt("run_tick_progress", station.runTickProgressForDisplay());
            data.putInt("run_tick_required", station.runTickRequiredForDisplay());
            data.putInt("completed_runs", station.completedRunsForDisplay());
            data.putInt("required_runs", station.requiredRunsForDisplay());

            if (accessor.getLevel() != null && accessor.getLevel().getServer() != null && !station.teamId().isBlank()) {
                var state = ResearchManager.ensureTeamState(accessor.getLevel().getServer(), station.teamId());
                if (!state.researchQueue().isEmpty()) {
                    data.putString("active_node", state.researchQueue().get(0).nodeId().toString());
                }
            }
        }

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            CompoundTag data = accessor.getServerData();
            if (data.isEmpty()) {
                return;
            }

            Component linkedText = data.getBoolean("team_linked")
                    ? Component.translatable("jade.incore.crude_station.team.linked")
                    : Component.translatable("jade.incore.crude_station.team.unlinked");
            tooltip.add(Component.translatable("jade.incore.crude_station.team", linkedText));
            tooltip.add(Component.translatable("jade.incore.crude_station.rp", data.getInt("rp_buffer")));
            tooltip.add(Component.translatable("jade.incore.crude_station.burn", data.getInt("burn_time"), data.getInt("burn_total")));

            int queueStatus = data.getInt("queue_status");
            if (queueStatus < 0) {
                tooltip.add(Component.translatable("jade.incore.crude_station.idle"));
                return;
            }

            int requiredRuns = Math.max(1, data.getInt("required_runs"));
            int completedRuns = Math.max(0, Math.min(data.getInt("completed_runs"), requiredRuns));
            tooltip.add(Component.translatable(
                    "jade.incore.crude_station.run",
                    Math.min(requiredRuns, completedRuns + 1),
                    requiredRuns
            ));
            tooltip.add(Component.translatable(
                    "jade.incore.crude_station.progress",
                    data.getInt("run_tick_progress"),
                    data.getInt("run_tick_required")
            ));

            if (data.contains("active_node")) {
                tooltip.add(Component.translatable("jade.incore.crude_station.node", data.getString("active_node")));
            }

            tooltip.add(Component.translatable("jade.incore.crude_station.status", crudeStationStatusText(queueStatus)));
        }
    }

    private static class ResearchControllerProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
        private final ResourceLocation uid = ResourceLocation.fromNamespaceAndPath(INCore.MODID, "research_controller_station");

        @Override
        public ResourceLocation getUid() {
            return uid;
        }

        @Override
        public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            if (!(accessor.getBlockEntity() instanceof ResearchControllerBlockEntity controller)) {
                return;
            }

            data.putBoolean("team_linked", !controller.teamId().isBlank());
            data.putBoolean("formed", controller.isFormed());
            data.putInt("tier", controller.stationTier());
            data.putInt("rp_available", controller.availableResearchPower(Integer.MAX_VALUE));
            data.putInt("part_count", controller.connectedPartCount());
            data.putString("power_family", controller.powerFamily() == null ? "" : controller.powerFamily().name());
            data.putInt("power_input_tier", controller.powerInputTier());
            data.putInt("input_count", controller.powerInputPositions().size());
            if (controller.isFormed() && !controller.stationId().isBlank()) {
                data.putString("station_id", controller.stationId());
                var descriptor = controller.describeStation();
                if (descriptor != null) {
                    data.putString("output_mode", descriptor.outputPortModes());
                    data.putInt("disk_tier", descriptor.mountedDiskTier());
                    data.putInt("disk_snapshots", descriptor.mountedDiskSnapshotCount());
                    data.putInt("disk_corrupted", descriptor.mountedDiskCorruptedSegmentCount());
                    data.putDouble("augment_speed", descriptor.activeSpeedMultiplier());
                    data.putDouble("augment_power", descriptor.activePowerMultiplier());
                    data.putDouble("augment_bonus", descriptor.activeBonusRunChance());
                    data.putDouble("augment_corruption", descriptor.activeCorruptionMultiplier());
                }
            }
            if (accessor.getLevel() != null && accessor.getLevel().getServer() != null && !controller.teamId().isBlank()) {
                var state = ResearchManager.ensureTeamState(accessor.getLevel().getServer(), controller.teamId());
                if (!state.researchQueue().isEmpty()) {
                    var head = state.researchQueue().get(0);
                    ActiveResearchRun activeRun = head.activeRun(controller.stationId());
                    data.putString("active_node", head.nodeId().toString());
                    ResearchNodeDefinition node = ResearchRegistry.nodes().get(head.nodeId());
                    if (node != null) {
                        data.putString("active_node_name", node.name());
                    }
                    data.putInt("queue_status", head.status().ordinal());
                    data.putInt("run_tick_progress", activeRun == null ? head.runTickProgress() : activeRun.runTickProgress());
                    data.putInt("run_tick_required", activeRun == null ? head.runTickRequired() : activeRun.runTickRequired());
                    data.putInt("completed_runs", head.completedRuns());
                    data.putInt("required_runs", head.requiredRuns());
                }
            }
        }

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            CompoundTag data = accessor.getServerData();
            if (data.isEmpty()) {
                return;
            }

            Component linkedText = data.getBoolean("team_linked")
                    ? Component.translatable("jade.incore.research_controller.team.linked")
                    : Component.translatable("jade.incore.research_controller.team.unlinked");
            tooltip.add(Component.translatable("jade.incore.research_controller.team", linkedText));
            tooltip.add(Component.translatable("jade.incore.research_controller.tier", data.getInt("tier")));
            tooltip.add(Component.translatable("jade.incore.research_controller.rp_available", data.getInt("rp_available")));

            if (!data.getBoolean("formed")) {
                tooltip.add(Component.translatable("jade.incore.research_controller.formed.no"));
                return;
            }

            tooltip.add(Component.translatable("jade.incore.research_controller.formed.yes"));
            tooltip.add(Component.translatable("jade.incore.research_controller.parts", data.getInt("part_count")));
            tooltip.add(Component.translatable("jade.incore.research_controller.power_family", data.getString("power_family")));
            tooltip.add(Component.translatable("jade.incore.research_controller.power_input_tier", data.getInt("power_input_tier")));
            tooltip.add(Component.translatable("jade.incore.research_controller.inputs", data.getInt("input_count")));
            if (data.contains("station_id")) {
                tooltip.add(Component.translatable("jade.incore.research_controller.station_id", data.getString("station_id")));
                tooltip.add(Component.literal("Output: " + data.getString("output_mode")));
                tooltip.add(Component.literal("Disk: T" + data.getInt("disk_tier") + " snapshots=" + data.getInt("disk_snapshots") + " corrupted=" + data.getInt("disk_corrupted")));
                tooltip.add(Component.literal(String.format(java.util.Locale.ROOT, "Augments: speed=%.2f power=%.2f bonus=%.2f corruption=%.2f",
                        data.getDouble("augment_speed"),
                        data.getDouble("augment_power"),
                        data.getDouble("augment_bonus"),
                        data.getDouble("augment_corruption"))));
            }
            int queueStatus = data.contains("queue_status") ? data.getInt("queue_status") : -1;
            if (queueStatus < 0) {
                tooltip.add(Component.translatable("jade.incore.research_controller.idle"));
                return;
            }
            int requiredRuns = Math.max(1, data.getInt("required_runs"));
            int completedRuns = Math.max(0, Math.min(data.getInt("completed_runs"), requiredRuns));
            tooltip.add(Component.translatable("jade.incore.research_controller.run", Math.min(requiredRuns, completedRuns + 1), requiredRuns));
            tooltip.add(Component.translatable("jade.incore.research_controller.progress", data.getInt("run_tick_progress"), data.getInt("run_tick_required")));
            float progress = Math.max(0.0F, Math.min(1.0F, data.getInt("run_tick_progress") / (float) Math.max(1, data.getInt("run_tick_required"))));
            IElementHelper elements = IElementHelper.get();
            tooltip.add(elements.progress(
                    progress,
                    Component.empty(),
                    elements.progressStyle().color(0xFF55A9E6).textColor(0xFFFFFFFF),
                    BoxStyle.getNestedBox(),
                    false
            ));
            if (data.contains("active_node_name")) {
                tooltip.add(Component.translatable("jade.incore.research_controller.node", data.getString("active_node_name")));
            } else if (data.contains("active_node")) {
                tooltip.add(Component.translatable("jade.incore.research_controller.node", data.getString("active_node")));
            }
            tooltip.add(Component.translatable("jade.incore.research_controller.status", crudeStationStatusText(queueStatus)));
        }
    }

    private static class ResearchOrchestratorProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
        private final ResourceLocation uid = ResourceLocation.fromNamespaceAndPath(INCore.MODID, "research_orchestrator_controller");

        @Override
        public ResourceLocation getUid() {
            return uid;
        }

        @Override
        public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            if (!(accessor.getBlockEntity() instanceof ResearchOrchestratorControllerBlockEntity orchestrator)) {
                return;
            }

            data.putBoolean("team_linked", !orchestrator.teamId().isBlank());
            data.putBoolean("formed", orchestrator.isFormed());
            data.putInt("part_count", orchestrator.connectedParts().size());
            data.putInt("input_count", orchestrator.powerInputPositions().size());
            data.putInt("linking_port_count", orchestrator.linkingPortPositions().size());
            data.putBoolean("has_wireless_link", orchestrator.wirelessLinkPos() != null);
            data.putBoolean("has_drive", orchestrator.orchestrationDrivePos() != null);
            data.putBoolean("has_augmenter", orchestrator.augmenterPos() != null);
            data.putString("power_family", orchestrator.powerFamily() == null ? "" : orchestrator.powerFamily().name());
            data.putInt("power_input_tier", orchestrator.powerInputTier());
            if (!orchestrator.orchestratorId().isBlank()) {
                data.putString("orchestrator_id", orchestrator.orchestratorId());
            }

            if (accessor.getLevel() == null || accessor.getLevel().getServer() == null || orchestrator.teamId().isBlank()) {
                return;
            }
            var orchestrationSnapshot = ResearchOrchestrationService.snapshot(accessor.getLevel().getServer(), orchestrator.teamId());
            var networkSnapshot = StationNetworkService.snapshot(accessor.getLevel().getServer(), orchestrator.teamId());
            data.putBoolean("orchestrator_required", orchestrationSnapshot.orchestratorRequired());
            data.putBoolean("orchestrator_present", orchestrationSnapshot.orchestratorPresent());
            data.putBoolean("orchestrator_valid", orchestrationSnapshot.orchestratorValid());
            data.putString("orchestrator_status", orchestrationSnapshot.orchestratorStatus());
            data.putString("orchestrator_warning", orchestrationSnapshot.orchestratorWarning());
            data.putString("wireless_channel_id", orchestrationSnapshot.wirelessChannelId());
            data.putInt("cable_capacity", orchestrationSnapshot.cableCapacityPerLink());
            data.putInt("wireless_capacity", orchestrationSnapshot.wirelessCapacity());
            data.putInt("wireless_range", orchestrationSnapshot.wirelessRange());
            data.putBoolean("infinite_wireless", orchestrationSnapshot.infiniteWireless());
            data.putBoolean("interdimensional_wireless", orchestrationSnapshot.interdimensionalWireless());
            data.putInt("valid_wireless_members", orchestrationSnapshot.validWirelessStationIds().size());
            data.putInt("invalid_wireless_members", orchestrationSnapshot.invalidWirelessStationIds().size());
            data.putInt("team_network_count", networkSnapshot.stationNetworkCount());
            data.putBoolean("team_network_valid", networkSnapshot.stationNetworkValid());
        }

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            CompoundTag data = accessor.getServerData();
            if (data.isEmpty()) {
                return;
            }

            Component linkedText = data.getBoolean("team_linked")
                    ? Component.translatable("jade.incore.research_orchestrator.team.linked")
                    : Component.translatable("jade.incore.research_orchestrator.team.unlinked");
            tooltip.add(Component.translatable("jade.incore.research_orchestrator.team", linkedText));
            tooltip.add(Component.translatable("jade.incore.research_orchestrator.required", yesNoText(data.getBoolean("orchestrator_required"))));

            if (!data.getBoolean("formed")) {
                tooltip.add(Component.translatable("jade.incore.research_orchestrator.formed.no"));
                return;
            }

            tooltip.add(Component.translatable("jade.incore.research_orchestrator.formed.yes"));
            tooltip.add(Component.translatable("jade.incore.research_orchestrator.parts", data.getInt("part_count")));
            tooltip.add(Component.translatable("jade.incore.research_orchestrator.inputs", data.getInt("input_count")));
            tooltip.add(Component.translatable("jade.incore.research_orchestrator.link_ports", data.getInt("linking_port_count")));
            tooltip.add(Component.translatable("jade.incore.research_orchestrator.power_family", data.getString("power_family")));
            tooltip.add(Component.translatable("jade.incore.research_orchestrator.power_input_tier", data.getInt("power_input_tier")));
            tooltip.add(Component.translatable("jade.incore.research_orchestrator.drive", yesNoText(data.getBoolean("has_drive"))));
            tooltip.add(Component.translatable("jade.incore.research_orchestrator.wireless_link", yesNoText(data.getBoolean("has_wireless_link"))));
            tooltip.add(Component.translatable("jade.incore.research_orchestrator.augmenter", yesNoText(data.getBoolean("has_augmenter"))));
            tooltip.add(Component.translatable("jade.incore.research_orchestrator.valid", yesNoText(data.getBoolean("orchestrator_valid"))));
            tooltip.add(Component.translatable("jade.incore.research_orchestrator.cable_capacity", data.getInt("cable_capacity")));
            tooltip.add(Component.translatable("jade.incore.research_orchestrator.wireless_capacity", data.getInt("wireless_capacity")));
            tooltip.add(Component.translatable("jade.incore.research_orchestrator.wireless_members", data.getInt("valid_wireless_members"), data.getInt("invalid_wireless_members")));
            tooltip.add(Component.translatable("jade.incore.research_orchestrator.networks", data.getInt("team_network_count")));
            if (!data.getString("wireless_channel_id").isBlank()) {
                String channelId = data.getString("wireless_channel_id");
                String shortChannel = channelId.length() > 8 ? channelId.substring(0, 8) : channelId;
                tooltip.add(Component.translatable("jade.incore.research_orchestrator.channel", shortChannel));
            }
            tooltip.add(Component.translatable("jade.incore.research_orchestrator.mode", wirelessModeText(data)));
            if (!data.getString("orchestrator_warning").isBlank()) {
                tooltip.add(Component.translatable("jade.incore.research_orchestrator.warning", Component.translatable(data.getString("orchestrator_warning"))));
            }
        }
    }

    private static class DataloggerProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
        private final ResourceLocation uid = ResourceLocation.fromNamespaceAndPath(INCore.MODID, "datalogger");

        @Override
        public ResourceLocation getUid() {
            return uid;
        }

        @Override
        public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            if (!(accessor.getBlockEntity() instanceof DataloggerBlockEntity datalogger)) {
                return;
            }
            data.putInt("progress", datalogger.progressTicks());
            data.putInt("max_progress", datalogger.maxProgressTicks());
            data.putBoolean("ready", datalogger.hasBufferedReport());
        }

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            CompoundTag data = accessor.getServerData();
            if (data.isEmpty()) {
                return;
            }
            tooltip.add(Component.translatable(
                    "jade.incore.datalogger.progress",
                    data.getInt("progress"),
                    data.getInt("max_progress")
            ));
            float progress = Math.max(0.0F, Math.min(1.0F, data.getInt("progress") / (float) Math.max(1, data.getInt("max_progress"))));
            IElementHelper elements = IElementHelper.get();
            tooltip.add(elements.progress(
                    progress,
                    Component.empty(),
                    elements.progressStyle().color(0xFFBE8A45).textColor(0xFFFFFFFF),
                    BoxStyle.getNestedBox(),
                    false
            ));
            tooltip.add(Component.translatable(data.getBoolean("ready")
                    ? "jade.incore.datalogger.status.ready"
                    : "jade.incore.datalogger.status.scanning"));
        }
    }

    private static class TranslatorProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
        private final ResourceLocation uid = ResourceLocation.fromNamespaceAndPath(INCore.MODID, "translator");

        @Override
        public ResourceLocation getUid() {
            return uid;
        }

        @Override
        public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            if (!(accessor.getBlockEntity() instanceof TranslatorBlockEntity translator)) {
                return;
            }
            data.putInt("progress", translator.progressTicks());
            data.putInt("max_progress", translator.maxProgressTicks());
            data.putBoolean("has_input", !translator.input().isEmpty());
            data.putBoolean("ready", !translator.output().isEmpty());
        }

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            CompoundTag data = accessor.getServerData();
            if (data.isEmpty()) {
                return;
            }
            tooltip.add(Component.translatable(
                    "jade.incore.translator.progress",
                    data.getInt("progress"),
                    data.getInt("max_progress")
            ));
            float progress = Math.max(0.0F, Math.min(1.0F, data.getInt("progress") / (float) Math.max(1, data.getInt("max_progress"))));
            IElementHelper elements = IElementHelper.get();
            tooltip.add(elements.progress(
                    progress,
                    Component.empty(),
                    elements.progressStyle().color(0xFF55A9E6).textColor(0xFFFFFFFF),
                    BoxStyle.getNestedBox(),
                    false
            ));
            String statusKey = data.getBoolean("ready")
                    ? "jade.incore.translator.status.ready"
                    : (data.getBoolean("has_input") ? "jade.incore.translator.status.decoding" : "jade.incore.translator.status.idle");
            tooltip.add(Component.translatable(statusKey));
        }
    }

    private static class ResearchSampleFabricatorProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
        private final ResourceLocation uid = ResourceLocation.fromNamespaceAndPath(INCore.MODID, "research_sample_fabricator");

        @Override
        public ResourceLocation getUid() {
            return uid;
        }

        @Override
        public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            if (!(accessor.getBlockEntity() instanceof ResearchSampleFabricatorBlockEntity fabricator)) {
                return;
            }
            data.putInt("progress", fabricator.progressTicks());
            data.putInt("max_progress", fabricator.maxProgressTicks());
            data.putBoolean("processing", fabricator.isProcessing());
            data.putBoolean("ready", !fabricator.itemHandler().getStackInSlot(1).isEmpty());
        }

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            CompoundTag data = accessor.getServerData();
            if (data.isEmpty()) {
                return;
            }
            tooltip.add(Component.translatable(
                    "jade.incore.research_sample_fabricator.progress",
                    data.getInt("progress"),
                    data.getInt("max_progress")
            ));
            float progress = Math.max(0.0F, Math.min(1.0F, data.getInt("progress") / (float) Math.max(1, data.getInt("max_progress"))));
            IElementHelper elements = IElementHelper.get();
            tooltip.add(elements.progress(
                    progress,
                    Component.empty(),
                    elements.progressStyle().color(0xFF55A9E6).textColor(0xFFFFFFFF),
                    BoxStyle.getNestedBox(),
                    false
            ));
            String statusKey = data.getBoolean("ready")
                    ? "jade.incore.research_sample_fabricator.status.ready"
                    : (data.getBoolean("processing") ? "jade.incore.research_sample_fabricator.status.processing" : "jade.incore.research_sample_fabricator.status.idle");
            tooltip.add(Component.translatable(statusKey));
        }
    }

    private static Component shipmentStatusText(int status) {
        return switch (status) {
            case ShipmentTerminalBlockEntity.STATUS_DISABLED -> Component.translatable("screen.incore.market.shipment.status.disabled");
            case ShipmentTerminalBlockEntity.STATUS_NO_CARD -> Component.translatable("screen.incore.market.shipment.status.no_card");
            case ShipmentTerminalBlockEntity.STATUS_NO_ITEMS -> Component.translatable("screen.incore.market.shipment.status.no_items");
            case ShipmentTerminalBlockEntity.STATUS_INVALID_ITEM -> Component.translatable("screen.incore.market.shipment.status.invalid_item");
            case ShipmentTerminalBlockEntity.STATUS_NEED_FULL_STACK -> Component.translatable("screen.incore.market.shipment.status.need_full_stack");
            case ShipmentTerminalBlockEntity.STATUS_NO_RPM -> Component.translatable("screen.incore.market.shipment.status.no_rpm");
            case ShipmentTerminalBlockEntity.STATUS_NO_STRESS -> Component.translatable("screen.incore.market.shipment.status.no_stress");
            case ShipmentTerminalBlockEntity.STATUS_NO_POWER -> Component.translatable("screen.incore.market.shipment.status.no_power");
            default -> Component.translatable("screen.incore.market.shipment.status.ready");
        };
    }

    private static Component autoTraderStatusText(int status) {
        return switch (status) {
            case MarketAutoTraderBlockEntity.STATUS_DISABLED -> Component.translatable("screen.incore.market.autotrader.status.disabled");
            case MarketAutoTraderBlockEntity.STATUS_NO_CARD -> Component.translatable("screen.incore.market.autotrader.status.no_card");
            case MarketAutoTraderBlockEntity.STATUS_NO_TARGET -> Component.translatable("screen.incore.market.autotrader.status.no_target");
            case MarketAutoTraderBlockEntity.STATUS_PRICE_TOO_HIGH -> Component.translatable("screen.incore.market.autotrader.status.price_too_high");
            case MarketAutoTraderBlockEntity.STATUS_NO_FUNDS -> Component.translatable("screen.incore.market.autotrader.status.no_funds");
            case MarketAutoTraderBlockEntity.STATUS_OUTPUT_FULL -> Component.translatable("screen.incore.market.autotrader.status.output_full");
            case MarketAutoTraderBlockEntity.STATUS_NO_RPM -> Component.translatable("screen.incore.market.autotrader.status.no_rpm");
            case MarketAutoTraderBlockEntity.STATUS_NO_STRESS -> Component.translatable("screen.incore.market.autotrader.status.no_stress");
            case MarketAutoTraderBlockEntity.STATUS_NO_POWER -> Component.translatable("screen.incore.market.autotrader.status.no_power");
            default -> Component.translatable("screen.incore.market.autotrader.status.ready");
        };
    }

    private static Component marketTerminalMeStatusText(boolean linked, boolean online) {
        if (!linked) {
            return Component.translatable("screen.incore.market.ae2.unlinked");
        }
        return online
                ? Component.translatable("screen.incore.market.ae2.online")
                : Component.translatable("screen.incore.market.ae2.offline");
    }

    private static Component automatorStatusText(int status) {
        return switch (status) {
            case DungeonAltarAutomatorBlockEntity.STATUS_AE2_OFFLINE -> Component.translatable("incore.roguelike.automator.status.ae2_offline");
            case DungeonAltarAutomatorBlockEntity.STATUS_NO_CRYSTAL -> Component.translatable("incore.roguelike.automator.status.no_crystal");
            case DungeonAltarAutomatorBlockEntity.STATUS_REQUESTING -> Component.translatable("incore.roguelike.automator.status.requesting");
            case DungeonAltarAutomatorBlockEntity.STATUS_ALTAR_COMPLETE -> Component.translatable("incore.roguelike.automator.status.altar_complete");
            case DungeonAltarAutomatorBlockEntity.STATUS_AWAITING_ITEMS -> Component.translatable("incore.roguelike.automator.status.awaiting_items");
            default -> Component.translatable("incore.roguelike.automator.status.no_altar_above");
        };
    }

    private static String posText(BlockPos pos) {
        return pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
    }

    private static Component crudeStationStatusText(int statusOrdinal) {
        if (statusOrdinal < 0 || statusOrdinal >= ResearchQueueStatus.values().length) {
            return Component.translatable("screen.incore.crude_research_station.status.idle");
        }
        ResearchQueueStatus status = ResearchQueueStatus.values()[statusOrdinal];
        return switch (status) {
            case RUNNING -> Component.translatable("screen.incore.crude_research_station.status.running");
            case PAUSED_MISSING_INPUTS -> Component.translatable("screen.incore.crude_research_station.status.missing_inputs");
            case PAUSED_NO_POWER -> Component.translatable("screen.incore.crude_research_station.status.no_power");
            case PAUSED_NETWORK_CONFLICT -> Component.translatable("screen.incore.research_controller.run_status.network_conflict");
            default -> Component.translatable("screen.incore.crude_research_station.status.queued");
        };
    }
    private static Component yesNoText(boolean value) {
        return Component.translatable(value ? "screen.incore.common.present" : "screen.incore.common.missing");
    }

    private static Component wirelessModeText(CompoundTag data) {
        if (data.getBoolean("interdimensional_wireless")) {
            return Component.translatable("screen.incore.research_orchestrator.mode.interdimensional");
        }
        if (data.getBoolean("infinite_wireless")) {
            return Component.translatable("screen.incore.research_orchestrator.mode.infinite");
        }
        return Component.translatable("screen.incore.research_orchestrator.mode.local");
    }
    private static String humanizeName(String serializedName) {
        if (serializedName == null || serializedName.isBlank()) {
            return "Unknown";
        }

        String[] parts = serializedName.split("_");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            String lower = part.toLowerCase(Locale.ROOT);
            builder.append(Character.toUpperCase(lower.charAt(0)));
            if (lower.length() > 1) {
                builder.append(lower.substring(1));
            }
        }
        return builder.length() == 0 ? "Unknown" : builder.toString();
    }
}
