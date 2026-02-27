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

        basicItem(Registration.SANITY_BOOSTER_SMALL_ITEM.get());
        basicItem(Registration.SANITY_BOOSTER_LARGE_ITEM.get());
        basicItem(Registration.SANITY_VESSEL_ITEM.get());

        //basicItem(Registration.SANITY_CRATE_ITEM.get());
        //basicItem(Registration.BATTLEPASS_LANE_UNLOCK_ITEM.get());

//        basicItem(Registration.CRIMSITE_ORE_LOCATOR.get());
//        basicItem(Registration.VERIDIUM_ORE_LOCATOR.get());
//        basicItem(Registration.ASURINE_ORE_LOCATOR.get());
//        basicItem(Registration.OCHRUM_ORE_LOCATOR.get());
//        basicItem(Registration.CINNABAR_ORE_LOCATOR.get());
//        basicItem(Registration.MIXED_METALS_ORE_LOCATOR.get());
//        basicItem(Registration.GEM_CLUSTERS_ORE_LOCATOR.get());
//        basicItem(Registration.NETHER_QUARTZ_ORE_LOCATOR.get());
//        basicItem(Registration.STONE_LOCATOR.get());
//        basicItem(Registration.DEEPSLATE_LOCATOR.get());
//        basicItem(Registration.LIMESTONE_LOCATOR.get());
//        basicItem(Registration.BASALT_LOCATOR.get());
//        basicItem(Registration.SCORIA_LOCATOR.get());
        basicItem(Registration.UNIVERSAL_ORE_LOCATOR.get());
        basicItem(Registration.UNIVERSAL_STONE_LOCATOR.get());
    }
}
