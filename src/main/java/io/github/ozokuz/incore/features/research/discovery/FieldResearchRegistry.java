package io.github.ozokuz.incore.features.research.discovery;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.ozokuz.incore.INCore;
import io.github.ozokuz.incore.features.research.registry.ResearchRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class FieldResearchRegistry extends SimpleJsonResourceReloadListener {
    private static volatile List<FieldResearchDefinition> definitions = List.of();

    public FieldResearchRegistry() {
        super(new Gson(), "research_field_research");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> jsons, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profilerFiller) {
        List<FieldResearchDefinition> next = new ArrayList<>();
        jsons.forEach((fileId, element) -> {
            if (!element.isJsonObject()) {
                return;
            }
            FieldResearchDefinition definition = parseDefinition(fileId, element.getAsJsonObject());
            if (definition != null) {
                next.add(definition);
            }
        });
        next.sort(Comparator.comparing(definition -> definition.id().toString()));
        definitions = List.copyOf(next);
        INCore.LOGGER.info("Loaded {} field research mappings.", definitions.size());
    }

    public static FieldResearchDefinition match(BlockState state) {
        for (FieldResearchDefinition definition : definitions) {
            if (definition.matches(state)) {
                return definition;
            }
        }
        return null;
    }

    private static FieldResearchDefinition parseDefinition(ResourceLocation fileId, JsonObject json) {
        ResourceLocation blockId = parseId(json, "block");
        ResourceLocation tagId = parseId(json, "tag");
        if ((blockId == null) == (tagId == null)) {
            return null;
        }

        Block block = blockId == null ? null : BuiltInRegistries.BLOCK.get(blockId);
        if (blockId != null && (block == null || block == net.minecraft.world.level.block.Blocks.AIR)) {
            return null;
        }

        List<ResourceLocation> nodeIds = parseNodeIds(json.getAsJsonArray("node_ids"));
        if (nodeIds.isEmpty()) {
            return null;
        }

        String noteName = json.has("note_name") ? json.get("note_name").getAsString() : humanize(fileId);
        return new FieldResearchDefinition(
                fileId,
                blockId,
                block,
                tagId == null ? null : TagKey.create(Registries.BLOCK, tagId),
                List.copyOf(nodeIds),
                noteName
        );
    }

    private static ResourceLocation parseId(JsonObject json, String key) {
        if (json == null || !json.has(key)) {
            return null;
        }
        return ResourceLocation.tryParse(json.get(key).getAsString());
    }

    private static List<ResourceLocation> parseNodeIds(JsonArray array) {
        if (array == null) {
            return List.of();
        }
        List<ResourceLocation> nodeIds = new ArrayList<>();
        for (JsonElement element : array) {
            ResourceLocation nodeId = ResourceLocation.tryParse(element.getAsString());
            if (nodeId != null && ResearchRegistry.nodes().containsKey(nodeId)) {
                nodeIds.add(nodeId);
            }
        }
        nodeIds.sort(Comparator.naturalOrder());
        return List.copyOf(nodeIds);
    }

    private static String humanize(ResourceLocation id) {
        String path = id.getPath();
        String[] parts = path.split("[_/-]");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                builder.append(part.substring(1));
            }
        }
        return builder.isEmpty() ? id.toString() : builder.toString();
    }

    public record FieldResearchDefinition(
            ResourceLocation id,
            ResourceLocation blockId,
            Block block,
            TagKey<Block> blockTag,
            List<ResourceLocation> nodeIds,
            String noteName
    ) {
        public boolean matches(BlockState state) {
            if (state == null) {
                return false;
            }
            if (block != null) {
                return state.is(block);
            }
            return blockTag != null && state.is(blockTag);
        }

        public net.minecraft.world.item.ItemStack createNote(net.minecraft.world.item.Item item) {
            net.minecraft.world.item.ItemStack stack = new net.minecraft.world.item.ItemStack(item);
            DiscoveryPayloadData.write(stack, new DiscoveryPayload(nodeIds, "field_research", id.toString(), noteName, ""));
            return stack;
        }
    }
}
