package com.deckscape.runelite;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.MouseEvent;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.client.input.MouseAdapter;

/**
 * Routes canvas clicks to the pack ceremony while it is on screen, translating
 * stretched-mode screen coordinates back into canvas space for hit-testing.
 */
@Singleton
public final class PackRevealInputListener extends MouseAdapter
{
    private final Client client;
    private final PackRevealOverlay overlay;

    @Inject
    public PackRevealInputListener(Client client, PackRevealOverlay overlay)
    {
        this.client = client;
        this.overlay = overlay;
    }

    @Override
    public MouseEvent mousePressed(MouseEvent event)
    {
        if (overlay.isActive() && overlay.handleClick(canvasPoint(event)))
        {
            event.consume();
        }
        return event;
    }

    @Override
    public MouseEvent mouseClicked(MouseEvent event)
    {
        if (overlay.isActive())
        {
            event.consume();
        }
        return event;
    }

    @Override
    public MouseEvent mouseReleased(MouseEvent event)
    {
        if (overlay.isActive())
        {
            event.consume();
        }
        return event;
    }

    private Point canvasPoint(MouseEvent event)
    {
        Point point = event.getPoint();
        if (!client.isStretchedEnabled())
        {
            return point;
        }
        Dimension stretched = client.getStretchedDimensions();
        Dimension real = client.getRealDimensions();
        if (stretched.width <= 0 || stretched.height <= 0)
        {
            return point;
        }
        return new Point(
            (int) (point.x * real.getWidth() / stretched.width),
            (int) (point.y * real.getHeight() / stretched.height));
    }
}
