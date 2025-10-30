import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.text.*;
import java.awt.*;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class CheckPointSwing extends JFrame {

    // Domain
    private final Library library = new Library();

    // UI: Table + Model
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
    private JButton addBtn, updateBtn, deleteBtn, clearBtn, importBtn, reportBtn;

    /**
     * method: CheckPointSwing (constructor)
     * parameters: none
     * return: (constructor)
     * purpose: Build the whole app window and show it. If there's a games.txt sitting next to the app, pull it in right away.
     */
    public CheckPointSwing() {
        super("CheckPoint (Swing)");

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1000, 560));

        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(new EmptyBorder(12, 12, 12, 12));
        setContentPane(root);

        // Table
        tableModel = new GameTableModel(library.listAll());
        table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getSelectionModel().addListSelectionListener(this::onRowSelected);

        JScrollPane tableScroll = new JScrollPane(table);
        root.add(tableScroll, BorderLayout.CENTER);

        // Form (right)
        JPanel form = buildFormPanel();
        root.add(form, BorderLayout.EAST);

        // Buttons (bottom)
        JPanel buttons = buildButtonsPanel();
        root.add(buttons, BorderLayout.SOUTH);

        // Auto-load games.txt if present
        autoLoadIfPresent();

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    // UI Builders

    /**
     * method: buildFormPanel
     * parameters: none
     * return: JPanel
     * purpose: Make the little form on the right where a user can type stuff in for a game.
     */
    private JPanel buildFormPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        panel.setPreferredSize(new Dimension(360, 0));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 220)),
                new EmptyBorder(12, 12, 12, 12)
        ));

        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(6, 6, 6, 6);
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1.0;

        JLabel title = new JLabel("Game Details");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        gc.gridx = 0; gc.gridy = 0; gc.gridwidth = 2;
        panel.add(title, gc);

        gc.gridwidth = 1;

        idField = makeIntegerField();
        nameField = new JTextField();
        platformField = new JTextField();
        statusBox = new JComboBox<>(Game.Status.values());
        statusBox.setSelectedItem(null);
        priorityField = makeIntegerField();
        ownershipBox = new JComboBox<>(Game.Ownership.values());
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

    /**
     * method: buildButtonsPanel
     * parameters: none
     * return: JPanel
     * purpose: Set up the buttons along the bottom and hook them up so the user actually do things.
     */
    private JPanel buildButtonsPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));

        addBtn = new JButton("Add");
        updateBtn = new JButton("Update Selected");
        deleteBtn = new JButton("Delete Selected");
        clearBtn = new JButton("Clear Form");
        importBtn = new JButton("Import From File");
        reportBtn = new JButton("CheckPoint Report");

        addBtn.addActionListener(e -> onAdd());
        updateBtn.addActionListener(e -> onUpdate());
        deleteBtn.addActionListener(e -> onDelete());
        clearBtn.addActionListener(e -> clearForm());
        importBtn.addActionListener(e -> onImport());
        reportBtn.addActionListener(e -> onReport());

        panel.add(addBtn);
        panel.add(updateBtn);
        panel.add(deleteBtn);
        panel.add(new JSeparator(SwingConstants.VERTICAL) {{ setPreferredSize(new Dimension(12, 22)); }});
        panel.add(clearBtn);
        panel.add(importBtn);
        panel.add(reportBtn);

        return panel;
    }

    /**
     * method: addRow
     * parameters: JPanel panel, GridBagConstraints gc, int row, String label, JComponent field
     * return: void
     * purpose: Drop a label and its input into the form at the right spot. No fuss.
     */
    private void addRow(JPanel panel, GridBagConstraints gc, int row, String label, JComponent field) {
        gc.gridx = 0; gc.gridy = row; gc.weightx = 0;
        panel.add(new JLabel(label), gc);
        gc.gridx = 1; gc.weightx = 1.0;
        panel.add(field, gc);
    }

    // Actions

    /**
     * method: onRowSelected
     * parameters: ListSelectionEvent e
     * return: void
     * purpose: Click a row, and the form fills itself so the user can tweak it without retyping.
     */
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

    /**
     * method: onAdd
     * parameters: none
     * return: void
     * purpose: Take whatever was typed, make a new game from it, toss it into the list, and show a little "yep, it worked."
     */
    private void onAdd() {
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

    /**
     * method: onUpdate
     * parameters: none
     * return: void
     * purpose: Change the selected game to match what’s in the form. Same game, cleaned up.
     */
    private void onUpdate() {
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

            // If ID changes, ensure it is unique.
            if (newId != sel.getId() && library.findById(newId).isPresent()) {
                throw new IllegalArgumentException("That ID already exists. Choose a different ID.");
            }

            // The validations using setters.
            sel.setId(newId);
            sel.setName(name);
            sel.setPlatform(platform);
            sel.setStatus(status);
            sel.setPriority(priority);
            sel.setOwnership(ownership);

            refreshTable();
            selectGameInTable(newId);
            showInfo("Update", "✅ Updated:\n" + sel);
        } catch (Exception ex) {
            showError("Update failed", ex.getMessage());
        }
    }

    /**
     * method: onDelete
     * parameters: none
     * return: void
     * purpose: Ask a prompt “you sure?”, then remove the picked game and tidy up the screen.
     */
    private void onDelete() {
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

    /**
     * method: onImport
     * parameters: none
     * return: void
     * purpose: Pick a text file and pull the games in—super quick way to load a list.
     */
    private void onImport() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select game list (id|name|platform|status|priority|ownership)");
        int res = chooser.showOpenDialog(this);
        if (res != JFileChooser.APPROVE_OPTION) return;

        File file = chooser.getSelectedFile();
        String msg = library.importFromFile(file.toPath());
        refreshTable();
        showInfo("Import", msg);
    }

    /**
     * method: onReport
     * parameters: none
     * return: void
     * purpose: Ask how many top priorities the user wants, whip up a quick report, and pop it open.
     */
    private void onReport() {
        String input = JOptionPane.showInputDialog(this, "How many top priority games do you want to show? (1-10)", "5");
        if (input == null) return;
        try {
            int n = parseIntStrict(input, "Top N");
            if (n < 1 || n > 10) throw new IllegalArgumentException("Enter a number between 1 and 10.");
            String report = library.backlogReport(n);

            JTextArea area = new JTextArea(report, 18, 60);
            area.setEditable(false);
            area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
            JScrollPane sp = new JScrollPane(area);
            JOptionPane.showMessageDialog(this, sp, "📊 Backlog Health", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            showError("Invalid number", ex.getMessage());
        }
    }

    // Helpers

    /**
     * method: autoLoadIfPresent
     * parameters: none
     * return: void
     * purpose: If there’s a games.txt around, it will load it so user is not starting from scratch.
     */
    private void autoLoadIfPresent() {
        Path p = Paths.get("games.txt");
        if (p.toFile().exists()) {
            String msg = library.importFromFile(p);
            refreshTable();
            System.out.println(msg);
        }
    }

    /**
     * method: refreshTable
     * parameters: none
     * return: void
     * purpose: Tell the table, “hey, the list changed” so it redo's with the latest.
     */
    private void refreshTable() {
        tableModel.setData(library.listAll());
    }

    /**
     * method: clearForm
     * parameters: none
     * return: void
     * purpose: Wipe the inputs so user can start fresh. Also unselect anything in the table.
     */
    private void clearForm() {
        idField.setText("");
        nameField.setText("");
        platformField.setText("");
        statusBox.setSelectedItem(null);
        priorityField.setText("");
        ownershipBox.setSelectedItem(null);
        table.clearSelection();
    }

    /**
     * method: clearFormKeepSelection
     * parameters: none
     * return: void
     * purpose: Empty the inputs but keep user's current row selected so they don’t lose their place.
     */
    private void clearFormKeepSelection() {
        idField.setText("");
        nameField.setText("");
        platformField.setText("");
        statusBox.setSelectedItem(null);
        priorityField.setText("");
        ownershipBox.setSelectedItem(null);
    }

    /**
     * method: selectGameInTable
     * parameters: int id
     * return: void
     * purpose: Jump the table to the game with this ID and highlight it for user.
     */
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

    /**
     * method: makeIntegerField
     * parameters: none
     * return: JTextField
     * purpose: A text box that ONLY takes digits. No accidental letters sneaking in.
     */
    private JTextField makeIntegerField() {
        JTextField tf = new JTextField();
        ((AbstractDocument) tf.getDocument()).setDocumentFilter(new IntegersOnlyFilter());
        return tf;
    }

    /**
     * method: parseIntStrict
     * parameters: String s, String label
     * return: int
     * purpose: Turn a string into a whole number or throw a clear, friendly error with the field name.
     */
    private int parseIntStrict(String s, String label) {
        if (s == null || s.isBlank()) throw new IllegalArgumentException(label + " is required.");
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException nfe) {
            throw new IllegalArgumentException(label + " must be a whole number.");
        }
    }

    /**
     * method: requirePositive
     * parameters: int n, String label
     * return: int
     * purpose: Make sure the number is above zero, no zero or negatives allowed.
     */
    private int requirePositive(int n, String label) {
        if (n <= 0) throw new IllegalArgumentException(label + " must be > 0.");
        return n;
    }

    /**
     * method: requirePriority
     * parameters: int p
     * return: int
     * purpose: Priority has to be between 1 and 5. If it isn’t, it will be called out with a message.
     */
    private int requirePriority(int p) {
        if (p < 1 || p > 5) throw new IllegalArgumentException("Priority must be 1–5.");
        return p;
    }

    /**
     * method: requireSelected
     * parameters: T value, String label
     * return: T
     * purpose: User has to actually choose something in the dropdown, no blank picks.
     */
    private <T> T requireSelected(T value, String label) {
        if (value == null) throw new IllegalArgumentException(label + " is required.");
        return value;
    }

    /**
     * method: requireNonEmpty
     * parameters: String s, String label
     * return: String
     * purpose: Trim the text and make sure User didn’t leave it empty.
     */
    private String requireNonEmpty(String s, String label) {
        if (s == null || s.trim().isEmpty()) throw new IllegalArgumentException(label + " is required.");
        return s.trim();
    }

    /**
     * method: ensureUniqueIdOnCreate
     * parameters: int id
     * return: void
     * purpose: Does not let a user add two games with the same ID.
     */
    private void ensureUniqueIdOnCreate(int id) {
        if (library.findById(id).isPresent()) {
            throw new IllegalArgumentException("That ID already exists. Choose a different ID.");
        }
    }

    /**
     * method: showInfo
     * parameters: String title, String message
     * return: void
     * purpose: Quick “heads up” box for normal messages.
     */
    private void showInfo(String title, String message) {
        JOptionPane.showMessageDialog(this, message, title, JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * method: showError
     * parameters: String title, String message
     * return: void
     * purpose: Pop a red dialog when something’s off so User know's what to fix.
     */
    private void showError(String title, String message) {
        JOptionPane.showMessageDialog(this, message, title, JOptionPane.ERROR_MESSAGE);
    }

    // TableModel

    private static class GameTableModel extends AbstractTableModel {
        private final String[] cols = {"ID", "Name", "Platform", "Status", "Priority", "Ownership"};
        private List<Game> data;

        /**
         * method: GameTableModel (constructor)
         * parameters: List<Game> data
         * return: (constructor)
         * purpose: Hold onto the list of games so the table knows what to show.
         */
        GameTableModel(List<Game> data) {
            this.data = data;
        }

        /**
         * method: setData
         * parameters: List<Game> fresh
         * return: void
         * purpose: Swap in a new list and nudge the table to refresh.
         */
        void setData(List<Game> fresh) {
            this.data = fresh;
            fireTableDataChanged();
        }

        /**
         * method: getAt
         * parameters: int row
         * return: Game
         * purpose: Hand back the game at this row, or nothing if that row doesn’t exist.
         */
        Game getAt(int row) {
            if (row < 0 || row >= getRowCount()) return null;
            return data.get(row);
        }

        /**
         * method: getRowCount
         * parameters: none
         * return: int
         * purpose: How many games are showing?
         */
        @Override public int getRowCount() { return data == null ? 0 : data.size(); }

        /**
         * method: getColumnCount
         * parameters: none
         * return: int
         * purpose: How many columns show up in the table.
         */
        @Override public int getColumnCount() { return cols.length; }

        /**
         * method: getColumnName
         * parameters: int col
         * return: String
         * purpose: The label at the top of each column.
         */
        @Override public String getColumnName(int col) { return cols[col]; }

        /**
         * method: getValueAt
         * parameters: int rowIndex, int columnIndex
         * return: Object
         * purpose: What should the table display in this exact cell?
         */
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

        /**
         * method: getColumnClass
         * parameters: int columnIndex
         * return: Class<?>
         * purpose: Tell the table what kind of data each column is so sorting looks right.
         */
        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return switch (columnIndex) {
                case 0, 4 -> Integer.class;
                case 3 -> Game.Status.class;
                case 5 -> Game.Ownership.class;
                default -> String.class;
            };
        }

        /**
         * method: isCellEditable
         * parameters: int rowIndex, int columnIndex
         * return: boolean
         * purpose: Keep the table read-only so edits go through the safer form on the right.
         */
        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return false;
        }
    }

    // Numbers only filter

    private static class IntegersOnlyFilter extends DocumentFilter {
        /**
         * method: insertString
         * parameters: FilterBypass fb, int offset, String string, AttributeSet attr
         * return: void
         * purpose: Only allow digits when user types. Letters get ignored.
         */
        @Override
        public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr)
                throws BadLocationException {
            if (string != null && string.matches("\\d+")) {
                super.insertString(fb, offset, string, attr);
            } // else ignore
        }

        /**
         * method: replace
         * parameters: FilterBypass fb, int offset, int length, String text, AttributeSet attrs
         * return: void
         * purpose: Replacing text, Still digits-only, unless user is deleting.
         */
        @Override
        public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
                throws BadLocationException {
            if (text == null || text.isEmpty()) {
                super.replace(fb, offset, length, text, attrs);
                return;
            }
            if (text.matches("\\d+")) {
                super.replace(fb, offset, length, text, attrs);
            } // else ignore
        }
    }

    /**
     * method: main
     * parameters: String[] args
     * return: void
     * purpose: Make the app look like an OS and kick it off on the Swing thread.
     */
    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}
        SwingUtilities.invokeLater(CheckPointSwing::new);
    }
} // END GUI
