package com.deckscape.runelite.ui;

import com.deckscape.runelite.model.PackType;
import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.runelite.client.util.ImageUtil;

public final class DeckscapeImages
{
    private static final Map<String, BufferedImage> CACHE = new ConcurrentHashMap<>();

    private DeckscapeImages() {}

    public static BufferedImage load(String resource)
    {
        if (resource == null || resource.isEmpty()) return null;
        return CACHE.computeIfAbsent(resource, key -> ImageUtil.loadImageResource(DeckscapeImages.class, key));
    }

    public static String packResource(PackType type)
    {
        switch (type)
        {
            case COMBAT: return "/com/deckscape/runelite/pack_combat.png";
            case SKILLING: return "/com/deckscape/runelite/pack_skilling.png";
            case FACTION_MISTHALIN: return "/com/deckscape/runelite/pack_misthalin.png";
            default: return "/com/deckscape/runelite/pack_general.png";
        }
    }
}
