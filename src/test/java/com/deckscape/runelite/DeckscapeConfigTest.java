package com.deckscape.runelite;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DeckscapeConfigTest
{
    private final DeckscapeConfig config = new DeckscapeConfig() {};

    @Test
    void enablesRewardPopupsAndNotificationsByDefault()
    {
        assertTrue(config.packRewardPopup());
        assertTrue(config.xpRewardNotifications());
    }
}
