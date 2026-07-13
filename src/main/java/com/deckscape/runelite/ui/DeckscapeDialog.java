package com.deckscape.runelite.ui;

import com.deckscape.runelite.DeckscapeStore;
import com.deckscape.runelite.model.CardCatalog;
import com.deckscape.runelite.model.DeckscapeCard;
import com.deckscape.runelite.model.DeckscapeState;
import com.deckscape.runelite.model.PackType;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.ImageIcon;
import net.runelite.client.util.ImageUtil;

public final class DeckscapeDialog extends JDialog
{
    public interface OpenPackAction { void open(PackType type); }

    private final DeckscapeStore store;
    private final OpenPackAction openPackAction;
    private final Runnable syncAction;
    private final java.util.function.Consumer<Integer> soundPlayer;
    private final JPanel content = new JPanel(new BorderLayout());
    
    private Point initialClick;
    private Tab activeTab = Tab.PACKS;

    public enum Tab { COLLECTION, PACKS, CHALLENGES }

    public DeckscapeDialog(Frame parent, DeckscapeStore store, OpenPackAction openPackAction, Runnable syncAction, java.util.function.Consumer<Integer> soundPlayer)
    {
        super(parent, "Deckscape Companion", false);
        this.store = store;
        this.openPackAction = openPackAction;
        this.syncAction = syncAction;
        this.soundPlayer = soundPlayer;

        setUndecorated(true);
        setSize(780, 520);
        setLocationRelativeTo(parent);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(DeckscapePalette.PANEL_DARK);
        mainPanel.setBorder(BorderFactory.createLineBorder(DeckscapePalette.BRASS, 3));

        // Title Bar / Dragging area
        JPanel titleBar = new JPanel(new BorderLayout());
        titleBar.setBackground(DeckscapePalette.PANEL);
        titleBar.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, DeckscapePalette.BRASS));

        MouseAdapter dragListener = new MouseAdapter()
        {
            @Override
            public void mousePressed(MouseEvent e)
            {
                initialClick = e.getPoint();
            }

            @Override
            public void mouseDragged(MouseEvent e)
            {
                int thisX = getLocation().x;
                int thisY = getLocation().y;
                int xMoved = e.getX() - initialClick.x;
                int yMoved = e.getY() - initialClick.y;
                setLocation(thisX + xMoved, thisY + yMoved);
            }
        };
        titleBar.addMouseListener(dragListener);
        titleBar.addMouseMotionListener(dragListener);

        JLabel titleLabel = new JLabel("<html><span style='color:#ffd45e; font-weight:bold;'>Deckscape Companion</span></html>");
        titleLabel.setFont(titleLabel.getFont().deriveFont(16f));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        titleBar.add(titleLabel, BorderLayout.WEST);

        // Header Tab buttons
        JPanel headerTabs = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 4));
        headerTabs.setOpaque(false);
        headerTabs.add(tabButton("Collection", Tab.COLLECTION));
        headerTabs.add(tabButton("Packs", Tab.PACKS));
        headerTabs.add(tabButton("Challenges", Tab.CHALLENGES));
        titleBar.add(headerTabs, BorderLayout.CENTER);

        // Close button
        JButton closeButton = new JButton("X");
        closeButton.setFont(new Font("Monospaced", Font.BOLD, 14));
        closeButton.setForeground(DeckscapePalette.GOLD);
        closeButton.setBackground(DeckscapePalette.PANEL);
        closeButton.setFocusPainted(false);
        closeButton.setBorder(BorderFactory.createEmptyBorder(6, 16, 6, 16));
        closeButton.addActionListener(e -> setVisible(false));
        titleBar.add(closeButton, BorderLayout.EAST);

        mainPanel.add(titleBar, BorderLayout.NORTH);

        // Content panel
        content.setBackground(DeckscapePalette.PANEL_DARK);
        mainPanel.add(content, BorderLayout.CENTER);

        add(mainPanel);
    }

    public void showTab(Tab tab)
    {
        this.activeTab = tab;
        refreshView();
    }

    public void refreshView()
    {
        content.removeAll();
        JPanel page = activeTab == Tab.COLLECTION ? collectionPage() : activeTab == Tab.CHALLENGES ? challengePage() : packPage();
        JScrollPane scroll = new JScrollPane(page);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        content.add(scroll, BorderLayout.CENTER);
        content.revalidate();
        content.repaint();
    }

    private JButton tabButton(String label, Tab tab)
    {
        JButton button = new JButton(label);
        button.setFont(button.getFont().deriveFont(Font.BOLD, 11f));
        button.setFocusPainted(false);
        button.setForeground(DeckscapePalette.PARCHMENT);
        button.setBackground(DeckscapePalette.PANEL_DARK);
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(DeckscapePalette.BRASS, 1),
            BorderFactory.createEmptyBorder(4, 10, 4, 10)
        ));
        button.addActionListener(event -> showTab(tab));
        return button;
    }

    private JPanel collectionPage()
    {
        DeckscapeState state = store.load();
        JPanel page = page();
        
        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);
        titleRow.add(sectionTitle("Collection · " + state.getCollection().values().stream().mapToInt(Integer::intValue).sum() + " cards"), BorderLayout.WEST);
        
        JButton syncBtn = PackOpeningView.button("Sync Offline");
        if (syncAction != null)
        {
            syncBtn.setText("Sync Account");
            syncBtn.addActionListener(e -> {
                syncBtn.setEnabled(false);
                syncBtn.setText("Syncing...");
                syncAction.run();
            });
        }
        else
        {
            syncBtn.setEnabled(false);
        }
        titleRow.add(syncBtn, BorderLayout.EAST);
        page.add(titleRow);
        page.add(Box.createRigidArea(new Dimension(0, 10)));

        // Wider grid layout for main screen display (5 columns)
        JPanel grid = new JPanel(new GridLayout(0, 5, 8, 10));
        grid.setOpaque(false);
        for (DeckscapeCard card : CardCatalog.all())
        {
            grid.add(new CardTile(card, state.getCollection().getOrDefault(card.getId(), 0)));
        }
        page.add(grid);
        return page;
    }

    private JPanel packPage()
    {
        DeckscapeState state = store.load();
        JPanel page = page();
        
        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);
        titleRow.add(sectionTitle("Packs Inventory"), BorderLayout.WEST);
        page.add(titleRow);

        JLabel testRule = new JLabel("<html>Testing rule: every <b>100 XP</b> rolls for a random pack. Progress: " + state.getXpTowardsPack() + "/100 XP</html>");
        testRule.setForeground(DeckscapePalette.MUTED);
        testRule.setBorder(BorderFactory.createEmptyBorder(2, 2, 8, 2));
        page.add(testRule);
        page.add(Box.createRigidArea(new Dimension(0, 10)));

        JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setOpaque(false);

        PackType[] visiblePacks = { PackType.GENERAL, PackType.COMBAT, PackType.SKILLING, PackType.FACTION_MISTHALIN };
        for (PackType type : visiblePacks)
        {
            JPanel row = new JPanel(new BorderLayout(15, 0));
            row.setBackground(DeckscapePalette.PANEL);
            row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(91, 82, 64)),
                BorderFactory.createEmptyBorder(12, 16, 12, 16)
            ));
            
            JLabel copy = new JLabel("<html><b>" + type.getDisplayName() + "</b><br><span style='color:#b9ae91'>Quantity: " + state.packCount(type) + "</span></html>");
            copy.setForeground(DeckscapePalette.PARCHMENT);
            copy.setFont(copy.getFont().deriveFont(13f));
            
            BufferedImage packImage = ImageUtil.loadImageResource(getClass(), packIcon(type));
            JLabel icon = new JLabel(new ImageIcon(ImageUtil.resizeImage(packImage, 42, 48)));
            
            row.add(icon, BorderLayout.WEST);
            row.add(copy, BorderLayout.CENTER);
            
            JButton open = PackOpeningView.button("Open Pack");
            open.setPreferredSize(new Dimension(100, 32));
            open.setEnabled(state.packCount(type) > 0);
            open.addActionListener(event ->
            {
                if (openPackAction == null) return;
                open.setEnabled(false);
                open.setText("Opening…");
                setVisible(false);
                openPackAction.open(type);
            });
            row.add(open, BorderLayout.EAST);
            
            listPanel.add(row);
            listPanel.add(Box.createRigidArea(new Dimension(0, 8)));
        }
        page.add(listPanel);
        return page;
    }

    private JPanel challengePage()
    {
        DeckscapeState state = store.load();
        boolean done = state.getCompletedChallenges().contains("elemental_strike_max_hit:gold");
        JPanel page = page();
        page.add(sectionTitle("OSRS linked challenges"));
        page.add(Box.createRigidArea(new Dimension(0, 10)));

        JPanel task = new JPanel();
        task.setLayout(new BoxLayout(task, BoxLayout.Y_AXIS));
        task.setBackground(done ? new Color(50, 43, 24) : DeckscapePalette.PANEL);
        task.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(done ? DeckscapePalette.BRASS : new Color(91, 82, 64), 2),
            BorderFactory.createEmptyBorder(16, 16, 16, 16)
        ));
        
        JLabel title = new JLabel(done ? "✓ Perfectly Elemental" : "○ Perfectly Elemental");
        title.setForeground(done ? DeckscapePalette.GOLD : DeckscapePalette.PARCHMENT);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));
        task.add(title);
        task.add(Box.createRigidArea(new Dimension(0, 8)));
        
        JLabel description = new JLabel("<html>Deal your maximum possible hit with Wind, Water, Earth, or Fire Strike.<br><br><b>Reward:</b> Elemental Strike gold trim card cosmetics</html>");
        description.setForeground(DeckscapePalette.MUTED);
        description.setFont(description.getFont().deriveFont(13f));
        task.add(description);
        
        page.add(task);
        page.add(Box.createRigidArea(new Dimension(0, 12)));

        for (java.util.Map.Entry<String, Integer> entry : state.getChallengeProgress().entrySet())
        {
            JLabel progress = new JLabel(entry.getKey().replace('_', ' ') + ": " + entry.getValue());
            progress.setForeground(DeckscapePalette.PARCHMENT);
            progress.setFont(progress.getFont().deriveFont(12f));
            page.add(progress);
            page.add(Box.createRigidArea(new Dimension(0, 4)));
        }
        
        JLabel note = new JLabel("<html>All values are read from your linked Deckscape account. Local state is a display cache only.</html>");
        note.setForeground(new Color(143, 132, 108));
        note.setFont(note.getFont().deriveFont(11f));
        page.add(note);
        return page;
    }

    public void showPackOpening(PackType type, List<DeckscapeCard> cards)
    {
        content.removeAll();
        // Wider layout for pack opening
        content.add(new PackOpeningView(type.getDisplayName(), cards, soundPlayer, () -> showTab(Tab.PACKS)), BorderLayout.CENTER);
        content.revalidate();
        content.repaint();
    }

    private JPanel page()
    {
        JPanel page = new JPanel();
        page.setLayout(new BoxLayout(page, BoxLayout.Y_AXIS));
        page.setBackground(DeckscapePalette.PANEL_DARK);
        page.setBorder(BorderFactory.createEmptyBorder(14, 16, 18, 16));
        return page;
    }

    private JLabel sectionTitle(String value)
    {
        JLabel label = new JLabel(value);
        label.setForeground(DeckscapePalette.GOLD);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 18f));
        label.setBorder(BorderFactory.createEmptyBorder(0, 2, 4, 2));
        label.setAlignmentX(LEFT_ALIGNMENT);
        return label;
    }

    private String packIcon(PackType type)
    {
        switch (type)
        {
            case COMBAT: return "/com/deckscape/runelite/pack_combat.png";
            case SKILLING: return "/com/deckscape/runelite/pack_skilling.png";
            case FACTION_MISTHALIN: return "/com/deckscape/runelite/pack_misthalin.png";
            default: return "/com/deckscape/runelite/pack_general.png";
        }
    }
}
