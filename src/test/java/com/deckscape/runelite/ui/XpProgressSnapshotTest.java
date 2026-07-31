package com.deckscape.runelite.ui;

import com.deckscape.runelite.model.DeckscapeState;
import com.deckscape.runelite.model.PendingSyncEvent;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class XpProgressSnapshotTest
{
    @Test
    void includesQueuedXpInBothLiveProgressCounters()
    {
        DeckscapeState state = new DeckscapeState();
        state.setXpTowardsPack(29_000);
        state.setXpTowardsCurrency(3_000);
        JsonObject payload = new JsonObject();
        payload.addProperty("xp", 934);
        state.getPendingEvents().add(new PendingSyncEvent("runelite_xp", "event-id", payload));

        XpProgressSnapshot progress = XpProgressSnapshot.from(state);

        assertEquals(29_934, progress.getPackProgress());
        assertEquals(59, progress.getPackPercent());
        assertEquals(3_934, progress.getCurrencyProgress());
        assertEquals(39, progress.getCurrencyPercent());
    }

    @Test
    void wrapsProgressAfterCrossingEachRewardInterval()
    {
        DeckscapeState state = new DeckscapeState();
        state.setXpTowardsPack(49_900);
        state.setXpTowardsCurrency(9_900);
        JsonObject payload = new JsonObject();
        payload.addProperty("xp", 250);
        state.getPendingEvents().add(new PendingSyncEvent("runelite_xp", "event-id", payload));

        XpProgressSnapshot progress = XpProgressSnapshot.from(state);

        assertEquals(150, progress.getPackProgress());
        assertEquals(150, progress.getCurrencyProgress());
        assertEquals(0, progress.getPackPercent());
        assertEquals(1, progress.getCurrencyPercent());
    }
}
