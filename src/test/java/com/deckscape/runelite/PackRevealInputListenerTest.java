package com.deckscape.runelite;

import java.awt.Canvas;
import java.awt.Point;
import java.awt.event.MouseEvent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PackRevealInputListenerTest
{
    @Test
    void preservesCanvasCoordinatesProvidedByMouseManager()
    {
        MouseEvent event = new MouseEvent(
            new Canvas(),
            MouseEvent.MOUSE_PRESSED,
            0L,
            0,
            742,
            391,
            1,
            false,
            MouseEvent.BUTTON1);

        assertEquals(new Point(742, 391), PackRevealInputListener.canvasPoint(event));
    }
}
