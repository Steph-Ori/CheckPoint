/**
 * Stephanie Ortiz
 * CEN 3024 - Software Development 1
 * November 9th, 2025
 * CheckPointSwing.java
 */

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.*;
import javax.swing.text.*;
import java.awt.*;
import java.io.File;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * CheckPointSwing — desktop GUI for the CheckPoint app (dark theme + mini dashboard + polished report).
 *
 * <p><b>What it does:</b> Lets the user pick an SQLite file, then perform full CRUD on
 * {@link Game} rows through {@link DbLibrary}. On top of that it now includes:</p>
 *
 * <ul>
 *     <li>Main grid of games with sorting.</li>
 *     <li>Game Details form to add / update / delete rows.</li>
 *     <li>Import-from-text helper.</li>
 *     <li>“CheckPoint Report” dialog with backlog stats.</li>
 *     <li>Search bar + search-mode dropdown (All / Name / Platform / Status / Ownership).</li>
 *     <li>A tiny live dashboard (Total / Unplayed / Playing / Beaten).</li>
 *     <li>Status filter “chips/buttons” (Unplayed / Playing / Beaten) sitting above Game Details.</li>
 * </ul>
 *
 * <p><b>Role in the system:</b> Presentation layer. This class owns the Swing widgets,
 * input validation, and calls into {@code DbLibrary} for persistence.</p>
 *
 * <p><b>Usage:</b> Run {@link #main(String[])} or construct directly. The constructor
 * builds the UI, asks for a DB on startup, and wires all actions.</p>
 */
public class CheckPointSwing extends JFrame {

    // == Domain ==
    private DbLibrary library; // null until user connects

    // == Root + Table ==
    private JPanel root;
    private JTable table;
    private GameTableModel tableModel;
    private TableRowSorter<GameTableModel> sorter;

    // == Form fields ==
    private JTextField idField;
    private JTextField nameField;
    private JTextField platformField;
    private JComboBox<Game.Status> statusBox;
    private JTextField priorityField;
    private JComboBox<Game.Ownership> ownershipBox;

    // == Search / filter ==
    private JTextField searchField;
    private JComboBox<String> searchModeBox; // All / Name / Platform / Status / Ownership

    // quick status filter chips (live above Game Details)
    private JToggleButton unplayedChip;
    private JToggleButton playingChip;
    private JToggleButton beatenChip;
    private Game.Status chipStatusFilter; // null = all statuses

    // == Mini dashboard labels (numbers / percents) ==
    private JLabel totalBigLabel;
    private JLabel unplayedBigLabel;
    private JLabel playingBigLabel;
    private JLabel beatenBigLabel;

    private JLabel totalSubLabel;
    private JLabel unplayedSubLabel;
    private JLabel playingSubLabel;
    private JLabel beatenSubLabel;

    // == Buttons ==
    private JButton connectBtn, addBtn, updateBtn, deleteBtn, clearBtn, importBtn, reportBtn;

    /**
     * Build the Swing app window, wire actions, and prompt for a database.
     * Keeps the UI disabled until a DB is connected.
     */
    public CheckPointSwing() {
        super("CheckPoint (Swing, Database)");

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1100, 620));

        // Root with gradient background
        root = new JPanel(new BorderLayout(12, 12)) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(new GradientPaint(0, 0, new Color(22, 23, 28),
                        0, getHeight(), new Color(32, 34, 41)));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        root.setBorder(new EmptyBorder(14, 14, 14, 14));
        setContentPane(root);

        // Top bar (buttons + search)
        JPanel topBar = buildTopBar();
        root.add(topBar, BorderLayout.NORTH);

        // Center table
        tableModel = new GameTableModel(List.of());
        table = new JTable(tableModel);
        styleMainTable(table);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getSelectionModel().addListSelectionListener(this::onRowSelected);

        sorter = new TableRowSorter<>(tableModel);
        table.setRowSorter(sorter);

        JScrollPane tableScroll = new JScrollPane(table);
        styleScrollPane(tableScroll);
        root.add(tableScroll, BorderLayout.CENTER);

        // Right side: mini dashboard + chips + Game Details card
        JPanel rightSide = buildRightSidePanel();
        root.add(rightSide, BorderLayout.EAST);

        setControlsEnabled(false);
        updateDashboard(); // safe even when library is null

        pack();
        setLocationRelativeTo(null);
        setVisible(true);

        // Let the user choose a DB on startup
        connectToDb();
    }

    // == Styling helpers ==

    private void styleScrollPane(JScrollPane sp) {
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.getViewport().setBackground(new Color(28, 29, 35));
        sp.getVerticalScrollBar().setUnitIncrement(24);
    }

    private void styleMainTable(JTable t) {
        t.setShowGrid(false);
        t.setIntercellSpacing(new Dimension(0, 0));
        t.setRowHeight(30);
        t.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        t.setForeground(new Color(225, 227, 232));
        t.setBackground(new Color(28, 29, 35));
        t.setSelectionBackground(new Color(40, 120, 70));
        t.setSelectionForeground(Color.WHITE);

        JTableHeader header = t.getTableHeader();
        header.setBackground(new Color(40, 42, 50));
        header.setForeground(Color.WHITE);
        header.setFont(new Font("Segoe UI", Font.BOLD, 14));
        header.setReorderingAllowed(false);

        DefaultTableCellRenderer zebra = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus,
                                                           int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? new Color(33, 35, 42) : new Color(28, 29, 35));
                    c.setForeground(new Color(220, 222, 228));
                }
                setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
                return c;
            }
        };
        t.setDefaultRenderer(Object.class, zebra);
        t.setDefaultRenderer(Integer.class, zebra);
        t.setDefaultRenderer(Game.Status.class, zebra);
        t.setDefaultRenderer(Game.Ownership.class, zebra);
    }

    private JPanel cardPanel() {
        JPanel p = new JPanel();
        p.setOpaque(true);
        p.setBackground(new Color(26, 27, 33));
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(52, 55, 65)),
                new EmptyBorder(12, 12, 12, 12)
        ));
        return p;
    }

    private JLabel heading(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(Color.WHITE);
        l.setFont(new Font("Segoe UI", Font.BOLD, 16));
        return l;
    }

    private JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(new Color(190, 192, 198));
        l.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        return l;
    }

    private JTextField darkField() {
        JTextField tf = new JTextField();
        tf.setForeground(new Color(235, 237, 242));
        tf.setBackground(new Color(40, 42, 50));
        tf.setCaretColor(Color.WHITE);
        // thicker padding & height so the fields feel more modern
        tf.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        tf.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        tf.setPreferredSize(new Dimension(0, 30));
        return tf;
    }

    @SuppressWarnings("rawtypes")
    private JComboBox<?> darkCombo(JComboBox cb) {
        cb.setForeground(new Color(235, 237, 242));
        cb.setBackground(new Color(40, 42, 50));
        cb.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cb.setPreferredSize(new Dimension(0, 30));
        return cb;
    }

    private JButton modernButton(String text, Color bg) {
        JButton b = new JButton(text);
        b.setFocusPainted(false);
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setFont(new Font("Segoe UI", Font.BOLD, 13));
        b.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
        return b;
    }

    /**
     * Chip-style toggle used for quick status filters.
     */
    private JToggleButton statusChip(String text) {
        JToggleButton chip = new JToggleButton(text);
        chip.setFocusPainted(false);
        chip.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        chip.setForeground(new Color(230, 232, 238));
        chip.setBackground(new Color(55, 57, 66));
        chip.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        chip.setOpaque(true);
        chip.setBorderPainted(false);

        chip.addChangeListener(e -> {
            if (chip.isSelected()) {
                chip.setBackground(new Color(90, 150, 110));
            } else {
                chip.setBackground(new Color(55, 57, 66));
            }
        });

        return chip;
    }

    // == Layout builders ==

    /**
     * Build the top button bar and wire all actions.
     * This bar is mostly "global" actions plus the search controls.
     */
    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0)) {
            @Override public boolean isOpaque() { return false; }
        };

        connectBtn = modernButton("Connect DB…", new Color(62, 64, 74));
        addBtn     = modernButton("Add", new Color(0, 150, 80));
        updateBtn  = modernButton("Update Selected", new Color(60, 110, 180));
        deleteBtn  = modernButton("Delete Selected", new Color(170, 60, 60));
        clearBtn   = modernButton("Clear Form", new Color(62, 64, 74));
        importBtn  = modernButton("Import From File", new Color(98, 91, 160));
        reportBtn  = modernButton("CheckPoint Report", new Color(80, 130, 100));

        connectBtn.addActionListener(e -> connectToDb());
        addBtn.addActionListener(e -> onAdd());
        updateBtn.addActionListener(e -> onUpdate());
        deleteBtn.addActionListener(e -> onDelete());
        clearBtn.addActionListener(e -> clearForm());
        importBtn.addActionListener(e -> onImport());
        reportBtn.addActionListener(e -> onReport());

        bar.add(connectBtn);
        bar.add(addBtn);
        bar.add(updateBtn);
        bar.add(deleteBtn);
        bar.add(clearBtn);
        bar.add(importBtn);
        bar.add(reportBtn);

        // Search controls (right side)
        searchField = darkField();
        searchField.setPreferredSize(new Dimension(220, 30));

        searchModeBox = (JComboBox<String>) darkCombo(
                new JComboBox<>(new String[]{"All", "Name", "Platform", "Status", "Ownership"})
        );
        searchModeBox.setSelectedIndex(0);
        searchModeBox.addActionListener(e -> applyFilter());

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { applyFilter(); }
            @Override public void removeUpdate(DocumentEvent e) { applyFilter(); }
            @Override public void changedUpdate(DocumentEvent e) { applyFilter(); }
        });

        bar.add(Box.createHorizontalStrut(18));
        bar.add(label("Search:"));
        bar.add(searchField);
        bar.add(searchModeBox);

        return bar;
    }

    /**
     * Build the entire right side:
     * <ul>
     *     <li>a mini dashboard (Total / Unplayed / Playing / Beaten)</li>
     *     <li>a status-chip row for quick filters</li>
     *     <li>the Game Details card</li>
     * </ul>
     */
    private JPanel buildRightSidePanel() {
        JPanel wrapper = new JPanel(new BorderLayout(0, 8));
        wrapper.setOpaque(false);

        // North: dashboard + chips stacked vertically
        JPanel northStack = new JPanel();
        northStack.setLayout(new BoxLayout(northStack, BoxLayout.Y_AXIS));
        northStack.setOpaque(false);

        northStack.add(buildDashboardRow());
        northStack.add(Box.createVerticalStrut(6));
        northStack.add(buildStatusChipRow());
        northStack.add(Box.createVerticalStrut(6));

        wrapper.add(northStack, BorderLayout.NORTH);

        // Center: Game Details card
        JPanel detailsCard = buildFormPanel();
        wrapper.add(detailsCard, BorderLayout.CENTER);

        return wrapper;
    }

    /**
     * Build the mini dashboard row with four compact cards:
     * Total / Unplayed / Playing / Beaten. Values are wired to
     * {@link #updateDashboard()}.
     */
    private JPanel buildDashboardRow() {
        JPanel row = new JPanel(new GridLayout(1, 4, 8, 0)) {
            @Override public boolean isOpaque() { return false; }
        };

        totalBigLabel     = bigNumberLabel();
        unplayedBigLabel  = bigNumberLabel();
        playingBigLabel   = bigNumberLabel();
        beatenBigLabel    = bigNumberLabel();

        totalSubLabel     = subNumberLabel();
        unplayedSubLabel  = subNumberLabel();
        playingSubLabel   = subNumberLabel();
        beatenSubLabel    = subNumberLabel();

        row.add(statCard("Total",    totalBigLabel,    totalSubLabel));
        row.add(statCard("Unplayed", unplayedBigLabel, unplayedSubLabel));
        row.add(statCard("Playing",  playingBigLabel,  playingSubLabel));
        row.add(statCard("Beaten",   beatenBigLabel,   beatenSubLabel));

        return row;
    }

    /** Helper to build a single dashboard card. */
    private JPanel statCard(String title, JLabel big, JLabel sub) {
        JPanel card = cardPanel();
        card.setLayout(new BorderLayout(0, 2));
        card.setPreferredSize(new Dimension(80, 60));

        JLabel titleLabel = new JLabel(title.toUpperCase());
        titleLabel.setForeground(new Color(200, 202, 210));
        titleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));

        card.add(titleLabel, BorderLayout.NORTH);
        card.add(big, BorderLayout.CENTER);
        card.add(sub, BorderLayout.SOUTH);
        return card;
    }

    private JLabel bigNumberLabel() {
        JLabel l = new JLabel("0");
        l.setForeground(new Color(235, 237, 242));
        l.setFont(new Font("Segoe UI", Font.BOLD, 22));
        return l;
    }

    private JLabel subNumberLabel() {
        JLabel l = new JLabel("0");
        l.setForeground(new Color(160, 162, 170));
        l.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        return l;
    }

    /**
     * Build the status-chip row that sits directly above the Game Details heading.
     */
    private JPanel buildStatusChipRow() {
        JPanel chipRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0)) {
            @Override public boolean isOpaque() { return false; }
        };

        JLabel chipLabel = label("Status filter:");
        unplayedChip = statusChip("Unplayed");
        playingChip  = statusChip("Playing");
        beatenChip   = statusChip("Beaten");

        unplayedChip.addActionListener(e -> toggleStatusChip(Game.Status.UNPLAYED));
        playingChip.addActionListener(e -> toggleStatusChip(Game.Status.PLAYING));
        beatenChip.addActionListener(e -> toggleStatusChip(Game.Status.BEATEN));

        chipRow.add(chipLabel);
        chipRow.add(unplayedChip);
        chipRow.add(playingChip);
        chipRow.add(beatenChip);

        return chipRow;
    }

    /**
     * Build the Game Details card used to create/update a {@link Game}.
     * Status chips + dashboard sit above this card in {@link #buildRightSidePanel()}.
     *
     * The card itself is kept relatively compact so the right side feels balanced
     * against the main table.
     */
    private JPanel buildFormPanel() {
        JPanel panel = cardPanel();

        // Make the card smaller/compact so it doesn't stretch the whole column
        panel.setPreferredSize(new Dimension(320, 260));
        panel.setMaximumSize(new Dimension(360, 280));

        panel.setLayout(new GridBagLayout());

        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(6, 6, 6, 6);
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1.0;

        int row = 0;

        // Slightly larger heading so the card feels intentional
        JLabel title = heading("Game Details");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        gc.gridx = 0; gc.gridy = row++; gc.gridwidth = 2;
        panel.add(title, gc);
        gc.gridwidth = 1;

        idField = makeIntegerField();
        nameField = darkField();
        platformField = darkField();
        statusBox = (JComboBox<Game.Status>) darkCombo(new JComboBox<>(Game.Status.values()));
        statusBox.setSelectedItem(null);
        priorityField = makeIntegerField();
        ownershipBox = (JComboBox<Game.Ownership>) darkCombo(new JComboBox<>(Game.Ownership.values()));
        ownershipBox.setSelectedItem(null);

        addRow(panel, gc, row++, "ID (>0):", idField);
        addRow(panel, gc, row++, "Name:", nameField);
        addRow(panel, gc, row++, "Platform:", platformField);

        // subtle visual break between identity and progress fields
        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(60, 62, 70));
        gc.gridx = 0; gc.gridy = row++; gc.gridwidth = 2;
        gc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(sep, gc);
        gc.gridwidth = 1;

        addRow(panel, gc, row++, "Status:", statusBox);
        addRow(panel, gc, row++, "Priority (1-5):", priorityField);
        addRow(panel, gc, row++, "Ownership:", ownershipBox);

        return panel;
    }

    private void addRow(JPanel panel, GridBagConstraints gc, int row, String labelText, JComponent field) {
        gc.gridx = 0; gc.gridy = row; gc.weightx = 0;
        panel.add(label(labelText), gc);
        gc.gridx = 1; gc.weightx = 1.0;
        panel.add(field, gc);
    }

    // == Quick status chip behavior ==

    /**
     * Handle clicks on the status chips. Clicking a chip applies that status filter.
     * Clicking the same chip again clears the filter and shows all statuses again.
     */
    private void toggleStatusChip(Game.Status status) {
        if (chipStatusFilter == status) {
            chipStatusFilter = null;
            unplayedChip.setSelected(false);
            playingChip.setSelected(false);
            beatenChip.setSelected(false);
        } else {
            chipStatusFilter = status;
            unplayedChip.setSelected(status == Game.Status.UNPLAYED);
            playingChip.setSelected(status == Game.Status.PLAYING);
            beatenChip.setSelected(status == Game.Status.BEATEN);
        }
        applyFilter();
    }

    // == Actions ==

    /** When a table row is picked, mirror that record into the form. */
    private void onRowSelected(ListSelectionEvent e) {
        if (e.getValueIsAdjusting()) return;
        int viewIdx = table.getSelectedRow();
        if (viewIdx < 0) return;

        int modelRow = table.convertRowIndexToModel(viewIdx);
        Game g = tableModel.getAt(modelRow);
        if (g == null) return;

        idField.setText(String.valueOf(g.getId()));
        nameField.setText(g.getName());
        platformField.setText(g.getPlatform());
        statusBox.setSelectedItem(g.getStatus());
        priorityField.setText(String.valueOf(g.getPriority()));
        ownershipBox.setSelectedItem(g.getOwnership());
    }

    /** Collect form values, validate, insert into DB, refresh table, and toast the result. */
    private void onAdd() {
        if (!ensureConnected()) return;
        try {
            int id = requirePositive(parseIntStrict(idField.getText(), "ID"), "ID");
            ensureUniqueIdOnCreate(id);

            String name = requireNonEmpty(nameField.getText(), "Name");
            String platform = requireNonEmpty(platformField.getText(), "Platform");
            Game.Status status = requireSelected((Game.Status) statusBox.getSelectedItem(), "Status");
            int priority = requirePriority(parseIntStrict(priorityField.getText(), "Priority"));
            Game.Ownership ownership = requireSelected((Game.Ownership) ownershipBox.getSelectedItem(), "Ownership");

            Game g = new Game(id, name, platform, status, priority, ownership);
            String msg = library.add(g);
            if (msg.startsWith("❌")) { showError("Add failed", msg); return; }

            refreshTable();
            selectGameInTable(id);
            showInfo("Add", msg);
            clearFormKeepSelection();
        } catch (Exception ex) {
            showError("Invalid input", ex.getMessage());
        }
    }

    /** Update the selected record. Handles id changes safely and persists per-field updates. */
    private void onUpdate() {
        if (!ensureConnected()) return;

        int viewIdx = table.getSelectedRow();
        if (viewIdx < 0) { showInfo("Nothing selected", "Select a row to update."); return; }

        int idx = table.convertRowIndexToModel(viewIdx);
        Game sel = tableModel.getAt(idx);
        if (sel == null) return;

        try {
            int newId = requirePositive(parseIntStrict(idField.getText(), "ID"), "ID");
            String name = requireNonEmpty(nameField.getText(), "Name");
            String platform = requireNonEmpty(platformField.getText(), "Platform");
            Game.Status status = requireSelected((Game.Status) statusBox.getSelectedItem(), "Status");
            int priority = requirePriority(parseIntStrict(priorityField.getText(), "Priority"));
            Game.Ownership ownership = requireSelected((Game.Ownership) ownershipBox.getSelectedItem(), "Ownership");

            if (newId != sel.getId() && library.findById(newId).isPresent()) {
                throw new IllegalArgumentException("That ID already exists. Choose a different ID.");
            }

            // keep local object in sync for selection experience
            sel.setId(newId);
            sel.setName(name);
            sel.setPlatform(platform);
            sel.setStatus(status);
            sel.setPriority(priority);
            sel.setOwnership(ownership);

            if (newId != tableModel.getAt(idx).getId()) {
                library.remove(tableModel.getAt(idx).getId());
                library.add(sel);
            } else {
                library.updateField(newId, "name", name);
                library.updateField(newId, "platform", platform);
                library.updateField(newId, "status", status.name());
                library.updateField(newId, "priority", String.valueOf(priority));
                library.updateField(newId, "ownership", ownership.name());
            }

            refreshTable();
            selectGameInTable(newId);
            showInfo("Update", "✅ Updated:\n" + sel);
        } catch (Exception ex) {
            showError("Update failed", ex.getMessage());
        }
    }

    /** Confirm and delete the selected record, then refresh the UI. */
    private void onDelete() {
        if (!ensureConnected()) return;

        int viewIdx = table.getSelectedRow();
        if (viewIdx < 0) { showInfo("Nothing selected", "Select a row to delete."); return; }

        int idx = table.convertRowIndexToModel(viewIdx);
        Game sel = tableModel.getAt(idx);
        if (sel == null) return;

        int res = JOptionPane.showConfirmDialog(
                this,
                "Delete game ID " + sel.getId() + "?",
                "Confirm deletion", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE
        );
        if (res == JOptionPane.OK_OPTION) {
            String msg = library.remove(sel.getId());
            refreshTable();
            clearForm();
            showInfo("Delete", msg);
        }
    }

    /** Choose a text file and bulk-import rows (id|name|platform|status|priority|ownership). */
    private void onImport() {
        if (!ensureConnected()) return;

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select game list (id|name|platform|status|priority|ownership)");
        int res = chooser.showOpenDialog(this);
        if (res != JFileChooser.APPROVE_OPTION) return;

        File file = chooser.getSelectedFile();
        String msg = library.importFromFile(file.toPath());
        refreshTable();
        showInfo("Import", msg);
    }

    /** Prompt for a Top-Number and show the report dialog with backlog stats. */
    private void onReport() {
        if (!ensureConnected()) return;

        String input = JOptionPane.showInputDialog(
                this,
                "How many priority games should the report include? (1–10)",
                "5"
        );
        if (input == null) return;
        try {
            int n = parseIntStrict(input, "Top Number");
            if (n < 1 || n > 10) throw new IllegalArgumentException("Enter a number between 1 and 10.");
            new ReportDialog(this, library.listAll(), n).setVisible(true);
        } catch (Exception ex) {
            showError("Invalid number", ex.getMessage());
        }
    }

    // == DB connect + guards ==

    /**
     * Let the user select an SQLite file and initialize {@link #library}.
     * Disables UI if the user cancels; enables and loads data on success.
     */
    private void connectToDb() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Choose SQLite database file");
        int res = chooser.showOpenDialog(this);
        if (res != JFileChooser.APPROVE_OPTION) {
            JOptionPane.showMessageDialog(this,
                    "No database selected. You can connect later with 'Connect DB…'.",
                    "Not connected", JOptionPane.INFORMATION_MESSAGE);
            setControlsEnabled(false);
            tableModel.setData(List.of());
            updateDashboard();
            return;
        }

        String path = chooser.getSelectedFile().getAbsolutePath();
        try {
            library = new DbLibrary(path);  // creates table if needed
            setControlsEnabled(true);
            refreshTable();
            JOptionPane.showMessageDialog(this, "Connected to: " + path,
                    "Database", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            library = null;
            setControlsEnabled(false);
            tableModel.setData(List.of());
            updateDashboard();
            showError("Connection failed", ex.getMessage());
        }
    }

    /** Guard: remind the user to connect to a DB first. */
    private boolean ensureConnected() {
        if (library == null) {
            showError("Not connected", "Use 'Connect DB…' to choose a database first.");
            return false;
        }
        return true;
    }

    private void setControlsEnabled(boolean enabled) {
        if (addBtn != null)     addBtn.setEnabled(enabled);
        if (updateBtn != null)  updateBtn.setEnabled(enabled);
        if (deleteBtn != null)  deleteBtn.setEnabled(enabled);
        if (clearBtn != null)   clearBtn.setEnabled(enabled);
        if (importBtn != null)  importBtn.setEnabled(enabled);
        if (reportBtn != null)  reportBtn.setEnabled(enabled);
        if (table != null)      table.setEnabled(enabled);
    }

    /**
     * Reload table rows from the DB snapshot, refresh live dashboard,
     * and keep filters applied.
     */
    private void refreshTable() {
        if (library == null) {
            tableModel.setData(List.of());
            updateDashboard();
            applyFilter();
            return;
        }

        tableModel.setData(library.listAll());
        updateDashboard();
        applyFilter();
    }

    // == Search / filter helpers ==

    /**
     * Apply a {@link RowFilter} to the table based on:
     * <ul>
     *     <li>Search text in {@link #searchField} (optional).</li>
     *     <li>Search mode in {@link #searchModeBox} (All / Name / Platform / Status / Ownership).</li>
     *     <li>Quick status chip selection in {@link #chipStatusFilter} (Unplayed / Playing / Beaten / All).</li>
     * </ul>
     * If both search text and status chip are neutral, all rows are shown.
     */
    private void applyFilter() {
        if (sorter == null) return;

        String raw = (searchField == null) ? "" : searchField.getText();
        final String query = (raw == null ? "" : raw.trim().toLowerCase());
        final int mode = (searchModeBox == null) ? 0 : searchModeBox.getSelectedIndex(); // 0=All, 1=Name, 2=Platform, 3=Status, 4=Ownership
        final Game.Status statusFilter = chipStatusFilter; // capture current chip state

        if (query.isBlank() && statusFilter == null) {
            sorter.setRowFilter(null);
            return;
        }

        sorter.setRowFilter(new RowFilter<>() {
            @Override
            public boolean include(Entry<? extends GameTableModel, ? extends Integer> entry) {
                int modelRow = entry.getIdentifier();
                Game g = tableModel.getAt(modelRow);
                if (g == null) return false;

                // First gate: status chips
                if (statusFilter != null && g.getStatus() != statusFilter) {
                    return false;
                }

                // If there is no search text, chips alone decide
                if (query.isBlank()) return true;

                boolean matchesName      = g.getName().toLowerCase().contains(query);
                boolean matchesPlatform  = g.getPlatform().toLowerCase().contains(query);
                boolean matchesStatus    = g.getStatus().name().toLowerCase().contains(query);
                boolean matchesOwnership = g.getOwnership().name().toLowerCase().contains(query);

                return switch (mode) {
                    case 1 -> matchesName;
                    case 2 -> matchesPlatform;
                    case 3 -> matchesStatus;
                    case 4 -> matchesOwnership;
                    default -> matchesName || matchesPlatform || matchesStatus || matchesOwnership;
                };
            }
        });
    }

    /**
     * Recalculate the live dashboard numbers (Total / Unplayed / Playing / Beaten)
     * and push them into the mini cards.
     *
     * <p>This always uses the full DB snapshot from {@link DbLibrary#listAll()},
     * so the counts reflects the whole backlog, not just whatever is currently
     * filtered in the table.</p>
     */
    private void updateDashboard() {
        if (totalBigLabel == null) {
            // UI not built yet; constructor will call this again later.
            return;
        }

        if (library == null) {
            totalBigLabel.setText("0");
            unplayedBigLabel.setText("0");
            playingBigLabel.setText("0");
            beatenBigLabel.setText("0");

            totalSubLabel.setText("No database");
            unplayedSubLabel.setText("-");
            playingSubLabel.setText("-");
            beatenSubLabel.setText("-");
            return;
        }

        List<Game> all = library.listAll();
        long total    = all.size();
        long unplayed = all.stream().filter(g -> g.getStatus() == Game.Status.UNPLAYED).count();
        long playing  = all.stream().filter(g -> g.getStatus() == Game.Status.PLAYING).count();
        long beaten   = all.stream().filter(g -> g.getStatus() == Game.Status.BEATEN).count();

        totalBigLabel.setText(String.valueOf(total));
        unplayedBigLabel.setText(String.valueOf(unplayed));
        playingBigLabel.setText(String.valueOf(playing));
        beatenBigLabel.setText(String.valueOf(beaten));

        if (total == 0) {
            totalSubLabel.setText("Games");
            unplayedSubLabel.setText("0%");
            playingSubLabel.setText("0%");
            beatenSubLabel.setText("0%");
        } else {
            totalSubLabel.setText("Games");
            unplayedSubLabel.setText(String.format("%d%%", Math.round(unplayed * 100.0 / total)));
            playingSubLabel.setText(String.format("%d%%", Math.round(playing * 100.0 / total)));
            beatenSubLabel.setText(String.format("%d%%", Math.round(beaten * 100.0 / total)));
        }
    }

    // == Misc helpers ==

    private void clearForm() {
        idField.setText("");
        nameField.setText("");
        platformField.setText("");
        statusBox.setSelectedItem(null);
        priorityField.setText("");
        ownershipBox.setSelectedItem(null);
        table.clearSelection();
    }

    private void clearFormKeepSelection() {
        idField.setText("");
        nameField.setText("");
        platformField.setText("");
        statusBox.setSelectedItem(null);
        priorityField.setText("");
        ownershipBox.setSelectedItem(null);
    }

    private void selectGameInTable(int id) {
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            Game g = tableModel.getAt(i);
            if (g != null && g.getId() == id) {
                int viewRow = table.convertRowIndexToView(i);
                table.getSelectionModel().setSelectionInterval(viewRow, viewRow);
                table.scrollRectToVisible(table.getCellRect(viewRow, 0, true));
                break;
            }
        }
    }

    private JTextField makeIntegerField() {
        JTextField tf = darkField();
        ((AbstractDocument) tf.getDocument()).setDocumentFilter(new IntegersOnlyFilter());
        return tf;
    }

    private int parseIntStrict(String s, String label) {
        if (s == null || s.isBlank()) throw new IllegalArgumentException(label + " is required.");
        try { return Integer.parseInt(s.trim()); }
        catch (NumberFormatException nfe) { throw new IllegalArgumentException(label + " must be a whole number."); }
    }

    private int requirePositive(int n, String label) {
        if (n <= 0) throw new IllegalArgumentException(label + " must be > 0.");
        return n;
    }

    private int requirePriority(int p) {
        if (p < 1 || p > 5) throw new IllegalArgumentException("Priority must be 1–5.");
        return p;
    }

    private <T> T requireSelected(T value, String label) {
        if (value == null) throw new IllegalArgumentException(label + " is required.");
        return value;
    }

    private String requireNonEmpty(String s, String label) {
        if (s == null || s.trim().isEmpty()) throw new IllegalArgumentException(label + " is required.");
        return s.trim();
    }

    private void ensureUniqueIdOnCreate(int id) {
        if (library != null && library.findById(id).isPresent()) {
            throw new IllegalArgumentException("That ID already exists. Choose a different ID.");
        }
    }

    private void showInfo(String title, String message) {
        JOptionPane.showMessageDialog(this, message, title, JOptionPane.INFORMATION_MESSAGE);
    }

    private void showError(String title, String message) {
        JOptionPane.showMessageDialog(this, message, title, JOptionPane.ERROR_MESSAGE);
    }

    // == Table model ==

    /**
     * Read-only table model used by the main grid.
     * Keeps a simple column map to {@link Game} getters.
     */
    private static class GameTableModel extends AbstractTableModel {
        private final String[] cols = {"ID", "Name", "Platform", "Status", "Priority", "Ownership"};
        private List<Game> data;

        GameTableModel(List<Game> data) { this.data = data; }
        void setData(List<Game> fresh) { this.data = fresh; fireTableDataChanged(); }

        Game getAt(int row) {
            if (data == null || row < 0 || row >= data.size()) return null;
            return data.get(row);
        }

        @Override public int getRowCount() { return data == null ? 0 : data.size(); }
        @Override public int getColumnCount() { return cols.length; }
        @Override public String getColumnName(int col) { return cols[col]; }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            Game g = data.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> g.getId();
                case 1 -> g.getName();
                case 2 -> g.getPlatform();
                case 3 -> g.getStatus();
                case 4 -> g.getPriority();
                case 5 -> g.getOwnership();
                default -> "";
            };
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return switch (columnIndex) {
                case 0, 4 -> Integer.class;
                case 3      -> Game.Status.class;
                case 5      -> Game.Ownership.class;
                default     -> String.class;
            };
        }

        @Override public boolean isCellEditable(int rowIndex, int columnIndex) { return false; }
    }

    /** DocumentFilter that allows only digits (for ID and Priority fields). */
    private static class IntegersOnlyFilter extends DocumentFilter {
        @Override
        public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
            if (string != null && string.matches("\\d+")) super.insertString(fb, offset, string, attr);
        }
        @Override
        public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
            if (text == null || text.isEmpty() || text.matches("\\d+")) super.replace(fb, offset, length, text, attrs);
        }
    }

    /**
     * Modal dialog that presents backlog stats and a Top-Number table.
     * Built only from the provided list—no DB writes.
     */
    private static class ReportDialog extends JDialog {
        /**
         * Construct the report window.
         *
         * @param owner parent frame (modal owner)
         * @param all   all games to summarize
         * @param topN  number of top items to show (already validated 1–10)
         */
        ReportDialog(JFrame owner, List<Game> all, int topN) {
            super(owner, "📊 CheckPoint Report", true);
            setDefaultCloseOperation(DISPOSE_ON_CLOSE);
            setMinimumSize(new Dimension(740, 540));

            JPanel root = new JPanel(new BorderLayout(12, 12)) {
                @Override protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setPaint(new GradientPaint(0, 0, new Color(28, 29, 35),
                            0, getHeight(), new Color(22, 23, 28)));
                    g2.fillRect(0, 0, getWidth(), getHeight());
                    g2.dispose();
                }
            };
            root.setBorder(new EmptyBorder(14, 14, 14, 14));
            setContentPane(root);

            // badges
            JPanel badges = new JPanel(new GridLayout(1, 0, 10, 0)) {
                @Override public boolean isOpaque() { return false; }
            };
            long total = all.size();
            long unplayed = all.stream().filter(g -> g.getStatus() == Game.Status.UNPLAYED).count();
            long playing  = all.stream().filter(g -> g.getStatus() == Game.Status.PLAYING).count();
            long beaten   = all.stream().filter(g -> g.getStatus() == Game.Status.BEATEN).count();
            badges.add(badge("Total", total, new Color(75, 110, 175)));
            badges.add(badge("Unplayed", unplayed, new Color(130, 130, 140)));
            badges.add(badge("Playing", playing, new Color(90, 160, 100)));
            badges.add(badge("Beaten", beaten, new Color(160, 95, 95)));
            root.add(badges, BorderLayout.NORTH);

            // split center
            JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
            split.setBorder(BorderFactory.createEmptyBorder());
            split.setDividerSize(10);
            split.setResizeWeight(0.32);

            // left: distribution bars
            JPanel dist = new JPanel();
            dist.setOpaque(false);
            dist.setLayout(new BoxLayout(dist, BoxLayout.Y_AXIS));
            dist.add(Box.createVerticalStrut(6));
            dist.add(distributionRow("UNPLAYED", unplayed, total, new Color(130, 130, 140)));
            dist.add(Box.createVerticalStrut(10));
            dist.add(distributionRow("PLAYING",  playing,  total, new Color(90, 160, 100)));
            dist.add(Box.createVerticalStrut(10));
            dist.add(distributionRow("BEATEN",   beaten,   total, new Color(160, 95, 95)));
            dist.add(Box.createVerticalGlue());
            JPanel distCard = card();
            distCard.setLayout(new BorderLayout());
            distCard.add(title("Status Distribution"), BorderLayout.NORTH);
            distCard.add(dist, BorderLayout.CENTER);
            split.setLeftComponent(distCard);

            // right: top Number table
            List<Game> top = all.stream()
                    .sorted(Comparator.comparingInt(Game::getPriority).reversed()
                            .thenComparing(Game::getStatus)
                            .thenComparing(Game::getName, String.CASE_INSENSITIVE_ORDER))
                    .limit(topN)
                    .collect(Collectors.toList());

            String[] cols = {"ID", "Name", "Platform", "Status", "Priority", "Ownership"};
            Object[][] rows = new Object[top.size()][cols.length];
            for (int i = 0; i < top.size(); i++) {
                Game g = top.get(i);
                rows[i][0] = g.getId();
                rows[i][1] = g.getName();
                rows[i][2] = g.getPlatform();
                rows[i][3] = g.getStatus();
                rows[i][4] = g.getPriority();
                rows[i][5] = g.getOwnership();
            }
            JTable t = new JTable(new DefaultTableModel(rows, cols) {
                @Override public boolean isCellEditable(int r, int c) { return false; }
                @Override public Class<?> getColumnClass(int c) {
                    return switch (c) {
                        case 0, 4 -> Integer.class;
                        case 3      -> Game.Status.class;
                        case 5      -> Game.Ownership.class;
                        default     -> String.class;
                    };
                }
            });
            styleReportTable(t);

            JScrollPane sp = new JScrollPane(t);
            sp.setBorder(BorderFactory.createEmptyBorder());
            JPanel tableCard = card();
            tableCard.setLayout(new BorderLayout());
            tableCard.add(title("Top " + topN + " by Priority"), BorderLayout.NORTH);
            tableCard.add(sp, BorderLayout.CENTER);
            split.setRightComponent(tableCard);

            root.add(split, BorderLayout.CENTER);

            JButton close = new JButton("Close");
            close.addActionListener(e -> dispose());
            JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0)) {
                @Override public boolean isOpaque() { return false; }
            };
            south.add(close);
            root.add(south, BorderLayout.SOUTH);

            pack();
            setLocationRelativeTo(owner);
            split.setDividerLocation(260);
        }

        private JLabel title(String s) {
            JLabel l = new JLabel(s);
            l.setForeground(Color.WHITE);
            l.setFont(new Font("Segoe UI", Font.BOLD, 16));
            l.setBorder(new EmptyBorder(0, 0, 8, 0));
            return l;
        }

        private JPanel card() {
            JPanel p = new JPanel();
            p.setOpaque(true);
            p.setBackground(new Color(26, 27, 33));
            p.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(52, 55, 65)),
                    new EmptyBorder(12, 12, 12, 12)
            ));
            return p;
        }

        private JPanel badge(String label, long value, Color color) {
            JPanel p = card();
            p.setLayout(new BorderLayout());
            JLabel title = new JLabel(label);
            title.setForeground(new Color(210, 212, 218));
            title.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            JLabel big = new JLabel(String.valueOf(value));
            big.setForeground(color);
            big.setFont(new Font("Segoe UI", Font.BOLD, 28));
            p.add(title, BorderLayout.NORTH);
            p.add(big, BorderLayout.CENTER);
            return p;
        }

        private JComponent distributionRow(String label, long count, long total, Color barColor) {
            JPanel row = new JPanel(new BorderLayout(8, 0)) {
                @Override public boolean isOpaque() { return false; }
            };
            JLabel name = new JLabel(label);
            name.setForeground(new Color(220, 222, 228));
            name.setFont(new Font("Segoe UI", Font.PLAIN, 13));

            int pct = total == 0 ? 0 : (int) Math.round((count * 100.0) / total);
            JProgressBar bar = new JProgressBar(0, 100);
            bar.setValue(pct);
            bar.setStringPainted(true);
            bar.setString(count + " (" + pct + "%)");
            bar.setForeground(barColor);
            bar.setBackground(new Color(40, 42, 50));
            bar.setBorder(BorderFactory.createLineBorder(new Color(52, 55, 65)));

            row.add(name, BorderLayout.WEST);
            row.add(bar, BorderLayout.CENTER);
            return row;
        }

        private void styleReportTable(JTable t) {
            t.setShowGrid(false);
            t.setIntercellSpacing(new Dimension(0, 0));
            t.setRowHeight(28);
            t.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            t.setForeground(new Color(225, 227, 232));
            t.setBackground(new Color(28, 29, 35));
            t.setSelectionBackground(new Color(40, 120, 70));
            t.setSelectionForeground(Color.WHITE);
            JTableHeader h = t.getTableHeader();
            h.setBackground(new Color(40, 42, 50));
            h.setForeground(Color.WHITE);
            h.setFont(new Font("Segoe UI", Font.BOLD, 13));
            t.setAutoCreateRowSorter(true);

            DefaultTableCellRenderer zebra = new DefaultTableCellRenderer() {
                @Override
                public Component getTableCellRendererComponent(JTable table, Object value,
                                                               boolean isSelected, boolean hasFocus,
                                                               int row, int column) {
                    Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                    if (!isSelected) {
                        c.setBackground(row % 2 == 0 ? new Color(33, 35, 42) : new Color(28, 29, 35));
                        c.setForeground(new Color(220, 222, 228));
                    }
                    setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
                    return c;
                }
            };
            t.setDefaultRenderer(Object.class, zebra);
            t.setDefaultRenderer(Integer.class, zebra);
            t.setDefaultRenderer(Game.Status.class, zebra);
            t.setDefaultRenderer(Game.Ownership.class, zebra);
        }
    }

    /**
     * App entry point: applies Nimbus L&amp;F (if available) and launches the window.
     *
     * @param args unused
     */
    public static void main(String[] args) {
        try { UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel"); } catch (Exception ignored) {}
        SwingUtilities.invokeLater(CheckPointSwing::new);
    }

} // === END GUI ===
