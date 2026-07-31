package com.deckscape.runelite.ui;

import com.deckscape.runelite.model.DeckscapeState;
import com.deckscape.runelite.model.PendingSyncEvent;
import com.google.gson.JsonObject;

/** Display-only XP progress, including XP safely queued for the next server sync. */
final class XpProgressSnapshot
{
    private final int packProgress;
    private final int packInterval;
    private final int currencyProgress;
    private final int currencyInterval;

    private XpProgressSnapshot(int packProgress, int packInterval,
                               int currencyProgress, int currencyInterval)
    {
        this.packProgress = packProgress;
        this.packInterval = packInterval;
        this.currencyProgress = currencyProgress;
        this.currencyInterval = currencyInterval;
    }

    static XpProgressSnapshot from(DeckscapeState state)
    {
        long queuedXp = 0L;
        synchronized (state)
        {
            for (PendingSyncEvent event : state.getPendingEvents())
            {
                if (!"runelite_xp".equals(event.getAction())) continue;
                JsonObject payload = event.getPayload();
                if (payload.has("xp"))
                    queuedXp += Math.max(0L, payload.get("xp").getAsLong());
            }
        }

        int packInterval = Math.max(1, state.getXpRewardInterval());
        int currencyInterval = Math.max(1, state.getXpCurrencyRewardInterval());
        int packProgress = moduloProgress(state.getXpTowardsPack(), queuedXp, packInterval);
        int currencyProgress = moduloProgress(state.getXpTowardsCurrency(), queuedXp, currencyInterval);
        return new XpProgressSnapshot(packProgress, packInterval, currencyProgress, currencyInterval);
    }

    private static int moduloProgress(int serverProgress, long queuedXp, int interval)
    {
        return (int) Math.floorMod(Math.max(0L, serverProgress) + queuedXp, interval);
    }

    private static int percentage(int progress, int interval)
    {
        return (int) Math.min(100L, (long) progress * 100L / Math.max(1, interval));
    }

    int getPackProgress() { return packProgress; }
    int getPackInterval() { return packInterval; }
    int getPackPercent() { return percentage(packProgress, packInterval); }
    int getCurrencyProgress() { return currencyProgress; }
    int getCurrencyInterval() { return currencyInterval; }
    int getCurrencyPercent() { return percentage(currencyProgress, currencyInterval); }
}
