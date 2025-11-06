/**
 * Stephanie Ortiz
 * CEN 3024 - Software Development 1
 * November 5th, 2025
 * CheckPointSwing.java
 * ---------------------------------
 * The GUI of CheckPoint
 */

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.text.*;
import java.awt.*;
import java.io.File;
import java.util.List;

/**
 * CheckPointSwing (Updated GUI Layout and Design)
 * Logic stays the same
 * Dark theme, improved readability, modern buttons, gradient background
 */
public class CheckPointSwing extends JFrame {

    // Domain (DB-backed)
    private DbLibrary library; // null until the user connects to a database

    // UI: Root + Table + Model
    private JPanel root;
    private JTable table;
    private GameTableModel tableModel;

    // UI: Form fields
    private JTextField idField;
    private JTextField nameField;
    private JTextField platformField;
    private JComboBox<Game.Status> statusBox;
    private JTextField priorityField;
    private JComboBox<Game.Ownership> ownershipBox;

    // UI: Buttons
    private JButton connectBtn, addBtn, updateBtn, deleteBtn, clearBtn, importBtn, reportBtn;

    public CheckPointSwing() {
        super("CheckPoint (Swing, Database)");

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1100, 600));

        // Root with gradient background
        root = new JPanel(new BorderLayout(12, 12)) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(
                        0, 0, new Color(22, 23, 28),
                        0, getHeight(), new Color(32, 34, 41)
                );
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        root.setBorder(new EmptyBorder(14, 14, 14, 14));
        setContentPane(root);

        // Table
        tableModel = new GameTableModel(List.of());
        table = new JTable(tableModel);
        styleTable(table);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getSelectionModel().addListSelectionListener(this::onRowSelected);

        JScrollPane tableScroll = new JScrollPane(table);
        styleScrollPane(tableScroll);
        root.add(tableScroll, BorderLayout.CENTER);

        // Form (right)
        JPanel form = buildFormPanel();
        root.add(form, BorderLayout.EAST);

        // Buttons (top)
        JPanel buttons = buildTopBar();
        root.add(buttons, BorderLayout.NORTH);

        pack();
        setLocationRelativeTo(null);
        setVisible(true);

        // Ask the user to pick a database at startup
        connectToDb();
    }

    // Styling Helpers

    private void styleScrollPane(JScrollPane sp) {
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.getViewport().setBackground(new Color(28, 29, 35));
        sp.getVerticalScrollBar().setUnitIncrement(24);
    }

    private void styleTable(JTable t) {
        t.setShowGrid(false);
        t.setIntercellSpacing(new Dimension(0, 0));
        t.setRowHeight(30);
        t.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        t.setForeground(new Color(225, 227, 232));
        t.setBackground(new Color(28, 29, 35));
        t.setSelectionBackground(new Color(40, 120, 70)); // Xbox-ish green
        t.setSelectionForeground(Color.WHITE);

        // Header
        JTableHeader header = t.getTableHeader();
        header.setBackground(new Color(40, 42, 50));
        header.setForeground(Color.WHITE);
        header.setFont(new Font("Segoe UI", Font.BOLD, 14));
        header.setReorderingAllowed(false);

        // Zebra rows + alignment
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
        tf.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        tf.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        return tf;
    }

    private JComboBox<?> darkCombo(JComboBox<?> cb) {
        cb.setForeground(new Color(235, 237, 242));
        cb.setBackground(new Color(40, 42, 50));
        cb.setFont(new Font("Segoe UI", Font.PLAIN, 13));
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

    // UI Builders

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

        setControlsEnabled(false); // disabled until a database is connected
        return bar;
    }

    /**
     * Build the form for game fields.
     */
    private JPanel buildFormPanel() {
        JPanel panel = cardPanel();
        panel.setPreferredSize(new Dimension(360, 0));
        panel.setLayout(new GridBagLayout());

        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(6, 6, 6, 6);
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1.0;

        JLabel title = heading("Game Details");
        gc.gridx = 0; gc.gridy = 0; gc.gridwidth = 2;
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

        int row = 1;
        addRow(panel, gc, row++, "ID (>0):", idField);
        addRow(panel, gc, row++, "Name:", nameField);
        addRow(panel, gc, row++, "Platform:", platformField);
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

    // Actions

    private void onRowSelected(ListSelectionEvent e) {
        if (e.getValueIsAdjusting()) return;
        int idx = table.getSelectedRow();
        if (idx < 0) return;

        Game g = tableModel.getAt(idx);
        if (g == null) return;

        idField.setText(String.valueOf(g.getId()));
        nameField.setText(g.getName());
        platformField.setText(g.getPlatform());
        statusBox.setSelectedItem(g.getStatus());
        priorityField.setText(String.valueOf(g.getPriority()));
        ownershipBox.setSelectedItem(g.getOwnership());
    }

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
            if (msg.startsWith("❌")) {
                showError("Add failed", msg);
                return;
            }
            refreshTable();
            selectGameInTable(id);
            showInfo("Add", msg);
            clearFormKeepSelection();
        } catch (Exception ex) {
            showError("Invalid input", ex.getMessage());
        }
    }

    private void onUpdate() {
        if (!ensureConnected()) return;

        int idx = table.getSelectedRow();
        if (idx < 0) {
            showInfo("Nothing selected", "Select a row to update.");
            return;
        }
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

            // Update in-memory model object (to keep the table neat)
            sel.setId(newId);
            sel.setName(name);
            sel.setPlatform(platform);
            sel.setStatus(status);
            sel.setPriority(priority);
            sel.setOwnership(ownership);

            // Save
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

    private void onDelete() {
        if (!ensureConnected()) return;

        int idx = table.getSelectedRow();
        if (idx < 0) {
            showInfo("Nothing selected", "Select a row to delete.");
            return;
        }
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

    private void onImport() {
        if (!ensureConnected()) return;

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select game list (id|name|platform|status|priority|ownership)");
        int res = chooser.showOpenDialog(this);
        if (res != JFileChooser.APPROVE_OPTION) return;

        File file = chooser.getSelectedFile();
        String msg = library.importFromFile(file.toPath()); // writes into DB
        refreshTable();
        showInfo("Import", msg);
    }

    private void onReport() {
        if (!ensureConnected()) return;

        String input = JOptionPane.showInputDialog(this, "How many priority games do you want to show? (1-10)", "5");
        if (input == null) return;
        try {
            int n = parseIntStrict(input, "Top Number");
            if (n < 1 || n > 10) throw new IllegalArgumentException("Enter a number between 1 and 10.");
            String report = library.backlogReport(n);

            JTextArea area = new JTextArea(report, 18, 64);
            area.setEditable(false);
            area.setFont(new Font("Consolas", Font.PLAIN, 13));
            area.setBackground(new Color(28, 29, 35));
            area.setForeground(new Color(230, 232, 238));

            JScrollPane sp = new JScrollPane(area);
            sp.setBorder(BorderFactory.createEmptyBorder());
            JOptionPane.showMessageDialog(this, sp, "📊 Backlog Health", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            showError("Invalid number", ex.getMessage());
        }
    }

    // DB Connect + Guards

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
            showError("Connection failed", ex.getMessage());
        }
    }

    private boolean ensureConnected() {
        if (library == null) {
            showError("Not connected", "Use 'Connect DB…' to choose a database first.");
            return false;
        }
        return true;
    }

    private void setControlsEnabled(boolean enabled) {
        addBtn.setEnabled(enabled);
        updateBtn.setEnabled(enabled);
        deleteBtn.setEnabled(enabled);
        clearBtn.setEnabled(enabled);
        importBtn.setEnabled(enabled);
        reportBtn.setEnabled(enabled);
        table.setEnabled(enabled);
    }

    private void refreshTable() {
        if (library == null) { tableModel.setData(List.of()); return; }
        tableModel.setData(library.listAll());
    }

    // Helpers

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
                table.getSelectionModel().setSelectionInterval(i, i);
                table.scrollRectToVisible(table.getCellRect(i, 0, true));
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
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException nfe) {
            throw new IllegalArgumentException(label + " must be a whole number.");
        }
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

    // Table Model

    private static class GameTableModel extends AbstractTableModel {
        private final String[] cols = {"ID", "Name", "Platform", "Status", "Priority", "Ownership"};
        private List<Game> data;

        GameTableModel(List<Game> data) { this.data = data; }
        void setData(List<Game> fresh) { this.data = fresh; fireTableDataChanged(); }

        Game getAt(int row) {
            if (row < 0 || row >= getRowCount()) return null;
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
                case 3 -> Game.Status.class;
                case 5 -> Game.Ownership.class;
                default -> String.class;
            };
        }

        @Override public boolean isCellEditable(int rowIndex, int columnIndex) { return false; }
    }

    // Filter: digits only

    private static class IntegersOnlyFilter extends DocumentFilter {
        @Override
        public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr)
                throws BadLocationException {
            if (string != null && string.matches("\\d+")) {
                super.insertString(fb, offset, string, attr);
            }
        }
        @Override
        public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
                throws BadLocationException {
            if (text == null || text.isEmpty() || text.matches("\\d+")) {
                super.replace(fb, offset, length, text, attrs);
            }
        }
    }

    // Main

    public static void main(String[] args) {
        try { UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel"); } catch (Exception ignored) {}
        SwingUtilities.invokeLater(CheckPointSwing::new);
    }
} // END GUI
