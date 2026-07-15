# Deckscape RuneLite plugin privacy notice

Last updated: 15 July 2026

Deckscape is an optional online service operated by the Deckscape plugin author and hosted on Supabase at
`https://ahqakitttmbcpxdpdlkx.supabase.co/functions/v1/economy`.
RuneLite remains usable without Deckscape, and this plugin sends nothing until the player explicitly enables
**Share data with Deckscape** in the plugin settings.

## Data processed

After consent, the plugin may send:

- a random, revocable Deckscape device token;
- XP delta amounts;
- verified gold-frame challenge observations, including relevant item/action quantities or hit damage;
- the current world number and RuneLite world-type flags, used to reject ineligible game modes;
- pack-open and account-synchronization actions; and
- network metadata necessarily handled by the service provider, including IP address and request timestamps.

The linked Deckscape account stores its Deckscape display name, collection, wallet, packs, challenge progress,
cosmetics, and processed event identifiers. The server stores only a SHA-256 digest of the device token.

The plugin does not send RuneScape credentials, the player's RuneScape display name, chat messages, bank or
inventory contents, or information about other players. Inventory/equipment checks used for challenges are
evaluated locally; only a qualifying observation is sent.

## Purpose and sharing

The data is used only to link the plugin, synchronize the Deckscape account, prevent duplicate or ineligible
rewards, grant server-authoritative rewards, and diagnose service failures. It is processed by Supabase as the
hosting and database provider under [Supabase's privacy policy](https://supabase.com/privacy). It is not sold
or used for advertising.

## Retention and control

Account state and idempotency/event records are retained while needed to operate the linked Deckscape account
and prevent duplicate rewards. Unchecking the consent option stops new requests immediately. Revoking the
RuneLite link on the Deckscape website invalidates the device token. For account-data access or deletion requests,
use the contact method published on the [Deckscape plugin repository](https://github.com/jona139/DeckscapePlugin).

This notice should be updated before release whenever the service endpoint, data categories, purposes, provider,
or retention behavior changes.
