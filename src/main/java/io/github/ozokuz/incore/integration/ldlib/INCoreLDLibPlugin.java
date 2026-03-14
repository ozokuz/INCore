package io.github.ozokuz.incore.integration.ldlib;

import com.lowdragmc.lowdraglib2.plugin.ILDLibPlugin;
import com.lowdragmc.lowdraglib2.plugin.LDLibPlugin;
import io.github.ozokuz.incore.INCore;

@LDLibPlugin
public class INCoreLDLibPlugin implements ILDLibPlugin {
    public void onLoad() {
        INCore.LOGGER.info("Incore LDLibPlugin loaded");
    }
}
