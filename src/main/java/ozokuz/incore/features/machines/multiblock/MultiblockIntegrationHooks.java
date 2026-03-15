package ozokuz.incore.features.machines.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public final class MultiblockIntegrationHooks {
    @FunctionalInterface
    public interface TopologyChangedHook {
        void onTopologyChanged(Level level, BlockPos pos);
    }

    @FunctionalInterface
    public interface OutputModeChangedHook {
        void onOutputModeChanged(OutputPortBlockEntity port, OutputPortMode mode);
    }

    @FunctionalInterface
    public interface StationTierResolver {
        int resolve(Level level, BlockPos controllerPos);
    }

    private static TopologyChangedHook topologyChangedHook = (level, pos) -> {
    };
    private static OutputModeChangedHook outputModeChangedHook = (port, mode) -> {
    };
    private static StationTierResolver stationTierResolver = (level, controllerPos) -> 1;

    private MultiblockIntegrationHooks() {
    }

    public static void setTopologyChangedHook(TopologyChangedHook hook) {
        topologyChangedHook = hook == null ? (level, pos) -> {
        } : hook;
    }

    public static void setOutputModeChangedHook(OutputModeChangedHook hook) {
        outputModeChangedHook = hook == null ? (port, mode) -> {
        } : hook;
    }

    public static void setStationTierResolver(StationTierResolver resolver) {
        stationTierResolver = resolver == null ? (level, controllerPos) -> 1 : resolver;
    }

    public static void onTopologyChanged(Level level, BlockPos pos) {
        if (level == null || pos == null || level.isClientSide) {
            return;
        }
        topologyChangedHook.onTopologyChanged(level, pos);
    }

    public static void onOutputModeChanged(OutputPortBlockEntity port, OutputPortMode mode) {
        if (port == null || port.getLevel() == null || port.getLevel().isClientSide) {
            return;
        }
        outputModeChangedHook.onOutputModeChanged(port, mode);
    }

    public static int stationTier(Level level, BlockPos controllerPos) {
        if (level == null || controllerPos == null) {
            return 1;
        }
        return Math.max(1, stationTierResolver.resolve(level, controllerPos));
    }
}
