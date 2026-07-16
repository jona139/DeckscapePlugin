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
    void usesLaunchRewardIntervalByDefault()
    {
        DeckscapeState state = new DeckscapeState();
        assertEquals(50000, state.getXpRewardInterval());
        assertEquals(10000, state.getXpCurrencyRewardInterval());
    }

    @Test
    void unlinkedCacheStartsWithNoPacksUntilServerPairingSnapshotArrives()
    {
        DeckscapeState state = new DeckscapeState();
        state.addPack(PackType.GENERAL);
        state.addPack(PackType.COMBAT);
        state.addPack(PackType.SKILLING);

        state.clearCachedAccountData();

        assertEquals(0, state.getPacks().values().stream().mapToInt(Integer::intValue).sum());
    }

    @Test
    void inventoryTotalExcludesHiddenLegacyAndFuturePackTypes()
    {
        DeckscapeState state = new DeckscapeState();
        for (int i = 0; i < 4; i++) state.addPack(PackType.GENERAL);
        for (int i = 0; i < 3; i++) state.addPack(PackType.FACTION_ASGARNIA);

        int visibleTotal = java.util.Arrays.stream(PackType.inventoryTypes())
            .mapToInt(state::packCount).sum();

        assertEquals(4, visibleTotal);
    }

    @Test
    void revokedLinkClearsLocalAccountAndAllowsPairingAgain()
    {
        DeckscapeState state = new DeckscapeState();
        state.setDeviceToken("revoked-device-token");
        state.setPairingCode("OLD-CODE-1");
        state.setPendingDeviceToken("old-pending-token");
        state.setCoins(25);
        state.setStardust(4);
        state.setGoldenNuggets(3);
        state.addCard("man");
        state.addPack(PackType.GENERAL);
        state.getChallengeProgress().put("matches_won", 2);
        state.getOwnedGoldTrims().add("man");

        state.resetAfterRevokedLink();

        assertFalse(state.isLinked());
        assertEquals("", state.getPairingCode());
        assertEquals("", state.getPendingDeviceToken());
        assertTrue(state.getCollection().isEmpty());
        assertEquals(0, state.packCount(PackType.GENERAL));
        assertEquals(0, state.getCoins());
        assertEquals(0, state.getStardust());
        assertEquals(0, state.getGoldenNuggets());
        assertTrue(state.getChallengeProgress().isEmpty());
        assertTrue(state.getOwnedGoldTrims().isEmpty());
    }

    @Test
    void batchesPassiveXpWithoutExceedingTheApiLimit()
    {
        JsonObject payload = new JsonObject();
        payload.addProperty("xp", 99_900);
        PendingSyncEvent event = new PendingSyncEvent("runelite_xp", "xp-event", payload);

        assertEquals(100, event.appendXp(500, 100_000));
        assertEquals(100_000, event.getPayload().get("xp").getAsInt());
        assertEquals(0, event.appendXp(400, 100_000));
    }

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
        state.setXpCurrencyRewardInterval(10_000);
        state.setXpTowardsCurrency(7_500);
        state.setWelcomeShown(true);
        state.getSelectedGoldTrims().add("adventurer_archer");
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
        assertEquals(10_000, restored.getXpCurrencyRewardInterval());
        assertEquals(7_500, restored.getXpTowardsCurrency());
        assertTrue(restored.isWelcomeShown());
        assertTrue(restored.getSelectedGoldTrims().contains("adventurer_archer"));
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
