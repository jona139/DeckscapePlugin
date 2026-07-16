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
    private final Deque<Reward> queue = new ArrayDeque<>();
    private Reward active;
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
        queue.addLast(Reward.pack(type));
        if (active == null) advance();
    }

    public synchronized void showCoins(int amount)
    {
        if (amount <= 0) return;
        queue.addLast(new Reward("COINS FOUND!", "+" + amount + " Coins", "Added to your wallet",
            "/com/deckscape/runelite/ui/wallet_coins.png", null));
        if (active == null) advance();
    }

    public synchronized void showStardust(int amount)
    {
        if (amount <= 0) return;
        queue.addLast(new Reward("STARDUST FOUND!", "+" + amount + " Stardust", "Added to your wallet",
            "/com/deckscape/runelite/ui/wallet_stardust.png", null));
        if (active == null) advance();
    }

    private void advance()
    {
        active = queue.pollFirst();
        startedAt = active == null ? 0L : System.currentTimeMillis();
        if (active == null) packIcon = null;
        else if (active.packType != null) packIcon = CardPainter.packImage(active.packType,
            RewardPopupLayout.scale(22), RewardPopupLayout.scale(32));
        else packIcon = fitIcon(DeckscapeImages.load(active.iconResource),
            RewardPopupLayout.scale(32), RewardPopupLayout.scale(32));
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
        graphics.setColor(new Color(10, 8, 5, 145));
        graphics.fillRoundRect(textX - RewardPopupLayout.scale(3), y + RewardPopupLayout.scale(7),
            x + width - textX - RewardPopupLayout.scale(6), RewardPopupLayout.scale(45),
            RewardPopupLayout.scale(4), RewardPopupLayout.scale(4));
        drawText(graphics, active.title, textX, y + RewardPopupLayout.scale(19),
            new Font("SansSerif", Font.BOLD, RewardPopupLayout.scale(9)), new Color(255, 231, 151));
        drawText(graphics, active.detail, textX, y + RewardPopupLayout.scale(34),
            new Font("SansSerif", Font.BOLD, RewardPopupLayout.scale(8)), Color.WHITE);
        drawText(graphics, active.footer, textX, y + RewardPopupLayout.scale(48),
            new Font("SansSerif", Font.BOLD, RewardPopupLayout.scale(7)), new Color(235, 220, 184));
        graphics.setComposite(AlphaComposite.SrcOver);
        return null;
    }

    private static void drawText(Graphics2D g, String text, int x, int baseline, Font font, Color color)
    {
        g.setFont(font);
        g.setColor(new Color(0, 0, 0, 230));
        g.drawString(text, x + 1, baseline + 1);
        g.setColor(color);
        g.drawString(text, x, baseline);
    }

    private static BufferedImage fitIcon(BufferedImage source, int maximumWidth, int maximumHeight)
    {
        if (source == null) return null;
        double scale = Math.min(maximumWidth / (double) source.getWidth(), maximumHeight / (double) source.getHeight());
        int width = Math.max(1, (int) Math.round(source.getWidth() * scale));
        int height = Math.max(1, (int) Math.round(source.getHeight() * scale));
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = result.createGraphics();
        try
        {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.drawImage(source, 0, 0, width, height, null);
        }
        finally { graphics.dispose(); }
        return result;
    }

    private static final class Reward
    {
        private final String title;
        private final String detail;
        private final String footer;
        private final String iconResource;
        private final PackType packType;

        private Reward(String title, String detail, String footer, String iconResource, PackType packType)
        {
            this.title = title;
            this.detail = detail;
            this.footer = footer;
            this.iconResource = iconResource;
            this.packType = packType;
        }

        private static Reward pack(PackType type)
        {
            return new Reward("PACK UNLOCKED!", "+1 " + type.getDisplayName(), "Open it in Deckscape", null, type);
        }
    }

    private static float ease(float value)
    {
        float t = Math.max(0f, Math.min(1f, value));
        return 1f - (1f - t) * (1f - t) * (1f - t);
    }
}
