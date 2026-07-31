package com.deckscape.runelite.ui;

import com.deckscape.runelite.DeckscapeStore;
import com.deckscape.runelite.model.DeckscapeState;
import java.awt.Component;
import java.awt.Container;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import net.runelite.client.ui.laf.RuneLiteScrollBarUI;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;

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
}
