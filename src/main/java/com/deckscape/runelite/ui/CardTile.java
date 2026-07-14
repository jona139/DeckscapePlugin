package com.deckscape.runelite.ui;

import com.deckscape.runelite.model.DeckscapeCard;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JComponent;

/** A collection tile rendered with the shared web-style card face. */
public class CardTile extends JComponent
{
    private final DeckscapeCard card;
    private final int owned;

    public CardTile(DeckscapeCard card, int owned)
    {
        this(card, owned, null);
    }

    public CardTile(DeckscapeCard card, int owned, Runnable onClick)
    {
        this.card = card;
        this.owned = owned;
        setPreferredSize(new Dimension(124, 186));
        setMinimumSize(new Dimension(84, 126));
        setToolTipText(card.getName() + " · " + card.getRarity());
        if (onClick != null)
        {
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter()
            {
                @Override
                public void mouseClicked(MouseEvent event) { onClick.run(); }
            });
        }
    }

    @Override
    protected void paintComponent(Graphics graphics)
    {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        paintCard(g, getWidth(), getHeight());
        g.dispose();
    }

    protected void paintCard(Graphics2D g, int width, int height)
    {
        CardPainter.paintFace(g, 0, 0, width, height, card, owned);
    }

    public DeckscapeCard getCard() { return card; }
}
