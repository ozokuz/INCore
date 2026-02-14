package io.github.ozokuz.incore.data;

import io.github.ozokuz.incore.INCore;
import io.github.ozokuz.incore.Registration;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public class ICBlockStateProvider extends BlockStateProvider {
    public ICBlockStateProvider(PackOutput output, ExistingFileHelper exFileHelper) {
        super(output, INCore.MODID, exFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        var spawnerModel = models().withExistingParent("encounter_spawner", mcLoc("block/cube_all_inner_faces"))
                    .texture("all", modLoc("block/encounter_spawner"))
                    .renderType("cutout");
        simpleBlock(Registration.ENCOUNTER_SPAWNER_BLOCK.get(), spawnerModel);
        simpleBlockItem(Registration.ENCOUNTER_SPAWNER_BLOCK.get(), spawnerModel);

        var gachaCrateModel = models().withExistingParent("gacha_crate", mcLoc("block/cube_all"))
                .texture("all", modLoc("item/sanity_crate"));
        simpleBlock(Registration.GACHA_CRATE_BLOCK.get(), gachaCrateModel);
        simpleBlockItem(Registration.GACHA_CRATE_BLOCK.get(), gachaCrateModel);
    }
}
