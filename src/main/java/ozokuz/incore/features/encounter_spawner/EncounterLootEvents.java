package ozokuz.incore.features.encounter_spawner;

import ozokuz.incore.INCore;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

@EventBusSubscriber(modid = INCore.MODID)
public class EncounterLootEvents {
    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        var entity = event.getEntity();
        var data = entity.getPersistentData();
        if (!data.contains("incore:loot_table")) return;

        event.getDrops().clear();

        ResourceLocation lootTableId = ResourceLocation.parse(data.getString("incore:loot_table"));

        if (entity.getServer() == null) return;

        LootTable table = entity
                .getServer()
                .reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, lootTableId));

        LootParams params = new LootParams.Builder((ServerLevel) entity.level())
                .withParameter(LootContextParams.THIS_ENTITY, entity)
                .withParameter(LootContextParams.ORIGIN, entity.position())
                .withParameter(LootContextParams.DAMAGE_SOURCE, entity.getLastDamageSource())
                .create(LootContextParamSets.ENTITY);

        table.getRandomItems(params, stack -> event.getDrops().add(new ItemEntity(entity.level(), entity.getX(), entity.getY(), entity.getZ(), stack)));
    }

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        INCore.LOGGER.info("Hello from EncounterLootEvents!");
    }
}
