package com.deckscape.runelite.ui;

import com.deckscape.runelite.model.DeckscapeCard;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.function.Consumer;
import javax.swing.Timer;

public final class RevealCardTile extends CardTile
{
    private final Consumer<Integer> soundPlayer;
    private boolean faceUp;
    private boolean arrived;
    private double flip;

    public RevealCardTile(DeckscapeCard card, Consumer<Integer> soundPlayer)
    {
        super(card, -1);
        this.soundPlayer = soundPlayer;
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addMouseListener(new java.awt.event.MouseAdapter()
        {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent event) { reveal(); }
        });
    }

    public void setArrived(boolean arrived)
    {
        this.arrived = arrived;
        repaint();
    }

    public void reveal()
    {
        if (!arrived || faceUp || flip > 0) return;
        
        // Play card flip sound (OSRS click sound)
        if (soundPlayer != null)
        {
            soundPlayer.accept(2266);
        }

        Timer timer = new Timer(16, null);
        timer.addActionListener(event ->
        {
            flip = Math.min(1, flip + .075);
            if (flip >= .5 && !faceUp)
            {
                faceUp = true;
                // Play rare item fanfare if it's EPIC or LEGENDARY
                if (soundPlayer != null)
                {
                    if (getCard().getRarity() == DeckscapeCard.Rarity.LEGENDARY || getCard().getRarity() == DeckscapeCard.Rarity.EPIC)
                    {
                        soundPlayer.accept(1401); // level up fanfare
                    }
                    else
                    {
                        soundPlayer.accept(227); // subtle magic hit sound
                    }
                }
            }
            repaint();
            if (flip >= 1) timer.stop();
        });
        timer.start();
    }

    @Override
    protected void paintComponent(Graphics graphics)
    {
        if (!arrived) return;
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        double scaleX = flip == 0 ? 1 : Math.abs(1 - flip * 2);
        g.translate(getWidth() / 2.0, 0);
        g.scale(Math.max(.02, scaleX), 1);
        g.translate(-getWidth() / 2.0, 0);
        if (faceUp) paintCard(g, getWidth(), getHeight()); else paintBack(g);
        g.dispose();
    }

    private void paintBack(Graphics2D g)
    {
        // The card's own regional back, exactly like the website's pack cinematic.
        CardPainter.paintBack(g, 0, 0, getWidth(), getHeight(), getCard().getFaction());
    }
}
