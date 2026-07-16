package com.deckscape.runelite;

import com.deckscape.runelite.model.CardCatalog;
import com.deckscape.runelite.model.DeckscapeCard;
import com.deckscape.runelite.model.DeckscapeState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CollectionRaritySummaryTest
{
    @Test
    void reportsUniqueCardsByRarityAndTotalCopies()
    {
        DeckscapeState state = new DeckscapeState();
        DeckscapeCard common = CardCatalog.all().stream()
            .filter(card -> card.getRarity() == DeckscapeCard.Rarity.COMMON).findFirst().orElseThrow();
        DeckscapeCard rare = CardCatalog.all().stream()
            .filter(card -> card.getRarity() == DeckscapeCard.Rarity.RARE).findFirst().orElseThrow();
        state.getCollection().put(common.getId(), 3);
        state.getCollection().put(rare.getId(), 1);

        String summary = CollectionRaritySummary.build(state);

        assertTrue(summary.contains("Common 1/"));
        assertTrue(summary.contains("Rare 1/"));
        assertTrue(summary.contains("4 total copies"));
        assertTrue(summary.contains("<col=5f5f5f>Common"));
        assertTrue(summary.contains("<col=237a32>Uncommon"));
        assertTrue(summary.contains("<col=245bb5>Rare"));
        assertTrue(summary.contains("<col=7737a8>Epic"));
        assertTrue(summary.contains("<col=b35c00>Legendary"));
    }
}
