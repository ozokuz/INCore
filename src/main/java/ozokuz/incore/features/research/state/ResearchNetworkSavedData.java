package ozokuz.incore.features.research.state;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ResearchNetworkSavedData extends SavedData {
    private static final String DATA_NAME = "incore_research";

    private final Map<String, TeamResearchState> stateByTeam = new HashMap<>();

    public static ResearchNetworkSavedData get(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        return overworld.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(ResearchNetworkSavedData::new, ResearchNetworkSavedData::load, DataFixTypes.LEVEL),
                DATA_NAME
        );
    }

    public static ResearchNetworkSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        ResearchNetworkSavedData data = new ResearchNetworkSavedData();
        ListTag teams = tag.getList("teams", Tag.TAG_COMPOUND);
        for (Tag teamTag : teams) {
            if (!(teamTag instanceof CompoundTag row)) {
                continue;
            }
            TeamResearchState state = TeamResearchState.fromTag(row);
            if (state.teamId() == null || state.teamId().isBlank()) {
                continue;
            }
            data.stateByTeam.put(state.teamId(), state);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag teams = new ListTag();
        for (TeamResearchState state : stateByTeam.values()) {
            teams.add(state.toTag());
        }
        tag.put("teams", teams);
        return tag;
    }

    public TeamResearchState getOrCreateTeamState(String teamId) {
        return stateByTeam.computeIfAbsent(teamId, TeamResearchState::new);
    }

    public @Nullable TeamResearchState getTeamState(String teamId) {
        return stateByTeam.get(teamId);
    }

    public Set<String> teamIds() {
        return Set.copyOf(new HashSet<>(stateByTeam.keySet()));
    }
}
