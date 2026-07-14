package com.deckscape.runelite;

import java.awt.Point;
import java.awt.event.MouseEvent;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.input.MouseAdapter;

/**
 * Routes canvas clicks to the pack ceremony while it is on screen.
 *
 * <p>MouseManager listeners receive the event returned by the listeners ahead
 * of them. Stretched Mode registers its coordinate translator at position zero,
 * so events reaching this listener are already in canvas space.</p>
 */
@Singleton
public final class PackRevealInputListener extends MouseAdapter
{
    private final PackRevealOverlay overlay;

    @Inject
    public PackRevealInputListener(PackRevealOverlay overlay)
    {
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

    static Point canvasPoint(MouseEvent event)
    {
        return event.getPoint();
    }
}
