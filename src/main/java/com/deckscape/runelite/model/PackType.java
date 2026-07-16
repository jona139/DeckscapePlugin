package com.deckscape.runelite.model;

public enum PackType
{
    GENERAL("General pack"),
    COMBAT("Combat pack"),
    SKILLING("Skilling pack"),
    FACTION_MISTHALIN("Misthalin pack"),
    FACTION_KARAMJA("Karamja pack"),
    FACTION_ASGARNIA("Asgarnia pack"),
    FACTION_KANDARIN("Kandarin pack"),
    FACTION_FREMENNIK("Fremennik pack"),
    FACTION_MORYTANIA("Morytania pack"),
    FACTION_DESERT("Desert pack"),
    FACTION_TIRANNWN("Tirannwn pack"),
    FACTION_WILDERNESS("Wilderness pack"),
    FACTION_KOUREND("Kourend pack"),
    FACTION_VARLAMORE("Varlamore pack"),
    FACTION_UNIVERSAL("Universal pack");

    private final String displayName;

    private static final PackType[] INVENTORY_TYPES = {
        GENERAL, COMBAT, SKILLING,
        FACTION_MISTHALIN, FACTION_VARLAMORE, FACTION_KANDARIN,
    };

    PackType(String displayName)
    {
        this.displayName = displayName;
    }

    public String getDisplayName()
    {
        return displayName;
    }

    /** Pack types currently available in the public inventory and included in its total. */
    public static PackType[] inventoryTypes()
    {
        return INVENTORY_TYPES.clone();
    }
}
