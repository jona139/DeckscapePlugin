package com.deckscape.runelite.model;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;
import java.util.List;

public final class DeckscapeState
{
    private final Map<String, Integer> collection = new LinkedHashMap<>();
    private final Map<PackType, Integer> packs = new EnumMap<>(PackType.class);
    private final Set<String> completedChallenges = new LinkedHashSet<>();
    private Map<String, Integer> challengeProgress = new LinkedHashMap<>();
    private int xpTowardsPack;
    private String deviceToken;
    private String pairingCode;
    private String pendingDeviceToken;
    private long pairingExpiresAt;
    private long lastSyncAt;
    private List<PendingSyncEvent> pendingEvents = new ArrayList<>();

    public DeckscapeState()
    {
        for (PackType type : PackType.values())
        {
            packs.put(type, 0);
        }
    }

    public String getUniqueId()
    {
        return deviceToken == null ? "" : deviceToken;
    }

    public void setUniqueId(String uniqueId)
    {
        this.deviceToken = uniqueId;
    }

    public String getDeviceToken() { return deviceToken == null ? "" : deviceToken; }
    public void setDeviceToken(String deviceToken) { this.deviceToken = deviceToken; }
    public boolean isLinked() { return deviceToken != null && !deviceToken.isEmpty(); }
    public String getPairingCode() { return pairingCode == null ? "" : pairingCode; }
    public void setPairingCode(String pairingCode) { this.pairingCode = pairingCode; }
    public String getPendingDeviceToken() { return pendingDeviceToken == null ? "" : pendingDeviceToken; }
    public void setPendingDeviceToken(String pendingDeviceToken) { this.pendingDeviceToken = pendingDeviceToken; }
    public long getPairingExpiresAt() { return pairingExpiresAt; }
    public void setPairingExpiresAt(long pairingExpiresAt) { this.pairingExpiresAt = pairingExpiresAt; }
    public void clearPairingSession() { pairingCode = null; pendingDeviceToken = null; pairingExpiresAt = 0L; }
    public long getLastSyncAt() { return lastSyncAt; }
    public void setLastSyncAt(long lastSyncAt) { this.lastSyncAt = lastSyncAt; }
    public List<PendingSyncEvent> getPendingEvents() { if (pendingEvents == null) pendingEvents = new ArrayList<>(); return pendingEvents; }

    public Map<String, Integer> getCollection() { return collection; }
    public Map<PackType, Integer> getPacks() { return packs; }
    public Set<String> getCompletedChallenges() { return completedChallenges; }
    public Map<String, Integer> getChallengeProgress() { if (challengeProgress == null) challengeProgress = new LinkedHashMap<>(); return challengeProgress; }
    public int getXpTowardsPack() { return xpTowardsPack; }
    public void setXpTowardsPack(int xpTowardsPack) { this.xpTowardsPack = Math.max(0, xpTowardsPack); }

    public int packCount(PackType type) { return packs.getOrDefault(type, 0); }
    public void addPack(PackType type) { packs.put(type, packCount(type) + 1); }
    public boolean consumePack(PackType type)
    {
        int count = packCount(type);
        if (count < 1) return false;
        packs.put(type, count - 1);
        return true;
    }
    public void addCard(String cardId) { collection.put(cardId, collection.getOrDefault(cardId, 0) + 1); }
}
