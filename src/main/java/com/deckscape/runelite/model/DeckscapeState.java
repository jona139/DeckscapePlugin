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
    private Set<String> ownedGoldTrims = new LinkedHashSet<>();
    private Set<String> selectedGoldTrims = new LinkedHashSet<>();
    private int coins;
    private int stardust;
    private int goldenNuggets;
    private int xpRewardInterval = 50000;
    private int xpTowardsPack;
    private int xpCurrencyRewardInterval = 10000;
    private int xpTowardsCurrency;
    private String deviceToken;
    private String pairingCode;
    private String pendingDeviceToken;
    private long pairingExpiresAt;
    private long lastSyncAt;
    private boolean welcomeShown;
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
    public void resetAfterRevokedLink()
    {
        deviceToken = null;
        clearPairingSession();
        clearCachedAccountData();
    }

    public void clearCachedAccountData()
    {
        collection.clear();
        for (PackType type : PackType.values()) packs.put(type, 0);
        completedChallenges.clear();
        getChallengeProgress().clear();
        getOwnedGoldTrims().clear();
        getSelectedGoldTrims().clear();
        getPendingEvents().clear();
        coins = 0;
        stardust = 0;
        goldenNuggets = 0;
        xpTowardsPack = 0;
        xpTowardsCurrency = 0;
        lastSyncAt = 0L;
    }
    public long getLastSyncAt() { return lastSyncAt; }
    public void setLastSyncAt(long lastSyncAt) { this.lastSyncAt = lastSyncAt; }
    public List<PendingSyncEvent> getPendingEvents() { if (pendingEvents == null) pendingEvents = new ArrayList<>(); return pendingEvents; }

    public Map<String, Integer> getCollection() { return collection; }
    public Map<PackType, Integer> getPacks() { return packs; }
    public Set<String> getCompletedChallenges() { return completedChallenges; }
    public Map<String, Integer> getChallengeProgress() { if (challengeProgress == null) challengeProgress = new LinkedHashMap<>(); return challengeProgress; }
    public Set<String> getOwnedGoldTrims() { if (ownedGoldTrims == null) ownedGoldTrims = new LinkedHashSet<>(); return ownedGoldTrims; }
    public Set<String> getSelectedGoldTrims() { if (selectedGoldTrims == null) selectedGoldTrims = new LinkedHashSet<>(); return selectedGoldTrims; }
    public int getCoins() { return coins; }
    public void setCoins(int coins) { this.coins = Math.max(0, coins); }
    public int getStardust() { return stardust; }
    public void setStardust(int stardust) { this.stardust = Math.max(0, stardust); }
    public int getGoldenNuggets() { return goldenNuggets; }
    public void setGoldenNuggets(int goldenNuggets) { this.goldenNuggets = Math.max(0, goldenNuggets); }
    public int getXpRewardInterval() { return xpRewardInterval <= 0 ? 50000 : xpRewardInterval; }
    public void setXpRewardInterval(int xpRewardInterval) { this.xpRewardInterval = Math.max(1, xpRewardInterval); }
    public int getXpTowardsPack() { return xpTowardsPack; }
    public void setXpTowardsPack(int xpTowardsPack) { this.xpTowardsPack = Math.max(0, xpTowardsPack); }
    public int getXpCurrencyRewardInterval() { return xpCurrencyRewardInterval <= 0 ? 10000 : xpCurrencyRewardInterval; }
    public void setXpCurrencyRewardInterval(int interval) { this.xpCurrencyRewardInterval = Math.max(1, interval); }
    public int getXpTowardsCurrency() { return xpTowardsCurrency; }
    public void setXpTowardsCurrency(int xpTowardsCurrency) { this.xpTowardsCurrency = Math.max(0, xpTowardsCurrency); }
    public boolean isWelcomeShown() { return welcomeShown; }
    public void setWelcomeShown(boolean welcomeShown) { this.welcomeShown = welcomeShown; }

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
