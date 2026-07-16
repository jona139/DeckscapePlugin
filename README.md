# Deckscape RuneLite plugin

Deckscape is the online RuneLite companion for a player's server-authoritative Deckscape account.
It synchronizes the current 361-card release catalog and collection, opens packs, displays Coins,
Stardust, and Golden Nuggets, awards balanced XP rolls, and verifies OSRS gold-frame challenges.
Play and manage the linked account at [deckscape.gamecubejona.com](https://deckscape.gamecubejona.com).

## Data sharing and privacy

Deckscape relies on an external service operated by the plugin author and hosted by
[Supabase](https://supabase.com) at
`https://ahqakitttmbcpxdpdlkx.supabase.co/functions/v1/economy`. Plugin Hub displays a third-party communication warning
before installation. Once installed and enabled, the plugin connects to the Deckscape service.

While enabled, the plugin sends only the information required for these features: a random revocable
device token; XP deltas; verified challenge event details; the current world number and world-type
flags; pack actions; and ordinary network metadata such as the IP address received by the service.
It does not send RuneScape login credentials, chat, bank contents, the player's RuneScape display name,
or information about other players. See the full [privacy notice](PRIVACY.md).

When a card has no artwork bundled in the plugin, Deckscape downloads its image over HTTPS directly
from the [Old School RuneScape Wiki](https://oldschool.runescape.wiki). Downloads are restricted to that host,
restricted to that host, size-limited, decoded in memory, and cached only for the current RuneLite session.

Disabling or uninstalling the plugin stops Deckscape requests. Players can also revoke the device link from the Deckscape website.

## How it works

- RuneLite shows a one-use code when the plugin starts; enter it on the signed-in Deckscape website within ten minutes.
- The plugin stores a revocable device token and a read-through display cache in RuneLite's `ConfigManager`.
- Collections, wallets, packs, gold trims, challenges, and the catalog are replaced by each canonical server snapshot.
- XP and challenge observations use a persisted retry outbox with stable event IDs.
- Pack rolls, XP rewards, pack opening, card grants, and challenge progress are server-authoritative.
- Opening a pack awards five cards plus 1-5 Stardust.
- Every 50,000 verified XP rolls independently for a core pack (50% chance).
- Every 10,000 verified XP separately awards either 100-200 Coins (80%) or 1-3 Stardust (20%).
- Gold-frame tasks count only on normal main-game worlds. PvP, high-risk, bounty, Leagues, Deadman,
  Last Man Standing, PvP Arena, no-save, tournament, beta, quest-speedrunning, and Fresh Start worlds are excluded.
- A failed or unavailable service never grants, opens, or consumes anything locally.

The official build includes Deckscape's public Supabase URL and publishable key. These are public client
identifiers, not privileged credentials. Passwords and reusable website credentials are never entered into RuneLite.

## Build and test

Deckscape uses Java 11 and RuneLite's `latest.release` client dependency.

```powershell
./gradlew test
```

For an in-client test session, run the Gradle `run` task and test pairing, XP rewards, pack opening,
every challenge, special-world rejection, plugin disable/re-enable, reconnects, and queued-event retries.

## License

[BSD 2-Clause](LICENSE)
