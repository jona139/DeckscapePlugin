package com.deckscape.runelite;

import com.deckscape.runelite.challenge.ElementalStrikeTracker;
import com.deckscape.runelite.challenge.VerifiedRuneLiteChallengeEvent;
import com.deckscape.runelite.model.CardCatalog;
import com.deckscape.runelite.model.DeckscapeCard;
import com.deckscape.runelite.model.DeckscapeState;
import com.deckscape.runelite.model.PackType;
import com.deckscape.runelite.model.PendingSyncEvent;
import com.deckscape.runelite.ui.DeckscapePanel;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.inject.Provides;
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
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GraphicChanged;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.events.StatChanged;
import net.runelite.client.Notifier;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ImageUtil;

@Slf4j
@PluginDescriptor(
    name = "Deckscape",
    description = "Earn and open Deckscape packs, browse cards, and complete OSRS-linked challenges.",
    tags = {"deckscape", "cards", "packs", "collection", "challenges"}
)
public final class DeckscapePlugin extends Plugin
{
    private static final String ELEMENTAL_TIER = "elemental_strike_max_hit:gold";
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
    @Inject private Notifier notifier;
    @Inject private DeckscapeSyncClient syncClient;
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
        panel.setHandlers(this::openPack, this::syncWithServer, this::beginPairing, client::playSoundEffect);
        elementalStrikeTracker.setCompletionHandler(this::completeElementalStrikeChallenge);
        overlayManager.add(completionOverlay);
        overlayManager.add(packRewardOverlay);
        overlayManager.add(packRevealOverlay);
        mouseManager.registerMouseListener(packRevealInputListener);
        BufferedImage icon = ImageUtil.loadImageResource(getClass(), "/com/deckscape/runelite/icon.png");
        navigationButton = NavigationButton.builder().tooltip("Deckscape").icon(icon).priority(7).panel(panel).build();
        clientToolbar.addNavigation(navigationButton);
        panel.refresh();
        if (state.isLinked())
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
        overlayManager.remove(completionOverlay);
        overlayManager.remove(packRewardOverlay);
        overlayManager.remove(packRevealOverlay);
        mouseManager.unregisterMouseListener(packRevealInputListener);
        if (navigationButton != null) clientToolbar.removeNavigation(navigationButton);
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event)
    {
        if (event.getGameState() == GameState.LOGIN_SCREEN || event.getGameState() == GameState.HOPPING)
        {
            previousXp.clear();
        }
        else if (event.getGameState() == GameState.LOGGED_IN)
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
        processXp(event.getXp() - previous);
    }

    @Subscribe public void onGraphicChanged(GraphicChanged event) { elementalStrikeTracker.onGraphicChanged(event); }
    @Subscribe public void onHitsplatApplied(HitsplatApplied event) { elementalStrikeTracker.onHitsplatApplied(event); }

    private void maintainSync()
    {
        try
        {
            DeckscapeState state = store.load();
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
        if (store.load().isLinked() || !pairingInFlight.compareAndSet(false, true)) return;
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
                String eventId = UUID.randomUUID().toString();
                state.getPendingEvents().add(new PendingSyncEvent("runelite_xp", eventId, payload));
                remaining -= amount;
            }
            store.save();
        }
        flushPendingEvents();
        panel.refresh();
    }

    private void completeElementalStrikeChallenge(VerifiedRuneLiteChallengeEvent event)
    {
        DeckscapeState state = store.load();
        if (!state.isLinked() || state.getCompletedChallenges().contains(ELEMENTAL_TIER)) return;
        synchronized (state)
        {
            boolean alreadyQueued = state.getPendingEvents().stream().anyMatch(item -> "runelite_challenge".equals(item.getAction()));
            if (alreadyQueued) return;
            JsonObject payload = new JsonObject();
            payload.addProperty("spell", event.getPayload().getSpell());
            payload.addProperty("damage", event.getPayload().getDamage());
            payload.addProperty("maxHit", event.getPayload().getMaxHit());
            state.getPendingEvents().add(new PendingSyncEvent("runelite_challenge", event.getEventId(), payload));
            store.save();
        }
        flushPendingEvents();
    }

    private void flushPendingEvents()
    {
        DeckscapeState state = store.load();
        if (!state.isLinked() || !eventInFlight.compareAndSet(false, true)) return;
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
                    if (response.has("state")) updateLocalState(current, response.getAsJsonObject("state"));
                    synchronized (current)
                    {
                        current.getPendingEvents().removeIf(event -> item.getEventId().equals(event.getEventId()));
                        current.setLastSyncAt(System.currentTimeMillis());
                        store.save();
                    }
                    if ("runelite_xp".equals(item.getAction())) showAwardedPacks(response);
                    else if ("runelite_challenge".equals(item.getAction()))
                    {
                        completionOverlay.showCompletion("Perfectly Elemental", "Elemental Strike gold trim unlocked");
                        notifier.notify("Deckscape challenge complete: Perfectly Elemental");
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

    private void showAwardedPacks(JsonObject response)
    {
        if (!response.has("awarded")) return;
        JsonArray awarded = response.getAsJsonArray("awarded");
        for (JsonElement value : awarded)
        {
            try { packRewardOverlay.showPack(PackType.valueOf(value.getAsString())); }
            catch (IllegalArgumentException ignored) { log.debug("Server awarded an unknown pack type: {}", value); }
        }
        if (awarded.size() > 0) notifier.notify("Deckscape: " + awarded.size() + " new pack" + (awarded.size() == 1 ? "" : "s") + " unlocked!");
    }

    private void openPack(PackType type)
    {
        DeckscapeState state = store.load();
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
                if (revealed.size() == 5) packRevealOverlay.showPack(type, revealed);
                else notifier.notify("The pack opened, but this plugin version could not display every card. Your collection is safe and synchronized.");
            }))
            .exceptionally(error -> { log.warn("Failed to open pack", error); notifier.notify("Deckscape could not open that pack. Nothing was opened locally."); return null; });
    }

    private void syncWithServer()
    {
        DeckscapeState state = store.load();
        if (!state.isLinked() || !syncInFlight.compareAndSet(false, true)) return;
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
        }
    }
}
