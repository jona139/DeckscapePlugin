package com.deckscape.runelite;

import net.runelite.api.ChatMessageType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeckscapeChatCommandTest
{
    @Test
    void recognisesSanitizedLocalPublicAndOutgoingPrivateMessages()
    {
        assertTrue(DeckscapePlugin.isLocalCommandSender(ChatMessageType.PUBLICCHAT,
            "<img=0>GamecubeJona", "GamecubeJona"));
        assertTrue(DeckscapePlugin.isLocalCommandSender(ChatMessageType.PRIVATECHATOUT,
            "Recipient", "GamecubeJona"));
        assertFalse(DeckscapePlugin.isLocalCommandSender(ChatMessageType.PUBLICCHAT,
            "Another player", "GamecubeJona"));
    }
}
