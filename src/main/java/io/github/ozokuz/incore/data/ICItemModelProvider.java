package io.github.ozokuz.incore.data;

import io.github.ozokuz.incore.INCore;
import io.github.ozokuz.incore.Registration;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public class ICItemModelProvider extends ItemModelProvider {
    public ICItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, INCore.MODID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        basicItem(Registration.ENCOUNTER_WAND_ITEM.get());
    }
}
