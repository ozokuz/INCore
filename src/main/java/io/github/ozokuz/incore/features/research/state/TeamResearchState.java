package io.github.ozokuz.incore.features.research.state;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class TeamResearchState {
    private final String teamId;
    private @Nullable ResourceLocation activeNetworkId;
    private final Set<ResourceLocation> discoveredNodes = new HashSet<>();
    private final Set<ResourceLocation> completedNodes = new HashSet<>();
    private final List<ResearchQueueEntry> researchQueue = new ArrayList<>();
    private int storedResearchPowerBuffer;
    private int controllerTier;
    private final Map<String, Integer> devResearchMaterials = new HashMap<>();
    private final Map<String, Integer> devLogicModules = new HashMap<>();

    public TeamResearchState(String teamId) {
        this.teamId = teamId;
    }

    public String teamId() {
        return teamId;
    }

    public @Nullable ResourceLocation activeNetworkId() {
        return activeNetworkId;
    }

    public void setActiveNetworkId(@Nullable ResourceLocation activeNetworkId) {
        this.activeNetworkId = activeNetworkId;
    }

    public Set<ResourceLocation> discoveredNodes() {
        return discoveredNodes;
    }

    public Set<ResourceLocation> completedNodes() {
        return completedNodes;
    }

    public List<ResearchQueueEntry> researchQueue() {
        return researchQueue;
    }

    public int storedResearchPowerBuffer() {
        return storedResearchPowerBuffer;
    }

    public void setStoredResearchPowerBuffer(int storedResearchPowerBuffer) {
        this.storedResearchPowerBuffer = Math.max(0, storedResearchPowerBuffer);
    }

    public int controllerTier() {
        return controllerTier;
    }

    public void setControllerTier(int controllerTier) {
        this.controllerTier = Math.max(0, controllerTier);
    }

    public Map<String, Integer> devResearchMaterials() {
        return devResearchMaterials;
    }

    public Map<String, Integer> devLogicModules() {
        return devLogicModules;
    }

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putString("teamId", teamId);
        if (activeNetworkId != null) {
            tag.putString("activeNetworkId", activeNetworkId.toString());
        }

        tag.put("discoveredNodes", toIdList(discoveredNodes));
        tag.put("completedNodes", toIdList(completedNodes));

        ListTag queueTag = new ListTag();
        for (ResearchQueueEntry entry : researchQueue) {
            queueTag.add(entry.toTag());
        }
        tag.put("researchQueue", queueTag);
        tag.putInt("storedResearchPowerBuffer", storedResearchPowerBuffer);
        tag.putInt("controllerTier", controllerTier);
        tag.put("devResearchMaterials", toStringIntMapTag(devResearchMaterials));
        tag.put("devLogicModules", toStringIntMapTag(devLogicModules));
        return tag;
    }

    public static TeamResearchState fromTag(CompoundTag tag) {
        String teamId = tag.getString("teamId");
        TeamResearchState state = new TeamResearchState(teamId);

        ResourceLocation activeNetwork = ResourceLocation.tryParse(tag.getString("activeNetworkId"));
        state.setActiveNetworkId(activeNetwork);

        readIdList(tag.getList("discoveredNodes", Tag.TAG_STRING), state.discoveredNodes);
        readIdList(tag.getList("completedNodes", Tag.TAG_STRING), state.completedNodes);

        ListTag queueTag = tag.getList("researchQueue", Tag.TAG_COMPOUND);
        for (Tag queueEntryTag : queueTag) {
            ResearchQueueEntry entry = ResearchQueueEntry.fromTag((CompoundTag) queueEntryTag);
            if (entry != null) {
                state.researchQueue.add(entry);
            }
        }

        state.setStoredResearchPowerBuffer(tag.getInt("storedResearchPowerBuffer"));
        state.setControllerTier(tag.getInt("controllerTier"));
        readStringIntMapTag(tag.getCompound("devResearchMaterials"), state.devResearchMaterials);
        readStringIntMapTag(tag.getCompound("devLogicModules"), state.devLogicModules);
        return state;
    }

    private static ListTag toIdList(Set<ResourceLocation> ids) {
        ListTag listTag = new ListTag();
        ids.stream().sorted().forEach(id -> listTag.add(StringTag.valueOf(id.toString())));
        return listTag;
    }

    private static void readIdList(ListTag listTag, Set<ResourceLocation> output) {
        for (Tag tag : listTag) {
            ResourceLocation id = ResourceLocation.tryParse(tag.getAsString());
            if (id != null) {
                output.add(id);
            }
        }
    }

    private static CompoundTag toStringIntMapTag(Map<String, Integer> values) {
        CompoundTag tag = new CompoundTag();
        values.forEach((key, value) -> {
            if (key != null && !key.isBlank() && value != null && value > 0) {
                tag.putInt(key, value);
            }
        });
        return tag;
    }

    private static void readStringIntMapTag(CompoundTag mapTag, Map<String, Integer> output) {
        for (String key : mapTag.getAllKeys()) {
            int value = Math.max(0, mapTag.getInt(key));
            if (!key.isBlank() && value > 0) {
                output.put(key, value);
            }
        }
    }
}
