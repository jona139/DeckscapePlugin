package com.deckscape.runelite;

import com.deckscape.runelite.challenge.ElementalStrikeTracker;
import com.deckscape.runelite.challenge.GoldenFrameChallengeTracker;
import com.deckscape.runelite.challenge.RuneLiteChallenge;
import com.deckscape.runelite.challenge.VerifiedRuneLiteChallengeEvent;
import com.deckscape.runelite.challenge.WorldEligibility;
import com.deckscape.runelite.model.CardCatalog;
import com.deckscape.runelite.model.DeckscapeCard;
import com.deckscape.runelite.model.DeckscapeState;
import com.deckscape.runelite.model.PackType;
import com.deckscape.runelite.model.PendingSyncEvent;
import com.deckscape.runelite.ui.DeckscapeImages;
import com.deckscape.runelite.ui.DeckscapePanel;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.inject.Provides;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.GameState;
import net.runelite.api.Skill;
import net.runelite.api.WorldType;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.GraphicChanged;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.StatChanged;
import net.runelite.api.events.ChatMessage;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.Notifier;
import net.runelite.client.chat.ChatCommandManager;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.Text;
import okhttp3.OkHttpClient;

@Slf4j
@PluginDescriptor(
    name = "Deckscape",
    description = "Earn and open Deckscape packs, browse cards, and complete OSRS-linked challenges.",
    tags = {"deckscape", "cards", "packs", "collection", "challenges"}
)
public final class DeckscapePlugin extends Plugin
{
    private static final long SYNC_INTERVAL_MS = 30_000L;
    private final Map<Skill, Integer> previousXp = new EnumMap<>(Skill.class);
    private final AtomicBoolean pairingInFlight = new AtomicBoolean();
    private final AtomicBoolean syncInFlight = new AtomicBoolean();
    private final AtomicBoolean eventInFlight = new AtomicBoolean();

    @Inject private net.runelite.api.Client client;
    @Inject private DeckscapeConfig config;
    @Inject private DeckscapeStore store;
    @Inject private DeckscapePanel panel;
    @Inject private ClientToolbar clientToolbar;
    @Inject private OverlayManager overlayManager;
    @Inject private ChallengeCompletionOverlay completionOverlay;
    @Inject private PackRewardOverlay packRewardOverlay;
    @Inject private PackRevealOverlay packRevealOverlay;
    @Inject private ElementalStrikeTracker elementalStrikeTracker;
    @Inject private GoldenFrameChallengeTracker goldenFrameChallengeTracker;
    @Inject private Notifier notifier;
    @Inject private ChatCommandManager chatCommandManager;
    @Inject private ChatMessageManager chatMessageManager;
    @Inject private DeckscapeSyncClient syncClient;
    @Inject private OkHttpClient httpClient;
    @Inject private ScheduledExecutorService executor;
    @Inject private net.runelite.client.input.MouseManager mouseManager;
    @Inject private PackRevealInputListener packRevealInputListener;

    private NavigationButton navigationButton;
    private ScheduledFuture<?> maintenanceTask;

    @Provides
    DeckscapeConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(DeckscapeConfig.class);
    }

    @Override
    protected void startUp()
    {
        DeckscapeState state = store.load();
        DeckscapeImages.configureRemoteArt(httpClient, config::dataSharingConsent, this::repaintDeckscapeArt);
        chatCommandManager.registerCommand("!deckscape", this::showDeckscapeCollectionSummary);
        panel.setHandlers(this::openPack, this::syncWithServer, this::beginPairing, client::playSoundEffect);
        elementalStrikeTracker.setCompletionHandler(this::completeRuneLiteChallenge);
        goldenFrameChallengeTracker.setCompletionHandler(this::completeRuneLiteChallenge);
        overlayManager.add(completionOverlay);
        overlayManager.add(packRewardOverlay);
        overlayManager.add(packRevealOverlay);
        mouseManager.registerMouseListener(packRevealInputListener);
        BufferedImage icon = ImageUtil.loadImageResource(getClass(), "/com/deckscape/runelite/icon.png");
        navigationButton = NavigationButton.builder().tooltip("Deckscape").icon(icon).priority(7).panel(panel).build();
        clientToolbar.addNavigation(navigationButton);
        panel.refresh();
        if (!config.dataSharingConsent())
        {
            panel.refresh();
        }
        else if (state.isLinked())
        {
            syncWithServer();
            flushPendingEvents();
        }
        else
        {
            beginPairing();
        }
        maintenanceTask = executor.scheduleWithFixedDelay(this::maintainSync, 5, 5, TimeUnit.SECONDS);
    }

    @Override
    protected void shutDown()
    {
        previousXp.clear();
        if (maintenanceTask != null) maintenanceTask.cancel(false);
        elementalStrikeTracker.setCompletionHandler(null);
        goldenFrameChallengeTracker.setCompletionHandler(null);
        goldenFrameChallengeTracker.resetSession();
        DeckscapeImages.clearRemoteArtConfiguration();
        chatCommandManager.unregisterCommand("!deckscape");
        overlayManager.remove(completionOverlay);
        overlayManager.remove(packRewardOverlay);
        overlayManager.remove(packRevealOverlay);
        mouseManager.unregisterMouseListener(packRevealInputListener);
        panel.disposeDialog();
        if (navigationButton != null) clientToolbar.removeNavigation(navigationButton);
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event)
    {
        if (event.getGameState() == GameState.LOGIN_SCREEN || event.getGameState() == GameState.HOPPING)
        {
            previousXp.clear();
            goldenFrameChallengeTracker.resetSession();
        }
        else if (event.getGameState() == GameState.LOGGED_IN && config.dataSharingConsent())
        {
            syncWithServer();
            flushPendingEvents();
        }
    }

    @Subscribe
    public void onStatChanged(StatChanged event)
    {
        Integer previous = previousXp.put(event.getSkill(), event.getXp());
        if (previous == null || event.getXp() <= previous) return;
        int gained = event.getXp() - previous;
        if (!canRecordGameplay()) return;
        goldenFrameChallengeTracker.onStatChanged(event.getSkill(), gained);
        processXp(gained);
    }

    @Subscribe public void onGraphicChanged(GraphicChanged event) { if (canRecordGameplay()) elementalStrikeTracker.onGraphicChanged(event); }
    @Subscribe public void onHitsplatApplied(HitsplatApplied event) { if (canRecordGameplay()) elementalStrikeTracker.onHitsplatApplied(event); }
    @Subscribe public void onItemContainerChanged(ItemContainerChanged event) { if (canRecordGameplay()) goldenFrameChallengeTracker.onItemContainerChanged(event); }
    @Subscribe public void onChatMessage(ChatMessage event) { if (canRecordGameplay()) goldenFrameChallengeTracker.onChatMessage(event); }
    @Subscribe public void onGameTick(GameTick event) { if (canRecordGameplay()) goldenFrameChallengeTracker.onGameTick(); }

    @Subscribe
    public void onConfigChanged(ConfigChanged event)
    {
        if (!"deckscape".equals(event.getGroup()) || !"dataSharingConsent".equals(event.getKey())) return;
        previousXp.clear();
        goldenFrameChallengeTracker.resetSession();
        if (config.dataSharingConsent())
        {
            if (store.load().isLinked())
            {
                syncWithServer();
                flushPendingEvents();
            }
            else beginPairing();
        }
        else DeckscapeImages.cancelRemoteArtRequests();
        panel.refresh();
    }

    private void repaintDeckscapeArt()
    {
        panel.repaint();
        for (Window window : Window.getWindows()) if (window.isShowing()) window.repaint();
    }

    private void showDeckscapeCollectionSummary(ChatMessage event, String message)
    {
        if (event.getMessageNode() == null || client.getLocalPlayer() == null) return;
        String sender = Text.removeTags(event.getName() == null ? "" : event.getName());
        if (!sender.equalsIgnoreCase(client.getLocalPlayer().getName())) return;
        event.getMessageNode().setRuneLiteFormatMessage(CollectionRaritySummary.build(store.load()));
        chatMessageManager.update(event.getMessageNode());
    }

    private void maintainSync()
    {
        try
        {
            DeckscapeState state = store.load();
            if (!config.dataSharingConsent()) return;
            if (!state.isLinked())
            {
                if (state.getPairingCode().isEmpty() || state.getPairingExpiresAt() <= System.currentTimeMillis()) beginPairing();
                else pollPairing();
                return;
            }
            flushPendingEvents();
            if (System.currentTimeMillis() - state.getLastSyncAt() >= SYNC_INTERVAL_MS) syncWithServer();
        }
        catch (RuntimeException error)
        {
            log.debug("Deckscape background sync maintenance failed", error);
        }
    }

    private void beginPairing()
    {
        if (!config.dataSharingConsent() || store.load().isLinked() || !pairingInFlight.compareAndSet(false, true)) return;
        syncClient.startPairing()
            .thenAccept(response -> SwingUtilities.invokeLater(() -> {
                try
                {
                    DeckscapeState state = store.load();
                    state.setPairingCode(response.get("pairingCode").getAsString());
                    state.setPendingDeviceToken(response.get("deviceToken").getAsString());
                    state.setPairingExpiresAt(Instant.parse(response.get("expiresAt").getAsString()).toEpochMilli());
                    store.save();
                    panel.refresh();
                }
                finally { pairingInFlight.set(false); }
            }))
            .exceptionally(error -> { pairingInFlight.set(false); log.warn("Could not create a RuneLite sync code", error); return null; });
    }

    private void pollPairing()
    {
        if (!config.dataSharingConsent()) return;
        DeckscapeState state = store.load();
        if (state.getPairingCode().isEmpty() || state.getPendingDeviceToken().isEmpty()
            || !pairingInFlight.compareAndSet(false, true)) return;
        String code = state.getPairingCode();
        String pendingToken = state.getPendingDeviceToken();
        syncClient.finishPairing(code, pendingToken)
            .thenAccept(response -> SwingUtilities.invokeLater(() -> {
                try
                {
                    if (!response.has("linked") || !response.get("linked").getAsBoolean()) return;
                    DeckscapeState current = store.load();
                    current.setDeviceToken(pendingToken);
                    current.clearPairingSession();
                    if (response.has("state")) updateLocalState(current, response.getAsJsonObject("state"));
                    current.setLastSyncAt(System.currentTimeMillis());
                    store.save();
                    panel.refresh();
                    notifier.notify("Deckscape linked. Your RuneLite and website collection are now in sync.");
                    flushPendingEvents();
                }
                finally { pairingInFlight.set(false); }
            }))
            .exceptionally(error -> { pairingInFlight.set(false); log.debug("Deckscape pairing is still pending", error); return null; });
    }

    private void processXp(int gained)
    {
        DeckscapeState state = store.load();
        if (!state.isLinked()) return;
        int remaining = gained;
        synchronized (state)
        {
            while (remaining > 0)
            {
                int amount = Math.min(100_000, remaining);
                JsonObject payload = new JsonObject();
                payload.addProperty("xp", amount);
                addWorldContext(payload);
                String eventId = UUID.randomUUID().toString();
                state.getPendingEvents().add(new PendingSyncEvent("runelite_xp", eventId, payload));
                remaining -= amount;
            }
            store.save();
        }
        flushPendingEvents();
        panel.refresh();
    }

    private void completeRuneLiteChallenge(VerifiedRuneLiteChallengeEvent event)
    {
        DeckscapeState state = store.load();
        RuneLiteChallenge challenge = RuneLiteChallenge.byKind(event.getKind());
        if (challenge == null || !canRecordGameplay() || !state.isLinked()) return;
        if (state.getChallengeProgress().getOrDefault(challenge.getMetric(), 0) >= challenge.getTarget()) return;
        synchronized (state)
        {
            boolean alreadyQueued = !challenge.isQuantity() && state.getPendingEvents().stream().anyMatch(item ->
                "runelite_challenge".equals(item.getAction())
                    && challenge.getKind().equals(stringValue(item.getPayload(), "kind")));
            if (alreadyQueued) return;
            JsonObject payload = event.toPayload();
            addWorldContext(payload);
            state.getPendingEvents().add(new PendingSyncEvent("runelite_challenge", event.getEventId(), payload));
            store.save();
        }
        flushPendingEvents();
    }

    private void flushPendingEvents()
    {
        DeckscapeState state = store.load();
        if (!config.dataSharingConsent() || !state.isLinked() || !eventInFlight.compareAndSet(false, true)) return;
        PendingSyncEvent item;
        synchronized (state)
        {
            if (state.getPendingEvents().isEmpty()) { eventInFlight.set(false); return; }
            item = state.getPendingEvents().get(0);
        }
        syncClient.request(state.getDeviceToken(), item.getAction(), item.getPayload())
            .thenAccept(response -> SwingUtilities.invokeLater(() -> {
                try
                {
                    DeckscapeState current = store.load();
                    RuneLiteChallenge pendingChallenge = "runelite_challenge".equals(item.getAction())
                        ? RuneLiteChallenge.byKind(stringValue(item.getPayload(), "kind")) : null;
                    boolean challengeWasComplete = pendingChallenge != null
                        && current.getChallengeProgress().getOrDefault(pendingChallenge.getMetric(), 0) >= pendingChallenge.getTarget();
                    if (response.has("state")) updateLocalState(current, response.getAsJsonObject("state"));
                    synchronized (current)
                    {
                        current.getPendingEvents().removeIf(event -> item.getEventId().equals(event.getEventId()));
                        current.setLastSyncAt(System.currentTimeMillis());
                        store.save();
                    }
                    if ("runelite_xp".equals(item.getAction())) showXpRewards(response);
                    else if ("runelite_challenge".equals(item.getAction()))
                    {
                        if (pendingChallenge != null && !challengeWasComplete
                            && current.getChallengeProgress().getOrDefault(pendingChallenge.getMetric(), 0) >= pendingChallenge.getTarget())
                        {
                            completionOverlay.showCompletion(pendingChallenge.getTitle(), "Verified - claim the gold trim on Deckscape");
                            notifier.notify("Deckscape challenge verified: " + pendingChallenge.getTitle() + ". Claim it on the website.");
                        }
                    }
                    panel.refresh();
                }
                finally
                {
                    eventInFlight.set(false);
                    flushPendingEvents();
                }
            }))
            .exceptionally(error -> { handleEventFailure(item, error); return null; });
    }

    private void handleEventFailure(PendingSyncEvent item, Throwable error)
    {
        Throwable cause = error;
        while (cause.getCause() != null) cause = cause.getCause();
        if (!(cause instanceof DeckscapeSyncClient.HttpException)
            || !((DeckscapeSyncClient.HttpException) cause).isPermanentValidationFailure())
        {
            eventInFlight.set(false);
            log.warn("Deckscape event retained for retry: " + item.getEventId(), error);
            return;
        }

        log.warn("Discarding permanently rejected Deckscape event {}: {}", item.getEventId(), cause.getMessage());
        SwingUtilities.invokeLater(() -> {
            DeckscapeState current = store.load();
            synchronized (current)
            {
                current.getPendingEvents().removeIf(event -> item.getEventId().equals(event.getEventId()));
                store.save();
            }
            eventInFlight.set(false);
            panel.refresh();
            notifier.notify("Deckscape skipped an invalid queued event and resumed synchronization.");
            flushPendingEvents();
        });
    }

    private void showXpRewards(JsonObject response)
    {
        JsonArray awarded = response.has("awarded") && response.get("awarded").isJsonArray()
            ? response.getAsJsonArray("awarded") : new JsonArray();
        for (JsonElement value : awarded)
        {
            try { packRewardOverlay.showPack(PackType.valueOf(value.getAsString())); }
            catch (IllegalArgumentException ignored) { log.debug("Server awarded an unknown pack type: {}", value); }
        }
        int coins = response.has("coinsAwarded") ? response.get("coinsAwarded").getAsInt() : 0;
        int stardust = response.has("stardustAwarded") ? response.get("stardustAwarded").getAsInt() : 0;
        List<String> rewards = new ArrayList<>();
        if (awarded.size() > 0) rewards.add(awarded.size() + " pack" + (awarded.size() == 1 ? "" : "s"));
        if (coins > 0) rewards.add(coins + " Coins");
        if (stardust > 0) rewards.add(stardust + " Stardust");
        if (!rewards.isEmpty()) notifier.notify("Deckscape XP reward: " + String.join(" + ", rewards));
    }

    private void openPack(PackType type)
    {
        DeckscapeState state = store.load();
        if (!config.dataSharingConsent())
        {
            notifier.notify("Enable 'Share data with Deckscape' in the plugin settings before using online features.");
            return;
        }
        if (!state.isLinked())
        {
            notifier.notify("Link Deckscape to your website account before opening packs.");
            return;
        }
        panel.hideDialog();
        JsonObject payload = new JsonObject();
        payload.addProperty("type", type.name());
        syncClient.request(state.getDeviceToken(), "open_pack", payload)
            .thenAccept(response -> SwingUtilities.invokeLater(() -> {
                List<DeckscapeCard> revealed = new ArrayList<>();
                if (response.has("revealed"))
                {
                    for (JsonElement element : response.getAsJsonArray("revealed"))
                    {
                        DeckscapeCard card = CardCatalog.byId(element.getAsJsonObject().get("id").getAsString());
                        if (card != null) revealed.add(card);
                    }
                }
                DeckscapeState current = store.load();
                if (response.has("state")) updateLocalState(current, response.getAsJsonObject("state"));
                current.setLastSyncAt(System.currentTimeMillis());
                store.save();
                panel.refresh();
                int stardustAward = response.has("stardustAward") ? response.get("stardustAward").getAsInt() : 0;
                if (revealed.size() == 5) packRevealOverlay.showPack(type, revealed, stardustAward);
                else notifier.notify("The pack opened, but this plugin version could not display every card. Your collection is safe and synchronized.");
            }))
            .exceptionally(error -> { log.warn("Failed to open pack", error); notifier.notify("Deckscape could not open that pack. Nothing was opened locally."); return null; });
    }

    private void syncWithServer()
    {
        DeckscapeState state = store.load();
        if (!config.dataSharingConsent() || !state.isLinked() || !syncInFlight.compareAndSet(false, true)) return;
        syncClient.request(state.getDeviceToken(), "get_state", null)
            .thenAccept(response -> SwingUtilities.invokeLater(() -> {
                try
                {
                    DeckscapeState current = store.load();
                    if (response.has("state")) updateLocalState(current, response.getAsJsonObject("state"));
                    current.setLastSyncAt(System.currentTimeMillis());
                    store.save();
                    panel.refresh();
                }
                finally { syncInFlight.set(false); }
            }))
            .exceptionally(error -> { syncInFlight.set(false); log.warn("Deckscape sync failed", error); return null; });
    }

    private void updateLocalState(DeckscapeState state, JsonObject serverState)
    {
        if (serverState.has("coins")) state.setCoins(serverState.get("coins").getAsInt());
        if (serverState.has("stardust")) state.setStardust(serverState.get("stardust").getAsInt());
        if (serverState.has("goldenNuggets")) state.setGoldenNuggets(serverState.get("goldenNuggets").getAsInt());
        if (serverState.has("catalog")) CardCatalog.replaceFromServer(serverState.getAsJsonArray("catalog"));
        if (serverState.has("packs"))
        {
            JsonObject packs = serverState.getAsJsonObject("packs");
            for (PackType type : PackType.values()) state.getPacks().put(type, packs.has(type.name()) ? packs.get(type.name()).getAsInt() : 0);
        }
        if (serverState.has("collection"))
        {
            state.getCollection().clear();
            for (Map.Entry<String, JsonElement> entry : serverState.getAsJsonObject("collection").entrySet())
                state.getCollection().put(entry.getKey(), entry.getValue().getAsInt());
        }
        if (serverState.has("claimedTiers"))
        {
            state.getCompletedChallenges().clear();
            for (JsonElement tier : serverState.getAsJsonArray("claimedTiers")) state.getCompletedChallenges().add(tier.getAsString());
        }
        if (serverState.has("ownedGoldTrims"))
        {
            state.getOwnedGoldTrims().clear();
            for (JsonElement card : serverState.getAsJsonArray("ownedGoldTrims")) state.getOwnedGoldTrims().add(card.getAsString());
        }
        if (serverState.has("challengeProgress"))
        {
            state.getChallengeProgress().clear();
            for (Map.Entry<String, JsonElement> entry : serverState.getAsJsonObject("challengeProgress").entrySet())
                state.getChallengeProgress().put(entry.getKey(), entry.getValue().getAsInt());
        }
        if (serverState.has("runelite"))
        {
            JsonObject runelite = serverState.getAsJsonObject("runelite");
            if (runelite.has("xpTowardsPack")) state.setXpTowardsPack(runelite.get("xpTowardsPack").getAsInt());
            if (runelite.has("xpRewardInterval")) state.setXpRewardInterval(runelite.get("xpRewardInterval").getAsInt());
        }
    }

    private boolean canRecordGameplay()
    {
        return config.dataSharingConsent()
            && WorldEligibility.isEligible(client.getGameState(), client.getWorldType());
    }

    private static String stringValue(JsonObject object, String key)
    {
        return object != null && object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsString() : "";
    }

    private void addWorldContext(JsonObject payload)
    {
        payload.addProperty("world", client.getWorld());
        JsonArray types = new JsonArray();
        for (WorldType type : client.getWorldType()) types.add(type.name());
        payload.add("worldTypes", types);
    }
}
