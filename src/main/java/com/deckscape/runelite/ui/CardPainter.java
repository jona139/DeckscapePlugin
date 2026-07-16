package com.deckscape.runelite.ui;

import com.deckscape.runelite.model.DeckscapeCard;
import com.deckscape.runelite.model.PackType;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Java2D port of DeckscapeWeb's card rendering: the collection/reveal card face
 * (styles.css .library-card.card), the pixel-art regional card back
 * (.cinematic-card-back + CardBackBands in assets.tsx), and the crimped pack visual
 * (.pack-visual). All methods scale to any size so Swing tiles and canvas overlays share them.
 */
public final class CardPainter
{
    // Web reference card is 116x174; scale factors are derived from that.
    private static final int REF_W = 116;
    private static final int REF_H = 174;

    private static final Color FRAME = new Color(0x17181c);
    private static final Color PARCHMENT = new Color(0xf3e6c4);
    private static final Color PARCHMENT_EDGE = new Color(0xd8c295);
    private static final Color NAME_TEXT = new Color(0xfff0c8);
    private static final Color NAME_RULE = new Color(255, 235, 179, 115);
    private static final Color GEM_TEXT = new Color(0x2a1c40);
    private static final Color COST_TEXT = new Color(0xf8efcf);
    private static final Color OWNED_TEXT = new Color(0x2b190a);

    // Posterised radial gradient grid, identical to assets.tsx CardBackBands.
    private static final int BACK_COLS = 32;
    private static final int BACK_ROWS = 48;
    private static final double[] BAND_STOPS = {1.0, 1.28, 1.56, 1.84};

    private static final Map<String, BufferedImage> SILHOUETTES = new ConcurrentHashMap<>();
    private static final Map<String, BufferedImage> SOFT_BACKDROPS = new ConcurrentHashMap<>();

    private CardPainter() {}

    // ---- card face -------------------------------------------------------

    /** Paints the web-style card face. Pass owned &lt; 0 to hide the xN badge. */
    public static void paintFace(Graphics2D graphics, int x, int y, int w, int h, DeckscapeCard card, int owned)
    {
        paintFace(graphics, x, y, w, h, card, owned, false);
    }

    /** Paints a synchronized gold-trim cosmetic around the normal card face. */
    public static void paintFace(Graphics2D graphics, int x, int y, int w, int h, DeckscapeCard card, int owned, boolean goldTrim)
    {
        Graphics2D g = (Graphics2D) graphics.create();
        g.translate(x, y);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        float s = Math.min(w / (float) REF_W, h / (float) REF_H);
        Color rarity = rarityColor(card.getRarity());

        // Rarity border: 2px frame with a 5px left ribbon, parchment body.
        int edge = Math.max(2, Math.round(2 * s));
        int ribbon = Math.max(4, Math.round(5 * s));
        if (goldTrim)
            g.setPaint(new GradientPaint(0, 0, new Color(0xffef91), w, h, new Color(0xa96808)));
        else
            g.setColor(rarity);
        g.fill(new RoundRectangle2D.Float(0, 0, w, h, 6, 6));
        g.setPaint(new GradientPaint(0, 0, PARCHMENT, w * .35f, h, PARCHMENT_EDGE));
        g.fill(new RoundRectangle2D.Float(ribbon, edge, w - ribbon - edge, h - edge * 2, 4, 4));
        if (goldTrim)
        {
            g.setColor(new Color(255, 238, 153, 220));
            g.setStroke(new BasicStroke(Math.max(1f, 1.15f * s)));
            g.draw(new RoundRectangle2D.Float(edge / 2f, edge / 2f, w - edge, h - edge, 5, 5));
        }

        int pad = Math.max(3, Math.round(4 * s));
        Rectangle inner = new Rectangle(ribbon + pad - 1, edge + pad - 1, w - ribbon - edge - pad * 2 + 2, h - edge * 2 - pad * 2 + 2);

        // Portrait: combat-style gradient, region scenery behind the art, art contained on top.
        Shape clip = g.getClip();
        g.clipRect(inner.x, inner.y, inner.width, inner.height);
        g.setPaint(styleGradient(card.getStyle(), inner));
        g.fillRect(inner.x, inner.y, inner.width, inner.height);
        RegionTheme theme = RegionTheme.forFaction(card.getFaction());
        BufferedImage backdrop = theme.backdrop == null ? null : softBackdrop(theme.backdrop);
        if (backdrop != null)
        {
            // Match the web's scale(1.12): blurred edges remain outside the portrait.
            int zoomX = Math.round(inner.width * .06f);
            int zoomY = Math.round(inner.height * .06f);
            drawCover(g, backdrop, new Rectangle(inner.x - zoomX, inner.y - zoomY,
                inner.width + zoomX * 2, inner.height + zoomY * 2));
        }
        BufferedImage art = DeckscapeImages.loadCardArt(card);
        if (art != null)
        {
            if (card.getKind() != DeckscapeCard.Kind.UNIT)
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            // The website only gives portrait art a 2px inset. Reusing the card-body
            // padding here made characters visibly smaller than their web cards.
            int artPad = Math.max(1, Math.round(2 * s));
            Rectangle artBox = new Rectangle(inner.x + artPad, inner.y + artPad,
                inner.width - artPad * 2, inner.height - artPad * 2);
            Rectangle shadowBox = new Rectangle(artBox.x, artBox.y + Math.max(1, Math.round(s)), artBox.width, artBox.height);
            drawContain(g, silhouette("card:" + card.getId(), art), shadowBox);
            drawContain(g, art, artBox);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        }
        else
        {
            g.setFont(DeckscapeFonts.display(Math.max(14, Math.round(20 * s))));
            g.setColor(new Color(255, 255, 255, 235));
            String initials = initials(card.getName());
            FontMetrics fm = g.getFontMetrics();
            g.drawString(initials, inner.x + (inner.width - fm.stringWidth(initials)) / 2,
                inner.y + (inner.height + fm.getAscent() - fm.getDescent()) / 2);
        }
        g.setClip(clip);

        // Name plate over the bottom of the portrait; grows for two-line names.
        int nameSize = Math.max(9, Math.round(11.5f * s));
        g.setFont(DeckscapeFonts.display(nameSize));
        int nameX = inner.x + Math.round(6 * s);
        int nameWidth = inner.width - Math.round(12 * s);
        List<String> nameLines = wrapName(g.getFontMetrics(), card.getName(), nameWidth);
        FontMetrics nameMetrics = g.getFontMetrics();
        int plateH = Math.max(Math.max(20, Math.round(29 * s)),
            nameLines.size() * (nameMetrics.getAscent() + nameMetrics.getDescent()) + Math.round(8 * s));
        int plateY = h - edge - pad + 1 - plateH;
        g.setPaint(new GradientPaint(inner.x, 0, new Color(25, 16, 10, 235), inner.x + inner.width, 0, new Color(25, 16, 10, 171)));
        g.fillRect(inner.x, plateY, inner.width, plateH);
        g.setColor(NAME_RULE);
        g.fillRect(inner.x, plateY, inner.width, 1);
        g.setColor(NAME_TEXT);
        drawNameLines(g, nameLines, nameX, plateY, plateH);

        // Cost gem (top-left) and combat-style icon (top-right).
        int costSize = Math.max(17, Math.round(24 * s));
        int corner = inner.x + Math.max(2, Math.round(2 * s));
        int costY = inner.y + Math.max(2, Math.round(2 * s));
        BufferedImage costSocket = DeckscapeImages.load("/com/deckscape/runelite/ui/cost_socket.png");
        if (costSocket != null) g.drawImage(costSocket, corner, costY, costSize, costSize, null);
        g.setColor(COST_TEXT);
        g.setFont(DeckscapeFonts.display(Math.max(10, Math.round(12 * s))));
        drawCenteredIn(g, String.valueOf(card.getCost()), corner, costY - Math.max(0, Math.round(1 * s)), costSize, costSize);

        BufferedImage styleIcon = DeckscapeImages.load("/com/deckscape/runelite/regions/style_" + card.getStyle().name().toLowerCase() + ".png");
        if (styleIcon != null)
        {
            int iconSize = Math.max(13, Math.round(19 * s));
            drawContain(g, styleIcon, new Rectangle(inner.x + inner.width - iconSize - Math.round(2 * s), inner.y + Math.round(3 * s), iconSize, iconSize));
        }

        // Region crest above the name plate (bottom-left).
        BufferedImage crest = DeckscapeImages.load(theme.crest);
        if (crest != null)
        {
            boolean universal = "Universal".equalsIgnoreCase(card.getFaction());
            int crestW = Math.max(10, Math.round((universal ? 20 : 14) * s));
            int crestH = Math.max(14, Math.round(21 * s));
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            drawContain(g, crest, new Rectangle(inner.x + Math.round(3 * s), plateY - crestH - Math.round(4 * s), crestW, crestH));
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        }

        // Website collection geometry: the stat socket sits above the nameplate.
        int gemW = Math.max(17, Math.round(24 * s));
        int gemH = card.getKind() == DeckscapeCard.Kind.UNIT
            ? Math.max(14, Math.round(20 * s)) : Math.max(17, Math.round(24 * s));
        int gemX = w - Math.round(7 * s) - gemW;
        int gemY = h - Math.round(41 * s) - gemH;
        if (card.getKind() == DeckscapeCard.Kind.UNIT)
        {
            BufferedImage heart = DeckscapeImages.load("/com/deckscape/runelite/ui/heart.png");
            if (heart != null) g.drawImage(heart, gemX, gemY, gemW, gemH, null);
            g.setColor(Color.WHITE);
            g.setFont(DeckscapeFonts.display(Math.max(10, Math.round(12 * s))));
            drawOutlinedCentered(g, String.valueOf(card.getPower()), gemX + Math.round(1 * s), gemY, gemW, gemH);
        }
        else
        {
            BufferedImage actionSocket = DeckscapeImages.load("/com/deckscape/runelite/ui/action_socket.png");
            if (actionSocket != null) g.drawImage(actionSocket, gemX, gemY, gemW, gemH, null);
            g.setColor(GEM_TEXT);
            g.fill(spark(gemX + gemW / 2f, gemY + gemH / 2f, Math.min(gemW, gemH) * .28f));
        }

        // Collection quantity protrudes below the lower-left card edge.
        if (owned >= 0)
        {
            g.setFont(DeckscapeFonts.display(Math.max(10, Math.round(13 * s))));
            FontMetrics fm = g.getFontMetrics();
            String label = "×" + owned;
            int badgeW = Math.max(Math.round(31 * s), fm.stringWidth(label) + Math.round(12 * s));
            int badgeH = Math.max(16, Math.round(22 * s));
            int badgeX = Math.round(6 * s);
            int badgeY = h - Math.round(11 * s);
            g.setPaint(new GradientPaint(badgeX, badgeY, new Color(0xfff0b9), badgeX, badgeY + badgeH, new Color(0xd9b965)));
            g.fillRect(badgeX, badgeY, badgeW, badgeH);
            g.setColor(new Color(0x4a3219));
            g.setStroke(new BasicStroke(Math.max(1.5f, 2 * s)));
            g.drawRect(badgeX, badgeY, badgeW, badgeH);
            g.setColor(new Color(255, 248, 207, 165));
            g.drawRect(badgeX + 2, badgeY + 2, badgeW - 4, badgeH - 4);
            g.setColor(OWNED_TEXT);
            drawCenteredIn(g, label, badgeX, badgeY, badgeW, badgeH);
        }
        g.dispose();
    }

    // ---- card back -------------------------------------------------------

    /** Paints the pixel-art regional card back used by the web's pack cinematic. */
    public static void paintBack(Graphics2D graphics, int x, int y, int w, int h, String faction)
    {
        RegionTheme theme = RegionTheme.forFaction(faction);
        Graphics2D g = (Graphics2D) graphics.create();
        g.translate(x, y);
        g.setColor(FRAME);
        g.fill(new RoundRectangle2D.Float(0, 0, w, h, 8, 8));
        int frame = Math.max(3, Math.round(w * 0.05f));
        Rectangle inner = new Rectangle(frame, frame, w - frame * 2, h - frame * 2);
        paintBands(g, inner, theme);

        // Two-tone metal corner brackets, light toward the outer edge.
        int bracket = Math.max(6, Math.round(w * 0.10f));
        int insetX = frame + Math.round(w * 0.035f);
        int insetY = frame + Math.round(h * 0.025f);
        paintBracket(g, insetX, insetY, bracket, false, false);
        paintBracket(g, w - insetX - bracket, insetY, bracket, true, false);
        paintBracket(g, insetX, h - insetY - bracket, bracket, false, true);
        paintBracket(g, w - insetX - bracket, h - insetY - bracket, bracket, true, true);

        BufferedImage crest = DeckscapeImages.load(theme.crest);
        if (crest != null)
        {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            int crestW = Math.round(w * 0.56f);
            int crestH = Math.round(h * 0.46f);
            drawContain(g, crest, new Rectangle((w - crestW) / 2, Math.round(h * 0.37f) - crestH / 2, crestW, crestH));
        }
        g.dispose();
    }

    // ---- pack visual -----------------------------------------------------

    /**
     * Paints the web pack visual into the given body rectangle. The serrated crimps
     * protrude ~12% beyond the top/bottom edges and ~5% sideways, so leave that margin.
     */
    public static void paintPack(Graphics2D graphics, int x, int y, int w, int h, PackType type)
    {
        RegionTheme theme = RegionTheme.forPack(type);
        float s = w / 132f;
        Graphics2D g = (Graphics2D) graphics.create();
        g.translate(x, y);

        int frame = Math.max(3, Math.round(4 * s));
        g.setColor(FRAME);
        g.fillRect(0, 0, w, h);
        paintBands(g, new Rectangle(frame, frame, w - frame * 2, h - frame * 2), theme);

        BufferedImage emblem = DeckscapeImages.load(RegionTheme.packEmblem(type));
        if (emblem != null)
        {
            boolean crest = type.name().startsWith("FACTION_");
            int emblemW = Math.round(w * (crest ? 0.52f : 0.56f));
            int emblemH = Math.round(h * (crest ? 0.52f : 0.40f));
            Rectangle box = new Rectangle((w - emblemW) / 2, (h - emblemH) / 2, emblemW, emblemH);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            Rectangle shadow = new Rectangle(box.x + Math.round(3 * s), box.y + Math.round(4 * s), box.width, box.height);
            drawContain(g, silhouette(RegionTheme.packEmblem(type), emblem), shadow);
            drawContain(g, emblem, box);
        }

        paintCrimp(g, w, h, s, theme, true);
        paintCrimp(g, w, h, s, theme, false);
        g.dispose();
    }

    /** Renders a pack (crimps included) into an image, for Swing icons and popups. */
    public static BufferedImage packImage(PackType type, int bodyWidth, int bodyHeight)
    {
        float s = bodyWidth / 132f;
        int overX = Math.round(7 * s) + 1;
        int overY = Math.round(15 * s) + 1;
        BufferedImage image = new BufferedImage(bodyWidth + overX * 2, bodyHeight + overY * 2, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        paintPack(g, overX, overY, bodyWidth, bodyHeight, type);
        g.dispose();
        return image;
    }

    // ---- shared drawing helpers -------------------------------------------

    /** The stepped concentric bands from assets.tsx, drawn on the 32x48 pixel grid. */
    private static void paintBands(Graphics2D g, Rectangle area, RegionTheme theme)
    {
        Color[] fills = {
            mix(theme.glow, theme.color, 0.42f),
            theme.color,
            mix(theme.color, theme.deep, 0.70f),
            mix(theme.color, theme.deep, 0.45f),
            mix(theme.color, theme.deep, 0.22f),
        };
        double cellW = area.width / (double) BACK_COLS;
        double cellH = area.height / (double) BACK_ROWS;
        for (int row = 0; row < BACK_ROWS; row++)
        {
            int y = area.y + (int) Math.round(row * cellH);
            int rowH = area.y + (int) Math.round((row + 1) * cellH) - y;
            int start = 0;
            int band = bandAt(0, row);
            for (int col = 1; col <= BACK_COLS; col++)
            {
                int next = col < BACK_COLS ? bandAt(col, row) : -1;
                if (next == band) continue;
                int x = area.x + (int) Math.round(start * cellW);
                int runW = area.x + (int) Math.round(col * cellW) - x;
                g.setColor(fills[band]);
                g.fillRect(x, y, runW, rowH);
                start = col;
                band = next;
            }
        }
    }

    private static int bandAt(int col, int row)
    {
        double dx = (col + 0.5 - BACK_COLS / 2.0) / (BACK_COLS * 0.57);
        double dy = (row + 0.5 - BACK_ROWS * 0.15) / (BACK_ROWS * 0.37);
        double distance = Math.sqrt(dx * dx + dy * dy);
        for (int i = 0; i < BAND_STOPS.length; i++) if (distance < BAND_STOPS[i]) return i;
        return BAND_STOPS.length;
    }

    private static void paintBracket(Graphics2D g, int x, int y, int size, boolean flipX, boolean flipY)
    {
        Graphics2D b = (Graphics2D) g.create();
        b.translate(x + (flipX ? size : 0), y + (flipY ? size : 0));
        b.scale(flipX ? -1 : 1, flipY ? -1 : 1);
        float arm = size * 0.34f;
        Path2D bracket = new Path2D.Float();
        bracket.moveTo(0, 0);
        bracket.lineTo(size, 0);
        bracket.lineTo(size, arm);
        bracket.lineTo(arm, arm);
        bracket.lineTo(arm, size);
        bracket.lineTo(0, size);
        bracket.closePath();
        b.setColor(new Color(0xcdd1d6));
        b.fill(bracket);
        b.clip(bracket);
        Path2D dark = new Path2D.Float();
        dark.moveTo(size * 0.92f, 0);
        dark.lineTo(size, 0);
        dark.lineTo(size, size);
        dark.lineTo(0, size);
        dark.lineTo(0, size * 0.92f);
        dark.closePath();
        b.setColor(new Color(0x7e838b));
        b.fill(dark);
        b.dispose();
    }

    /** A serrated foil crimp: striped bar with a zig-zag edge, matching .pack-crimp. */
    private static void paintCrimp(Graphics2D g, int w, int h, float s, RegionTheme theme, boolean top)
    {
        int crimpH = Math.max(8, Math.round(22 * s));
        int overX = Math.round(7 * s);
        int flat = Math.round(crimpH * 0.72f);
        int yTop = top ? -Math.round(15 * s) : h + Math.round(15 * s) - crimpH;
        int left = -overX;
        int width = w + overX * 2;

        Path2D path = new Path2D.Float();
        if (top)
        {
            path.moveTo(left, yTop);
            path.lineTo(left + width, yTop);
            path.lineTo(left + width, yTop + flat);
            for (int i = 20; i >= 0; i--)
            {
                float px = left + width * i / 20f;
                path.lineTo(px, yTop + (i % 2 == 0 ? flat : crimpH));
            }
        }
        else
        {
            path.moveTo(left, yTop + crimpH);
            path.lineTo(left, yTop + (crimpH - flat));
            for (int i = 0; i <= 20; i++)
            {
                float px = left + width * i / 20f;
                path.lineTo(px, yTop + (i % 2 == 0 ? crimpH - flat : 0));
            }
            path.lineTo(left + width, yTop + crimpH);
        }
        path.closePath();

        Shape clip = g.getClip();
        g.clip(path);
        g.setColor(theme.color);
        g.fillRect(left, yTop, width, crimpH);
        int stripe = Math.max(2, Math.round(3 * s));
        for (int px = left; px < left + width; px += stripe * 2)
        {
            g.setColor(new Color(255, 255, 255, 46));
            g.fillRect(px, yTop, stripe, crimpH);
            g.setColor(new Color(0, 0, 0, 41));
            g.fillRect(px + stripe, yTop, stripe, crimpH);
        }
        g.setClip(clip);
        g.setColor(FRAME);
        g.setStroke(new BasicStroke(Math.max(2f, 3 * s)));
        g.draw(path);
    }

    private static java.awt.Paint styleGradient(DeckscapeCard.Style style, Rectangle r)
    {
        Color from;
        Color to;
        switch (style)
        {
            case MELEE: from = new Color(0xb5462f); to = new Color(0x7a2416); break;
            case RANGED: from = new Color(0x2f7d3f); to = new Color(0x1c4a26); break;
            default: from = new Color(0x4257b7); to = new Color(0x26326e); break;
        }
        return new GradientPaint(r.x, r.y, from, r.x + r.width, r.y + r.height, to);
    }

    public static Color rarityColor(DeckscapeCard.Rarity rarity)
    {
        switch (rarity)
        {
            case UNCOMMON: return new Color(0x4d84d7);
            case RARE: return new Color(0x9c5ad5);
            case EPIC: return new Color(0xd94ea5);
            case LEGENDARY: return new Color(0xe38a32);
            default: return new Color(0x4f9a57);
        }
    }

    private static Color mix(Color a, Color b, float amountOfA)
    {
        float t = Math.max(0f, Math.min(1f, amountOfA));
        return new Color(
            Math.round(a.getRed() * t + b.getRed() * (1 - t)),
            Math.round(a.getGreen() * t + b.getGreen() * (1 - t)),
            Math.round(a.getBlue() * t + b.getBlue() * (1 - t)));
    }

    private static Path2D spark(float cx, float cy, float radius)
    {
        Path2D star = new Path2D.Float();
        float waist = radius * 0.28f;
        star.moveTo(cx, cy - radius);
        star.quadTo(cx + waist * .4f, cy - waist, cx + radius, cy);
        star.quadTo(cx + waist * .4f, cy + waist, cx, cy + radius);
        star.quadTo(cx - waist * .4f, cy + waist, cx - radius, cy);
        star.quadTo(cx - waist * .4f, cy - waist, cx, cy - radius);
        star.closePath();
        return star;
    }

    private static String initials(String value)
    {
        StringBuilder out = new StringBuilder();
        for (String word : value.split(" ")) if (!word.isEmpty() && out.length() < 2) out.append(word.charAt(0));
        return out.toString();
    }

    private static List<String> wrapName(FontMetrics fm, String name, int maxWidth)
    {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : name.split(" "))
        {
            String candidate = line.length() == 0 ? word : line + " " + word;
            if (fm.stringWidth(candidate) > maxWidth && line.length() > 0 && lines.isEmpty())
            {
                lines.add(line.toString());
                line = new StringBuilder(word);
            }
            else line = new StringBuilder(candidate);
        }
        lines.add(line.toString());
        for (int i = 0; i < lines.size(); i++)
        {
            String value = lines.get(i);
            while (value.length() > 3 && fm.stringWidth(value) > maxWidth) value = value.substring(0, value.length() - 2) + "…";
            lines.set(i, value);
        }
        return lines;
    }

    private static void drawNameLines(Graphics2D g, List<String> lines, int x, int plateY, int plateH)
    {
        FontMetrics fm = g.getFontMetrics();
        int lineH = fm.getAscent() + fm.getDescent();
        int total = lineH * Math.min(2, lines.size());
        int startY = plateY + Math.max(0, (plateH - total) / 2) + fm.getAscent();
        for (int i = 0; i < lines.size() && i < 2; i++) g.drawString(lines.get(i), x, startY + i * lineH);
    }

    private static void drawCenteredIn(Graphics2D g, String text, int x, int y, int w, int h)
    {
        FontMetrics fm = g.getFontMetrics();
        g.drawString(text, x + (w - fm.stringWidth(text)) / 2, y + (h + fm.getAscent() - fm.getDescent()) / 2);
    }

    private static void drawOutlinedCentered(Graphics2D g, String text, int x, int y, int w, int h)
    {
        FontMetrics fm = g.getFontMetrics();
        int textX = x + (w - fm.stringWidth(text)) / 2;
        int textY = y + (h + fm.getAscent() - fm.getDescent()) / 2;
        Color fill = g.getColor();
        g.setColor(new Color(0x3c1307));
        g.drawString(text, textX - 1, textY);
        g.drawString(text, textX + 1, textY);
        g.drawString(text, textX, textY - 1);
        g.drawString(text, textX, textY + 1);
        g.setColor(fill);
        g.drawString(text, textX, textY);
    }

    private static void drawContain(Graphics2D g, BufferedImage image, Rectangle r)
    {
        double scale = Math.min(r.width / (double) image.getWidth(), r.height / (double) image.getHeight());
        int w = (int) Math.round(image.getWidth() * scale);
        int h = (int) Math.round(image.getHeight() * scale);
        g.drawImage(image, r.x + (r.width - w) / 2, r.y + (r.height - h) / 2, w, h, null);
    }

    private static void drawCover(Graphics2D g, BufferedImage image, Rectangle r)
    {
        double scale = Math.max(r.width / (double) image.getWidth(), r.height / (double) image.getHeight());
        int w = (int) Math.ceil(image.getWidth() * scale);
        int h = (int) Math.ceil(image.getHeight() * scale);
        Shape clip = g.getClip();
        g.clipRect(r.x, r.y, r.width, r.height);
        g.drawImage(image, r.x + (r.width - w) / 2, r.y + (r.height - h) / 2, w, h, null);
        g.setClip(clip);
    }

    /** Black copy of a sprite, cached, used as a cheap drop shadow. */
    private static BufferedImage silhouette(String key, BufferedImage source)
    {
        return SILHOUETTES.computeIfAbsent(key, ignored -> {
            BufferedImage shadow = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
            for (int py = 0; py < source.getHeight(); py++)
                for (int px = 0; px < source.getWidth(); px++)
                {
                    int alpha = (source.getRGB(px, py) >>> 24);
                    if (alpha > 0) shadow.setRGB(px, py, (Math.min(140, alpha) << 24));
                }
            return shadow;
        });
    }

    /** Caches a modestly downscaled 3x3 blur matching the web portrait's 2px filter. */
    private static BufferedImage softBackdrop(String resource)
    {
        return SOFT_BACKDROPS.computeIfAbsent(resource, key -> {
            BufferedImage source = DeckscapeImages.load(key);
            if (source == null) return null;
            double scale = Math.min(1d, 320d / Math.max(source.getWidth(), source.getHeight()));
            int w = Math.max(1, (int) Math.round(source.getWidth() * scale));
            int h = Math.max(1, (int) Math.round(source.getHeight() * scale));
            BufferedImage small = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = small.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(source, 0, 0, w, h, null);
            g.dispose();
            float[] blur = new float[9];
            java.util.Arrays.fill(blur, 1f / blur.length);
            BufferedImage softened = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            new ConvolveOp(new Kernel(3, 3, blur), ConvolveOp.EDGE_NO_OP, null).filter(small, softened);
            return softened;
        });
    }
}
