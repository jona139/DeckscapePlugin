package com.deckscape.runelite;

import net.runelite.api.Client;

/** Shared top-centre geometry for compact reward notifications. */
final class RewardPopupLayout
{
    static final int WIDTH = 134;
    static final int HEIGHT = 62;
    static final int TOP = 6;

    private RewardPopupLayout() {}

    static int viewportWidth(Client client)
    {
        int width = client.getViewportWidth();
        return width > 0 ? width : client.getCanvasWidth();
    }

    static int centerX(Client client)
    {
        int viewportWidth = client.getViewportWidth();
        if (viewportWidth <= 0) return client.getCanvasWidth() / 2;
        return client.getViewportXOffset() + viewportWidth / 2;
    }
}
