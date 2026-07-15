package com.deckscape.runelite;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("deckscape")
public interface DeckscapeConfig extends Config
{
    @ConfigItem(
        keyName = "dataSharingConsent",
        name = "Share data with Deckscape",
        description = "Required for account sync and rewards. Sends XP gains, verified challenge events, "
            + "pack actions, a revocable device token, and your IP address to the Deckscape service. "
            + "No RuneScape login credentials or other players' data are sent.",
        position = 0
    )
    default boolean dataSharingConsent()
    {
        return false;
    }

    @ConfigItem(
        keyName = "completionPopup",
        name = "Challenge completion popup",
        description = "Show the animated Deckscape banner when an OSRS challenge is completed.",
        position = 1
    )
    default boolean completionPopup()
    {
        return true;
    }

    @ConfigItem(
        keyName = "packRewardPopup",
        name = "Pack reward popup",
        description = "Show a notification when XP awards a pack.",
        position = 2
    )
    default boolean packRewardPopup()
    {
        return true;
    }

}
