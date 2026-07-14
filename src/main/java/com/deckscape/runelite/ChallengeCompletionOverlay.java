package com.deckscape.runelite;

import com.deckscape.runelite.ui.DeckscapeImages;
import com.deckscape.runelite.ui.DeckscapePalette;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;

/** A high-priority milestone ceremony following the tracker → server → notifier pipeline. */
@Singleton
public final class ChallengeCompletionOverlay extends Overlay
{
    private static final long SWEEP = 480L;
    private static final long OPEN = 620L;
    private static final long HOLD = 3400L;
    private static final long OUT = 700L;
    private static final long TOTAL = SWEEP + OPEN + HOLD + OUT;
    private final Client client;
    private final DeckscapeConfig config;
    private long startedAt = -1L;
    private String title = "";
    private String reward = "";

    @Inject
    public ChallengeCompletionOverlay(Client client, DeckscapeConfig config)
    {
        this.client = client;
        this.config = config;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
        setPriority(OverlayPriority.HIGH);
    }

    public synchronized void showCompletion(String title, String reward)
    {
        this.title = title;
        this.reward = reward;
        this.startedAt = System.currentTimeMillis();
    }

    @Override
    public synchronized Dimension render(Graphics2D graphics)
    {
        if (startedAt < 0L || !config.completionPopup()) return null;
        long elapsed = System.currentTimeMillis() - startedAt;
        if (elapsed >= TOTAL) { startedAt = -1L; return null; }
        float sweep = ease(Math.min(1f, elapsed / (float) SWEEP));
        float open = elapsed <= SWEEP ? 0f : ease(Math.min(1f, (elapsed - SWEEP) / (float) OPEN));
        float alpha = elapsed > TOTAL - OUT ? 1f - ease((elapsed - (TOTAL - OUT)) / (float) OUT) : 1f;
        int canvasWidth = client.getCanvasWidth();
        int centerX = canvasWidth / 2;
        int centerY = Math.max(70, client.getCanvasHeight() / 6);
        int width = Math.min(320, canvasWidth - 30);
        int fullHeight = 104;
        int height = Math.round(fullHeight * open);

        Graphics2D g = (Graphics2D) graphics.create();
        try
        {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setComposite(AlphaComposite.SrcOver.derive(alpha));
            int lineWidth = Math.round((canvasWidth - 36) * sweep);
            g.setPaint(new java.awt.GradientPaint(centerX - lineWidth / 2f, 0, new Color(115, 69, 13, 0), centerX, 0, DeckscapePalette.GOLD, true));
            g.fillRect(centerX - lineWidth / 2, centerY - 2, lineWidth, 4);
            if (height < 6) return null;

            int x = centerX - width / 2;
            int y = centerY;
            BufferedImage panel = DeckscapeImages.load("/background.png");
            if (panel != null)
            {
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                g.drawImage(panel, x, y, width, height, null);
            }
            else
            {
                g.setColor(new Color(8, 7, 5, 238));
                g.fillRoundRect(x, y, width, height, 10, 10);
            }
            if (open < .72f) return null;

            float textAlpha = Math.min(1f, (open - .72f) / .28f);
            g.setComposite(AlphaComposite.SrcOver.derive(alpha * textAlpha));
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            BufferedImage art = DeckscapeImages.load("/com/deckscape/runelite/cards/fire_strike.png");
            int artSize = 62;
            if (art != null)
            {
                g.setColor(new Color(255, 202, 72, 90));
                g.fillRoundRect(x + 14, y + (fullHeight - artSize) / 2, artSize, artSize, 8, 8);
                g.drawImage(art, x + 17, y + (fullHeight - artSize) / 2 + 3, artSize - 6, artSize - 6, null);
            }
            int textX = x + 14 + artSize + 12;
            g.setColor(new Color(184, 151, 73));
            g.setFont(new Font("SansSerif", Font.BOLD, 9));
            g.drawString("OSRS CHALLENGE COMPLETE", textX, y + 26);
            g.setColor(DeckscapePalette.GOLD);
            g.setFont(new Font("Serif", Font.BOLD, 16));
            g.drawString(title, textX, y + 47);
            g.setColor(DeckscapePalette.PARCHMENT);
            g.setFont(new Font("SansSerif", Font.PLAIN, 10));
            g.drawString(reward, textX, y + 66);
            g.setColor(new Color(164, 145, 103));
            g.setFont(new Font("SansSerif", Font.BOLD, 9));
            g.drawString("Saved to your Deckscape account", textX, y + 86);
        }
        finally { g.dispose(); }
        return null;
    }

    private static float ease(float value)
    {
        float t = Math.max(0f, Math.min(1f, value));
        return t < .5f ? 4f * t * t * t : 1f - (float) Math.pow(-2f * t + 2f, 3) / 2f;
    }
}
