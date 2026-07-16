package com.deckscape.runelite;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("deckscape")
public interface DeckscapeConfig extends Config
{
    @ConfigItem(
        keyName = "dataSharingConsent",
        name = "Share data with Deckscape (third party)",
        description = "Required for account sync and rewards. Sends XP gains, verified challenge events and world-type flags, "
            + "pack actions, a revocable device token, and your IP address to the Deckscape service, and "
            + "downloads card artwork from the Old School RuneScape Wiki. "
            + "No RuneScape login credentials or other players' data are sent.",
        warning = "Enabling this option submits your IP address, XP gains, verified challenge events, world-type flags, "
            + "pack actions, and a revocable device token to a 3rd-party server not controlled or verified by the "
            + "RuneLite Developers.",
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
        name = "XP reward popup",
        description = "Show a notification when XP awards a pack, Coins, or Stardust.",
        position = 2
    )
    default boolean packRewardPopup()
    {
        return true;
    }

}
