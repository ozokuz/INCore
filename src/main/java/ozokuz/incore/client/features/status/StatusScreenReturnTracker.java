package ozokuz.incore.client.features.status;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;

public final class StatusScreenReturnTracker {
    private static @Nullable Screen pendingParent;
    private static @Nullable Screen externalParent;
    private static @Nullable Screen externalScreen;

    private StatusScreenReturnTracker() {
    }

    public static void prepare(Screen parent) {
        pendingParent = parent;
    }

    public static @Nullable Screen consumePendingParent() {
        Screen parent = pendingParent;
        pendingParent = null;
        return parent;
    }

    public static void prepareExternal(Screen parent) {
        externalParent = parent;
        externalScreen = null;
    }

    public static void onClientTick(Minecraft minecraft) {
        if (externalParent == null) {
            return;
        }

        Screen current = minecraft.screen;
        if (externalScreen == null) {
            if (current != null && current != externalParent) {
                externalScreen = current;
            }
            return;
        }

        if (current == null) {
            Screen parent = externalParent;
            externalParent = null;
            externalScreen = null;
            minecraft.setScreen(parent);
        }
    }
}
