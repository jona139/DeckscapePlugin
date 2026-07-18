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
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.LayoutManager;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.WindowConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public final class DeckscapeDialog extends JFrame
{
    public interface OpenPackAction { void open(PackType type); }
    public interface PurchasePackAction { void purchase(PackType type); }

    private static final int MIN_WIDTH = 560;
    private static final int MIN_HEIGHT = 420;

    private final DeckscapeStore store;
    private final OpenPackAction openPackAction;
    private final PurchasePackAction purchasePackAction;
    private final Runnable syncAction;
    private final java.util.function.Consumer<Integer> soundPlayer;
    private final JPanel content = new JPanel(new BorderLayout());
    private JPanel collectionGrid;
    private JLabel collectionResultCount;
    private JLabel collectionTitle;
    private JButton collectionSyncButton;

    private Tab activeTab = Tab.PACKS;
    private String collectionSearch = "";
    private String collectionFaction = "All factions";
    private String collectionRarity = "All rarities";
    private String collectionKind = "All types";
    private String collectionOwnership = "All cards";
    private String collectionSort = "Name A-Z";
    private boolean hideCompletedChallenges;

    public enum Tab { COLLECTION, PACKS, CHALLENGES }

    public DeckscapeDialog(Frame parent, DeckscapeStore store, OpenPackAction openPackAction,
                           PurchasePackAction purchasePackAction, Runnable syncAction,
                           java.util.function.Consumer<Integer> soundPlayer)
    {
        super("Deckscape Companion");
        this.store = store;
        this.openPackAction = openPackAction;
        this.purchasePackAction = purchasePackAction;
        this.syncAction = syncAction;
        this.soundPlayer = soundPlayer;

        setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
        setResizable(true);
        setSize(780, 560);
        setMinimumSize(new Dimension(MIN_WIDTH, MIN_HEIGHT));
        setLocationRelativeTo(parent);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(DeckscapePalette.PANEL_DARK);
        mainPanel.setBorder(BorderFactory.createLineBorder(DeckscapePalette.BRASS, 2));

        // Keep Deckscape navigation inside a normal Windows frame. The operating system
        // supplies dragging, edge/corner resizing, minimize, maximize, and close controls.
        JPanel titleBar = new JPanel(new BorderLayout());
        titleBar.setBackground(DeckscapePalette.PANEL);
        titleBar.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, DeckscapePalette.BRASS));

        JLabel titleLabel = new JLabel("<html><span style='color:#ffd45e; font-weight:bold;'>Deckscape Companion</span></html>");
        titleLabel.setFont(titleLabel.getFont().deriveFont(18f));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        titleBar.add(titleLabel, BorderLayout.WEST);

        // Header Tab buttons
        JPanel headerTabs = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 4));
        headerTabs.setOpaque(false);
        headerTabs.add(tabButton("Collection", Tab.COLLECTION));
        headerTabs.add(tabButton("Packs", Tab.PACKS));
        headerTabs.add(tabButton("Challenges", Tab.CHALLENGES));
        titleBar.add(headerTabs, BorderLayout.CENTER);

        mainPanel.add(titleBar, BorderLayout.NORTH);

        // Content panel
        content.setBackground(DeckscapePalette.PANEL_DARK);
        mainPanel.add(content, BorderLayout.CENTER);

        setContentPane(mainPanel);
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

    /** Refresh synchronized data without replacing focused search/filter controls. */
    public void refreshData()
    {
        if (collectionSyncButton != null)
        {
            collectionSyncButton.setEnabled(syncAction != null);
            collectionSyncButton.setText(syncAction == null ? "Sync Offline" : "Sync Account");
        }
        if (activeTab == Tab.COLLECTION && collectionGrid != null && collectionResultCount != null)
        {
            DeckscapeState state = store.load();
            if (collectionTitle != null)
            {
                collectionTitle.setText(collectionTitleText(state));
            }
            rebuildCollectionGrid(collectionGrid, collectionResultCount, state);
            return;
        }
        refreshView();
    }

    private JButton tabButton(String label, Tab tab)
    {
        JButton button = new JButton(label);
        button.setFont(button.getFont().deriveFont(Font.BOLD, 13f));
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
        collectionTitle = sectionTitle(collectionTitleText(state));
        titleRow.add(collectionTitle, BorderLayout.WEST);

        JButton syncBtn = PackOpeningView.button("Sync Offline");
        collectionSyncButton = syncBtn;
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
        page.add(Box.createRigidArea(new Dimension(0, 6)));

        JPanel filters = new JPanel();
        filters.setLayout(new BoxLayout(filters, BoxLayout.Y_AXIS));
        filters.setBackground(DeckscapePalette.PANEL);
        filters.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(91, 82, 64)),
            BorderFactory.createEmptyBorder(4, 5, 4, 5)));
        filters.setAlignmentX(Component.LEFT_ALIGNMENT);
        filters.setMaximumSize(new Dimension(Integer.MAX_VALUE, 144));

        JPanel searchRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 7, 4));
        searchRow.setOpaque(false);
        searchRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        JPanel selectRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 7, 4));
        selectRow.setOpaque(false);
        selectRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        JPanel sortRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 7, 4));
        sortRow.setOpaque(false);
        sortRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JTextField search = new JTextField(collectionSearch, 16);
        search.setToolTipText("Search card names");
        search.setPreferredSize(new Dimension(180, 28));
        search.setEditable(true);
        search.setFocusable(true);
        search.setCaretColor(DeckscapePalette.GOLD);
        styleFilter(search);

        Set<String> factions = new TreeSet<>();
        for (DeckscapeCard card : CardCatalog.all()) factions.add(card.getFaction());
        JComboBox<String> faction = new JComboBox<>();
        faction.addItem("All factions");
        for (String value : factions) faction.addItem(value);
        faction.setSelectedItem(collectionFaction);
        styleFilter(faction);

        JComboBox<String> rarity = new JComboBox<>(new String[] {
            "All rarities", "Common", "Uncommon", "Rare", "Epic", "Legendary"
        });
        rarity.setSelectedItem(collectionRarity);
        styleFilter(rarity);

        JComboBox<String> kind = new JComboBox<>(new String[] {
            "All types", "Unit", "Equipment", "Spell", "Prayer"
        });
        kind.setSelectedItem(collectionKind);
        styleFilter(kind);

        JComboBox<String> ownership = new JComboBox<>(new String[] {"All cards", "Missing"});
        ownership.setSelectedItem("Missing".equals(collectionOwnership) ? "Missing" : "All cards");
        styleFilter(ownership);

        JCheckBox onlyOwned = new JCheckBox("Only owned", "Owned".equals(collectionOwnership));
        onlyOwned.setOpaque(false);
        onlyOwned.setForeground(DeckscapePalette.PARCHMENT);
        onlyOwned.setFont(onlyOwned.getFont().deriveFont(13f));
        onlyOwned.setFocusPainted(false);
        onlyOwned.setToolTipText("Hide cards you have not collected");

        JComboBox<String> sort = new JComboBox<>(new String[] {
            "Name A-Z", "Name Z-A", "Rarity: Common first", "Rarity: Legendary first",
            "Most owned", "Least owned", "Cost: Low to high", "Cost: High to low", "Power: High to low"
        });
        sort.setSelectedItem(collectionSort);
        sort.setToolTipText("Sort collection");
        styleFilter(sort);

        JLabel searchLabel = new JLabel("Search:");
        searchLabel.setForeground(DeckscapePalette.PARCHMENT);
        searchLabel.setFont(searchLabel.getFont().deriveFont(13f));
        JLabel sortLabel = new JLabel("Sort:");
        sortLabel.setForeground(DeckscapePalette.PARCHMENT);
        sortLabel.setFont(sortLabel.getFont().deriveFont(13f));
        searchRow.add(searchLabel);
        searchRow.add(search);
        searchRow.add(onlyOwned);
        searchRow.add(ownership);
        selectRow.add(faction);
        selectRow.add(rarity);
        selectRow.add(kind);
        sortRow.add(sortLabel);
        sortRow.add(sort);
        filters.add(searchRow);
        filters.add(selectRow);
        filters.add(sortRow);
        page.add(filters);
        page.add(Box.createRigidArea(new Dimension(0, 5)));

        JLabel resultCount = new JLabel();
        resultCount.setForeground(DeckscapePalette.MUTED);
        resultCount.setFont(resultCount.getFont().deriveFont(13f));
        resultCount.setAlignmentX(Component.LEFT_ALIGNMENT);
        page.add(resultCount);
        page.add(Box.createRigidArea(new Dimension(0, 8)));

        // Aspect-locked, responsive grid: resizing/maximizing adds columns instead of stretching five cards indefinitely.
        JPanel grid = new JPanel(new CardGridLayout(142, 10, 12));
        grid.setOpaque(false);
        grid.setAlignmentX(Component.LEFT_ALIGNMENT);
        page.add(grid);

        collectionGrid = grid;
        collectionResultCount = resultCount;
        Runnable rebuild = () -> rebuildCollectionGrid(grid, resultCount, store.load());
        search.getDocument().addDocumentListener(new DocumentListener()
        {
            private void changed()
            {
                collectionSearch = search.getText().trim();
                rebuild.run();
            }

            @Override public void insertUpdate(DocumentEvent event) { changed(); }
            @Override public void removeUpdate(DocumentEvent event) { changed(); }
            @Override public void changedUpdate(DocumentEvent event) { changed(); }
        });
        faction.addActionListener(event -> { collectionFaction = String.valueOf(faction.getSelectedItem()); rebuild.run(); });
        rarity.addActionListener(event -> { collectionRarity = String.valueOf(rarity.getSelectedItem()); rebuild.run(); });
        kind.addActionListener(event -> { collectionKind = String.valueOf(kind.getSelectedItem()); rebuild.run(); });
        onlyOwned.addActionListener(event -> {
            collectionOwnership = onlyOwned.isSelected() ? "Owned" : "All cards";
            if (onlyOwned.isSelected()) ownership.setSelectedItem("All cards");
            rebuild.run();
        });
        ownership.addActionListener(event -> {
            String selected = String.valueOf(ownership.getSelectedItem());
            if ("Missing".equals(selected))
            {
                onlyOwned.setSelected(false);
                collectionOwnership = "Missing";
            }
            else if (!onlyOwned.isSelected()) collectionOwnership = "All cards";
            rebuild.run();
        });
        sort.addActionListener(event -> { collectionSort = String.valueOf(sort.getSelectedItem()); rebuild.run(); });
        rebuild.run();
        return page;
    }

    private void rebuildCollectionGrid(JPanel grid, JLabel resultCount, DeckscapeState state)
    {
        grid.removeAll();
        String search = collectionSearch.toLowerCase(Locale.ROOT);
        List<DeckscapeCard> shown = new ArrayList<>();
        for (DeckscapeCard card : CardCatalog.all())
        {
            int owned = state.getCollection().getOrDefault(card.getId(), 0);
            if (!search.isEmpty() && !card.getName().toLowerCase(Locale.ROOT).contains(search)) continue;
            if (!"All factions".equals(collectionFaction) && !collectionFaction.equals(card.getFaction())) continue;
            if (!"All rarities".equals(collectionRarity) && !collectionRarity.equalsIgnoreCase(card.getRarity().name())) continue;
            if (!"All types".equals(collectionKind) && !collectionKind.equalsIgnoreCase(card.getKind().name())) continue;
            if ("Owned".equals(collectionOwnership) && owned <= 0) continue;
            if ("Missing".equals(collectionOwnership) && owned > 0) continue;
            shown.add(card);
        }
        Comparator<DeckscapeCard> byName = Comparator.comparing(DeckscapeCard::getName, String.CASE_INSENSITIVE_ORDER);
        Comparator<DeckscapeCard> order;
        switch (collectionSort)
        {
            case "Name Z-A": order = byName.reversed(); break;
            case "Rarity: Common first": order = Comparator.comparingInt(card -> card.getRarity().ordinal()); break;
            case "Rarity: Legendary first": order = Comparator.comparingInt((DeckscapeCard card) -> card.getRarity().ordinal()).reversed(); break;
            case "Most owned": order = Comparator.comparingInt((DeckscapeCard card) -> state.getCollection().getOrDefault(card.getId(), 0)).reversed(); break;
            case "Least owned": order = Comparator.comparingInt(card -> state.getCollection().getOrDefault(card.getId(), 0)); break;
            case "Cost: Low to high": order = Comparator.comparingInt(DeckscapeCard::getCost); break;
            case "Cost: High to low": order = Comparator.comparingInt(DeckscapeCard::getCost).reversed(); break;
            case "Power: High to low": order = Comparator.comparingInt(DeckscapeCard::getPower).reversed(); break;
            default: order = byName;
        }
        shown.sort(order.thenComparing(byName));
        for (DeckscapeCard card : shown)
        {
            int owned = state.getCollection().getOrDefault(card.getId(), 0);
            boolean goldTrim = state.getSelectedGoldTrims().contains(card.getId());
            grid.add(new CardTile(card, owned, goldTrim, () -> showCardDetail(card, owned, goldTrim)));
        }
        resultCount.setText("Showing " + shown.size() + " of " + CardCatalog.all().size() + " cards");
        grid.revalidate();
        grid.repaint();
    }

    private static void styleFilter(JComponent component)
    {
        component.setForeground(DeckscapePalette.PARCHMENT);
        component.setBackground(DeckscapePalette.PANEL_DARK);
        component.setFont(component.getFont().deriveFont(13f));
        component.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(119, 94, 48)),
            BorderFactory.createEmptyBorder(3, 6, 3, 6)));
        if (component instanceof JComboBox)
        {
            JComboBox<?> combo = (JComboBox<?>) component;
            // Keep long menus above RuneLite's component hierarchy so lower rows
            // remain visible and clickable near the bottom of the window.
            combo.setLightWeightPopupEnabled(false);
            combo.setMaximumRowCount(14);
        }
    }

    /** Web-inspector-style detail view on the glass pane: big card left, details right. */
    private void showCardDetail(DeckscapeCard card, int owned, boolean goldTrim)
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

        CardTile preview = new CardTile(card, -1, goldTrim);
        preview.setPreferredSize(new Dimension(220, 330));
        sheet.add(preview, BorderLayout.WEST);

        JPanel copy = new JPanel();
        copy.setLayout(new BoxLayout(copy, BoxLayout.Y_AXIS));
        copy.setOpaque(false);

        JLabel kicker = new JLabel("CARD DETAILS");
        kicker.setForeground(new Color(168, 143, 91));
        kicker.setFont(new Font("SansSerif", Font.BOLD, 12));
        kicker.setAlignmentX(Component.LEFT_ALIGNMENT);
        copy.add(kicker);
        copy.add(Box.createRigidArea(new Dimension(0, 4)));

        JLabel name = new JLabel(card.getName());
        name.setForeground(DeckscapePalette.GOLD);
        name.setFont(new Font("Serif", Font.BOLD, 26));
        name.setAlignmentX(Component.LEFT_ALIGNMENT);
        copy.add(name);
        copy.add(Box.createRigidArea(new Dimension(0, 6)));

        JLabel meta = new JLabel(title(card.getRarity().name()) + " · " + title(card.getKind().name()) + " · " + card.getFaction());
        meta.setForeground(DeckscapePalette.MUTED);
        meta.setFont(new Font("SansSerif", Font.PLAIN, 14));
        meta.setAlignmentX(Component.LEFT_ALIGNMENT);
        copy.add(meta);

        JLabel stats = new JLabel(card.getKind() == DeckscapeCard.Kind.UNIT
            ? "Cost " + card.getCost() + " · Power " + card.getPower() + " · " + title(card.getStyle().name())
            : "Cost " + card.getCost() + " · " + title(card.getStyle().name()));
        stats.setForeground(DeckscapePalette.PARCHMENT);
        stats.setFont(new Font("SansSerif", Font.BOLD, 14));
        stats.setAlignmentX(Component.LEFT_ALIGNMENT);
        copy.add(Box.createRigidArea(new Dimension(0, 4)));
        copy.add(stats);
        copy.add(Box.createRigidArea(new Dimension(0, 12)));

        JLabel abilityTitle = new JLabel("ABILITY");
        abilityTitle.setForeground(new Color(168, 143, 91));
        abilityTitle.setFont(new Font("SansSerif", Font.BOLD, 12));
        abilityTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        copy.add(abilityTitle);
        copy.add(Box.createRigidArea(new Dimension(0, 4)));

        JLabel ability = new JLabel("<html><div style='width:230px'>" + escape(card.getText()) + "</div></html>");
        ability.setForeground(DeckscapePalette.PARCHMENT);
        ability.setFont(new Font("SansSerif", Font.PLAIN, 15));
        ability.setAlignmentX(Component.LEFT_ALIGNMENT);
        copy.add(ability);
        copy.add(Box.createVerticalGlue());

        JLabel ownedLabel = new JLabel(owned > 0 ? "In collection: ×" + owned : "Not collected yet");
        ownedLabel.setForeground(owned > 0 ? DeckscapePalette.GOLD : DeckscapePalette.MUTED);
        ownedLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
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

        JLabel testRule = new JLabel("<html><b>Pack track:</b> every " + String.format("%,d", state.getXpRewardInterval())
            + " XP gives a 50% core-pack chance. Progress: " + String.format("%,d", state.getXpTowardsPack())
            + "/" + String.format("%,d", state.getXpRewardInterval()) + " XP.<br>"
            + "<b>Currency track:</b> every " + String.format("%,d", state.getXpCurrencyRewardInterval())
            + " XP gives either 100-200 Coins (80%) or 1-3 Stardust (20%). Progress: "
            + String.format("%,d", state.getXpTowardsCurrency()) + "/"
            + String.format("%,d", state.getXpCurrencyRewardInterval()) + " XP.</html>");
        testRule.setForeground(DeckscapePalette.MUTED);
        testRule.setFont(testRule.getFont().deriveFont(13f));
        testRule.setBorder(BorderFactory.createEmptyBorder(2, 2, 8, 2));
        testRule.setAlignmentX(Component.LEFT_ALIGNMENT);
        page.add(testRule);
        page.add(Box.createRigidArea(new Dimension(0, 10)));

        JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setOpaque(false);
        listPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        for (PackType type : PackType.inventoryTypes())
        {
            JPanel row = new JPanel(new BorderLayout(15, 0));
            row.setBackground(DeckscapePalette.PANEL);
            row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(91, 82, 64)),
                BorderFactory.createEmptyBorder(12, 16, 12, 16)
            ));

            JLabel copy = new JLabel("<html><b>" + type.getDisplayName() + "</b><br><span style='color:#b9ae91'>Quantity: " + state.packCount(type) + "</span></html>");
            copy.setForeground(DeckscapePalette.PARCHMENT);
            copy.setFont(copy.getFont().deriveFont(15f));

            JLabel icon = new JLabel(new ImageIcon(CardPainter.packImage(type, 40, 58)));

            row.add(icon, BorderLayout.WEST);
            row.add(copy, BorderLayout.CENTER);

            JButton buy = PackOpeningView.button("Buy · " + String.format("%,d", type.getPrice()));
            buy.setPreferredSize(new Dimension(108, 32));
            buy.setEnabled(purchasePackAction != null && state.isLinked() && state.getCoins() >= type.getPrice());
            if (state.getCoins() < type.getPrice())
                buy.setToolTipText("You need " + String.format("%,d", type.getPrice()) + " Coins");
            buy.addActionListener(event ->
            {
                if (purchasePackAction == null) return;
                buy.setEnabled(false);
                buy.setText("Buying…");
                purchasePackAction.purchase(type);
            });

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
            JPanel actions = new JPanel(new GridLayout(1, 2, 8, 0));
            actions.setOpaque(false);
            actions.add(buy);
            actions.add(open);
            row.add(actions, BorderLayout.EAST);

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
        JCheckBox hideCompleted = new JCheckBox("Hide completed challenges", hideCompletedChallenges);
        hideCompleted.setOpaque(false);
        hideCompleted.setForeground(DeckscapePalette.PARCHMENT);
        hideCompleted.setFont(hideCompleted.getFont().deriveFont(14f));
        hideCompleted.setFocusPainted(false);
        hideCompleted.setAlignmentX(Component.LEFT_ALIGNMENT);
        hideCompleted.addActionListener(event -> {
            hideCompletedChallenges = hideCompleted.isSelected();
            refreshView();
        });
        page.add(hideCompleted);
        page.add(Box.createRigidArea(new Dimension(0, 10)));

        int visibleChallenges = 0;
        for (RuneLiteChallenge challenge : RuneLiteChallenge.ALL)
        {
            int progress = Math.min(challenge.getTarget(), state.getChallengeProgress().getOrDefault(challenge.getMetric(), 0));
            boolean verified = progress >= challenge.getTarget();
            boolean claimed = state.getCompletedChallenges().contains(challenge.getTierKey());
            if (hideCompletedChallenges && claimed) continue;
            visibleChallenges++;
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
            title.setFont(title.getFont().deriveFont(Font.BOLD, 17f));
            task.add(title);
            task.add(Box.createRigidArea(new Dimension(0, 5)));

            String progressLabel = challenge.getTarget() > 1
                ? "<br><b>Progress:</b> " + String.format("%,d", progress) + "/" + String.format("%,d", challenge.getTarget())
                : "";
            JLabel description = new JLabel("<html>" + challenge.getDescription() + progressLabel
                + "<br><b>Reward:</b> " + challenge.getReward() + "<br><b>Status:</b> " + status + "</html>");
            description.setForeground(DeckscapePalette.MUTED);
            description.setFont(description.getFont().deriveFont(14f));
            task.add(description);
            page.add(task);
            page.add(Box.createRigidArea(new Dimension(0, 8)));
        }

        if (visibleChallenges == 0)
        {
            JLabel empty = new JLabel("All challenges are complete. Disable the filter to review them.");
            empty.setForeground(DeckscapePalette.MUTED);
            empty.setFont(empty.getFont().deriveFont(14f));
            empty.setAlignmentX(Component.LEFT_ALIGNMENT);
            page.add(empty);
            page.add(Box.createRigidArea(new Dimension(0, 10)));
        }

        JLabel note = new JLabel("<html>Only verified activity on normal main-game worlds counts. "
            + "PvP, high-risk, bounty, Leagues, Deadman, Last Man Standing, PvP Arena, beta, tournament, speedrunning, and other no-save worlds are excluded. "
            + "Claim completed rewards from Deckscape's Challenges page.</html>");
        note.setForeground(new Color(143, 132, 108));
        note.setFont(note.getFont().deriveFont(13f));
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
        label.setFont(label.getFont().deriveFont(Font.BOLD, 20f));
        label.setBorder(BorderFactory.createEmptyBorder(0, 2, 4, 2));
        label.setAlignmentX(LEFT_ALIGNMENT);
        return label;
    }

    private static String title(String value)
    {
        String lower = value.toLowerCase();
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    private static String collectionTitleText(DeckscapeState state)
    {
        return "Collection · " + state.getCollection().values().stream().mapToInt(Integer::intValue).sum() + " cards";
    }

    private static String escape(String value)
    {
        return value == null ? "" : value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    /** Responsive column count with card tiles locked to the 2:3 aspect ratio. */
    private static final class CardGridLayout implements LayoutManager
    {
        private final int targetWidth;
        private final int hgap;
        private final int vgap;

        CardGridLayout(int targetWidth, int hgap, int vgap)
        {
            this.targetWidth = targetWidth;
            this.hgap = hgap;
            this.vgap = vgap;
        }

        @Override public void addLayoutComponent(String name, Component component) { }
        @Override public void removeLayoutComponent(Component component) { }

        @Override
        public Dimension preferredLayoutSize(Container parent)
        {
            int width = parent.getWidth() > 0 ? parent.getWidth() : 5 * targetWidth + 4 * hgap;
            int columns = columns(width);
            int tileW = (width - (columns - 1) * hgap) / columns;
            int tileH = CardTile.collectionHeightForWidth(tileW);
            int rows = (parent.getComponentCount() + columns - 1) / columns;
            return new Dimension(width, rows * tileH + Math.max(0, rows - 1) * vgap);
        }

        @Override
        public Dimension minimumLayoutSize(Container parent)
        {
            return new Dimension(2 * 84 + hgap, CardTile.collectionHeightForWidth(84));
        }

        @Override
        public void layoutContainer(Container parent)
        {
            int width = parent.getWidth();
            int columns = columns(width);
            int tileW = (width - (columns - 1) * hgap) / columns;
            int tileH = CardTile.collectionHeightForWidth(tileW);
            for (int i = 0; i < parent.getComponentCount(); i++)
            {
                int col = i % columns;
                int row = i / columns;
                parent.getComponent(i).setBounds(col * (tileW + hgap), row * (tileH + vgap), tileW, tileH);
            }
        }

        private int columns(int width)
        {
            return Math.max(2, Math.min(10, Math.max(1, (width + hgap) / (targetWidth + hgap))));
        }
    }
}
