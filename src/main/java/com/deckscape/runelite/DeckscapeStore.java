package com.deckscape.runelite;

import com.deckscape.runelite.model.DeckscapeState;
import com.google.gson.Gson;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.config.ConfigManager;

@Singleton
public final class DeckscapeStore
{
    private static final String GROUP = "deckscape";
    private static final String KEY = "prototypeState";
    private final ConfigManager configManager;
    private final Gson gson;
    private DeckscapeState state;

    @Inject
    public DeckscapeStore(ConfigManager configManager, Gson gson)
    {
        this.configManager = configManager;
        this.gson = gson;
    }

    public DeckscapeState load()
    {
        if (state != null) return state;
        String json = configManager.getConfiguration(GROUP, KEY);
        try
        {
            state = json == null || json.isEmpty() ? new DeckscapeState() : gson.fromJson(json, DeckscapeState.class);
        }
        catch (RuntimeException ignored)
        {
            state = new DeckscapeState();
        }
        if (state == null) state = new DeckscapeState();
        return state;
    }

    public void save()
    {
        configManager.setConfiguration(GROUP, KEY, gson.toJson(load()));
    }
}
