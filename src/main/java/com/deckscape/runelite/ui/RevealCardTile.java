package com.deckscape.runelite.ui;

import com.deckscape.runelite.model.DeckscapeCard;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
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
        int w = getWidth();
        int h = getHeight();
        
        // Draw card base border container
        g.setColor(DeckscapePalette.INK);
        g.fillRoundRect(0, 0, w, h, 6, 6);

        // Clip grid inside rounded borders
        java.awt.Shape oldClip = g.getClip();
        g.clip(new RoundRectangle2D.Float(3, 3, w - 6, h - 6, 4, 4));

        double cellW = (w - 6) / 32.0;
        double cellH = (h - 6) / 48.0;

        // Concentric pixel colors in OSRS brass theme
        Color[] bandColors = {
            new Color(255, 215, 0),     // Center: Gold
            new Color(218, 165, 32),    // Goldenrod
            new Color(171, 125, 17),    // Dark Gold
            new Color(111, 75, 17),     // Shadow
            new Color(36, 26, 10)       // Deep Shadow
        };

        for (int row = 0; row < 48; row++)
        {
            for (int col = 0; col < 32; col++)
            {
                int band = bandAt(col, row);
                g.setColor(bandColors[band]);
                int x = 3 + (int) Math.round(col * cellW);
                int y = 3 + (int) Math.round(row * cellH);
                int cw = (int) Math.round((col + 1) * cellW) - (int) Math.round(col * cellW);
                int ch = (int) Math.round((row + 1) * cellH) - (int) Math.round(row * cellH);
                g.fillRect(x, y, cw, ch);
            }
        }

        g.setClip(oldClip);

        // Overlay inner border
        g.setColor(DeckscapePalette.GOLD);
        g.setStroke(new java.awt.BasicStroke(1.5f));
        g.drawRoundRect(3, 3, w - 7, h - 7, 4, 4);
    }

    private static int bandAt(int col, int row)
    {
        double dx = (col + 0.5 - 16.0) / 18.24;
        double dy = (row + 0.5 - 7.2) / 17.76;
        double distance = Math.sqrt(dx * dx + dy * dy);
        if (distance < 1.0) return 0;
        if (distance < 1.28) return 1;
        if (distance < 1.56) return 2;
        if (distance < 1.84) return 3;
        return 4;
    }
}
