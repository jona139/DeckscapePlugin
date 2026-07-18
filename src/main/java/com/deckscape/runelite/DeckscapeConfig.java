package com.deckscape.runelite;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("deckscape")
public interface DeckscapeConfig extends Config
{
    @ConfigItem(
        keyName = "completionPopup",
        name = "Challenge completion popup",
        description = "Show the animated Deckscape banner when an OSRS challenge is completed.",
        position = 0
    )
    default boolean completionPopup()
    {
        return true;
    }

    @ConfigItem(
        keyName = "packRewardPopup",
        name = "XP reward popup",
        description = "Show an in-game popup when XP awards a pack, Coins, or Stardust.",
        position = 1
    )
    default boolean packRewardPopup()
    {
        return true;
    }

    @ConfigItem(
        keyName = "xpRewardNotifications",
        name = "XP reward notifications",
        description = "Show a Windows notification when XP awards a pack, Coins, or Stardust.",
        position = 2
    )
    default boolean xpRewardNotifications()
    {
        return true;
    }

}
