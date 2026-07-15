package com.deckscape.runelite.ui;

import com.deckscape.runelite.DeckscapeStore;
import com.deckscape.runelite.model.CardCatalog;
import com.deckscape.runelite.model.DeckscapeCard;
import com.deckscape.runelite.model.DeckscapeState;
import com.deckscape.runelite.model.PackType;
import com.deckscape.runelite.challenge.RuneLiteChallenge;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.LayoutManager;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

public final class DeckscapeDialog extends JDialog
{
    public interface OpenPackAction { void open(PackType type); }

    private static final int MIN_WIDTH = 560;
    private static final int MIN_HEIGHT = 420;

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
        setSize(780, 560);
        setMinimumSize(new Dimension(MIN_WIDTH, MIN_HEIGHT));
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
        installResizeGrip();
    }

    /** Undecorated dialogs have no OS resize border; this grip in the bottom-right provides one. */
    private void installResizeGrip()
    {
        JComponent grip = new JComponent()
        {
            @Override
            protected void paintComponent(Graphics graphics)
            {
                Graphics2D g = (Graphics2D) graphics.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(DeckscapePalette.BRASS);
                int size = getWidth();
                for (int i = 1; i <= 3; i++)
                {
                    int offset = i * 4;
                    g.drawLine(size - offset, size - 2, size - 2, size - offset);
                }
                g.dispose();
            }
        };
        grip.setSize(18, 18);
        grip.setCursor(Cursor.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR));
        grip.setToolTipText("Drag to resize");
        MouseAdapter resizer = new MouseAdapter()
        {
            private Point start;
            private Dimension startSize;

            @Override
            public void mousePressed(MouseEvent event)
            {
                start = event.getLocationOnScreen();
                startSize = getSize();
            }

            @Override
            public void mouseDragged(MouseEvent event)
            {
                Point now = event.getLocationOnScreen();
                int width = Math.max(MIN_WIDTH, startSize.width + now.x - start.x);
                int height = Math.max(MIN_HEIGHT, startSize.height + now.y - start.y);
                setSize(width, height);
                revalidate();
            }
        };
        grip.addMouseListener(resizer);
        grip.addMouseMotionListener(resizer);
        getLayeredPane().add(grip, JLayeredPane.DRAG_LAYER);
        addComponentListener(new ComponentAdapter()
        {
            @Override
            public void componentResized(ComponentEvent event)
            {
                grip.setLocation(getLayeredPane().getWidth() - grip.getWidth() - 3, getLayeredPane().getHeight() - grip.getHeight() - 3);
            }
        });
        grip.setLocation(getLayeredPane().getWidth() - grip.getWidth() - 3, getLayeredPane().getHeight() - grip.getHeight() - 3);
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
        titleRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        titleRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        page.add(titleRow);
        page.add(Box.createRigidArea(new Dimension(0, 10)));

        // Aspect-locked grid: tiles keep the 2:3 card shape at any window width.
        JPanel grid = new JPanel(new CardGridLayout(5, 10, 12));
        grid.setOpaque(false);
        grid.setAlignmentX(Component.LEFT_ALIGNMENT);
        for (DeckscapeCard card : CardCatalog.all())
        {
            int owned = state.getCollection().getOrDefault(card.getId(), 0);
            grid.add(new CardTile(card, owned, () -> showCardDetail(card, owned)));
        }
        page.add(grid);
        return page;
    }

    /** Web-inspector-style detail view on the glass pane: big card left, details right. */
    private void showCardDetail(DeckscapeCard card, int owned)
    {
        JPanel backdrop = new JPanel(new GridBagLayout())
        {
            @Override
            protected void paintComponent(Graphics g)
            {
                g.setColor(new Color(8, 6, 4, 205));
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        backdrop.setOpaque(false);
        backdrop.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mousePressed(MouseEvent event) { hideCardDetail(); }
        });

        JPanel sheet = new JPanel(new BorderLayout(18, 0));
        sheet.setBackground(DeckscapePalette.PANEL);
        sheet.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(DeckscapePalette.BRASS, 2),
            BorderFactory.createEmptyBorder(16, 18, 16, 18)));
        // Swallow clicks so only the backdrop closes the inspector.
        sheet.addMouseListener(new MouseAdapter() { });

        CardTile preview = new CardTile(card, -1);
        preview.setPreferredSize(new Dimension(220, 330));
        sheet.add(preview, BorderLayout.WEST);

        JPanel copy = new JPanel();
        copy.setLayout(new BoxLayout(copy, BoxLayout.Y_AXIS));
        copy.setOpaque(false);

        JLabel kicker = new JLabel("CARD DETAILS");
        kicker.setForeground(new Color(168, 143, 91));
        kicker.setFont(new Font("SansSerif", Font.BOLD, 10));
        kicker.setAlignmentX(Component.LEFT_ALIGNMENT);
        copy.add(kicker);
        copy.add(Box.createRigidArea(new Dimension(0, 4)));

        JLabel name = new JLabel(card.getName());
        name.setForeground(DeckscapePalette.GOLD);
        name.setFont(new Font("Serif", Font.BOLD, 24));
        name.setAlignmentX(Component.LEFT_ALIGNMENT);
        copy.add(name);
        copy.add(Box.createRigidArea(new Dimension(0, 6)));

        JLabel meta = new JLabel(title(card.getRarity().name()) + " · " + title(card.getKind().name()) + " · " + card.getFaction());
        meta.setForeground(DeckscapePalette.MUTED);
        meta.setFont(new Font("SansSerif", Font.PLAIN, 12));
        meta.setAlignmentX(Component.LEFT_ALIGNMENT);
        copy.add(meta);

        JLabel stats = new JLabel(card.getKind() == DeckscapeCard.Kind.UNIT
            ? "Cost " + card.getCost() + " · Power " + card.getPower() + " · " + title(card.getStyle().name())
            : "Cost " + card.getCost() + " · " + title(card.getStyle().name()));
        stats.setForeground(DeckscapePalette.PARCHMENT);
        stats.setFont(new Font("SansSerif", Font.BOLD, 12));
        stats.setAlignmentX(Component.LEFT_ALIGNMENT);
        copy.add(Box.createRigidArea(new Dimension(0, 4)));
        copy.add(stats);
        copy.add(Box.createRigidArea(new Dimension(0, 12)));

        JLabel abilityTitle = new JLabel("ABILITY");
        abilityTitle.setForeground(new Color(168, 143, 91));
        abilityTitle.setFont(new Font("SansSerif", Font.BOLD, 10));
        abilityTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        copy.add(abilityTitle);
        copy.add(Box.createRigidArea(new Dimension(0, 4)));

        JLabel ability = new JLabel("<html><div style='width:230px'>" + escape(card.getText()) + "</div></html>");
        ability.setForeground(DeckscapePalette.PARCHMENT);
        ability.setFont(new Font("SansSerif", Font.PLAIN, 13));
        ability.setAlignmentX(Component.LEFT_ALIGNMENT);
        copy.add(ability);
        copy.add(Box.createVerticalGlue());

        JLabel ownedLabel = new JLabel(owned > 0 ? "In collection: ×" + owned : "Not collected yet");
        ownedLabel.setForeground(owned > 0 ? DeckscapePalette.GOLD : DeckscapePalette.MUTED);
        ownedLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        ownedLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        copy.add(Box.createRigidArea(new Dimension(0, 12)));
        copy.add(ownedLabel);

        JPanel right = new JPanel(new BorderLayout());
        right.setOpaque(false);
        right.add(copy, BorderLayout.CENTER);
        JButton close = new JButton("×");
        close.setFont(new Font("Monospaced", Font.BOLD, 16));
        close.setForeground(DeckscapePalette.GOLD);
        close.setBackground(DeckscapePalette.PANEL);
        close.setFocusPainted(false);
        close.setBorder(BorderFactory.createEmptyBorder(2, 10, 2, 10));
        close.addActionListener(event -> hideCardDetail());
        JPanel closeRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        closeRow.setOpaque(false);
        closeRow.add(close);
        right.add(closeRow, BorderLayout.NORTH);
        sheet.add(right, BorderLayout.CENTER);

        backdrop.add(sheet, new GridBagConstraints());
        setGlassPane(backdrop);
        backdrop.setVisible(true);
        if (soundPlayer != null) soundPlayer.accept(2266);
    }

    private void hideCardDetail()
    {
        getGlassPane().setVisible(false);
    }

    private JPanel packPage()
    {
        DeckscapeState state = store.load();
        JPanel page = page();

        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);
        titleRow.add(sectionTitle("Packs Inventory"), BorderLayout.WEST);
        titleRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        titleRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        page.add(titleRow);

        JLabel testRule = new JLabel("<html>Every <b>" + String.format("%,d", state.getXpRewardInterval())
            + " XP</b> rolls for a pack, Coins, or Stardust. Progress: "
            + String.format("%,d", state.getXpTowardsPack()) + "/" + String.format("%,d", state.getXpRewardInterval()) + " XP</html>");
        testRule.setForeground(DeckscapePalette.MUTED);
        testRule.setBorder(BorderFactory.createEmptyBorder(2, 2, 8, 2));
        testRule.setAlignmentX(Component.LEFT_ALIGNMENT);
        page.add(testRule);
        page.add(Box.createRigidArea(new Dimension(0, 10)));

        JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setOpaque(false);
        listPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        PackType[] visiblePacks = {
            PackType.GENERAL, PackType.COMBAT, PackType.SKILLING,
            PackType.FACTION_MISTHALIN, PackType.FACTION_VARLAMORE, PackType.FACTION_KANDARIN,
        };
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

            JLabel icon = new JLabel(new ImageIcon(CardPainter.packImage(type, 40, 58)));

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
        JPanel page = page();
        page.add(sectionTitle("OSRS gold-frame challenges"));
        page.add(Box.createRigidArea(new Dimension(0, 10)));

        for (RuneLiteChallenge challenge : RuneLiteChallenge.ALL)
        {
            int progress = Math.min(challenge.getTarget(), state.getChallengeProgress().getOrDefault(challenge.getMetric(), 0));
            boolean verified = progress >= challenge.getTarget();
            boolean claimed = state.getCompletedChallenges().contains(challenge.getTierKey());
            JPanel task = new JPanel();
            task.setLayout(new BoxLayout(task, BoxLayout.Y_AXIS));
            task.setBackground(claimed ? new Color(50, 43, 24) : DeckscapePalette.PANEL);
            task.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(verified ? DeckscapePalette.BRASS : new Color(91, 82, 64), verified ? 2 : 1),
                BorderFactory.createEmptyBorder(12, 14, 12, 14)));
            task.setAlignmentX(Component.LEFT_ALIGNMENT);

            String status = claimed ? "Claimed" : verified ? "Ready to claim on website" : "In progress";
            JLabel title = new JLabel((claimed ? "✓ " : verified ? "◆ " : "○ ") + challenge.getTitle());
            title.setForeground(verified ? DeckscapePalette.GOLD : DeckscapePalette.PARCHMENT);
            title.setFont(title.getFont().deriveFont(Font.BOLD, 15f));
            task.add(title);
            task.add(Box.createRigidArea(new Dimension(0, 5)));

            String progressLabel = challenge.getTarget() > 1
                ? "<br><b>Progress:</b> " + String.format("%,d", progress) + "/" + String.format("%,d", challenge.getTarget())
                : "";
            JLabel description = new JLabel("<html>" + challenge.getDescription() + progressLabel
                + "<br><b>Reward:</b> " + challenge.getReward() + "<br><b>Status:</b> " + status + "</html>");
            description.setForeground(DeckscapePalette.MUTED);
            description.setFont(description.getFont().deriveFont(12f));
            task.add(description);
            page.add(task);
            page.add(Box.createRigidArea(new Dimension(0, 8)));
        }

        JLabel note = new JLabel("<html>Only verified activity on normal main-game worlds counts. "
            + "PvP, high-risk, bounty, Leagues, Deadman, Last Man Standing, PvP Arena, beta, tournament, speedrunning, and other no-save worlds are excluded. "
            + "Claim completed rewards from Deckscape's Challenges page.</html>");
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

    private static String title(String value)
    {
        String lower = value.toLowerCase();
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    private static String escape(String value)
    {
        return value == null ? "" : value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    /** Fixed column count, tile height locked to 1.5x tile width, wrapping rows. */
    private static final class CardGridLayout implements LayoutManager
    {
        private final int columns;
        private final int hgap;
        private final int vgap;

        CardGridLayout(int columns, int hgap, int vgap)
        {
            this.columns = columns;
            this.hgap = hgap;
            this.vgap = vgap;
        }

        @Override public void addLayoutComponent(String name, Component component) { }
        @Override public void removeLayoutComponent(Component component) { }

        @Override
        public Dimension preferredLayoutSize(Container parent)
        {
            int width = parent.getWidth() > 0 ? parent.getWidth() : columns * 124 + (columns - 1) * hgap;
            int tileW = (width - (columns - 1) * hgap) / columns;
            int tileH = Math.round(tileW * 1.5f);
            int rows = (parent.getComponentCount() + columns - 1) / columns;
            return new Dimension(width, rows * tileH + Math.max(0, rows - 1) * vgap);
        }

        @Override
        public Dimension minimumLayoutSize(Container parent)
        {
            return new Dimension(columns * 84, 126);
        }

        @Override
        public void layoutContainer(Container parent)
        {
            int width = parent.getWidth();
            int tileW = (width - (columns - 1) * hgap) / columns;
            int tileH = Math.round(tileW * 1.5f);
            for (int i = 0; i < parent.getComponentCount(); i++)
            {
                int col = i % columns;
                int row = i / columns;
                parent.getComponent(i).setBounds(col * (tileW + hgap), row * (tileH + vgap), tileW, tileH);
            }
        }
    }
}
