package com.deckscape.runelite.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PackTypePriceTest
{
    @Test
    void matchesServerPricesForAvailablePacks()
    {
        assertEquals(800, PackType.GENERAL.getPrice());
        assertEquals(1100, PackType.COMBAT.getPrice());
        assertEquals(1100, PackType.SKILLING.getPrice());
        assertEquals(1600, PackType.FACTION_MISTHALIN.getPrice());
        assertEquals(1600, PackType.FACTION_VARLAMORE.getPrice());
        assertEquals(1600, PackType.FACTION_KANDARIN.getPrice());
    }
}
