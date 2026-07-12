package com.deckscape.runelite;

import com.deckscape.runelite.challenge.ElementalStrikeTracker;
import com.deckscape.runelite.challenge.VerifiedRuneLiteChallengeEvent;
import com.deckscape.runelite.model.CardCatalog;
import com.deckscape.runelite.model.DeckscapeCard;
import com.deckscape.runelite.model.DeckscapeState;
import com.deckscape.runelite.model.PackType;
import com.deckscape.runelite.ui.DeckscapePanel;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.inject.Provides;
import java.awt.image.BufferedImage;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
    private static final String ELEMENTAL_CHALLENGE = "elemental_strike_max_hit";
    private final Map<Skill, Integer> previousXp = new EnumMap<>(Skill.class);

    @Inject private net.runelite.api.Client client;
    @Inject private DeckscapeConfig config;
    @Inject private DeckscapeStore store;
    @Inject private DeckscapePanel panel;
    @Inject private ClientToolbar clientToolbar;
    @Inject private OverlayManager overlayManager;
    @Inject private ChallengeCompletionOverlay completionOverlay;
    @Inject private ElementalStrikeTracker elementalStrikeTracker;
    @Inject private Notifier notifier;
    @Inject private DeckscapeSyncClient syncClient;

    private NavigationButton navigationButton;

    @Provides
    DeckscapeConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(DeckscapeConfig.class);
    }

    @Override
    protected void startUp()
    {
        store.load();
        panel.setHandlers(this::openPack, this::syncWithServer, this::pairWithServer, client::playSoundEffect);
        elementalStrikeTracker.setCompletionHandler(this::completeElementalStrikeChallenge);
        overlayManager.add(completionOverlay);
        BufferedImage icon = ImageUtil.loadImageResource(getClass(), "/com/deckscape/runelite/icon.png");
        navigationButton = NavigationButton.builder()
            .tooltip("Deckscape")
            .icon(icon)
            .priority(7)
            .panel(panel)
            .build();
        clientToolbar.addNavigation(navigationButton);
        panel.refresh();
        syncWithServer();
    }

    @Override
    protected void shutDown()
    {
        previousXp.clear();
        elementalStrikeTracker.setCompletionHandler(null);
        overlayManager.remove(completionOverlay);
        if (navigationButton != null) clientToolbar.removeNavigation(navigationButton);
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event)
    {
        if (event.getGameState() == GameState.LOGIN_SCREEN || event.getGameState() == GameState.HOPPING)
        {
            previousXp.clear();
        }
    }

    @Subscribe
    public void onStatChanged(StatChanged event)
    {
        Integer previous = previousXp.put(event.getSkill(), event.getXp());
        if (previous == null || event.getXp() <= previous) return;
        processXp(event.getXp() - previous);
    }

    @Subscribe
    public void onGraphicChanged(GraphicChanged event)
    {
        elementalStrikeTracker.onGraphicChanged(event);
    }

    @Subscribe
    public void onHitsplatApplied(HitsplatApplied event)
    {
        elementalStrikeTracker.onHitsplatApplied(event);
    }

    private void processXp(int gained)
    {
        DeckscapeState state = store.load();
        if (!state.isLinked() || config.apiUrl().isEmpty() || config.apiKey().isEmpty()) return;
        JsonObject payload = new JsonObject();
        payload.addProperty("eventId", UUID.randomUUID().toString());
        payload.addProperty("xp", gained);
        syncClient.request(config.apiUrl(), config.apiKey(), state.getDeviceToken(), "runelite_xp", payload)
            .thenAccept(response -> SwingUtilities.invokeLater(() -> {
                if (response.has("state")) updateLocalState(state, response.getAsJsonObject("state"));
                store.save(); panel.refresh();
                if (config.packRewardPopup() && response.has("awarded") && response.getAsJsonArray("awarded").size() > 0)
                    notifier.notify("Deckscape: You earned " + response.getAsJsonArray("awarded").size() + " pack(s)!");
            }))
            .exceptionally(ex -> { log.warn("Deckscape XP event was not accepted; no local reward was granted", ex); return null; });
    }

    private List<DeckscapeCard> openPack(PackType type)
    {
        DeckscapeState state = store.load();
        boolean hasApi = !config.apiUrl().isEmpty() && !config.apiKey().isEmpty() && state.isLinked();
        if (hasApi)
        {
            try
            {
                JsonObject payload = new JsonObject();
                payload.addProperty("type", type.name());
                JsonObject response = syncClient.request(config.apiUrl(), config.apiKey(), state.getDeviceToken(), "open_pack", payload)
                    .get(5, java.util.concurrent.TimeUnit.SECONDS);
                
                if (response.has("revealed"))
                {
                    JsonArray revealed = response.getAsJsonArray("revealed");
                    List<DeckscapeCard> cards = new java.util.ArrayList<>();
                    for (JsonElement el : revealed)
                    {
                        String cardId = el.getAsJsonObject().get("id").getAsString();
                        DeckscapeCard card = CardCatalog.byId(cardId);
                        if (card != null) cards.add(card);
                    }
                    
                    if (response.has("state"))
                    {
                        updateLocalState(state, response.getAsJsonObject("state"));
                        store.save();
                    }
                    return cards;
                }
            }
            catch (Exception ex)
            {
                log.warn("Failed to open pack on server: ", ex);
                return null;
            }
        }

        return null;
    }

    private void completeElementalStrikeChallenge(VerifiedRuneLiteChallengeEvent event)
    {
        DeckscapeState state = store.load();
        if (state.getCompletedChallenges().contains("elemental_strike_max_hit:gold")) return;
        boolean hasApi = !config.apiUrl().isEmpty() && !config.apiKey().isEmpty() && state.isLinked();
        if (hasApi)
        {
            JsonObject payload = new JsonObject();
            payload.addProperty("eventId", event.getEventId());
            payload.addProperty("spell", event.getPayload().getSpell());
            payload.addProperty("damage", event.getPayload().getDamage());
            payload.addProperty("maxHit", event.getPayload().getMaxHit());
            syncClient.request(config.apiUrl(), config.apiKey(), state.getDeviceToken(), "runelite_challenge", payload)
                .thenAccept(response -> {
                    if (response.has("state"))
                    {
                        SwingUtilities.invokeLater(() -> {
                            updateLocalState(state, response.getAsJsonObject("state"));
                            store.save();
                            completionOverlay.showCompletion("Perfectly Elemental", "Elemental Strike gold trim unlocked");
                            notifier.notify("Deckscape challenge complete: Perfectly Elemental");
                            panel.refresh();
                        });
                    }
                })
                .exceptionally(ex -> {
                    log.warn("Failed to claim challenge tier on server: ", ex);
                    return null;
                });
        }
        log.debug("Queued Deckscape RuneLite event {} ({})", event.getEventId(), event.getKind());
    }

    private void syncWithServer()
    {
        if (config.apiUrl().isEmpty() || config.apiKey().isEmpty()) return;
        DeckscapeState state = store.load();
        if (!state.isLinked()) return;
        syncClient.request(config.apiUrl(), config.apiKey(), state.getDeviceToken(), "get_state", null)
            .thenAccept(response -> {
                if (response.has("state"))
                {
                    JsonObject serverState = response.getAsJsonObject("state");
                    SwingUtilities.invokeLater(() -> {
                        updateLocalState(state, serverState);
                        store.save();
                        panel.refresh();
                    });
                }
            })
            .exceptionally(ex -> {
                log.warn("Deckscape sync failed: ", ex);
                return null;
            });
    }

    private void pairWithServer(String code)
    {
        syncClient.pair(config.apiUrl(), config.apiKey(), code).thenAccept(response -> {
            if (!response.has("deviceToken")) return;
            SwingUtilities.invokeLater(() -> {
                DeckscapeState state = store.load();
                state.setDeviceToken(response.get("deviceToken").getAsString());
                if (response.has("state")) updateLocalState(state, response.getAsJsonObject("state"));
                store.save(); panel.refresh(); notifier.notify("Deckscape RuneLite account linked.");
            });
        }).exceptionally(ex -> { log.warn("Deckscape pairing failed", ex); notifier.notify("Deckscape pairing failed. Check the code and try again."); return null; });
    }

    private void updateLocalState(DeckscapeState state, JsonObject serverState)
    {
        if (serverState.has("catalog")) CardCatalog.replaceFromServer(serverState.getAsJsonArray("catalog"));
        if (serverState.has("packs"))
        {
            JsonObject packsObj = serverState.getAsJsonObject("packs");
            for (PackType type : PackType.values())
            {
                String key = type.name();
                int qty = packsObj.has(key) ? packsObj.get(key).getAsInt() : 0;
                state.getPacks().put(type, qty);
            }
        }

        if (serverState.has("collection"))
        {
            JsonObject collObj = serverState.getAsJsonObject("collection");
            state.getCollection().clear();
            for (java.util.Map.Entry<String, JsonElement> entry : collObj.entrySet())
            {
                state.getCollection().put(entry.getKey(), entry.getValue().getAsInt());
            }
        }
        
        if (serverState.has("claimedTiers"))
        {
            state.getCompletedChallenges().clear();
            for (JsonElement tier : serverState.getAsJsonArray("claimedTiers"))
            {
                state.getCompletedChallenges().add(tier.getAsString());
            }
        }
        if (serverState.has("challengeProgress"))
        {
            state.getChallengeProgress().clear();
            for (java.util.Map.Entry<String, JsonElement> entry : serverState.getAsJsonObject("challengeProgress").entrySet())
                state.getChallengeProgress().put(entry.getKey(), entry.getValue().getAsInt());
        }
    }
}
