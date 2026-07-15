package com.deckscape.runelite.challenge;

import java.util.EnumSet;
import net.runelite.api.GameState;
import net.runelite.api.WorldType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorldEligibilityTest
{
    @Test
    void acceptsPersistentMainGameWorlds()
    {
        assertTrue(WorldEligibility.isEligible(GameState.LOGGED_IN, EnumSet.of(WorldType.MEMBERS)));
        assertTrue(WorldEligibility.isEligible(GameState.LOGGED_IN, EnumSet.of(WorldType.MEMBERS, WorldType.SKILL_TOTAL)));
    }

    @Test
    void rejectsEveryTemporaryOrNoSaveMode()
    {
        WorldType[] excluded = {
            WorldType.DEADMAN, WorldType.SEASONAL, WorldType.PVP, WorldType.BOUNTY, WorldType.HIGH_RISK,
            WorldType.LAST_MAN_STANDING,
            WorldType.PVP_ARENA, WorldType.NOSAVE_MODE, WorldType.TOURNAMENT_WORLD,
            WorldType.BETA_WORLD, WorldType.QUEST_SPEEDRUNNING, WorldType.FRESH_START_WORLD,
        };
        for (WorldType type : excluded)
            assertFalse(WorldEligibility.isEligible(GameState.LOGGED_IN, EnumSet.of(WorldType.MEMBERS, type)), type.name());
    }

    @Test
    void rejectsActivityOutsideTheLoggedInState()
    {
        assertFalse(WorldEligibility.isEligible(GameState.LOGIN_SCREEN, EnumSet.noneOf(WorldType.class)));
    }
}
