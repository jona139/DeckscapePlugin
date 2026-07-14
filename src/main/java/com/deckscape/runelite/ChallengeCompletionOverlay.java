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
    private static final long HOLD = RewardPopupLayout.linger(3400L);
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
        int viewportWidth = RewardPopupLayout.viewportWidth(client);
        int centerX = RewardPopupLayout.centerX(client);
        int centerY = RewardPopupLayout.TOP;
        int width = Math.min(RewardPopupLayout.WIDTH, viewportWidth - 12);
        int fullHeight = RewardPopupLayout.HEIGHT;
        int height = Math.round(fullHeight * open);

        Graphics2D g = (Graphics2D) graphics.create();
        try
        {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setComposite(AlphaComposite.SrcOver.derive(alpha));
            int lineWidth = Math.round((viewportWidth - 36) * sweep);
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
            int artSize = RewardPopupLayout.scale(34);
            if (art != null)
            {
                g.setColor(new Color(255, 202, 72, 90));
                g.fillRoundRect(x + RewardPopupLayout.scale(7), y + (fullHeight - artSize) / 2,
                    artSize, artSize, RewardPopupLayout.scale(5), RewardPopupLayout.scale(5));
                g.drawImage(art, x + RewardPopupLayout.scale(9),
                    y + (fullHeight - artSize) / 2 + RewardPopupLayout.scale(2),
                    artSize - RewardPopupLayout.scale(4), artSize - RewardPopupLayout.scale(4), null);
            }
            int textX = x + RewardPopupLayout.scale(7) + artSize + RewardPopupLayout.scale(6);
            g.setColor(new Color(184, 151, 73));
            g.setFont(new Font("SansSerif", Font.BOLD, RewardPopupLayout.scale(6)));
            g.drawString("CHALLENGE COMPLETE", textX, y + RewardPopupLayout.scale(14));
            g.setColor(DeckscapePalette.GOLD);
            g.setFont(new Font("Serif", Font.BOLD, RewardPopupLayout.scale(9)));
            g.drawString(title, textX, y + RewardPopupLayout.scale(29));
            g.setColor(DeckscapePalette.PARCHMENT);
            g.setFont(new Font("SansSerif", Font.PLAIN, RewardPopupLayout.scale(6)));
            g.drawString(reward, textX, y + RewardPopupLayout.scale(41));
            g.setColor(new Color(164, 145, 103));
            g.setFont(new Font("SansSerif", Font.BOLD, RewardPopupLayout.scale(6)));
            g.drawString("Saved to Deckscape", textX, y + RewardPopupLayout.scale(52));
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
