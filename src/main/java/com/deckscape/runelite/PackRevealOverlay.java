package com.deckscape.runelite;

import com.deckscape.runelite.model.DeckscapeCard;
import com.deckscape.runelite.model.PackType;
import com.deckscape.runelite.ui.CardPainter;
import com.deckscape.runelite.ui.DeckscapePalette;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;

/**
 * Canvas pack ceremony matching the website's pack cinematic: the sealed pack (web pack
 * visual), a staggered face-down deal showing each card's regional back, player-driven
 * flips, and a Claim button — nothing disappears until the player claims.
 */
@Singleton
public final class PackRevealOverlay extends Overlay
{
    private static final long PACK_HOLD_END = 1050L;
    private static final long DEAL_START = 850L;
    private static final long DEAL_STAGGER = 145L;
    private static final long DEAL_FLIGHT = 430L;
    private static final long FLIP_TIME = 360L;
    private static final long OUTRO = 520L;

    private final Client client;
    private final ClientThread clientThread;
    private List<DeckscapeCard> cards = Collections.emptyList();
    private PackType packType = PackType.GENERAL;
    private int stardustAward;
    private long startedAt = -1L;
    private long claimedAt = -1L;
    private long[] flipStartedAt = new long[0];

    // Hit targets from the last rendered frame, read by the mouse listener.
    private Rectangle[] cardBounds = new Rectangle[0];
    private Rectangle claimBounds;
    private Rectangle closeBounds;

    @Inject
    public PackRevealOverlay(Client client, ClientThread clientThread)
    {
        this.client = client;
        this.clientThread = clientThread;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
        setPriority(OverlayPriority.HIGH);
    }

    public synchronized void showPack(PackType type, List<DeckscapeCard> revealed)
    {
        showPack(type, revealed, 0);
    }

    public synchronized void showPack(PackType type, List<DeckscapeCard> revealed, int stardustAward)
    {
        this.packType = type == null ? PackType.GENERAL : type;
        this.cards = revealed == null ? Collections.emptyList() : new ArrayList<>(revealed);
        this.stardustAward = Math.max(0, stardustAward);
        this.startedAt = System.currentTimeMillis();
        this.claimedAt = -1L;
        this.flipStartedAt = new long[cards.size()];
        java.util.Arrays.fill(flipStartedAt, -1L);
        this.cardBounds = new Rectangle[cards.size()];
        this.claimBounds = null;
        this.closeBounds = null;
    }

    public synchronized boolean isActive()
    {
        return startedAt >= 0L && !cards.isEmpty();
    }

    /** Handles a canvas click; returns true when the ceremony consumed it. */
    public synchronized boolean handleClick(Point point)
    {
        if (!isActive()) return false;
        long now = System.currentTimeMillis();
        if (claimedAt > 0L) return true;
        if (closeBounds != null && closeBounds.contains(point) || claimBounds != null && claimBounds.contains(point))
        {
            claimedAt = now;
            playSound(2266);
            return true;
        }
        long elapsed = now - startedAt;
        for (int i = 0; i < cardBounds.length; i++)
        {
            Rectangle bounds = cardBounds[i];
            boolean dealt = elapsed >= DEAL_START + i * DEAL_STAGGER + DEAL_FLIGHT;
            if (bounds != null && dealt && flipStartedAt[i] < 0L && bounds.contains(point))
            {
                flipStartedAt[i] = now;
                DeckscapeCard.Rarity rarity = cards.get(i).getRarity();
                playSound(rarity == DeckscapeCard.Rarity.LEGENDARY || rarity == DeckscapeCard.Rarity.EPIC ? 1401 : 2266);
                return true;
            }
        }
        // The ceremony is modal: swallow stray clicks so they never reach the game.
        return true;
    }

    private void playSound(int id)
    {
        clientThread.invoke(() -> client.playSoundEffect(id));
    }

    @Override
    public synchronized Dimension render(Graphics2D graphics)
    {
        if (!isActive()) return null;
        long now = System.currentTimeMillis();
        long elapsed = now - startedAt;
        if (claimedAt > 0L && now - claimedAt >= OUTRO)
        {
            startedAt = -1L;
            cards = Collections.emptyList();
            return null;
        }

        int canvasWidth = client.getCanvasWidth();
        int canvasHeight = client.getCanvasHeight();
        float outro = claimedAt > 0L ? 1f - ease((now - claimedAt) / (float) OUTRO) : 1f;
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
                drawCards(g, canvasWidth, canvasHeight, elapsed, now);
            }
            drawFooter(g, canvasWidth, canvasHeight, elapsed, now);
            drawClose(g, canvasWidth);
        }
        finally
        {
            g.dispose();
        }
        return null;
    }

    /** The sealed pack, drawn with the website's crimped pack visual. */
    private void drawPack(Graphics2D g, int canvasWidth, int canvasHeight, long elapsed)
    {
        float intro = ease(Math.min(1f, elapsed / 440f));
        float fade = elapsed <= PACK_HOLD_END ? 1f : 1f - ease((elapsed - PACK_HOLD_END) / 420f);
        double pulse = 1d + Math.sin(elapsed / 150d) * .014d;
        int baseHeight = Math.min(300, Math.max(180, canvasHeight * 46 / 100));
        int baseWidth = baseHeight * 132 / 190;
        int height = (int) (baseHeight * (.82d + intro * .18d) * pulse);
        int width = (int) (baseWidth * (.82d + intro * .18d) * pulse);
        int x = (canvasWidth - width) / 2;
        int y = (canvasHeight - height) / 2 + 16;
        java.awt.Composite old = g.getComposite();
        g.setComposite(AlphaComposite.SrcOver.derive(Math.max(0f, fade)));
        for (int ring = 4; ring >= 1; ring--)
        {
            g.setColor(new Color(237, 187, 60, 10 + ring * 5));
            g.fillRoundRect(x - ring * 8, y - ring * 8, width + ring * 16, height + ring * 16, 22, 22);
        }
        CardPainter.paintPack(g, x, y, width, height, packType);
        g.setComposite(old);
    }

    private void drawCards(Graphics2D g, int canvasWidth, int canvasHeight, long elapsed, long now)
    {
        int count = cards.size();
        int gap = Math.max(7, Math.min(18, canvasWidth / 70));
        int availableWidth = canvasWidth - 36 - gap * Math.max(0, count - 1);
        int cardWidth = Math.max(74, Math.min(148, availableWidth / Math.max(1, count)));
        int cardHeight = Math.min((int) (cardWidth * 1.5), canvasHeight - 170);
        cardWidth = Math.max(60, (int) (cardHeight / 1.5));
        int totalWidth = count * cardWidth + (count - 1) * gap;
        int startX = (canvasWidth - totalWidth) / 2;
        int destinationY = Math.max(74, (canvasHeight - cardHeight) / 2 + 10);
        Rectangle pile = new Rectangle(canvasWidth / 2 - cardWidth / 2, canvasHeight / 2 - cardHeight / 2 + 25, cardWidth, cardHeight);

        for (int i = 0; i < count; i++)
        {
            float arrival = clamp((elapsed - DEAL_START - i * DEAL_STAGGER) / (float) DEAL_FLIGHT);
            if (arrival <= 0f)
            {
                cardBounds[i] = null;
                continue;
            }
            Rectangle destination = new Rectangle(startX + i * (cardWidth + gap), destinationY, cardWidth, cardHeight);
            Rectangle bounds = interpolate(pile, destination, ease(arrival));
            cardBounds[i] = destination;
            float flip = flipStartedAt[i] < 0L ? 0f : clamp((now - flipStartedAt[i]) / (float) FLIP_TIME);
            boolean faceUp = flip >= .5f;
            float scaleX = flip <= 0f ? 1f : Math.max(.025f, Math.abs(1f - 2f * flip));

            Graphics2D cardGraphics = (Graphics2D) g.create();
            cardGraphics.translate(bounds.getCenterX(), bounds.getCenterY());
            cardGraphics.scale(scaleX, 1d);
            cardGraphics.translate(-bounds.width / 2d, -bounds.height / 2d);
            DeckscapeCard card = cards.get(i);
            if (faceUp)
            {
                // Rarity glow behind the revealed card, like the web's reveal slots.
                Color rarity = CardPainter.rarityColor(card.getRarity());
                float pulse = .18f + .10f * (float) ((Math.sin(now / 180d + i) + 1d) / 2d);
                cardGraphics.setColor(new Color(rarity.getRed(), rarity.getGreen(), rarity.getBlue(), Math.round(255 * pulse)));
                cardGraphics.fillRoundRect(-7, -7, bounds.width + 14, bounds.height + 14, 18, 18);
                CardPainter.paintFace(cardGraphics, 0, 0, bounds.width, bounds.height, card, -1);
            }
            else
            {
                boolean clickable = arrival >= 1f;
                if (clickable)
                {
                    float shimmer = .10f + .07f * (float) ((Math.sin(now / 260d + i * 1.4d) + 1d) / 2d);
                    cardGraphics.setColor(new Color(255, 226, 143, Math.round(255 * shimmer)));
                    cardGraphics.fillRoundRect(-5, -5, bounds.width + 10, bounds.height + 10, 14, 14);
                }
                CardPainter.paintBack(cardGraphics, 0, 0, bounds.width, bounds.height, card.getFaction());
            }
            cardGraphics.dispose();
        }
    }

    private void drawFooter(Graphics2D g, int canvasWidth, int canvasHeight, long elapsed, long now)
    {
        if (elapsed < DEAL_START + DEAL_FLIGHT) return;
        int revealed = 0;
        for (long at : flipStartedAt) if (at >= 0L && now - at >= FLIP_TIME / 2) revealed++;
        boolean all = revealed >= cards.size();
        String message = all
            ? "Added to your shared collection" + (stardustAward > 0 ? " + " + stardustAward + " Stardust" : "")
            : "Click each card to reveal it - " + revealed + "/" + cards.size();
        drawCentered(g, message, canvasWidth / 2, canvasHeight - (all ? 74 : 32),
            new Font("SansSerif", Font.BOLD, 12), DeckscapePalette.PARCHMENT);

        if (all)
        {
            int buttonWidth = 150;
            int buttonHeight = 36;
            Rectangle button = new Rectangle((canvasWidth - buttonWidth) / 2, canvasHeight - 58, buttonWidth, buttonHeight);
            claimBounds = button;
            g.setColor(new Color(0x11100d));
            g.fillRect(button.x - 3, button.y - 3, button.width + 6, button.height + 6);
            g.setColor(new Color(0xd8b64d));
            g.fillRect(button.x, button.y, button.width, button.height);
            g.setColor(new Color(255, 240, 155, 200));
            g.drawLine(button.x + 1, button.y + 1, button.x + button.width - 2, button.y + 1);
            g.drawLine(button.x + 1, button.y + 1, button.x + 1, button.y + button.height - 2);
            drawCentered(g, "CLAIM", button.x + button.width / 2, button.y + button.height / 2 + 5,
                new Font("SansSerif", Font.BOLD, 13), new Color(0x24190b));
        }
        else
        {
            claimBounds = null;
        }
    }

    private void drawClose(Graphics2D g, int canvasWidth)
    {
        Rectangle button = new Rectangle(canvasWidth - 42, 18, 26, 26);
        closeBounds = button;
        g.setColor(new Color(24, 20, 14, 220));
        g.fillRoundRect(button.x, button.y, button.width, button.height, 6, 6);
        g.setColor(DeckscapePalette.BRASS);
        g.setStroke(new BasicStroke(1.5f));
        g.drawRoundRect(button.x, button.y, button.width, button.height, 6, 6);
        drawCentered(g, "×", button.x + button.width / 2, button.y + button.height / 2 + 6,
            new Font("SansSerif", Font.BOLD, 17), DeckscapePalette.GOLD);
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

    private static void drawCentered(Graphics2D g, String text, int centerX, int baseline, Font font, Color color)
    {
        g.setFont(font); g.setColor(color);
        FontMetrics metrics = g.getFontMetrics();
        g.drawString(text, centerX - metrics.stringWidth(text) / 2, baseline);
    }

    private static float clamp(float value) { return Math.max(0f, Math.min(1f, value)); }
    private static float ease(float value)
    {
        float t = clamp(value);
        return t < .5f ? 4f * t * t * t : 1f - (float) Math.pow(-2f * t + 2f, 3) / 2f;
    }
}
