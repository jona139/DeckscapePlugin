# Deckscape RuneLite plugin

Deckscape is the opt-in RuneLite companion for a player's server-authoritative Deckscape account.
It synchronizes the current 361-card release catalog and collection, opens packs, displays Coins,
Stardust, and Golden Nuggets, awards balanced XP rolls, and verifies OSRS gold-frame challenges.

## Data sharing and privacy

Deckscape relies on an external service operated by the plugin author and hosted by
[Supabase](https://supabase.com) at
`https://ahqakitttmbcpxdpdlkx.supabase.co/functions/v1/economy`. The plugin makes no network requests until the player checks
**Share data with Deckscape** in RuneLite's Deckscape settings.

After consent, the plugin sends only the information required for these features: a random revocable
device token; XP deltas; verified challenge event details; the current world number and world-type
flags; pack actions; and ordinary network metadata such as the IP address received by the service.
It does not send RuneScape login credentials, chat, bank contents, the player's RuneScape display name,
or information about other players. See the full [privacy notice](PRIVACY.md).

Unchecking the option stops all new Deckscape requests immediately. Players can also revoke the device link
from the Deckscape website.

## How it works

- RuneLite shows a one-use code after consent; enter it on the signed-in Deckscape website within ten minutes.
- The plugin stores a revocable device token and a read-through display cache in RuneLite's `ConfigManager`.
- Collections, wallets, packs, gold trims, challenges, and the catalog are replaced by each canonical server snapshot.
- XP and challenge observations use a persisted retry outbox with stable event IDs.
- Pack rolls, XP rewards, pack opening, card grants, and challenge progress are server-authoritative.
- Opening a pack awards five cards plus 1-5 Stardust.
- Every 50,000 verified XP rolls server-side for a pack (45%), 25-75 Coins (35%), 1 Stardust (1%), or no reward (19%).
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
every challenge, special-world rejection, consent withdrawal, reconnects, and queued-event retries.

## License

[BSD 2-Clause](LICENSE)
