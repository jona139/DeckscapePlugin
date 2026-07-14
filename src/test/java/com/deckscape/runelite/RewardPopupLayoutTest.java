package com.deckscape.runelite;

import java.lang.reflect.Proxy;
import net.runelite.api.Client;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RewardPopupLayoutTest
{
    @Test
    void centersFixedModePopupInsideGameViewport()
    {
        Client client = clientWithGeometry(765, 4, 512);

        assertEquals(512, RewardPopupLayout.viewportWidth(client));
        assertEquals(260, RewardPopupLayout.centerX(client));
    }

    @Test
    void fallsBackToCanvasWhenViewportIsUnavailable()
    {
        Client client = clientWithGeometry(800, 0, 0);

        assertEquals(800, RewardPopupLayout.viewportWidth(client));
        assertEquals(400, RewardPopupLayout.centerX(client));
    }

    private static Client clientWithGeometry(int canvasWidth, int viewportOffset, int viewportWidth)
    {
        return (Client) Proxy.newProxyInstance(Client.class.getClassLoader(), new Class<?>[] {Client.class},
            (proxy, method, args) -> {
                if ("getCanvasWidth".equals(method.getName())) return canvasWidth;
                if ("getViewportXOffset".equals(method.getName())) return viewportOffset;
                if ("getViewportWidth".equals(method.getName())) return viewportWidth;
                Class<?> type = method.getReturnType();
                if (type == boolean.class) return false;
                if (type == int.class) return 0;
                if (type == long.class) return 0L;
                if (type == float.class) return 0f;
                if (type == double.class) return 0d;
                return null;
            });
    }
}
