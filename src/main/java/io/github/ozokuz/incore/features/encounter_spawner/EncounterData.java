package io.github.ozokuz.incore.features.encounter_spawner;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;

import java.util.List;

public record EncounterData(List<MobEntry> mobs, String lootTable) {
    public static EncounterData fromJson(JsonObject json) {
        return new EncounterData(
                json.getAsJsonArray("mobs").asList().stream().map(JsonElement::getAsJsonObject).map(mob -> new MobEntry(
                        fromString(mob.get("type").getAsString()), mob.get("count").getAsInt())).toList(),
                json.get("loot_table").getAsString()
        );
    }

    @SuppressWarnings("unchecked")
    private static EntityType<? extends Mob> fromString(String id) {
       return (EntityType<? extends Mob>) BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.tryParse(id));
    }

    public record MobEntry(EntityType<? extends Mob> type, int count) {}
}
