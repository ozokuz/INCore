package io.github.ozokuz.incore.client.compat.jade;

import io.github.ozokuz.incore.INCore;
import io.github.ozokuz.incore.features.market.content.MarketAutoBuyerBlock;
import io.github.ozokuz.incore.features.market.content.MarketAutoBuyerBlockEntity;
import io.github.ozokuz.incore.features.market.content.MarketAutoBuyerMk2Block;
import io.github.ozokuz.incore.features.market.content.MarketAutoBuyerMk2BlockEntity;
import io.github.ozokuz.incore.features.market.content.ShipmentTerminalBlock;
import io.github.ozokuz.incore.features.market.content.ShipmentTerminalBlockEntity;
import io.github.ozokuz.incore.features.market.content.ShipmentTerminalMk2Block;
import io.github.ozokuz.incore.features.market.content.ShipmentTerminalMk2BlockEntity;
import io.github.ozokuz.incore.features.research.BurnerLabBlock;
import io.github.ozokuz.incore.features.research.LabBlockEntity;
import io.github.ozokuz.incore.features.research.LabTier;
import io.github.ozokuz.incore.features.research.MechanicalLabBlock;
import io.github.ozokuz.incore.features.research.ModularLabBlock;
import io.github.ozokuz.incore.features.surfaceore.SurfaceOreSpotBlock;
import io.github.ozokuz.incore.features.surfaceore.SurfaceOreSpotBlockEntity;
import io.github.ozokuz.incore.features.surfaceore.SurfaceStoneSpotBlock;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

import java.util.Locale;

@WailaPlugin
public class INCoreJadePlugin implements IWailaPlugin {
    private static final BurnerProvider BURNER_PROVIDER = new BurnerProvider();
    private static final MechanicalProvider MECHANICAL_PROVIDER = new MechanicalProvider();
    private static final ModularProvider MODULAR_PROVIDER = new ModularProvider();
    private static final MarketAutoBuyerProvider MARKET_AUTOBUYER_PROVIDER = new MarketAutoBuyerProvider();
    private static final MarketAutoBuyerMk2Provider MARKET_AUTOBUYER_MK2_PROVIDER = new MarketAutoBuyerMk2Provider();
    private static final ShipmentTerminalProvider SHIPMENT_TERMINAL_PROVIDER = new ShipmentTerminalProvider();
    private static final ShipmentTerminalMk2Provider SHIPMENT_TERMINAL_MK2_PROVIDER = new ShipmentTerminalMk2Provider();
    private static final SurfaceOreSpotProvider SURFACE_ORE_SPOT_PROVIDER = new SurfaceOreSpotProvider();
    private static final SurfaceStoneSpotProvider SURFACE_STONE_SPOT_PROVIDER = new SurfaceStoneSpotProvider();

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(BURNER_PROVIDER, LabBlockEntity.class);
        registration.registerBlockDataProvider(MECHANICAL_PROVIDER, LabBlockEntity.class);
        registration.registerBlockDataProvider(MODULAR_PROVIDER, LabBlockEntity.class);
        registration.registerBlockDataProvider(MARKET_AUTOBUYER_PROVIDER, MarketAutoBuyerBlockEntity.class);
        registration.registerBlockDataProvider(MARKET_AUTOBUYER_MK2_PROVIDER, MarketAutoBuyerMk2BlockEntity.class);
        registration.registerBlockDataProvider(SHIPMENT_TERMINAL_PROVIDER, ShipmentTerminalBlockEntity.class);
        registration.registerBlockDataProvider(SHIPMENT_TERMINAL_MK2_PROVIDER, ShipmentTerminalMk2BlockEntity.class);
        registration.registerBlockDataProvider(SURFACE_ORE_SPOT_PROVIDER, SurfaceOreSpotBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(BURNER_PROVIDER, BurnerLabBlock.class);
        registration.registerBlockComponent(MECHANICAL_PROVIDER, MechanicalLabBlock.class);
        registration.registerBlockComponent(MODULAR_PROVIDER, ModularLabBlock.class);
        registration.registerBlockComponent(MARKET_AUTOBUYER_PROVIDER, MarketAutoBuyerBlock.class);
        registration.registerBlockComponent(MARKET_AUTOBUYER_MK2_PROVIDER, MarketAutoBuyerMk2Block.class);
        registration.registerBlockComponent(SHIPMENT_TERMINAL_PROVIDER, ShipmentTerminalBlock.class);
        registration.registerBlockComponent(SHIPMENT_TERMINAL_MK2_PROVIDER, ShipmentTerminalMk2Block.class);
        registration.registerBlockComponent(SURFACE_ORE_SPOT_PROVIDER, SurfaceOreSpotBlock.class);
        registration.registerBlockComponent(SURFACE_STONE_SPOT_PROVIDER, SurfaceStoneSpotBlock.class);
    }

    private abstract static class BaseProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
        private final LabTier tier;
        private final ResourceLocation uid;

        protected BaseProvider(LabTier tier, String uidPath) {
            this.tier = tier;
            this.uid = ResourceLocation.fromNamespaceAndPath(INCore.MODID, uidPath);
        }

        @Override
        public ResourceLocation getUid() {
            return uid;
        }

        @Override
        public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            LabBlockEntity lab = labIfSupported(accessor);
            if (lab == null) {
                return;
            }

            data.putString("tier", lab.labTierId());
            data.putString("owner_name", lab.ownerNameForDisplay());
            data.putInt("status", lab.labStatusForDisplay());
            data.putInt("progress", lab.progressForDisplay());
            data.putInt("max_progress", lab.maxProgressForDisplay());
            data.putInt("overall_progress", lab.overallProgressForDisplay());
            data.putInt("overall_max", lab.overallMaxForDisplay());
            appendTierServerData(data, lab);
        }

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            CompoundTag data = accessor.getServerData();
            if (data.isEmpty() || !matchesDataTier(data)) {
                return;
            }

            String tierId = data.getString("tier");
            tooltip.add(Component.translatable("jade.incore.lab.tier", tierId));
            tooltip.add(Component.translatable("jade.incore.lab.owner", data.getString("owner_name")));

            int status = data.getInt("status");
            Component statusText = switch (status) {
                case LabBlockEntity.STATUS_WORKING -> Component.translatable("screen.incore.research_lab.status.working");
                case LabBlockEntity.STATUS_NOT_ENOUGH_MATERIALS -> Component.translatable("screen.incore.research_lab.status.not_enough_materials");
                default -> Component.translatable("screen.incore.research_lab.status.no_research_selected");
            };
            tooltip.add(Component.translatable("jade.incore.lab.status", statusText));
            tooltip.add(Component.translatable(
                    "jade.incore.lab.progress",
                    data.getInt("progress"),
                    data.getInt("max_progress")
            ));
            tooltip.add(Component.translatable(
                    "jade.incore.lab.overall",
                    data.getInt("overall_progress"),
                    data.getInt("overall_max")
            ));
            appendTierTooltip(tooltip, data);
        }

        private boolean matchesDataTier(CompoundTag data) {
            return tier.id().equals(data.getString("tier"));
        }

        private LabBlockEntity labIfSupported(BlockAccessor accessor) {
            if (!(accessor.getBlockEntity() instanceof LabBlockEntity lab)) {
                return null;
            }
            return lab.labTier() == tier ? lab : null;
        }

        protected abstract void appendTierServerData(CompoundTag data, LabBlockEntity lab);

        protected abstract void appendTierTooltip(ITooltip tooltip, CompoundTag data);
    }

    private static class BurnerProvider extends BaseProvider {
        private BurnerProvider() {
            super(LabTier.BURNER, "lab_status_burner");
        }

        @Override
        protected void appendTierServerData(CompoundTag data, LabBlockEntity lab) {
            data.putInt("burn_time", lab.burnTimeForDisplay());
            data.putInt("burn_total", lab.burnTimeTotalForDisplay());
        }

        @Override
        protected void appendTierTooltip(ITooltip tooltip, CompoundTag data) {
            tooltip.add(Component.translatable("jade.incore.lab.burn", data.getInt("burn_time"), data.getInt("burn_total")));
        }
    }

    private static class MechanicalProvider extends BaseProvider {
        private MechanicalProvider() {
            super(LabTier.MECHANICAL, "lab_status_mechanical");
        }

        @Override
        protected void appendTierServerData(CompoundTag data, LabBlockEntity lab) {
            data.putInt("rpm", Math.round(lab.mechanicalRpmForDisplay()));
            data.putInt("stress", Math.round(lab.mechanicalStressForDisplay()));
        }

        @Override
        protected void appendTierTooltip(ITooltip tooltip, CompoundTag data) {
            tooltip.add(Component.translatable("jade.incore.lab.mechanical", data.getInt("rpm"), data.getInt("stress")));
        }
    }

    private static class ModularProvider extends BaseProvider {
        private ModularProvider() {
            super(LabTier.MODULAR, "lab_status_modular");
        }

        @Override
        protected void appendTierServerData(CompoundTag data, LabBlockEntity lab) {
            data.putInt("fe", lab.energyStoredForDisplay());
            data.putInt("fe_cap", lab.energyCapacityForDisplay());
        }

        @Override
        protected void appendTierTooltip(ITooltip tooltip, CompoundTag data) {
            tooltip.add(Component.translatable("jade.incore.lab.energy", data.getInt("fe"), data.getInt("fe_cap")));
        }
    }

    private abstract static class BaseMarketAutoBuyerProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
        private final ResourceLocation uid;

        protected BaseMarketAutoBuyerProvider(String uidPath) {
            this.uid = ResourceLocation.fromNamespaceAndPath(INCore.MODID, uidPath);
        }

        @Override
        public ResourceLocation getUid() {
            return uid;
        }

        @Override
        public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            MarketAutoBuyerBlockEntity autoBuyer = autoBuyerIfSupported(accessor);
            if (autoBuyer == null) {
                return;
            }

            data.putInt("status", autoBuyer.statusForDisplay());
            data.putInt("progress", autoBuyer.progressForDisplay());
            data.putInt("max_progress", autoBuyer.maxProgressForDisplay());
            data.putInt("price_cap", autoBuyer.priceCapSpurForDisplay());
            data.putInt("batch_size", autoBuyer.batchSizeForDisplay());
            data.putBoolean("enabled", autoBuyer.enabledForDisplay());
            ResourceLocation targetItemId = autoBuyer.targetItemIdForDisplay();
            if (targetItemId != null) {
                data.putString("target_item_id", targetItemId.toString());
            }
            appendPowerServerData(data, autoBuyer);
        }

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            if (autoBuyerIfSupported(accessor) == null) {
                return;
            }

            CompoundTag data = accessor.getServerData();
            if (data.isEmpty()) {
                return;
            }

            Component statusText = autoBuyerStatusText(data.getInt("status"));
            tooltip.add(Component.translatable("jade.incore.autobuyer.status", statusText));
            tooltip.add(Component.translatable(
                    "jade.incore.autobuyer.progress",
                    data.getInt("progress"),
                    data.getInt("max_progress")
            ));
            String targetId = data.getString("target_item_id");
            Component targetText = targetId.isBlank()
                    ? Component.translatable("jade.incore.autobuyer.target.none")
                    : Component.literal(targetId);
            tooltip.add(Component.translatable("jade.incore.autobuyer.target", targetText));
            tooltip.add(Component.translatable("jade.incore.autobuyer.price_cap", data.getInt("price_cap")));
            tooltip.add(Component.translatable("jade.incore.autobuyer.batch_size", data.getInt("batch_size")));
            Component enabledText = data.getBoolean("enabled")
                    ? Component.translatable("jade.incore.autobuyer.enabled.on")
                    : Component.translatable("jade.incore.autobuyer.enabled.off");
            tooltip.add(Component.translatable("jade.incore.autobuyer.enabled", enabledText));
            appendPowerTooltip(tooltip, data);
        }

        protected abstract MarketAutoBuyerBlockEntity autoBuyerIfSupported(BlockAccessor accessor);

        protected abstract void appendPowerServerData(CompoundTag data, MarketAutoBuyerBlockEntity autoBuyer);

        protected abstract void appendPowerTooltip(ITooltip tooltip, CompoundTag data);
    }

    private static class MarketAutoBuyerProvider extends BaseMarketAutoBuyerProvider {
        private MarketAutoBuyerProvider() {
            super("market_autobuyer");
        }

        @Override
        protected MarketAutoBuyerBlockEntity autoBuyerIfSupported(BlockAccessor accessor) {
            if (!(accessor.getBlockEntity() instanceof MarketAutoBuyerBlockEntity autoBuyer)) {
                return null;
            }
            return autoBuyer instanceof MarketAutoBuyerMk2BlockEntity ? null : autoBuyer;
        }

        @Override
        protected void appendPowerServerData(CompoundTag data, MarketAutoBuyerBlockEntity autoBuyer) {
            data.putInt("rpm", autoBuyer.rpmForDisplay());
        }

        @Override
        protected void appendPowerTooltip(ITooltip tooltip, CompoundTag data) {
            tooltip.add(Component.translatable("jade.incore.autobuyer.rpm", data.getInt("rpm")));
        }
    }

    private static class MarketAutoBuyerMk2Provider extends BaseMarketAutoBuyerProvider {
        private MarketAutoBuyerMk2Provider() {
            super("market_autobuyer_mk2");
        }

        @Override
        protected MarketAutoBuyerBlockEntity autoBuyerIfSupported(BlockAccessor accessor) {
            if (!(accessor.getBlockEntity() instanceof MarketAutoBuyerMk2BlockEntity autoBuyer)) {
                return null;
            }
            return autoBuyer;
        }

        @Override
        protected void appendPowerServerData(CompoundTag data, MarketAutoBuyerBlockEntity autoBuyer) {
            if (!(autoBuyer instanceof MarketAutoBuyerMk2BlockEntity mk2)) {
                return;
            }
            data.putInt("fe", mk2.energyStoredForDisplay());
            data.putInt("fe_cap", mk2.energyCapacityForDisplay());
        }

        @Override
        protected void appendPowerTooltip(ITooltip tooltip, CompoundTag data) {
            tooltip.add(Component.translatable("jade.incore.autobuyer.energy", data.getInt("fe"), data.getInt("fe_cap")));
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

    private static Component shipmentStatusText(int status) {
        return switch (status) {
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

    private static Component autoBuyerStatusText(int status) {
        return switch (status) {
            case MarketAutoBuyerBlockEntity.STATUS_DISABLED -> Component.translatable("screen.incore.market.autobuyer.status.disabled");
            case MarketAutoBuyerBlockEntity.STATUS_NO_CARD -> Component.translatable("screen.incore.market.autobuyer.status.no_card");
            case MarketAutoBuyerBlockEntity.STATUS_NO_TARGET -> Component.translatable("screen.incore.market.autobuyer.status.no_target");
            case MarketAutoBuyerBlockEntity.STATUS_PRICE_TOO_HIGH -> Component.translatable("screen.incore.market.autobuyer.status.price_too_high");
            case MarketAutoBuyerBlockEntity.STATUS_NO_FUNDS -> Component.translatable("screen.incore.market.autobuyer.status.no_funds");
            case MarketAutoBuyerBlockEntity.STATUS_OUTPUT_FULL -> Component.translatable("screen.incore.market.autobuyer.status.output_full");
            case MarketAutoBuyerBlockEntity.STATUS_NO_RPM -> Component.translatable("screen.incore.market.autobuyer.status.no_rpm");
            case MarketAutoBuyerBlockEntity.STATUS_NO_STRESS -> Component.translatable("screen.incore.market.autobuyer.status.no_stress");
            case MarketAutoBuyerBlockEntity.STATUS_NO_POWER -> Component.translatable("screen.incore.market.autobuyer.status.no_power");
            default -> Component.translatable("screen.incore.market.autobuyer.status.ready");
        };
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
