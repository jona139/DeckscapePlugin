package com.deckscape.runelite.challenge;

import java.util.Collection;
import java.util.EnumSet;
import net.runelite.api.GameState;
import net.runelite.api.WorldType;

/** Keeps rewards on persistent, main-game worlds and out of temporary or no-save modes. */
public final class WorldEligibility
{
    private static final EnumSet<WorldType> DISALLOWED = EnumSet.of(
        WorldType.DEADMAN,
        WorldType.SEASONAL,
        WorldType.PVP,
        WorldType.BOUNTY,
        WorldType.HIGH_RISK,
        WorldType.LAST_MAN_STANDING,
        WorldType.PVP_ARENA,
        WorldType.NOSAVE_MODE,
        WorldType.TOURNAMENT_WORLD,
        WorldType.BETA_WORLD,
        WorldType.QUEST_SPEEDRUNNING,
        WorldType.FRESH_START_WORLD
    );

    private WorldEligibility() {}

    public static boolean isEligible(GameState gameState, Collection<WorldType> worldTypes)
    {
        if (gameState != GameState.LOGGED_IN || worldTypes == null) return false;
        for (WorldType type : worldTypes)
        {
            if (DISALLOWED.contains(type)) return false;
        }
        return true;
    }
}
