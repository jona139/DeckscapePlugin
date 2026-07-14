package com.deckscape.runelite.ui;

import com.deckscape.runelite.model.PackType;
import java.awt.Color;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Region and pack colour themes plus crest/backdrop resources, mirroring DeckscapeWeb's
 * styles.css (.region-* / .pack-* custom properties) and assets.tsx so both clients match.
 */
public final class RegionTheme
{
    public final Color color;
    public final Color deep;
    public final Color glow;
    public final String crest;
    public final String backdrop;

    private static final String REGIONS = "/com/deckscape/runelite/regions/";
    private static final Map<String, RegionTheme> BY_FACTION = new HashMap<>();
    private static final RegionTheme FALLBACK = new RegionTheme(0x765b35, 0x3c2b1b, 0xe7bc5e, null, null);

    static
    {
        region("misthalin", 0x2b69b7, 0x15305e, 0x9dcaff, true);
        region("karamja", 0x299464, 0x104b34, 0x97e6b8, false);
        region("asgarnia", 0x486fba, 0x273c68, 0xc3d6ff, false);
        region("kandarin", 0xa4443d, 0x58231f, 0xf1a798, false);
        region("fremennik", 0x89654e, 0x453126, 0xd8b394, false);
        region("morytania", 0x286c7d, 0x123a49, 0x8ed1db, false);
        region("desert", 0x2f9274, 0x154a3c, 0xa4e1c0, false);
        region("tirannwn", 0x8d949b, 0x454b52, 0xe0e5e8, false);
        region("wilderness", 0x5c6268, 0x292d31, 0xb8c0c6, false);
        region("kourend", 0x34799e, 0x173c55, 0x91cae8, false);
        region("varlamore", 0xe9a414, 0xbd5009, 0xffdc62, false);
        region("universal", 0x34456d, 0x18223b, 0x89a6ff, true);
    }

    private RegionTheme(int color, int deep, int glow, String crest, String backdrop)
    {
        this.color = new Color(color);
        this.deep = new Color(deep);
        this.glow = new Color(glow);
        this.crest = crest;
        this.backdrop = backdrop;
    }

    private static void region(String key, int color, int deep, int glow, boolean hasBackdrop)
    {
        BY_FACTION.put(key, new RegionTheme(color, deep, glow,
            REGIONS + "crest_" + key + ".png",
            hasBackdrop ? REGIONS + "backdrop_" + key + ".png" : null));
    }

    public static RegionTheme forFaction(String faction)
    {
        if (faction == null) return FALLBACK;
        return BY_FACTION.getOrDefault(faction.toLowerCase(Locale.ROOT), FALLBACK);
    }

    /** Faction packs reuse their region theme; the three XP packs have their own colours. */
    public static RegionTheme forPack(PackType type)
    {
        switch (type)
        {
            case GENERAL: return new RegionTheme(0x566989, 0x29344d, 0xb9c8e6, null, null);
            case COMBAT: return new RegionTheme(0x9a493c, 0x4a2623, 0xefae72, null, null);
            case SKILLING: return new RegionTheme(0x4b8b49, 0x244b2b, 0xa6db78, null, null);
            default: return forFaction(type.name().substring("FACTION_".length()));
        }
    }

    /** The picture at the centre of a pack: the XP packs' emblem art, or the region crest. */
    public static String packEmblem(PackType type)
    {
        switch (type)
        {
            case GENERAL: return "/com/deckscape/runelite/pack_general.png";
            case COMBAT: return "/com/deckscape/runelite/pack_combat.png";
            case SKILLING: return "/com/deckscape/runelite/pack_skilling.png";
            default: return forPack(type).crest;
        }
    }
}
