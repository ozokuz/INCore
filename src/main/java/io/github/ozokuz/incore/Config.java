package io.github.ozokuz.incore;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

// An example config class. This is not required, but it's a good idea to have one to keep your config organized.
// Demonstrates how to use Neo's config APIs
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue LOG_DIRT_BLOCK = BUILDER
            .comment("Whether to log the dirt block on common setup")
            .define("logDirtBlock", true);

    public static final ModConfigSpec.IntValue MAGIC_NUMBER = BUILDER
            .comment("A magic number")
            .defineInRange("magicNumber", 42, 0, Integer.MAX_VALUE);

    public static final ModConfigSpec.ConfigValue<String> MAGIC_NUMBER_INTRODUCTION = BUILDER
            .comment("What you want the introduction message to be for the magic number")
            .define("magicNumberIntroduction", "The magic number is... ");

    // a list of strings that are treated as resource locations for items
    public static final ModConfigSpec.ConfigValue<List<? extends String>> ITEM_STRINGS = BUILDER
            .comment("A list of items to log on common setup.")
            .defineListAllowEmpty("items", List.of("minecraft:iron_ingot"), () -> "", Config::validateItemName);

    public static final ModConfigSpec.IntValue SANITY_REGEN_PER_MINUTE = BUILDER
            .comment("How much sanity players regain every real-world minute.")
            .defineInRange("sanityRegenPerMinute", 1, 0, Integer.MAX_VALUE);

    public static final ModConfigSpec.IntValue SANITY_BASE_CAP = BUILDER
            .comment("Default sanity cap before any bonus cap extensions are applied.")
            .defineInRange("sanityBaseCap", 120, 0, Integer.MAX_VALUE);

    public static final ModConfigSpec.IntValue SANITY_CRATE_COST = BUILDER
            .comment("Sanity cost to open one sanity crate.")
            .defineInRange("sanityCrateCost", 60, 0, Integer.MAX_VALUE);

    public static final ModConfigSpec.IntValue SANITY_CAP_UPGRADE_AMOUNT = BUILDER
            .comment("How much max sanity a single sanity vessel upgrades.")
            .defineInRange("sanityCapUpgradeAmount", 20, 1, Integer.MAX_VALUE);

    static final ModConfigSpec SPEC = BUILDER.build();

    private static boolean validateItemName(final Object obj) {
        return obj instanceof String itemName && BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse(itemName));
    }
}
