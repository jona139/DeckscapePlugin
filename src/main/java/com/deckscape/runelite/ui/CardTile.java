package com.deckscape.runelite.ui;

import com.deckscape.runelite.model.DeckscapeCard;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.JComponent;

public class CardTile extends JComponent
{
    private final DeckscapeCard card;
    private final int owned;
    private BufferedImage art;

    public CardTile(DeckscapeCard card, int owned)
    {
        this.card = card;
        this.owned = owned;
        setPreferredSize(new Dimension(100, 154));
        setMinimumSize(new Dimension(76, 118));
        setToolTipText(card.getName() + " · " + card.getRarity());
        if (card.getArtResource() != null)
        {
            try
            {
                art = ImageIO.read(CardTile.class.getResource(card.getArtResource()));
            }
            catch (IOException | IllegalArgumentException ignored)
            {
                art = null;
            }
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
        int arc = 5;
        Color rarity = rarityColor(card.getRarity());
        g.setColor(DeckscapePalette.INK);
        g.fill(new RoundRectangle2D.Float(0, 0, width, height, arc, arc));
        g.setColor(rarity);
        g.setStroke(new BasicStroke(3f));
        g.draw(new RoundRectangle2D.Float(2, 2, width - 5, height - 5, arc, arc));
        int inset = 7;
        int nameHeight = Math.max(25, height / 7);
        if (art != null)
        {
            drawCover(g, art, inset, inset, width - inset * 2, height - inset * 2);
        }
        else
        {
            g.setPaint(new java.awt.GradientPaint(0, 0, new Color(75, 63, 46), width, height, new Color(30, 27, 23)));
            g.fillRect(inset, inset, width - inset * 2, height - inset * 2);
            String initials = initials(card.getName());
            g.setFont(new Font("Serif", Font.BOLD, Math.max(18, width / 4)));
            FontMetrics metrics = g.getFontMetrics();
            g.setColor(new Color(235, 218, 171, 170));
            g.drawString(initials, (width - metrics.stringWidth(initials)) / 2, height / 2);
        }
        g.setColor(new Color(22, 15, 10, 225));
        g.fillRect(inset, height - inset - nameHeight, width - inset * 2, nameHeight);
        g.setColor(DeckscapePalette.PARCHMENT);
        g.setFont(new Font("Serif", Font.BOLD, Math.max(10, width / 10)));
        drawCentered(g, card.getName(), inset + 3, height - inset - nameHeight, width - inset * 2 - 6, nameHeight);
        if (owned >= 0)
        {
            g.setColor(DeckscapePalette.GOLD);
            g.setFont(new Font("SansSerif", Font.BOLD, 10));
            g.drawString("×" + owned, width - 25, 15);
        }
    }

    public DeckscapeCard getCard() { return card; }

    private static void drawCover(Graphics2D g, BufferedImage image, int x, int y, int width, int height)
    {
        double scale = Math.max(width / (double) image.getWidth(), height / (double) image.getHeight());
        int drawW = (int) Math.ceil(image.getWidth() * scale);
        int drawH = (int) Math.ceil(image.getHeight() * scale);
        java.awt.Shape clip = g.getClip();
        g.clipRect(x, y, width, height);
        g.drawImage(image, x + (width - drawW) / 2, y + (height - drawH) / 2, drawW, drawH, null);
        g.setClip(clip);
    }

    private static void drawCentered(Graphics2D g, String value, int x, int y, int width, int height)
    {
        FontMetrics fm = g.getFontMetrics();
        String text = value;
        while (text.length() > 3 && fm.stringWidth(text) > width) text = text.substring(0, text.length() - 2) + "…";
        g.drawString(text, x + (width - fm.stringWidth(text)) / 2, y + (height + fm.getAscent() - fm.getDescent()) / 2);
    }

    private static String initials(String value)
    {
        StringBuilder out = new StringBuilder();
        for (String word : value.split(" ")) if (!word.isEmpty() && out.length() < 2) out.append(word.charAt(0));
        return out.toString();
    }

    private static Color rarityColor(DeckscapeCard.Rarity rarity)
    {
        switch (rarity)
        {
            case UNCOMMON: return new Color(78, 137, 216);
            case RARE: return new Color(163, 89, 214);
            case EPIC: return new Color(218, 78, 166);
            case LEGENDARY: return new Color(230, 147, 47);
            default: return new Color(79, 157, 89);
        }
    }
}
