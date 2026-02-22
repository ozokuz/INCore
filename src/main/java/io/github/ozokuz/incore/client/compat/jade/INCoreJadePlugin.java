package io.github.ozokuz.incore.client.compat.jade;

import io.github.ozokuz.incore.INCore;
import io.github.ozokuz.incore.features.research.BurnerLabBlock;
import io.github.ozokuz.incore.features.research.LabBlockEntity;
import io.github.ozokuz.incore.features.research.LabTier;
import io.github.ozokuz.incore.features.research.MechanicalLabBlock;
import io.github.ozokuz.incore.features.research.ModularLabBlock;
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

@WailaPlugin
public class INCoreJadePlugin implements IWailaPlugin {
    private static final BurnerProvider BURNER_PROVIDER = new BurnerProvider();
    private static final MechanicalProvider MECHANICAL_PROVIDER = new MechanicalProvider();
    private static final ModularProvider MODULAR_PROVIDER = new ModularProvider();

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(BURNER_PROVIDER, LabBlockEntity.class);
        registration.registerBlockDataProvider(MECHANICAL_PROVIDER, LabBlockEntity.class);
        registration.registerBlockDataProvider(MODULAR_PROVIDER, LabBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(BURNER_PROVIDER, BurnerLabBlock.class);
        registration.registerBlockComponent(MECHANICAL_PROVIDER, MechanicalLabBlock.class);
        registration.registerBlockComponent(MODULAR_PROVIDER, ModularLabBlock.class);
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
}
