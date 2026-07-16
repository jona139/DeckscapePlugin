package com.deckscape.runelite.ui;

import com.deckscape.runelite.DeckscapeConfig;
import com.deckscape.runelite.DeckscapeStore;
import com.deckscape.runelite.model.DeckscapeState;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import net.runelite.client.ui.PluginPanel;

@Singleton
public final class DeckscapePanel extends PluginPanel
{
    private final DeckscapeStore store;
    private final DeckscapeConfig config;
    private DeckscapeDialog dialog;
    private DeckscapeDialog.OpenPackAction openPackAction;
    private Runnable syncAction;
    private Runnable beginPairingAction;
    private java.util.function.Consumer<Integer> soundPlayer;
    private final JLabel statusLabel = new JLabel();
    private final JLabel syncDetail = new JLabel();
    private final JLabel statsLabel = new JLabel();
    private final JTextField codeField = new JTextField();
    private final JPanel pairingBox = new JPanel();

    @Inject
    public DeckscapePanel(DeckscapeStore store, DeckscapeConfig config)
    {
        super(false);
        this.store = store;
        this.config = config;
        setLayout(new BorderLayout());
        setBackground(DeckscapePalette.PANEL_DARK);

        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(DeckscapePalette.PANEL_DARK);
        body.setBorder(BorderFactory.createEmptyBorder(18, 12, 18, 12));

        JLabel eyebrow = new JLabel("OSRS COMPANION", SwingConstants.CENTER);
        eyebrow.setForeground(new Color(169, 139, 74));
        eyebrow.setFont(new Font("SansSerif", Font.BOLD, 9));
        eyebrow.setAlignmentX(CENTER_ALIGNMENT);
        body.add(eyebrow);
        JLabel title = new JLabel("<html><span style='color:#ffe18a'>DECK</span><span style='color:#f2e2bb'>SCAPE</span></html>", SwingConstants.CENTER);
        title.setFont(new Font("Serif", Font.BOLD, 25));
        title.setAlignmentX(CENTER_ALIGNMENT);
        body.add(title);
        JLabel subtitle = new JLabel("One collection. Two worlds.", SwingConstants.CENTER);
        subtitle.setForeground(DeckscapePalette.MUTED);
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 10));
        subtitle.setAlignmentX(CENTER_ALIGNMENT);
        body.add(subtitle);
        body.add(Box.createRigidArea(new Dimension(0, 16)));

        JPanel statusCard = card();
        statusLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        statusLabel.setAlignmentX(LEFT_ALIGNMENT);
        syncDetail.setForeground(DeckscapePalette.MUTED);
        syncDetail.setFont(new Font("SansSerif", Font.PLAIN, 10));
        syncDetail.setAlignmentX(LEFT_ALIGNMENT);
        statusCard.add(statusLabel);
        statusCard.add(Box.createRigidArea(new Dimension(0, 4)));
        statusCard.add(syncDetail);
        body.add(statusCard);
        body.add(Box.createRigidArea(new Dimension(0, 10)));

        pairingBox.setLayout(new BoxLayout(pairingBox, BoxLayout.Y_AXIS));
        pairingBox.setBackground(new Color(31, 27, 20));
        pairingBox.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(181, 140, 53), 2),
            BorderFactory.createEmptyBorder(13, 13, 13, 13)));
        pairingBox.setAlignmentX(CENTER_ALIGNMENT);
        JLabel pairTitle = new JLabel("LINK YOUR WEBSITE ACCOUNT");
        pairTitle.setForeground(DeckscapePalette.GOLD);
        pairTitle.setFont(new Font("SansSerif", Font.BOLD, 10));
        pairTitle.setAlignmentX(LEFT_ALIGNMENT);
        pairingBox.add(pairTitle);
        JLabel pairCopy = new JLabel("<html>Enter this code on the Deckscape<br>account page. It expires in 10 minutes.</html>");
        pairCopy.setForeground(DeckscapePalette.MUTED);
        pairCopy.setFont(new Font("SansSerif", Font.PLAIN, 10));
        pairCopy.setAlignmentX(LEFT_ALIGNMENT);
        pairingBox.add(Box.createRigidArea(new Dimension(0, 5)));
        pairingBox.add(pairCopy);
        pairingBox.add(Box.createRigidArea(new Dimension(0, 10)));
        codeField.setEditable(false);
        codeField.setHorizontalAlignment(JTextField.CENTER);
        codeField.setBackground(new Color(10, 10, 8));
        codeField.setForeground(new Color(255, 225, 139));
        codeField.setCaretColor(DeckscapePalette.GOLD);
        codeField.setFont(new Font("Monospaced", Font.BOLD, 19));
        codeField.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(DeckscapePalette.BRASS), BorderFactory.createEmptyBorder(6, 4, 6, 4)));
        codeField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        codeField.setAlignmentX(LEFT_ALIGNMENT);
        pairingBox.add(codeField);
        pairingBox.add(Box.createRigidArea(new Dimension(0, 8)));
        JPanel pairButtons = new JPanel(new GridLayout(1, 2, 7, 0));
        pairButtons.setOpaque(false);
        pairButtons.setMaximumSize(new Dimension(Integer.MAX_VALUE, 31));
        pairButtons.setAlignmentX(LEFT_ALIGNMENT);
        JButton copyButton = smallButton("Copy code");
        copyButton.addActionListener(event -> copyPairingCode());
        JButton renewButton = smallButton("New code");
        renewButton.addActionListener(event -> { if (beginPairingAction != null) beginPairingAction.run(); });
        pairButtons.add(copyButton);
        pairButtons.add(renewButton);
        pairingBox.add(pairButtons);
        body.add(pairingBox);
        body.add(Box.createRigidArea(new Dimension(0, 10)));

        JPanel summary = card();
        JLabel summaryTitle = new JLabel("SHARED PROGRESS");
        summaryTitle.setForeground(new Color(168, 143, 91));
        summaryTitle.setFont(new Font("SansSerif", Font.BOLD, 9));
        summaryTitle.setAlignmentX(LEFT_ALIGNMENT);
        statsLabel.setForeground(DeckscapePalette.PARCHMENT);
        statsLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        statsLabel.setAlignmentX(LEFT_ALIGNMENT);
        summary.add(summaryTitle);
        summary.add(Box.createRigidArea(new Dimension(0, 7)));
        summary.add(statsLabel);
        body.add(summary);
        body.add(Box.createRigidArea(new Dimension(0, 14)));

        body.add(actionButton("Collection", "Browse every synchronized card", DeckscapeDialog.Tab.COLLECTION));
        body.add(Box.createRigidArea(new Dimension(0, 8)));
        body.add(actionButton("Packs", "Open rewards on the game canvas", DeckscapeDialog.Tab.PACKS));
        body.add(Box.createRigidArea(new Dimension(0, 8)));
        body.add(actionButton("OSRS challenges", "Track verified in-game feats", DeckscapeDialog.Tab.CHALLENGES));
        body.add(Box.createVerticalGlue());

        add(body, BorderLayout.CENTER);
        refresh();
    }

    private JPanel card()
    {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(DeckscapePalette.PANEL);
        panel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(78, 69, 53)), BorderFactory.createEmptyBorder(11, 12, 11, 12)));
        panel.setAlignmentX(CENTER_ALIGNMENT);
        return panel;
    }

    private JButton actionButton(String title, String detail, DeckscapeDialog.Tab tab)
    {
        JButton button = new JButton("<html><b>" + title + "</b><br><span style='font-size:9px;color:#5b431f'>" + detail + "</span></html>");
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setFocusPainted(false);
        button.setForeground(DeckscapePalette.INK);
        button.setBackground(new Color(205, 163, 68));
        button.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(105, 74, 22), 2), BorderFactory.createEmptyBorder(7, 11, 7, 11)));
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 51));
        button.setAlignmentX(CENTER_ALIGNMENT);
        button.addActionListener(event -> showDialog(tab));
        return button;
    }

    private JButton smallButton(String text)
    {
        JButton button = new JButton(text);
        button.setFocusPainted(false);
        button.setForeground(DeckscapePalette.PARCHMENT);
        button.setBackground(new Color(55, 47, 34));
        button.setFont(new Font("SansSerif", Font.BOLD, 10));
        button.setBorder(BorderFactory.createLineBorder(new Color(119, 94, 48)));
        return button;
    }

    private void showDialog(DeckscapeDialog.Tab tab)
    {
        Frame frame = (Frame) SwingUtilities.getWindowAncestor(this);
        if (dialog == null || !dialog.isDisplayable()) dialog = new DeckscapeDialog(frame, store, openPackAction, syncAction, soundPlayer);
        dialog.showTab(tab);
        dialog.setVisible(true);
    }

    private void copyPairingCode()
    {
        String code = store.load().getPairingCode();
        if (!code.isEmpty()) Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(code), null);
    }

    public void refresh()
    {
        SwingUtilities.invokeLater(() -> {
            DeckscapeState state = store.load();
            if (!config.dataSharingConsent())
            {
                statusLabel.setText("Data sharing disabled");
                statusLabel.setForeground(new Color(190, 180, 156));
            }
            else if (state.isLinked())
            {
                statusLabel.setText("● Connected to Deckscape");
                statusLabel.setForeground(new Color(111, 204, 120));
            }
            else
            {
                statusLabel.setText("● Waiting for account link");
                statusLabel.setForeground(new Color(236, 184, 79));
            }
            String lastSync = !config.dataSharingConsent() ? "Enable sharing in the Deckscape plugin settings"
                : state.getLastSyncAt() <= 0 ? "Not synchronized yet" : "Last sync " + new SimpleDateFormat("HH:mm:ss").format(new Date(state.getLastSyncAt()));
            if (!state.getPendingEvents().isEmpty()) lastSync += " · " + state.getPendingEvents().size() + " queued";
            syncDetail.setText(lastSync);
            codeField.setText(state.getPairingCode().isEmpty() ? "CREATING…" : state.getPairingCode());
            pairingBox.setVisible(config.dataSharingConsent() && !state.isLinked());
            int packs = state.getPacks().values().stream().mapToInt(Integer::intValue).sum();
            int cards = state.getCollection().values().stream().mapToInt(Integer::intValue).sum();
            statsLabel.setText("<html><span style='color:#ffe18a'>" + state.getCoins() + "</span> Coins &nbsp;·&nbsp; "
                + "<span style='color:#ffe18a'>" + state.getStardust() + "</span> Stardust<br>"
                + "<span style='color:#ffe18a'>" + packs + "</span> packs &nbsp;·&nbsp; "
                + "<span style='color:#ffe18a'>" + cards + "</span> cards &nbsp;·&nbsp; "
                + "<span style='color:#ffe18a'>" + state.getCompletedChallenges().size() + "</span> claimed</html>");
            if (dialog != null && dialog.isVisible()) dialog.refreshView();
            revalidate();
            repaint();
        });
    }

    public void hideDialog()
    {
        SwingUtilities.invokeLater(() -> { if (dialog != null) dialog.setVisible(false); });
    }

    public void disposeDialog()
    {
        SwingUtilities.invokeLater(() -> {
            if (dialog != null) dialog.dispose();
            dialog = null;
        });
    }

    public void setHandlers(DeckscapeDialog.OpenPackAction openPackAction, Runnable syncAction, Runnable beginPairingAction, java.util.function.Consumer<Integer> soundPlayer)
    {
        this.openPackAction = openPackAction;
        this.syncAction = syncAction;
        this.beginPairingAction = beginPairingAction;
        this.soundPlayer = soundPlayer;
    }
}
