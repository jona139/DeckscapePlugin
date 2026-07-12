# Deckscape RuneLite plugin

RuneLite companion for the server-authoritative Deckscape account.

## Behavior

- Enter the one-time code created on the Deckscape website to pair the plugin.
- The plugin stores a revocable device token and a read-through display cache in `ConfigManager`.
- Collections, packs, and completed challenges are replaced by every canonical server snapshot.
- XP and verified challenge observations are submitted with unique event IDs.
- Pack rolls and pack opening happen on the server.
- A failed or unavailable server never grants, opens, or consumes anything locally.

The official build must be configured with the production Supabase URL and publishable key. Users never
enter a password into RuneLite.

## Build

```powershell
./gradlew test
```

The project uses Java 11 and RuneLite's `latest.release` client dependency.
