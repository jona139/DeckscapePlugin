package com.deckscape.runelite.challenge;

import com.google.gson.JsonObject;
import java.time.Instant;
import java.util.UUID;

/** Authenticated observation sent through the persisted server outbox. */
public final class VerifiedRuneLiteChallengeEvent
{
    public static final int VERSION = 1;
    private final int version = VERSION;
    private final String eventId = UUID.randomUUID().toString();
    private final String kind;
    private final String observedAt = Instant.now().toString();
    private final int quantity;
    private final int damage;
    private final String spell;

    private VerifiedRuneLiteChallengeEvent(String kind, int quantity, int damage, String spell)
    {
        this.kind = kind;
        this.quantity = quantity;
        this.damage = damage;
        this.spell = spell;
    }

    public static VerifiedRuneLiteChallengeEvent observed(RuneLiteChallenge challenge)
    {
        return new VerifiedRuneLiteChallengeEvent(challenge.getKind(), 1, 0, null);
    }

    public static VerifiedRuneLiteChallengeEvent quantity(RuneLiteChallenge challenge, int quantity)
    {
        return new VerifiedRuneLiteChallengeEvent(challenge.getKind(), Math.max(1, quantity), 0, null);
    }

    public static VerifiedRuneLiteChallengeEvent strike(String spell, int damage)
    {
        return new VerifiedRuneLiteChallengeEvent(RuneLiteChallenge.STRIKE_HIT.getKind(), 1, damage, spell);
    }

    public int getVersion() { return version; }
    public String getEventId() { return eventId; }
    public String getKind() { return kind; }
    public String getObservedAt() { return observedAt; }
    public int getQuantity() { return quantity; }
    public int getDamage() { return damage; }
    public String getSpell() { return spell; }

    public JsonObject toPayload()
    {
        JsonObject payload = new JsonObject();
        payload.addProperty("version", version);
        payload.addProperty("kind", kind);
        payload.addProperty("observedAt", observedAt);
        if (RuneLiteChallenge.PURE_ESSENCE.getKind().equals(kind)
            || RuneLiteChallenge.RUNE_KITESHIELD.getKind().equals(kind)) payload.addProperty("quantity", quantity);
        if (RuneLiteChallenge.STRIKE_HIT.getKind().equals(kind))
        {
            payload.addProperty("damage", damage);
            if (spell != null) payload.addProperty("spell", spell);
        }
        return payload;
    }
}
