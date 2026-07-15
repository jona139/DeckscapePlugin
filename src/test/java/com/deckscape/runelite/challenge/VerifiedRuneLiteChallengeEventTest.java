package com.deckscape.runelite.challenge;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class VerifiedRuneLiteChallengeEventTest
{
    @Test
    void quantityEventsMatchTheServerContract()
    {
        VerifiedRuneLiteChallengeEvent event = VerifiedRuneLiteChallengeEvent.quantity(RuneLiteChallenge.PURE_ESSENCE, 2);
        JsonObject payload = event.toPayload();

        assertEquals("PURE_ESSENCE_MINED", payload.get("kind").getAsString());
        assertEquals(2, payload.get("quantity").getAsInt());
        assertEquals(1, payload.get("version").getAsInt());
        assertFalse(payload.has("damage"));
    }

    @Test
    void strikeEventsRequireTheCurrentSixteenHitContract()
    {
        JsonObject payload = VerifiedRuneLiteChallengeEvent.strike("FIRE_STRIKE", 16).toPayload();
        assertEquals("STRIKE_HIT_16", payload.get("kind").getAsString());
        assertEquals(16, payload.get("damage").getAsInt());
        assertEquals("FIRE_STRIKE", payload.get("spell").getAsString());
    }
}
