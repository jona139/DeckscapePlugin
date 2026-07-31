package com.deckscape.runelite.ui;

import com.deckscape.runelite.DeckscapeStore;
import com.deckscape.runelite.model.DeckscapeState;
import java.awt.Component;
import java.awt.Container;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JScrollPane;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.laf.RuneLiteScrollBarUI;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeckscapePanelLayoutTest
{
    @Test
    void usesRuneLiteScrollablePanelWrapper() throws Exception
    {
        DeckscapeStore store = new DeckscapeStore(null, null);
        Field stateField = DeckscapeStore.class.getDeclaredField("state");
        stateField.setAccessible(true);
        stateField.set(store, new DeckscapeState());

        AtomicReference<DeckscapePanel> reference = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> reference.set(new DeckscapePanel(store)));
        DeckscapePanel panel = reference.get();

        assertNotSame(panel, panel.getWrappedPanel());
        JScrollPane scrollPane = findScrollPane(panel.getWrappedPanel());
        assertNotNull(scrollPane);
        assertEquals(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER, scrollPane.getHorizontalScrollBarPolicy());
        assertEquals(16, scrollPane.getVerticalScrollBar().getUnitIncrement());
        assertEquals(RuneLiteScrollBarUI.class, scrollPane.getVerticalScrollBar().getUI().getClass());
        assertEquals(ColorScheme.DARK_GRAY_COLOR, scrollPane.getViewport().getBackground());
        assertEquals(ColorScheme.SCROLL_TRACK_COLOR, scrollPane.getVerticalScrollBar().getBackground());

        JLabel featureCopy = findLabel(panel, "Use the cards you earn here");
        assertNotNull(featureCopy);
        assertTrue(featureCopy.getPreferredSize().width <= 165);
    }

    private static JScrollPane findScrollPane(Container root)
    {
        for (Component component : root.getComponents())
        {
            if (component instanceof JScrollPane) return (JScrollPane) component;
            if (component instanceof Container)
            {
                JScrollPane nested = findScrollPane((Container) component);
                if (nested != null) return nested;
            }
        }
        return null;
    }

    private static JLabel findLabel(Container root, String text)
    {
        for (Component component : root.getComponents())
        {
            if (component instanceof JLabel && ((JLabel) component).getText().contains(text))
                return (JLabel) component;
            if (component instanceof Container)
            {
                JLabel nested = findLabel((Container) component, text);
                if (nested != null) return nested;
            }
        }
        return null;
    }
}
