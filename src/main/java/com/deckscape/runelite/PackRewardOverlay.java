package com.deckscape.runelite;

import com.deckscape.runelite.model.PackType;
import com.deckscape.runelite.ui.DeckscapeImages;
import com.deckscape.runelite.ui.DeckscapePalette;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayDeque;
import java.util.Deque;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;

/** High-priority in-game reward banner, queued so simultaneous XP rolls are never hidden. */
@Singleton
public final class PackRewardOverlay extends Overlay
{
    private static final long INTRO = 420L;
    private static final long HOLD = 2500L;
    private static final long OUTRO = 520L;
    private static final long TOTAL = INTRO + HOLD + OUTRO;

    private final Client client;
    private final DeckscapeConfig config;
    private final Deque<PackType> queue = new ArrayDeque<>();
    private PackType active;
    private long startedAt;

    @Inject
    public PackRewardOverlay(Client client, DeckscapeConfig config)
    {
        this.client = client;
        this.config = config;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
        setPriority(OverlayPriority.HIGH);
    }

    public synchronized void showPack(PackType type)
    {
        queue.addLast(type);
        if (active == null) advance();
    }

    private void advance()
    {
        active = queue.pollFirst();
        startedAt = active == null ? 0L : System.currentTimeMillis();
    }

    @Override
    public synchronized Dimension render(Graphics2D graphics)
    {
        if (!config.packRewardPopup() || active == null) return null;
        long elapsed = System.currentTimeMillis() - startedAt;
        if (elapsed >= TOTAL)
        {
            advance();
            return null;
        }
        float alpha = elapsed < INTRO ? ease(elapsed / (float) INTRO)
            : elapsed > INTRO + HOLD ? 1f - ease((elapsed - INTRO - HOLD) / (float) OUTRO) : 1f;
        float rise = 1f - ease(Math.min(1f, elapsed / (float) INTRO));
        int width = Math.min(390, client.getCanvasWidth() - 30);
        int height = 118;
        int x = (client.getCanvasWidth() - width) / 2;
        int y = 42 + Math.round(rise * -30f);

        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setComposite(AlphaComposite.SrcOver.derive(alpha));
        graphics.setColor(new Color(8, 7, 5, 225));
        graphics.fillRoundRect(x, y, width, height, 12, 12);
        graphics.setPaint(new java.awt.GradientPaint(x, y, new Color(255, 211, 91, 70), x + width, y + height, new Color(74, 43, 8, 10)));
        graphics.fillRoundRect(x + 3, y + 3, width - 6, height - 6, 10, 10);
        graphics.setColor(DeckscapePalette.BRASS);
        graphics.setStroke(new BasicStroke(2f));
        graphics.drawRoundRect(x, y, width, height, 12, 12);
        graphics.setColor(new Color(255, 231, 155, 110));
        graphics.drawLine(x + 12, y + 8, x + width - 12, y + 8);

        BufferedImage pack = DeckscapeImages.load(DeckscapeImages.packResource(active));
        if (pack != null) graphics.drawImage(pack, x + 18, y + 13, 70, 90, null);
        drawText(graphics, "PACK UNLOCKED", x + 105, y + 39, new Font("Serif", Font.BOLD, 18), DeckscapePalette.GOLD);
        drawText(graphics, "+1 " + active.getDisplayName(), x + 105, y + 68, new Font("SansSerif", Font.BOLD, 14), DeckscapePalette.PARCHMENT);
        drawText(graphics, "Open it from the Deckscape panel", x + 105, y + 91, new Font("SansSerif", Font.PLAIN, 11), DeckscapePalette.MUTED);
        graphics.setComposite(AlphaComposite.SrcOver);
        return null;
    }

    private static void drawText(Graphics2D g, String text, int x, int baseline, Font font, Color color)
    {
        g.setFont(font); g.setColor(color); g.drawString(text, x, baseline);
    }

    private static float ease(float value)
    {
        float t = Math.max(0f, Math.min(1f, value));
        return 1f - (1f - t) * (1f - t) * (1f - t);
    }
}
