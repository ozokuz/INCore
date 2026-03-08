package io.github.ozokuz.incore.data;

import io.github.ozokuz.incore.INCore;
import io.github.ozokuz.incore.Registration;
import net.minecraft.data.PackOutput;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelFile;
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

        registerMachineBlock(Registration.BURNER_POWER_INPUT_BLOCK.get(), "burner_power_input", "research/burner_input");
        registerMachineBlock(Registration.ELECTRIC_POWER_INPUT_BLOCK.get(), "electric_power_input", "research/fe_input");
        registerMachineBlock(Registration.ELECTRIC_POWER_INPUT_T2_BLOCK.get(), "electric_power_input_t2", "research/fe_input");
        registerMachineBlock(Registration.ELECTRIC_POWER_INPUT_T3_BLOCK.get(), "electric_power_input_t3", "research/fe_input");
        registerMachineBlock(Registration.ELECTRIC_POWER_INPUT_T4_BLOCK.get(), "electric_power_input_t4", "research/fe_input");
        registerMechanicalInputBlock("mechanical_power_input", "research/mechanical_input");

//        var gachaRiftModel = models().withExistingParent("gacha_rift", mcLoc("block/cube_all"))
//                .texture("all", modLoc("item/entropy_crate"));
//        simpleBlock(Registration.GACHA_RIFT_BLOCK.get(), gachaRiftModel);
//        simpleBlockItem(Registration.GACHA_RIFT_BLOCK.get(), gachaRiftModel);
    }

    private void registerMachineBlock(Block block, String name, String frontTexture) {
        ModelFile model = machineModel(name, frontTexture);
        getVariantBuilder(block).forAllStates(state -> {
            Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
            return ConfiguredModel.builder()
                    .modelFile(model)
                    .rotationY(((int) facing.toYRot() + 180) % 360)
                    .build();
        });
        simpleBlockItem(block, model);
    }

    private void registerMechanicalInputBlock(String name, String frontTexture) {
        ModelFile model = machineModel(name, frontTexture);
        getVariantBuilder(Registration.MECHANICAL_POWER_INPUT_BLOCK.get()).forAllStates(state -> {
            Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
            return ConfiguredModel.builder()
                    .modelFile(model)
                    .rotationY(((int) facing.toYRot() + 180) % 360)
                    .build();
        });
        simpleBlockItem(Registration.MECHANICAL_POWER_INPUT_BLOCK.get(), model);
    }

    private ModelFile machineModel(String name, String frontTexture) {
        return models().withExistingParent(name, mcLoc("block/cube"))
                .texture("down", modLoc("block/research/casing"))
                .texture("up", modLoc("block/research/casing"))
                .texture("north", modLoc("block/" + frontTexture))
                .texture("south", modLoc("block/research/casing"))
                .texture("east", modLoc("block/research/casing"))
                .texture("west", modLoc("block/research/casing"));
    }
}
