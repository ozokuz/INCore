package ozokuz.incore.data;

import ozokuz.incore.INCore;
import ozokuz.incore.Registration;
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

        basicItem(Registration.ENTROPY_BOOSTER_SMALL_ITEM.get());
        basicItem(Registration.ENTROPY_BOOSTER_LARGE_ITEM.get());
        basicItem(Registration.ENTROPY_VESSEL_ITEM.get());
        withExistingParent("field_pen", mcLoc("item/generated")).texture("layer0", mcLoc("item/ink_sac"));
        withExistingParent("field_research_note", mcLoc("item/generated")).texture("layer0", mcLoc("item/paper"));
        withExistingParent("research_data_report", mcLoc("item/generated")).texture("layer0", mcLoc("item/map"));
        withExistingParent("continuum_data_report", mcLoc("item/generated")).texture("layer0", mcLoc("item/paper"));
        withExistingParent("decoded_continuum_report", mcLoc("item/generated")).texture("layer0", mcLoc("item/writable_book"));
        withExistingParent("blank_research_sample", mcLoc("item/generated")).texture("layer0", mcLoc("item/paper"));
        withExistingParent("research_sample", mcLoc("item/generated")).texture("layer0", mcLoc("item/knowledge_book"));

        //basicItem(Registration.ENTROPY_CRATE_ITEM.get());
        //basicItem(Registration.BATTLEPASS_LANE_UNLOCK_ITEM.get());

        basicItem(Registration.CRIMSITE_ORE_LOCATOR.get());
        basicItem(Registration.VERIDIUM_ORE_LOCATOR.get());
        basicItem(Registration.ASURINE_ORE_LOCATOR.get());
        basicItem(Registration.OCHRUM_ORE_LOCATOR.get());
        basicItem(Registration.CINNABAR_ORE_LOCATOR.get());
//        basicItem(Registration.MIXED_METALS_ORE_LOCATOR.get());
        basicItem(Registration.GEM_CLUSTERS_ORE_LOCATOR.get());
//        basicItem(Registration.NETHER_QUARTZ_ORE_LOCATOR.get());
        basicItem(Registration.STONE_LOCATOR.get());
        basicItem(Registration.DEEPSLATE_LOCATOR.get());
        basicItem(Registration.LIMESTONE_LOCATOR.get());
        basicItem(Registration.BASALT_LOCATOR.get());
        basicItem(Registration.SCORIA_LOCATOR.get());
        basicItem(Registration.UNIVERSAL_ORE_LOCATOR.get());
        basicItem(Registration.UNIVERSAL_STONE_LOCATOR.get());
    }
}
