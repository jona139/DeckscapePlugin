package com.deckscape.runelite.ui;

import com.deckscape.runelite.model.CardCatalog;
import com.deckscape.runelite.model.DeckscapeCard;
import com.deckscape.runelite.model.PackType;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

/** Temporary visual harness: renders the painters to PNGs for manual inspection. */
class RenderPreviewTest
{
    private static final File OUT = new File(System.getProperty("deckscape.preview.dir", "build/preview"));

    @Test
    void renderPreviews() throws Exception
    {
        OUT.mkdirs();
        face("face_grayzag", CardCatalog.byId("wizard_grayzag"), 3, 200, 300);
        face("face_goblin_mail", CardCatalog.byId("goblin_mail"), 0, 200, 300);
        face("face_imp", CardCatalog.byId("imp"), 0, 200, 300);
        face("face_casket", CardCatalog.byId("casket"), 0, 200, 300);
        face("face_magic_ball", CardCatalog.byId("magic_ball"), 0, 200, 300);
        face("face_smite_universal", CardCatalog.byId("smite"), 2, 200, 300);
        face("face_small_tile", CardCatalog.byId("archmage_sedridor"), 1, 124, 186);
        collectionFace("face_kandarin_collection", new DeckscapeCard("demonic_gorilla", "Demonic Gorilla", "Kandarin",
            DeckscapeCard.Rarity.EPIC, DeckscapeCard.Kind.UNIT, DeckscapeCard.Style.MELEE, 10, 9, "", null), 2, 200);
        collectionFace("face_varlamore_collection", new DeckscapeCard("blood_moon", "Blood Moon", "Varlamore",
            DeckscapeCard.Rarity.RARE, DeckscapeCard.Kind.UNIT, DeckscapeCard.Style.MELEE, 9, 9, "", null), 3, 200);

        back("back_misthalin", "Misthalin");
        back("back_universal", "Universal");

        ImageIO.write(CardPainter.packImage(PackType.SKILLING, 132, 190), "png", new File(OUT, "pack_skilling.png"));
        ImageIO.write(CardPainter.packImage(PackType.GENERAL, 132, 190), "png", new File(OUT, "pack_general.png"));
        ImageIO.write(CardPainter.packImage(PackType.FACTION_MISTHALIN, 132, 190), "png", new File(OUT, "pack_misthalin.png"));
    }

    private static void face(String name, DeckscapeCard card, int owned, int w, int h) throws Exception
    {
        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        CardPainter.paintFace(g, 0, 0, w, h, card, owned);
        g.dispose();
        ImageIO.write(image, "png", new File(OUT, name + ".png"));
    }

    private static void back(String name, String faction) throws Exception
    {
        BufferedImage image = new BufferedImage(200, 300, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        CardPainter.paintBack(g, 0, 0, 200, 300, faction);
        g.dispose();
        ImageIO.write(image, "png", new File(OUT, name + ".png"));
    }

    private static void collectionFace(String name, DeckscapeCard card, int owned, int width) throws Exception
    {
        int cardHeight = CardTile.cardHeightForWidth(width);
        BufferedImage image = new BufferedImage(width, CardTile.collectionHeightForWidth(width), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        CardPainter.paintFace(g, 0, 0, width, cardHeight, card, owned);
        g.dispose();
        ImageIO.write(image, "png", new File(OUT, name + ".png"));
    }
}
