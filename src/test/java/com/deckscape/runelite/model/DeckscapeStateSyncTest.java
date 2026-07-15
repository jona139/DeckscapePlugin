package com.deckscape.runelite.model;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeckscapeStateSyncTest
{
    @Test
    void pairingSessionAndOutboxSurviveCacheRestart()
    {
        DeckscapeState state = new DeckscapeState();
        state.setPairingCode("ABC-DEF-234");
        state.setPendingDeviceToken("abcdefghijklmnopqrstuvwxyz_ABCDEFGHIJKLMNOPQRSTUVWXYZ-1234567890");
        state.setPairingExpiresAt(123456789L);
        state.setCoins(123);
        state.setStardust(7);
        state.setGoldenNuggets(45);
        state.setXpRewardInterval(50_000);
        JsonObject payload = new JsonObject();
        payload.addProperty("xp", 75);
        state.getPendingEvents().add(new PendingSyncEvent("runelite_xp", "123e4567-e89b-12d3-a456-426614174000", payload));

        DeckscapeState restored = new Gson().fromJson(new Gson().toJson(state), DeckscapeState.class);

        assertFalse(restored.isLinked());
        assertEquals("ABC-DEF-234", restored.getPairingCode());
        assertEquals(123456789L, restored.getPairingExpiresAt());
        assertEquals(1, restored.getPendingEvents().size());
        assertEquals(123, restored.getCoins());
        assertEquals(7, restored.getStardust());
        assertEquals(45, restored.getGoldenNuggets());
        assertEquals(50_000, restored.getXpRewardInterval());
        assertEquals(75, restored.getPendingEvents().get(0).getPayload().get("xp").getAsInt());
        assertEquals("123e4567-e89b-12d3-a456-426614174000", restored.getPendingEvents().get(0).getPayload().get("eventId").getAsString());
    }

    @Test
    void promotedDeviceTokenClearsOnlyShortLivedPairingMaterial()
    {
        DeckscapeState state = new DeckscapeState();
        state.setPairingCode("ABC-DEF-234");
        state.setPendingDeviceToken("pending");
        state.setDeviceToken("linked-device-token");
        state.clearPairingSession();

        assertTrue(state.isLinked());
        assertEquals("linked-device-token", state.getDeviceToken());
        assertEquals("", state.getPairingCode());
        assertEquals("", state.getPendingDeviceToken());
    }
}
