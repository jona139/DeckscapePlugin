# Deckscape RuneLite Plugin Hub submission guide

This checklist follows the official [Plugin Hub repository instructions](https://github.com/runelite/plugin-hub) and [Plugin Hub Review policy](https://github.com/runelite/runelite/wiki/Plugin-Hub-Review), reviewed on 16 July 2026.

## Current readiness audit

The local plugin currently meets the code and repository-shape requirements:

- intended source repository: `https://github.com/jona139/DeckscapePlugin`
- Java 11 source target and RuneLite `latest.release`
- standard Plugin Hub build with no runtime third-party dependency
- root `runelite-plugin.properties` with `displayName`, `author`, `description`, `tags`, `plugins`, and `build=standard`
- root `icon.png` at 48×48 pixels (within the 48×72 maximum)
- README, BSD 2-Clause license, privacy notice, and bundled-art attribution
- resources loaded from the classpath so they work from the built JAR
- no reflection, native libraries, process execution, arbitrary file access, or local server
- third-party networking is disclosed through the Plugin Hub installation warning and stops when the plugin is disabled or uninstalled
- server-authoritative/idempotent rewards and normal-world validation, including exclusions for Leagues, Deadman, LMS and other special modes

The built plugin JAR is currently about 20.3 MB because card artwork is bundled. The official instructions do not state a JAR-size limit, and bundling avoids most runtime downloads, but call this out in the pull request so reviewers are not surprised.

**Submission blocker:** an unauthenticated GitHub API check on 16 July 2026 returned `404 Not Found` for `jona139/DeckscapePlugin`. This normally means the repository is private or not publicly available under that path. Plugin Hub must be able to fetch it without credentials. Before submission, make that repository public (or correct both the Git remote and marker URL to the actual public repository), then open the repository and its final commit in a private/incognito browser window to verify access.

Plugin Hub review is primarily a security, Jagex-rules, and repository-compliance review. Passing review does not certify functionality, correctness, performance, privacy-law compliance, or the external Deckscape service, so complete the manual test matrix below first.

## 1. Deploy and verify the server contract

The service controls reward cadence and amounts. Before publishing the plugin:

1. In `C:\Projects\DeckscapeWeb`, review the pending forward migrations, ending with `supabase/migrations/20260733_split_runelite_xp_rewards.sql`.
2. Confirm `npx supabase migration list` shows no duplicate local migration versions. Never edit or rename an already-applied migration.
3. Apply the forward migration with `npx supabase db push`.
4. Deploy the synchronized edge function with `npx supabase functions deploy economy`.
5. Deploy the web app containing the matching generated economy data.
6. Pair a disposable test account and confirm Sync reports independent 50,000-XP pack and 10,000-XP currency tracks. Verify pack rolls are 50%, and every currency roll yields either 100–200 Coins or 1–3 Stardust.

Do not submit the plugin while production still reports the old testing interval; the server snapshot overrides the local fallback.

## 2. Perform the release test matrix

Run automated checks from the plugin repository:

```powershell
./gradlew clean test
./gradlew jar
```

Then launch a development client with `./gradlew run` and verify all of the following:

1. Fresh install: the guide opens once when the side panel is first shown and can be reopened with **Guide, setup & privacy**.
2. Installation warning: the Plugin Hub warning accurately lists the external data sharing before installation.
3. Pairing: an idle unlinked plugin makes no requests; clicking **New code** creates a one-use pairing code, linking succeeds, and no RuneScape/web password is requested.
4. Synchronization: automatic idle sync is limited to once per five minutes; manual **Sync now** works inside that window; pack interactions sync immediately.
5. Collection: full catalog, search, filters, sorting, owned/missing state, alternate web-selected art, and web-selected gold trim refresh after Sync.
6. Rewards: independent rolls at 50,000 eligible XP for a 50% core-pack chance and at 10,000 XP for 80% 100–200 Coins or 20% 1–3 Stardust; only General/Combat/Skilling packs can roll; reconnect/retry cannot duplicate either reward track.
7. Packs: inventory is server-authoritative, opening consumes exactly one pack, grants five cards and 1–5 Stardust, and failures consume nothing.
8. Challenges: each task verifies only its intended in-game event; completed challenges can be hidden; claim state refreshes after website claim.
9. World restrictions: repeat XP/challenge attempts on a normal world and on Leagues, Deadman, LMS, PvP/high-risk, PvP Arena, beta/tournament/speedrunning/no-save worlds. Only the normal-world attempt may progress.
10. Failure handling: offline service, timeout, malformed response, revoked token, logout/world hop, and plugin disable/re-enable are safe and understandable.
11. Chat: `!deckscape` reports collected rarity counts and does not expose private identifiers.
12. UI: side panel, maximized/resized Collection/Packs/Challenges windows, card art, dialogs and canvas overlays remain readable at common RuneLite sizes/scaling.

Fix all release-blocking failures and rerun the automated suite. Commit only source/resources/docs—not `.gradle`, `.gradle-home`, `build`, IDE metadata, logs, tokens, Supabase secrets, or local state.

## 3. Publish the exact plugin commit

From `C:\Projects\DeckscapePlugin`:

```powershell
git status --short
git diff --check
git add <reviewed files>
git commit -m "Prepare Deckscape for Plugin Hub"
git push origin main
git rev-parse HEAD
```

Save the final 40-character hash printed by the last command. Plugin Hub builds exactly that commit, not an uncommitted working tree or a branch name. Confirm the repository and commit are publicly accessible while signed out of GitHub.

## 4. Create the Plugin Hub marker file

1. Fork `runelite/plugin-hub` on GitHub.
2. Clone your fork and add the official repository as `upstream`.
3. Update your local default branch from upstream, then create a focused branch such as `add-deckscape`.
4. Create one extensionless file at `plugins/deckscape` with exactly:

```properties
repository=https://github.com/jona139/DeckscapePlugin.git
commit=REPLACE_WITH_THE_FULL_40_CHARACTER_PLUGIN_COMMIT
warning=This plugin submits your IP address, XP gains, verified challenge events, world-type flags, pack actions, and a revocable device token to a 3rd-party server not controlled or verified by the RuneLite Developers.
```

5. Check that the marker is the only intentional change in the Plugin Hub fork.
6. Commit and push that branch.

Example commands inside the Plugin Hub clone:

```powershell
git remote add upstream https://github.com/runelite/plugin-hub.git
git fetch upstream
git checkout master
git pull --ff-only upstream master
git checkout -b add-deckscape
git add plugins/deckscape
git commit -m "Add Deckscape"
git push -u origin add-deckscape
```

Use the default branch name actually present in the repository if it differs from `master`.

## 5. Open and shepherd the Plugin Hub pull request

Open a pull request from your fork branch to `runelite/plugin-hub`. In its description, concisely state:

- what the plugin does;
- that Deckscape is an independent third-party service;
- the exact Supabase endpoint and the optional OSRS Wiki artwork host;
- that Plugin Hub displays the third-party networking warning before installation;
- the data categories sent and links to `README.md` and `PRIVACY.md`;
- that rewards are server-authoritative/idempotent and restricted to normal worlds;
- that there is no reflection, native code, arbitrary file access, process execution, or extra runtime dependency;
- automated test result and the manual test matrix completed;
- why the JAR is close to 9 MB (bundled artwork).

Watch every automated check. If a check or reviewer requires a plugin change:

1. change and test the Deckscape plugin repository;
2. commit and push it;
3. replace the marker's `commit=` value with the new full hash;
4. commit and push the marker update to the same Plugin Hub pull request.

Do not open repeated replacement pull requests. Respond directly to reviewer questions and keep the single PR current until it is approved and merged.

## 6. Updating after acceptance

For each future release, keep compatibility with Java 11 and current RuneLite, update disclosures when endpoints/data handling change, run the same tests, push a new public plugin commit, and open a Plugin Hub PR changing only `plugins/deckscape` to the new 40-character commit hash. A changed branch without a marker update is not released.
