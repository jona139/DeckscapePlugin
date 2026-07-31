package com.deckscape.runelite;

import com.deckscape.runelite.model.DeckscapeState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeckscapePairingScheduleTest
{
    @Test
    void idleUnlinkedClientNeverPolls()
    {
        DeckscapeState state = new DeckscapeState();

        assertFalse(DeckscapePlugin.shouldPollPairing(state, 100_000L, 0L));
    }

    @Test
    void activePairingPollsAtMostOncePerTenSeconds()
    {
        DeckscapeState state = new DeckscapeState();
        state.setPairingCode("ABC-DEF-234");
        state.setPairingExpiresAt(120_000L);

        assertFalse(DeckscapePlugin.shouldPollPairing(state, 105_000L, 100_000L));
        assertTrue(DeckscapePlugin.shouldPollPairing(state, 110_000L, 100_000L));
        assertFalse(DeckscapePlugin.shouldPollPairing(state, 120_000L, 100_000L));
    }
}
