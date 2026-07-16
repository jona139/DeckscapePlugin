package com.deckscape.runelite.model;

import com.google.gson.JsonObject;

/** Persisted at-least-once outbox item. The server de-duplicates the stable event id. */
public final class PendingSyncEvent
{
    private String action;
    private String eventId;
    private JsonObject payload;

    public PendingSyncEvent(String action, String eventId, JsonObject payload)
    {
        this.action = action;
        this.eventId = eventId;
        this.payload = payload == null ? new JsonObject() : payload.deepCopy();
        this.payload.addProperty("eventId", eventId);
    }

    public String getAction() { return action; }
    public String getEventId() { return eventId; }
    public JsonObject getPayload() { return payload == null ? new JsonObject() : payload.deepCopy(); }

    /** Adds passive XP to an unsent event while respecting the API's per-event limit. */
    public int appendXp(int additionalXp, int maximumXp)
    {
        if (!"runelite_xp".equals(action) || additionalXp <= 0 || payload == null) return 0;
        int currentXp = payload.has("xp") ? payload.get("xp").getAsInt() : 0;
        int accepted = Math.min(additionalXp, Math.max(0, maximumXp - currentXp));
        if (accepted > 0) payload.addProperty("xp", currentXp + accepted);
        return accepted;
    }
}
