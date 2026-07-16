package com.deckscape.runelite;

import com.deckscape.runelite.model.CardCatalog;
import com.deckscape.runelite.model.DeckscapeCard;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class PackRevealOverlayTest
{
    @Test
    void marksOnlyCardsMissingBeforeThePackWasOpenedAsNew()
    {
        DeckscapeCard owned = CardCatalog.all().get(0);
        DeckscapeCard newCard = CardCatalog.all().get(1);

        boolean[] flags = PackRevealOverlay.newCardFlags(
            Arrays.asList(owned, newCard, newCard), Collections.singleton(owned.getId()));

        assertArrayEquals(new boolean[] {false, true, true}, flags);
    }
}
