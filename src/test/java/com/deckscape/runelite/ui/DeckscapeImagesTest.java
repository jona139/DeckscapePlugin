package com.deckscape.runelite.ui;

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeckscapeImagesTest
{
    @Test
    void keepsDirectWikiImageUrlsAndAddsNameFallbacks()
    {
        String direct = "https://oldschool.runescape.wiki/w/Special:FilePath/Quetzal_Whistle.png";
        List<String> candidates = DeckscapeImages.remoteCandidates("Quetzal Whistle", direct);

        assertEquals(direct, candidates.get(0));
        assertTrue(candidates.contains("https://oldschool.runescape.wiki/w/Special:FilePath/Quetzal_Whistle_detail.png"));
    }

    @Test
    void convertsWikiMediaFragmentsToDirectFileRequests()
    {
        List<String> candidates = DeckscapeImages.remoteCandidates("Ernest (Chicken)",
            "https://oldschool.runescape.wiki/w/Ernest_the_Chicken#/media/File:Chicken_(Ernest).png");

        assertTrue(candidates.get(0).contains("Special:FilePath/Chicken_%28Ernest%29.png"));
    }

    @Test
    void rejectsNonWikiHosts()
    {
        List<String> candidates = DeckscapeImages.remoteCandidates("Pygmy Hippo Call",
            "https://example.com/tracker/card.png");

        assertEquals(2, candidates.size());
        assertTrue(candidates.get(0).startsWith("https://oldschool.runescape.wiki/w/Special:FilePath/"));
    }
}
