package com.deckscape.runelite.ui;

import java.awt.Font;
import java.io.InputStream;

/** Fonts shared with the web client, bundled so cards render consistently offline. */
public final class DeckscapeFonts
{
    private static final String DISPLAY_RESOURCE = "/com/deckscape/runelite/fonts/IMFellEnglishSC-Regular.ttf";
    private static final Font DISPLAY = load(DISPLAY_RESOURCE, new Font("Serif", Font.PLAIN, 12));

    private DeckscapeFonts() { }

    public static Font display(float size)
    {
        return DISPLAY.deriveFont(Font.PLAIN, size);
    }

    private static Font load(String resource, Font fallback)
    {
        try (InputStream stream = DeckscapeFonts.class.getResourceAsStream(resource))
        {
            return stream == null ? fallback : Font.createFont(Font.TRUETYPE_FONT, stream);
        }
        catch (Exception ignored)
        {
            return fallback;
        }
    }
}
