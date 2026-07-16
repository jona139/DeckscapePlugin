package com.deckscape.runelite.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import net.runelite.client.util.LinkBrowser;

/** First-run and manually reopenable guide for linking and privacy. */
final class DeckscapeWelcomeDialog extends JDialog
{
    static final String WEBSITE = "https://deckscape.gamecubejona.com";

    DeckscapeWelcomeDialog(Component parent)
    {
        super(SwingUtilities.getWindowAncestor(parent), "Welcome to Deckscape", ModalityType.MODELESS);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(520, 440));
        setSize(650, 610);
        setResizable(true);

        JPanel root = new JPanel(new BorderLayout(0, 12));
        root.setBackground(DeckscapePalette.PANEL_DARK);
        root.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));

        JTextPane copy = new JTextPane();
        copy.setEditable(false);
        copy.setContentType("text/html");
        copy.setBackground(DeckscapePalette.PANEL_DARK);
        copy.setForeground(DeckscapePalette.PARCHMENT);
        copy.setFont(new Font("SansSerif", Font.PLAIN, 14));
        copy.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 8));
        copy.setText("<html><body style='font-family:sans-serif;font-size:13px;color:#e9ddbd'>"
            + "<h1 style='color:#ffcf4e'>Welcome to Deckscape</h1>"
            + "<p>Deckscape turns eligible Old School RuneScape activity into a synchronized card collection. "
            + "Earn reward rolls from XP, open packs, browse cards, and complete verified gold-frame challenges.</p>"
            + "<h2 style='color:#ffdf83'>Play your collection</h2>"
            + "<p>The cards you earn through the plugin can be used in a fully working card game at "
            + "<b>deckscape.gamecubejona.com</b>. Collect alternate card art created by OSRS artists, and unlock gold card trims "
            + "that show off verified achievements from the real game.</p>"
            + "<h2 style='color:#ffdf83'>Link and synchronize</h2>"
            + "<ol><li>Open the Deckscape plugin settings and check <b>Share data with Deckscape (third party)</b>.</li>"
            + "<li>Copy the one-use pairing code shown in the side panel.</li>"
            + "<li>Sign in at <b>deckscape.gamecubejona.com</b> and enter the code on your account page.</li>"
            + "<li>Use <b>Sync now</b> whenever you want to push progress or immediately pull website card-art and gold-trim choices. "
            + "Otherwise the plugin synchronizes at most once every five minutes while idle; pack interactions synchronize immediately.</li></ol>"
            + "<h2 style='color:#ffdf83'>Privacy and eligibility</h2>"
            + "<p>Deckscape is a third-party service independent of RuneLite and Jagex and is hosted using Supabase. Data sharing is disabled until you explicitly enable it. "
            + "When enabled, the plugin sends a revocable device token, XP deltas, verified challenge progress and world context, pack actions, "
            + "and normal server connection metadata. It never asks for or sends your RuneScape or website password.</p>"
            + "<p>XP and challenge progress only count on eligible normal main-game worlds. Leagues, Deadman, Last Man Standing, PvP and high-risk worlds, "
            + "PvP Arena, beta, tournament, speedrunning and other no-save modes are excluded. Rewards have no real-world or in-game trade value, "
            + "and the external service may occasionally be unavailable.</p>"
            + "<p>You can disable sharing at any time in the plugin settings. See the repository privacy notice for full details.</p>"
            + "</body></html>");
        copy.setCaretPosition(0);
        JScrollPane scroll = new JScrollPane(copy);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(91, 82, 64)));
        root.add(scroll, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        JButton website = PackOpeningView.button("Open Deckscape website");
        website.addActionListener(event -> LinkBrowser.browse(WEBSITE));
        JButton close = PackOpeningView.button("Close");
        close.addActionListener(event -> dispose());
        actions.add(website);
        actions.add(close);
        root.add(actions, BorderLayout.SOUTH);

        setContentPane(root);
        setLocationRelativeTo(parent);
    }
}
