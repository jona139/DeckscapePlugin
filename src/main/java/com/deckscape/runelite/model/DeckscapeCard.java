package com.deckscape.runelite.model;

public final class DeckscapeCard
{
    public enum Rarity { COMMON, UNCOMMON, RARE, EPIC, LEGENDARY }
    public enum Kind { UNIT, EQUIPMENT, SPELL, PRAYER }
    public enum Style { MELEE, RANGED, MAGIC }

    private final String id;
    private final String name;
    private final String faction;
    private final Rarity rarity;
    private final Kind kind;
    private final Style style;
    private final int cost;
    private final int power;
    private final String text;
    private final String artResource;
    private final String artUrl;

    public DeckscapeCard(String id, String name, String faction, Rarity rarity, Kind kind,
                         Style style, int cost, int power, String text, String artResource)
    {
        this(id, name, faction, rarity, kind, style, cost, power, text, artResource, null);
    }

    public DeckscapeCard(String id, String name, String faction, Rarity rarity, Kind kind,
                         Style style, int cost, int power, String text, String artResource, String artUrl)
    {
        this.id = id;
        this.name = name;
        this.faction = faction;
        this.rarity = rarity;
        this.kind = kind;
        this.style = style;
        this.cost = cost;
        this.power = power;
        this.text = text;
        this.artResource = artResource;
        this.artUrl = artUrl;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getFaction() { return faction; }
    public Rarity getRarity() { return rarity; }
    public Kind getKind() { return kind; }
    public Style getStyle() { return style; }
    public int getCost() { return cost; }
    public int getPower() { return power; }
    public String getText() { return text; }
    public String getArtResource() { return artResource; }
    public String getArtUrl() { return artUrl; }
}
