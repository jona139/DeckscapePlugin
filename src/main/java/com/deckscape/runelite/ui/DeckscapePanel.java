package com.deckscape.runelite.ui;

import com.deckscape.runelite.DeckscapeConfig;
import com.deckscape.runelite.DeckscapeStore;
import com.deckscape.runelite.model.DeckscapeCard;
import com.deckscape.runelite.model.DeckscapeState;
import com.deckscape.runelite.model.PackType;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.util.List;
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
    private java.util.function.Consumer<String> pairAction;
    private java.util.function.Consumer<Integer> soundPlayer;
    
    private final JLabel statusLabel;
    private final JTextField codeField;

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
        body.setBorder(BorderFactory.createEmptyBorder(15, 10, 15, 10));
        
        // Header
        JLabel title = new JLabel("<html><span style='color:#ffd45e'>Deck</span>scape</html>", SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(java.awt.Font.BOLD, 22f));
        title.setAlignmentX(CENTER_ALIGNMENT);
        body.add(title);
        body.add(Box.createRigidArea(new Dimension(0, 15)));
        
        // Status Box
        JPanel statusBox = new JPanel();
        statusBox.setLayout(new BoxLayout(statusBox, BoxLayout.Y_AXIS));
        statusBox.setBackground(DeckscapePalette.PANEL);
        statusBox.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(91, 82, 64)),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        statusBox.setAlignmentX(CENTER_ALIGNMENT);
        
        JLabel statusTitle = new JLabel("Sync Status:", SwingConstants.LEFT);
        statusTitle.setForeground(DeckscapePalette.MUTED);
        statusTitle.setFont(statusTitle.getFont().deriveFont(Font.BOLD, 11f));
        statusTitle.setAlignmentX(LEFT_ALIGNMENT);
        statusBox.add(statusTitle);
        
        statusLabel = new JLabel("Offline (Local Mode)");
        statusLabel.setForeground(DeckscapePalette.MUTED);
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.BOLD, 13f));
        statusLabel.setAlignmentX(LEFT_ALIGNMENT);
        statusBox.add(statusLabel);
        statusBox.add(Box.createRigidArea(new Dimension(0, 8)));
        
        JLabel codeTitle = new JLabel("Website Pairing Code:", SwingConstants.LEFT);
        codeTitle.setForeground(DeckscapePalette.MUTED);
        codeTitle.setFont(codeTitle.getFont().deriveFont(Font.BOLD, 11f));
        codeTitle.setAlignmentX(LEFT_ALIGNMENT);
        statusBox.add(codeTitle);
        
        codeField = new JTextField();
        codeField.setEditable(true);
        codeField.setBackground(DeckscapePalette.PANEL_DARK);
        codeField.setForeground(DeckscapePalette.GOLD);
        codeField.setHorizontalAlignment(JTextField.CENTER);
        codeField.setFont(new Font("Monospaced", Font.BOLD, 14));
        codeField.setBorder(BorderFactory.createLineBorder(DeckscapePalette.BRASS, 1));
        codeField.setMaximumSize(new Dimension(180, 26));
        codeField.setAlignmentX(LEFT_ALIGNMENT);
        statusBox.add(codeField);
        statusBox.add(Box.createRigidArea(new Dimension(0, 6)));
        
        JButton pairBtn = PackOpeningView.button("Pair Account");
        pairBtn.setFont(pairBtn.getFont().deriveFont(10f));
        pairBtn.setAlignmentX(LEFT_ALIGNMENT);
        pairBtn.addActionListener(e -> { if (pairAction != null) pairAction.accept(codeField.getText()); });
        statusBox.add(pairBtn);
        
        body.add(statusBox);
        body.add(Box.createRigidArea(new Dimension(0, 20)));
        
        // Action Buttons
        body.add(actionButton("See Collection", DeckscapeDialog.Tab.COLLECTION));
        body.add(Box.createRigidArea(new Dimension(0, 10)));
        body.add(actionButton("Check Packs", DeckscapeDialog.Tab.PACKS));
        body.add(Box.createRigidArea(new Dimension(0, 10)));
        body.add(actionButton("OSRS Challenges", DeckscapeDialog.Tab.CHALLENGES));
        
        add(body, BorderLayout.CENTER);
        refresh();
    }
    
    private JButton actionButton(String label, DeckscapeDialog.Tab tab)
    {
        JButton button = PackOpeningView.button(label);
        button.setMaximumSize(new Dimension(200, 36));
        button.setAlignmentX(CENTER_ALIGNMENT);
        button.addActionListener(e -> {
            Frame parentFrame = (Frame) SwingUtilities.getWindowAncestor(this);
            if (dialog == null || !dialog.isDisplayable())
            {
                dialog = new DeckscapeDialog(parentFrame, store, openPackAction, syncAction, soundPlayer);
            }
            dialog.showTab(tab);
            dialog.setVisible(true);
        });
        return button;
    }
    
    public void refresh()
    {
        SwingUtilities.invokeLater(() -> {
            DeckscapeState state = store.load();
            if (state.isLinked()) codeField.setText("");
            
            boolean hasApi = config.apiUrl() != null && !config.apiUrl().isEmpty() &&
                             config.apiKey() != null && !config.apiKey().isEmpty();
            if (hasApi && state.isLinked())
            {
                statusLabel.setText("Linked · Server authoritative");
                statusLabel.setForeground(Color.GREEN);
            }
            else
            {
                statusLabel.setText(hasApi ? "Not linked" : "Server not configured");
                statusLabel.setForeground(DeckscapePalette.MUTED);
            }
            
            if (dialog != null && dialog.isVisible())
            {
                dialog.refreshView();
            }
        });
    }
    
    public void setHandlers(DeckscapeDialog.OpenPackAction openPackAction, Runnable syncAction, java.util.function.Consumer<String> pairAction, java.util.function.Consumer<Integer> soundPlayer)
    {
        this.openPackAction = openPackAction;
        this.syncAction = syncAction;
        this.pairAction = pairAction;
        this.soundPlayer = soundPlayer;
    }
    
    public void showPackOpening(PackType type, List<DeckscapeCard> cards)
    {
        SwingUtilities.invokeLater(() -> {
            Frame parentFrame = (Frame) SwingUtilities.getWindowAncestor(this);
            if (dialog == null || !dialog.isDisplayable())
            {
                dialog = new DeckscapeDialog(parentFrame, store, openPackAction, syncAction, soundPlayer);
            }
            dialog.showPackOpening(type, cards);
            dialog.setVisible(true);
        });
    }
}
