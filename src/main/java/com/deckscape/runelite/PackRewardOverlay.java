package com.deckscape.runelite;

import com.deckscape.runelite.model.PackType;
import com.deckscape.runelite.ui.CardPainter;
import com.deckscape.runelite.ui.DeckscapeImages;
import com.deckscape.runelite.ui.DeckscapePalette;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
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

/**
 * Compact in-game reward toast on the bundled OSRS-style panel art (background.png),
 * queued so simultaneous XP rolls are never hidden.
 */
@Singleton
public final class PackRewardOverlay extends Overlay
{
    private static final long INTRO = 420L;
    private static final long HOLD = RewardPopupLayout.linger(2500L);
    private static final long OUTRO = 520L;
    private static final long TOTAL = INTRO + HOLD + OUTRO;

    private final Client client;
    private final DeckscapeConfig config;
    private final Deque<PackType> queue = new ArrayDeque<>();
    private PackType active;
    private long startedAt;
    private BufferedImage packIcon;

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
        packIcon = active == null ? null : CardPainter.packImage(active,
            RewardPopupLayout.scale(22), RewardPopupLayout.scale(32));
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
        int width = Math.min(RewardPopupLayout.WIDTH, RewardPopupLayout.viewportWidth(client) - 12);
        int height = RewardPopupLayout.HEIGHT;
        int x = RewardPopupLayout.centerX(client) - width / 2;
        int y = RewardPopupLayout.TOP + Math.round(rise * -14f);

        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setComposite(AlphaComposite.SrcOver.derive(alpha));

        BufferedImage panel = DeckscapeImages.load("/background.png");
        if (panel != null)
        {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            graphics.drawImage(panel, x, y, width, height, null);
        }
        else
        {
            graphics.setColor(new Color(8, 7, 5, 225));
            graphics.fillRoundRect(x, y, width, height, 12, 12);
        }

        if (packIcon != null)
        {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.drawImage(packIcon, x + RewardPopupLayout.scale(7), y + (height - packIcon.getHeight()) / 2, null);
        }
        int textX = x + RewardPopupLayout.scale(7)
            + (packIcon == null ? 0 : packIcon.getWidth() + RewardPopupLayout.scale(5));
        drawText(graphics, "Pack unlocked!", textX, y + RewardPopupLayout.scale(19),
            new Font("Serif", Font.BOLD, RewardPopupLayout.scale(10)), DeckscapePalette.GOLD);
        drawText(graphics, "+1 " + active.getDisplayName(), textX, y + RewardPopupLayout.scale(34),
            new Font("SansSerif", Font.BOLD, RewardPopupLayout.scale(8)), DeckscapePalette.PARCHMENT);
        drawText(graphics, "Open in Deckscape", textX, y + RewardPopupLayout.scale(48),
            new Font("SansSerif", Font.PLAIN, RewardPopupLayout.scale(7)), DeckscapePalette.MUTED);
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
