package com.deckscape.runelite.ui;

import com.deckscape.runelite.model.DeckscapeCard;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.Timer;

public final class PackOpeningView extends JPanel
{
    public PackOpeningView(String packName, List<DeckscapeCard> cards, Consumer<Integer> soundPlayer, Runnable onDone)
    {
        setLayout(new BorderLayout(0, 12));
        setBackground(DeckscapePalette.PANEL_DARK);
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel heading = new JLabel("<html><center><span style='font-size:16px; color:#ffd45e;'><b>" + packName + "</b></span><br><span style='color:#a8a08a;'>Click each card to reveal your rewards!</span></center></html>", SwingConstants.CENTER);
        add(heading, BorderLayout.NORTH);

        // Layout cards in a single horizontal row (5 columns)
        JPanel grid = new JPanel(new GridLayout(1, 5, 12, 12));
        grid.setOpaque(false);
        
        List<RevealCardTile> tiles = new ArrayList<>();
        for (DeckscapeCard card : cards)
        {
            RevealCardTile tile = new RevealCardTile(card, soundPlayer);
            // Size cards beautifully for the larger 5-column layout
            tile.setPreferredSize(new Dimension(120, 185));
            tiles.add(tile);
            grid.add(tile);
        }
        
        // Wrap grid with horizontal glue to center it if smaller than window width
        JPanel centerWrapper = new JPanel();
        centerWrapper.setLayout(new BorderLayout());
        centerWrapper.setOpaque(false);
        centerWrapper.add(Box.createHorizontalGlue(), BorderLayout.WEST);
        centerWrapper.add(grid, BorderLayout.CENTER);
        centerWrapper.add(Box.createHorizontalGlue(), BorderLayout.EAST);
        
        add(centerWrapper, BorderLayout.CENTER);

        JButton done = button("Return to Packs");
        done.setPreferredSize(new java.awt.Dimension(200, 36));
        done.addActionListener(event -> onDone.run());
        
        JPanel southPanel = new JPanel();
        southPanel.setOpaque(false);
        southPanel.add(done);
        add(southPanel, BorderLayout.SOUTH);

        // Staggered card entry animations (slide/fade sequence)
        Timer arrivals = new Timer(180, null);
        final int[] index = {0};
        arrivals.addActionListener(event ->
        {
            if (index[0] >= tiles.size())
            {
                arrivals.stop();
                return;
            }
            tiles.get(index[0]++).setArrived(true);
            if (soundPlayer != null)
            {
                soundPlayer.accept(2581); // Subtle card place/swoosh sound
            }
        });
        arrivals.setInitialDelay(150);
        arrivals.start();
    }

    static JButton button(String text)
    {
        JButton button = new JButton(text);
        button.setFocusPainted(false);
        button.setForeground(DeckscapePalette.INK);
        button.setBackground(DeckscapePalette.BRASS);
        button.setFont(button.getFont().deriveFont(java.awt.Font.BOLD, 12f));
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new java.awt.Color(111, 75, 17), 2),
            BorderFactory.createEmptyBorder(6, 16, 6, 16)));
        return button;
    }
}
