package com.deckscape.runelite;

import com.deckscape.runelite.model.DeckscapeCard;
import com.deckscape.runelite.model.PackType;
import com.deckscape.runelite.ui.DeckscapeImages;
import com.deckscape.runelite.ui.DeckscapePalette;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;

/**
 * Canvas pack ceremony inspired by physical booster openings: sealed pack, staggered deal, then
 * individual flips. Rendering is original and uses Deckscape's own card and pack assets.
 */
@Singleton
public final class PackRevealOverlay extends Overlay
{
    private static final long PACK_HOLD_END = 1050L;
    private static final long DEAL_START = 850L;
    private static final long DEAL_STAGGER = 145L;
    private static final long DEAL_FLIGHT = 430L;
    private static final long FLIP_START = 2100L;
    private static final long FLIP_STAGGER = 390L;
    private static final long FLIP_TIME = 360L;
    private static final long HOLD_END = 6100L;
    private static final long TOTAL = 6850L;

    private final Client client;
    private List<DeckscapeCard> cards = Collections.emptyList();
    private PackType packType = PackType.GENERAL;
    private long startedAt = -1L;

    @Inject
    public PackRevealOverlay(Client client)
    {
        this.client = client;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
        setPriority(OverlayPriority.HIGH);
    }

    public synchronized void showPack(PackType type, List<DeckscapeCard> revealed)
    {
        this.packType = type == null ? PackType.GENERAL : type;
        this.cards = revealed == null ? Collections.emptyList() : new ArrayList<>(revealed);
        this.startedAt = System.currentTimeMillis();
    }

    @Override
    public synchronized Dimension render(Graphics2D graphics)
    {
        if (startedAt < 0L || cards.isEmpty()) return null;
        long elapsed = System.currentTimeMillis() - startedAt;
        if (elapsed >= TOTAL)
        {
            startedAt = -1L;
            cards = Collections.emptyList();
            return null;
        }

        int canvasWidth = client.getCanvasWidth();
        int canvasHeight = client.getCanvasHeight();
        float outro = elapsed > HOLD_END ? 1f - ease((elapsed - HOLD_END) / (float) (TOTAL - HOLD_END)) : 1f;
        Graphics2D g = (Graphics2D) graphics.create();
        try
        {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setComposite(AlphaComposite.SrcOver.derive(outro));
            g.setColor(new Color(5, 5, 4, 225));
            g.fillRect(0, 0, canvasWidth, canvasHeight);
            paintVignette(g, canvasWidth, canvasHeight, elapsed);

            drawCentered(g, "DECKSCAPE", canvasWidth / 2, Math.max(30, canvasHeight / 11),
                new Font("Serif", Font.BOLD, 20), new Color(255, 218, 111));
            drawCentered(g, packType.getDisplayName().toUpperCase(), canvasWidth / 2, Math.max(50, canvasHeight / 11 + 22),
                new Font("SansSerif", Font.BOLD, 11), new Color(185, 166, 123));

            if (elapsed < PACK_HOLD_END + 420L)
            {
                drawPack(g, canvasWidth, canvasHeight, elapsed);
            }
            if (elapsed >= DEAL_START)
            {
                drawCards(g, canvasWidth, canvasHeight, elapsed);
            }

            if (elapsed >= FLIP_START)
            {
                drawCentered(g, elapsed < FLIP_START + cards.size() * FLIP_STAGGER + FLIP_TIME
                        ? "Revealing your rewards…" : "Added to your shared collection",
                    canvasWidth / 2, canvasHeight - 28, new Font("SansSerif", Font.BOLD, 12), DeckscapePalette.PARCHMENT);
            }
        }
        finally
        {
            g.dispose();
        }
        return null;
    }

    private void drawPack(Graphics2D g, int canvasWidth, int canvasHeight, long elapsed)
    {
        float intro = ease(Math.min(1f, elapsed / 440f));
        float fade = elapsed <= PACK_HOLD_END ? 1f : 1f - ease((elapsed - PACK_HOLD_END) / 420f);
        double pulse = 1d + Math.sin(elapsed / 150d) * .014d;
        int baseHeight = Math.min(310, Math.max(190, canvasHeight * 56 / 100));
        int baseWidth = baseHeight * 4 / 5;
        int height = (int) (baseHeight * (.82d + intro * .18d) * pulse);
        int width = (int) (baseWidth * (.82d + intro * .18d) * pulse);
        int x = (canvasWidth - width) / 2;
        int y = (canvasHeight - height) / 2 + 16;
        Composite old = g.getComposite();
        g.setComposite(AlphaComposite.SrcOver.derive(Math.max(0f, fade)));
        for (int ring = 4; ring >= 1; ring--)
        {
            g.setColor(new Color(237, 187, 60, 10 + ring * 5));
            g.fillRoundRect(x - ring * 8, y - ring * 8, width + ring * 16, height + ring * 16, 22, 22);
        }
        Color theme = packColor(packType);
        g.setPaint(new java.awt.GradientPaint(x, y, theme.brighter(), x + width, y + height, new Color(24, 18, 11)));
        g.fillRoundRect(x, y, width, height, 16, 16);
        g.setColor(new Color(244, 207, 111, 210));
        g.setStroke(new BasicStroke(3f));
        g.drawRoundRect(x + 2, y + 2, width - 5, height - 5, 16, 16);
        g.setColor(new Color(15, 12, 8, 155));
        g.fillRect(x + 9, y + 16, width - 18, 18);
        g.fillRect(x + 9, y + height - 34, width - 18, 18);
        g.setColor(new Color(255, 226, 143, 145));
        for (int crimp = 14; crimp < width - 14; crimp += 9)
        {
            g.drawLine(x + crimp, y + 18, x + crimp + 4, y + 30);
            g.drawLine(x + crimp, y + height - 31, x + crimp + 4, y + height - 19);
        }
        BufferedImage pack = DeckscapeImages.load(DeckscapeImages.packResource(packType));
        if (pack != null) drawContain(g, pack, new Rectangle(x + width / 8, y + height / 5, width * 3 / 4, height * 3 / 5));
        drawCentered(g, "DECKSCAPE", x + width / 2, y + height * 3 / 4,
            new Font("Serif", Font.BOLD, Math.max(14, width / 10)), new Color(255, 232, 168));
        drawCentered(g, packType.getDisplayName().toUpperCase(), x + width / 2, y + height * 3 / 4 + 21,
            new Font("SansSerif", Font.BOLD, Math.max(8, width / 22)), new Color(226, 203, 147));
        g.setComposite(old);
    }

    private void drawCards(Graphics2D g, int canvasWidth, int canvasHeight, long elapsed)
    {
        int count = cards.size();
        int gap = Math.max(7, Math.min(18, canvasWidth / 70));
        int availableWidth = canvasWidth - 36 - gap * Math.max(0, count - 1);
        int cardWidth = Math.max(74, Math.min(142, availableWidth / Math.max(1, count)));
        int cardHeight = Math.min((int) (cardWidth * 1.52), canvasHeight - 150);
        cardWidth = Math.max(60, (int) (cardHeight / 1.52));
        int totalWidth = count * cardWidth + (count - 1) * gap;
        int startX = (canvasWidth - totalWidth) / 2;
        int destinationY = Math.max(74, (canvasHeight - cardHeight) / 2 + 25);
        Rectangle pile = new Rectangle(canvasWidth / 2 - cardWidth / 2, canvasHeight / 2 - cardHeight / 2 + 25, cardWidth, cardHeight);

        for (int i = 0; i < count; i++)
        {
            float arrival = clamp((elapsed - DEAL_START - i * DEAL_STAGGER) / (float) DEAL_FLIGHT);
            if (arrival <= 0f) continue;
            Rectangle destination = new Rectangle(startX + i * (cardWidth + gap), destinationY, cardWidth, cardHeight);
            Rectangle bounds = interpolate(pile, destination, ease(arrival));
            float flip = clamp((elapsed - FLIP_START - i * FLIP_STAGGER) / (float) FLIP_TIME);
            boolean faceUp = flip >= .5f;
            float scaleX = flip <= 0f ? 1f : Math.max(.025f, Math.abs(1f - 2f * flip));

            Graphics2D cardGraphics = (Graphics2D) g.create();
            cardGraphics.translate(bounds.getCenterX(), bounds.getCenterY());
            cardGraphics.scale(scaleX, 1d);
            cardGraphics.translate(-bounds.width / 2d, -bounds.height / 2d);
            Rectangle local = new Rectangle(0, 0, bounds.width, bounds.height);
            if (faceUp) drawCardFace(cardGraphics, local, cards.get(i), elapsed, i);
            else drawCardBack(cardGraphics, local, i);
            cardGraphics.dispose();
        }
    }

    private static void drawCardFace(Graphics2D g, Rectangle r, DeckscapeCard card, long elapsed, int index)
    {
        Color rarity = rarityColor(card.getRarity());
        float pulse = .18f + .10f * (float) ((Math.sin(elapsed / 180d + index) + 1d) / 2d);
        g.setColor(new Color(rarity.getRed(), rarity.getGreen(), rarity.getBlue(), Math.round(255 * pulse)));
        g.fillRoundRect(-7, -7, r.width + 14, r.height + 14, 18, 18);
        g.setColor(new Color(18, 14, 9));
        g.fillRoundRect(0, 0, r.width, r.height, 9, 9);
        BufferedImage art = DeckscapeImages.load(card.getArtResource());
        Rectangle artBox = new Rectangle(6, 6, r.width - 12, r.height - 12);
        if (art != null) drawCover(g, art, artBox);
        else
        {
            g.setPaint(new java.awt.GradientPaint(0, 0, new Color(94, 74, 43), r.width, r.height, new Color(25, 22, 18)));
            g.fillRoundRect(6, 6, r.width - 12, r.height - 12, 6, 6);
        }
        int footer = Math.max(31, r.height / 5);
        g.setColor(new Color(12, 10, 7, 225));
        g.fillRect(6, r.height - footer - 6, r.width - 12, footer);
        g.setColor(rarity);
        g.setStroke(new BasicStroke(3f));
        g.drawRoundRect(2, 2, r.width - 5, r.height - 5, 9, 9);
        g.setColor(DeckscapePalette.PARCHMENT);
        g.setFont(new Font("Serif", Font.BOLD, Math.max(10, r.width / 11)));
        drawCenteredEllipsized(g, card.getName(), r.width / 2, r.height - footer / 2, r.width - 18);
        g.setFont(new Font("SansSerif", Font.BOLD, Math.max(8, r.width / 15)));
        g.setColor(rarity);
        drawCenteredEllipsized(g, card.getRarity().name(), r.width / 2, r.height - 10, r.width - 18);
    }

    private static void drawCardBack(Graphics2D g, Rectangle r, int index)
    {
        g.setColor(new Color(19, 15, 10));
        g.fillRoundRect(0, 0, r.width, r.height, 9, 9);
        g.setPaint(new java.awt.GradientPaint(4, 4, new Color(207, 158, 45), r.width, r.height, new Color(58, 35, 10)));
        g.fillRoundRect(5, 5, r.width - 10, r.height - 10, 7, 7);
        g.setColor(new Color(25, 20, 13));
        g.fillRoundRect(12, 12, r.width - 24, r.height - 24, 5, 5);
        g.setColor(new Color(238, 196, 77));
        g.setStroke(new BasicStroke(2f));
        int inset = Math.max(18, r.width / 5);
        g.drawRect(inset, inset, r.width - inset * 2, r.height - inset * 2);
        g.setFont(new Font("Serif", Font.BOLD, Math.max(18, r.width / 4)));
        drawCentered(g, "D", r.width / 2, r.height / 2 + g.getFontMetrics().getAscent() / 3, g.getFont(), DeckscapePalette.GOLD);
    }

    private static void paintVignette(Graphics2D g, int width, int height, long elapsed)
    {
        float glow = .12f + .04f * (float) Math.sin(elapsed / 260d);
        g.setPaint(new java.awt.RadialGradientPaint(width / 2f, height / 2f, Math.max(width, height) * .58f,
            new float[] {0f, .55f, 1f}, new Color[] {new Color(217, 157, 46, Math.round(255 * glow)), new Color(39, 28, 13, 55), new Color(0, 0, 0, 180)}));
        g.fillRect(0, 0, width, height);
    }

    private static Rectangle interpolate(Rectangle from, Rectangle to, float amount)
    {
        return new Rectangle(Math.round(from.x + (to.x - from.x) * amount), Math.round(from.y + (to.y - from.y) * amount),
            Math.round(from.width + (to.width - from.width) * amount), Math.round(from.height + (to.height - from.height) * amount));
    }

    private static void drawContain(Graphics2D g, BufferedImage image, Rectangle r)
    {
        double scale = Math.min(r.width / (double) image.getWidth(), r.height / (double) image.getHeight());
        int w = (int) Math.round(image.getWidth() * scale), h = (int) Math.round(image.getHeight() * scale);
        g.drawImage(image, r.x + (r.width - w) / 2, r.y + (r.height - h) / 2, w, h, null);
    }

    private static void drawCover(Graphics2D g, BufferedImage image, Rectangle r)
    {
        double scale = Math.max(r.width / (double) image.getWidth(), r.height / (double) image.getHeight());
        int w = (int) Math.ceil(image.getWidth() * scale), h = (int) Math.ceil(image.getHeight() * scale);
        Shape clip = g.getClip();
        g.clipRect(r.x, r.y, r.width, r.height);
        g.drawImage(image, r.x + (r.width - w) / 2, r.y + (r.height - h) / 2, w, h, null);
        g.setClip(clip);
    }

    private static void drawCentered(Graphics2D g, String text, int centerX, int baseline, Font font, Color color)
    {
        g.setFont(font); g.setColor(color);
        FontMetrics metrics = g.getFontMetrics();
        g.drawString(text, centerX - metrics.stringWidth(text) / 2, baseline);
    }

    private static void drawCenteredEllipsized(Graphics2D g, String text, int centerX, int baseline, int maxWidth)
    {
        FontMetrics metrics = g.getFontMetrics();
        String value = text;
        while (value.length() > 3 && metrics.stringWidth(value) > maxWidth) value = value.substring(0, value.length() - 2) + "…";
        g.drawString(value, centerX - metrics.stringWidth(value) / 2, baseline);
    }

    private static Color rarityColor(DeckscapeCard.Rarity rarity)
    {
        switch (rarity)
        {
            case UNCOMMON: return new Color(86, 148, 229);
            case RARE: return new Color(176, 99, 224);
            case EPIC: return new Color(232, 77, 174);
            case LEGENDARY: return new Color(246, 163, 49);
            default: return new Color(87, 174, 99);
        }
    }

    private static Color packColor(PackType type)
    {
        switch (type)
        {
            case COMBAT: return new Color(116, 39, 30);
            case SKILLING: return new Color(47, 102, 61);
            case FACTION_MISTHALIN: return new Color(43, 69, 126);
            default: return new Color(124, 91, 34);
        }
    }

    private static float clamp(float value) { return Math.max(0f, Math.min(1f, value)); }
    private static float ease(float value)
    {
        float t = clamp(value);
        return t < .5f ? 4f * t * t * t : 1f - (float) Math.pow(-2f * t + 2f, 3) / 2f;
    }
}
