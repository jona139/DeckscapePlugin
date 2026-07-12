package com.deckscape.runelite.challenge;

import java.time.Instant;
import java.util.UUID;

/** JSON-compatible version of the website's VerifiedRuneLiteChallengeEvent contract. */
public final class VerifiedRuneLiteChallengeEvent
{
    public static final int VERSION = 1;
    private final int version = VERSION;
    private final String eventId = UUID.randomUUID().toString();
    private final String kind = "ELEMENTAL_STRIKE_MAX_HIT";
    private final String observedAt = Instant.now().toString();
    private final Payload payload;

    public VerifiedRuneLiteChallengeEvent(String spell, int damage, int maxHit)
    {
        this.payload = new Payload(spell, damage, maxHit);
    }

    public int getVersion() { return version; }
    public String getEventId() { return eventId; }
    public String getKind() { return kind; }
    public String getObservedAt() { return observedAt; }
    public Payload getPayload() { return payload; }

    public static final class Payload
    {
        private final String spell;
        private final int damage;
        private final int maxHit;

        private Payload(String spell, int damage, int maxHit)
        {
            this.spell = spell;
            this.damage = damage;
            this.maxHit = maxHit;
        }

        public String getSpell() { return spell; }
        public int getDamage() { return damage; }
        public int getMaxHit() { return maxHit; }
    }
}
