package com.deckscape.runelite;

import com.deckscape.runelite.ui.DeckscapePalette;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;

/** Chunkblazer-style completion pipeline, rendered with Deckscape's brass/parchment visual language. */
@Singleton
public final class ChallengeCompletionOverlay extends Overlay
{
    private static final long INTRO = 700;
    private static final long EXPAND = 700;
    private static final long HOLD = 3500;
    private static final long OUTRO = 900;
    private static final long TOTAL = INTRO + EXPAND + HOLD + OUTRO;

    private final Client client;
    private final DeckscapeConfig config;
    private long startTime = -1;
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

    public void showCompletion(String title, String reward)
    {
        this.title = title;
        this.reward = reward;
        this.startTime = System.currentTimeMillis();
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (startTime < 0 || !config.completionPopup()) return null;
        long elapsed = System.currentTimeMillis() - startTime;
        if (elapsed >= TOTAL)
        {
            startTime = -1;
            return null;
        }
        float visibility = elapsed < INTRO ? ease(elapsed / (float) INTRO)
            : elapsed > TOTAL - OUTRO ? 1f - ease((elapsed - (TOTAL - OUTRO)) / (float) OUTRO) : 1f;
        float expansion = elapsed < INTRO ? 0f : ease(Math.min(1f, (elapsed - INTRO) / (float) EXPAND));
        int canvasWidth = client.getCanvasWidth();
        int canvasHeight = client.getCanvasHeight();
        int centerX = canvasWidth / 2;
        int centerY = canvasHeight / 7;
        int barWidth = (int) (320 * visibility);
        int panelHeight = (int) (92 * expansion);

        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setColor(new Color(233, 191, 66, Math.round(230 * visibility)));
        graphics.fillRect(centerX - barWidth / 2, centerY - 2, barWidth, 4);
        if (panelHeight > 4)
        {
            int x = centerX - 155;
            int y = centerY;
            graphics.setColor(new Color(22, 19, 14, Math.round(235 * visibility)));
            graphics.fillRoundRect(x, y, 310, panelHeight, 5, 5);
            graphics.setColor(new Color(233, 191, 66, Math.round(240 * visibility)));
            graphics.setStroke(new BasicStroke(2f));
            graphics.drawRoundRect(x, y, 310, panelHeight, 5, 5);
            if (expansion > .72f)
            {
                float textAlpha = Math.min(1f, (expansion - .72f) / .28f) * visibility;
                drawCentered(graphics, "CHALLENGE COMPLETE", centerX, y + 25, new Font("Serif", Font.BOLD, 14), new Color(255, 207, 78, Math.round(255 * textAlpha)));
                drawCentered(graphics, title, centerX, y + 49, new Font("SansSerif", Font.BOLD, 13), new Color(239, 226, 193, Math.round(255 * textAlpha)));
                drawCentered(graphics, reward, centerX, y + 70, new Font("SansSerif", Font.PLAIN, 11), new Color(196, 181, 142, Math.round(255 * textAlpha)));
            }
        }
        return new Dimension(canvasWidth, canvasHeight);
    }

    private static void drawCentered(Graphics2D graphics, String text, int centerX, int baseline, Font font, Color color)
    {
        graphics.setFont(font);
        graphics.setColor(color);
        FontMetrics metrics = graphics.getFontMetrics();
        graphics.drawString(text, centerX - metrics.stringWidth(text) / 2, baseline);
    }

    private static float ease(float value)
    {
        float t = Math.max(0f, Math.min(1f, value));
        return t < .5f ? 4f * t * t * t : 1f - (float) Math.pow(-2f * t + 2f, 3) / 2f;
    }
}
