package com.deckscape.runelite.model;

import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CardCatalogTest
{
    @Test
    void containsTheWebsiteCardSet()
    {
        assertEquals(43, CardCatalog.all().size());
        assertEquals(43, CardCatalog.all().stream().map(DeckscapeCard::getId).distinct().count());
    }

    @Test
    void everyCardHasBundledArtwork()
    {
        for (DeckscapeCard card : CardCatalog.all())
        {
            assertTrue(card.getArtResource() != null, card.getId() + " has no art resource");
            assertTrue(CardCatalogTest.class.getResource(card.getArtResource()) != null,
                card.getId() + " art is not bundled: " + card.getArtResource());
        }
    }

    @Test
    void resolvesWebsiteArtworkSelectionsToBundledPluginArt()
    {
        assertEquals("/com/deckscape/runelite/cards/archmage_sedridor_arcane.png",
            CardCatalog.selectedArtResource("archmage_sedridor", "archmage_sedridor:arcane", null));
        assertEquals("/com/deckscape/runelite/cards/archmage_sedridor.png",
            CardCatalog.selectedArtResource("archmage_sedridor", "", null));
        assertEquals("/com/deckscape/runelite/cards/man_alternate.png",
            CardCatalog.selectedArtResource("man", "man:alternate", null));
        assertEquals("/com/deckscape/runelite/cards/man.png",
            CardCatalog.selectedArtResource("man", "", null));
        assertEquals("/com/deckscape/runelite/cards/excalibur_phatraccoon.png",
            CardCatalog.selectedArtResource("excalibur", "excalibur:phatraccoon", null));
        assertEquals("/com/deckscape/runelite/cards/excalibur.png",
            CardCatalog.selectedArtResource("excalibur", "", null));
        assertEquals("/com/deckscape/runelite/cards/eye_of_ayak_generikwaffles.png",
            CardCatalog.selectedArtResource("eye_of_ayak", "eye_of_ayak:generikwaffles", null));
        assertEquals("/com/deckscape/runelite/cards/eye_of_ayak.png",
            CardCatalog.selectedArtResource("eye_of_ayak", "", null));
        assertEquals("/com/deckscape/runelite/cards/tormented_demon_cruk.png",
            CardCatalog.selectedArtResource("tormented_demon", "tormented_demon:cruk", null));
        assertEquals("/com/deckscape/runelite/cards/tormented_demon.png",
            CardCatalog.selectedArtResource("tormented_demon", "", null));
    }

    @Test
    void everyPackContainsFiveEligibleCards()
    {
        Random random = new Random(42);
        for (PackType type : PackType.values())
        {
            List<DeckscapeCard> cards = CardCatalog.openPack(type, random);
            assertEquals(5, cards.size());
            if (type == PackType.COMBAT) assertTrue(cards.stream().allMatch(card -> card.getKind() == DeckscapeCard.Kind.UNIT));
            if (type == PackType.SKILLING) assertTrue(cards.stream().allMatch(card -> card.getKind() != DeckscapeCard.Kind.UNIT));
            if (type == PackType.FACTION_MISTHALIN) assertTrue(cards.stream().allMatch(card -> "Misthalin".equals(card.getFaction())));
        }
    }
}
