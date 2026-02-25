package io.github.ozokuz.incore.features.surfaceore;

import io.github.ozokuz.incore.INCore;
import net.minecraft.client.Minecraft;

import xaero.common.minimap.waypoints.Waypoint;
import xaero.hud.minimap.BuiltInHudModules;
import xaero.hud.minimap.waypoint.WaypointColor;
import xaero.hud.minimap.waypoint.set.WaypointSet;

public class XaeroWaypointIntegration {
    private static String SURFACE_SPOT_WAYPOINT_SET_KEY = "incore:surface_spots";

    public static void addWaypoint(String name, String marker, int x, int y, int z) {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null) {
                return;
            }

            getSurfaceSpotWaypointSet().add(new Waypoint(x, y, z, name, marker, WaypointColor.GRAY));
            BuiltInHudModules.MINIMAP.getCurrentSession().getWorldManager().getCurrentWorld().setCurrentWaypointSetId(SURFACE_SPOT_WAYPOINT_SET_KEY);
        } catch (Exception e) {
            INCore.LOGGER.warn("Failed to add Xaero waypoint: {}", e.getMessage());
        }
    }

    private static WaypointSet getSurfaceSpotWaypointSet() {
        var manager = BuiltInHudModules.MINIMAP.getCurrentSession().getWorldManager().getCurrentWorld();

        WaypointSet set = manager.getWaypointSet(SURFACE_SPOT_WAYPOINT_SET_KEY);
        if (set == null) {
            manager.addWaypointSet(SURFACE_SPOT_WAYPOINT_SET_KEY);
            set = manager.getWaypointSet(SURFACE_SPOT_WAYPOINT_SET_KEY);
        }

        return set;
    }
}
