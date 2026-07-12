package com.deckscape.runelite.model;

public final class DeckscapeCard
{
    public enum Rarity { COMMON, UNCOMMON, RARE, EPIC, LEGENDARY }
    public enum Kind { UNIT, EQUIPMENT, SPELL, PRAYER }

    private final String id;
    private final String name;
    private final String faction;
    private final Rarity rarity;
    private final Kind kind;
    private final String artResource;

    public DeckscapeCard(String id, String name, String faction, Rarity rarity, Kind kind, String artResource)
    {
        this.id = id;
        this.name = name;
        this.faction = faction;
        this.rarity = rarity;
        this.kind = kind;
        this.artResource = artResource;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getFaction() { return faction; }
    public Rarity getRarity() { return rarity; }
    public Kind getKind() { return kind; }
    public String getArtResource() { return artResource; }
}
