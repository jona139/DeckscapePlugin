package com.deckscape.runelite;

import net.runelite.api.Client;

/** Shared top-centre geometry for compact reward notifications. */
final class RewardPopupLayout
{
    private static final float SCALE = 1.2f;
    private static final float LINGER_MULTIPLIER = 2.5f;

    static final int WIDTH = scale(134);
    static final int HEIGHT = scale(62);
    static final int TOP = 6;

    private RewardPopupLayout() {}

    static int scale(int value)
    {
        return Math.round(value * SCALE);
    }

    static long linger(long milliseconds)
    {
        return Math.round(milliseconds * LINGER_MULTIPLIER);
    }

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
