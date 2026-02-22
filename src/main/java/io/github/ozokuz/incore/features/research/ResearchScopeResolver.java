package io.github.ozokuz.incore.features.research;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.UUID;

/**
 * Resolves research ownership scope.
 * If an FTB Team is available for a player, research scope is team-wide.
 * Otherwise, scope falls back to player-only.
 */
public final class ResearchScopeResolver {
    private static final String TEAM_PREFIX = "team:";
    private static final String PLAYER_PREFIX = "player:";

    private static boolean initialized;
    private static boolean ftbTeamsApiAvailable;
    private static @Nullable Method apiGetter;
    private static @Nullable Method isManagerLoadedMethod;
    private static @Nullable Method getManagerMethod;
    private static @Nullable Method getTeamForPlayerIdMethod;
    private static @Nullable Method getTeamIdMethod;
    private static @Nullable Method getIdMethod;
    private static @Nullable Method getNameMethod;
    private static @Nullable Method getDisplayNameMethod;
    private static @Nullable Method getTitleMethod;

    private ResearchScopeResolver() {
    }

    public static String ownerKey(ServerPlayer player) {
        UUID teamId = resolveTeamId(player);
        if (teamId != null) {
            return TEAM_PREFIX + teamId;
        }
        return PLAYER_PREFIX + player.getUUID();
    }

    public static String ownerDisplayName(ServerPlayer player) {
        Object team = resolveTeam(player);
        String teamName = extractTeamDisplayName(team);
        if (teamName != null && !teamName.isBlank()) {
            return teamName;
        }

        String playerName = player.getGameProfile().getName();
        if (playerName != null && !playerName.isBlank()) {
            return playerName;
        }
        return player.getUUID().toString();
    }

    public static boolean isTeamScope(String ownerKey) {
        return ownerKey != null && ownerKey.startsWith(TEAM_PREFIX);
    }

    private static @Nullable UUID resolveTeamId(ServerPlayer player) {
        Object team = resolveTeam(player);
        if (team == null) {
            return null;
        }
        try {
            Object rawTeamId = null;
            if (getTeamIdMethod != null) {
                rawTeamId = getTeamIdMethod.invoke(team);
            }
            if (rawTeamId == null && getIdMethod != null) {
                rawTeamId = getIdMethod.invoke(team);
            }
            return rawTeamId instanceof UUID id ? id : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static @Nullable Object resolveTeam(ServerPlayer player) {
        try {
            initializeReflection();
            if (!ftbTeamsApiAvailable || apiGetter == null || isManagerLoadedMethod == null || getManagerMethod == null || getTeamForPlayerIdMethod == null) {
                return null;
            }

            Object api = apiGetter.invoke(null);
            if (api == null) {
                return null;
            }

            Object managerLoaded = isManagerLoadedMethod.invoke(api);
            if (!(managerLoaded instanceof Boolean loaded) || !loaded) {
                return null;
            }

            Object manager = getManagerMethod.invoke(api);
            if (manager == null) {
                return null;
            }

            Object optionalTeam = getTeamForPlayerIdMethod.invoke(manager, player.getUUID());
            if (!(optionalTeam instanceof Optional<?> teamOptional) || teamOptional.isEmpty()) {
                return null;
            }
            return teamOptional.get();
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static @Nullable String extractTeamDisplayName(@Nullable Object team) {
        if (team == null) {
            return null;
        }

        Object nameValue = invokeNoArgs(team, getNameMethod);
        if (nameValue == null) {
            nameValue = invokeNoArgs(team, getDisplayNameMethod);
        }
        if (nameValue == null) {
            nameValue = invokeNoArgs(team, getTitleMethod);
        }

        if (nameValue instanceof Component component) {
            return component.getString();
        }
        if (nameValue instanceof CharSequence chars) {
            return chars.toString();
        }
        return nameValue == null ? null : nameValue.toString();
    }

    private static @Nullable Object invokeNoArgs(Object target, @Nullable Method method) {
        if (method == null) {
            return null;
        }
        try {
            return method.invoke(target);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static @Nullable Method optionalNoArgMethod(Class<?> owner, String name) {
        try {
            return owner.getMethod(name);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static void initializeReflection() {
        if (initialized) {
            return;
        }
        initialized = true;

        try {
            Class<?> ftbTeamsApiClass = Class.forName("dev.ftb.mods.ftbteams.api.FTBTeamsAPI");
            Class<?> apiClass = Class.forName("dev.ftb.mods.ftbteams.api.FTBTeamsAPI$API");
            Class<?> teamManagerClass = Class.forName("dev.ftb.mods.ftbteams.api.TeamManager");
            Class<?> teamClass = Class.forName("dev.ftb.mods.ftbteams.api.Team");

            apiGetter = ftbTeamsApiClass.getMethod("api");
            isManagerLoadedMethod = apiClass.getMethod("isManagerLoaded");
            getManagerMethod = apiClass.getMethod("getManager");
            getTeamForPlayerIdMethod = teamManagerClass.getMethod("getTeamForPlayerID", UUID.class);
            getTeamIdMethod = teamClass.getMethod("getTeamId");
            getIdMethod = teamClass.getMethod("getId");
            getNameMethod = optionalNoArgMethod(teamClass, "getName");
            getDisplayNameMethod = optionalNoArgMethod(teamClass, "getDisplayName");
            getTitleMethod = optionalNoArgMethod(teamClass, "getTitle");
            ftbTeamsApiAvailable = true;
        } catch (Throwable ignored) {
            ftbTeamsApiAvailable = false;
        }
    }
}
