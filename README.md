# Deckscape RuneLite plugin

RuneLite companion for the server-authoritative Deckscape account.

## Behavior

- RuneLite displays a one-time code; enter it on the signed-in Deckscape website within ten minutes.
- The plugin stores a revocable device token and a read-through display cache in `ConfigManager`.
- Collections, packs, and completed challenges are replaced by every canonical server snapshot.
- XP and verified challenge observations use a persisted retry outbox with unique event IDs.
- Pack rolls and pack opening happen on the server.
- A failed or unavailable server never grants, opens, or consumes anything locally.
- New packs use an in-game unlock banner; pack opening uses a full-canvas sealed-pack, staggered deal,
  and rarity reveal animation built from Deckscape assets.

The official build includes Deckscape's public Supabase project URL and publishable key. Users never
configure backend details or enter a password or reusable website credential into RuneLite.

## Build

```powershell
./gradlew test
```

The project uses Java 11 and RuneLite's `latest.release` client dependency.
