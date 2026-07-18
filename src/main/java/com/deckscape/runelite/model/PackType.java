package com.deckscape.runelite.model;

public enum PackType
{
    GENERAL("General pack", 800),
    COMBAT("Combat pack", 1100),
    SKILLING("Skilling pack", 1100),
    FACTION_MISTHALIN("Misthalin pack", 1600),
    FACTION_KARAMJA("Karamja pack", 1600),
    FACTION_ASGARNIA("Asgarnia pack", 1600),
    FACTION_KANDARIN("Kandarin pack", 1600),
    FACTION_FREMENNIK("Fremennik pack", 1600),
    FACTION_MORYTANIA("Morytania pack", 1600),
    FACTION_DESERT("Desert pack", 1600),
    FACTION_TIRANNWN("Tirannwn pack", 1600),
    FACTION_WILDERNESS("Wilderness pack", 1600),
    FACTION_KOUREND("Kourend pack", 1600),
    FACTION_VARLAMORE("Varlamore pack", 1600),
    FACTION_UNIVERSAL("Universal pack", 0);

    private final String displayName;
    private final int price;

    private static final PackType[] INVENTORY_TYPES = {
        GENERAL, COMBAT, SKILLING,
        FACTION_MISTHALIN, FACTION_VARLAMORE, FACTION_KANDARIN,
    };

    PackType(String displayName, int price)
    {
        this.displayName = displayName;
        this.price = price;
    }

    public String getDisplayName()
    {
        return displayName;
    }

    public int getPrice()
    {
        return price;
    }

    /** Pack types currently available in the public inventory and included in its total. */
    public static PackType[] inventoryTypes()
    {
        return INVENTORY_TYPES.clone();
    }
}
