/**
 * Stephanie Ortiz
 * CEN 3024 - Software Development 1
 * November 5th, 2025
 * CheckPointSwing.java
 * ---------------------------------
 * Everything that puts CheckPoint Together
 */



import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.nio.file.Files;
import java.nio.file.Path;

public class App {
    private final Scanner in = new Scanner(System.in);

    // NOTE: now we talk to the DB-backed library (not in-memory).
    // It starts as null until the user supplies a database path.
    private DbLibrary library = null;

    /**
     * method: main
     * parameters: args: String[]
     * return: void
     * purpose: Entry point for CLI application.
     */
    public static void main(String[] args) {
        new App().run();
    }

    /**
     * method: run
     * parameters: none
     * return: void
     * purpose: Main loop with menu; never exits unless the user chooses to.
     */
    public void run() {
        println("\n=== CHECKPOINT (Phase 4: Database) ===\n");

        boolean running = true;
        while (running) {
            showMenu();
            int choice = readIntInRange("Choose an option", 0, 6);
            switch (choice) {
                case 1 -> handleConnect();     // NEW: user supplies DB path here
                case 2 -> handleDisplay();
                case 3 -> handleCreate();
                case 4 -> handleRemove();
                case 5 -> handleUpdate();
                case 6 -> handleCustomFeature();
                case 0 -> {
                    println("Goodbye!");
                    running = false;
                }
            }
            if (running) {
                println("\nPress ENTER to continue...");
                in.nextLine();
            }
        }
    }

    private void showMenu() {
        println("""
                ---------------------------
                1) Connect / Change SQLite database
                2) Display all game records
                3) Create (add) a new game record
                4) Remove a game record
                5) Update a game record
                6) Custom: Backlog score & stats
                0) Exit
                ---------------------------""");
    }

    // Menu Handlers

    /**
     * method: handleConnect
     * parameters: none
     * return: void
     * purpose: Ask the user for an SQLite file path and connect to it (no hardcoding).
     *          If the file does not exist, DbLibrary will still create the table as needed.
     */
    private void handleConnect() {
        while (true) {
            String pathStr = readNonEmpty("Enter the path to your SQLite database file (e.g., G:/checkpoint.db)");
            try {
                // It is OK if the file doesn't exist yet, DbLibrary ensures the table exists.
                Path p = Path.of(pathStr);
                // Optional user feedback if they mistyped a folder etc.
                if (p.getParent() != null && !Files.exists(p.getParent())) {
                    println("⚠️  The folder '" + p.getParent() + "' does not exist. Try again.");
                    continue;
                }
                library = new DbLibrary(pathStr);
                println("✅ Connected to database: " + pathStr);
                // Show what’s inside right away so user sees it’s working.
                handleDisplay();
                return;
            } catch (Exception ex) {
                println("❌ Could not connect: " + ex.getMessage());
            }
        }
    }

    /**
     * method: handleDisplay
     * parameters: none
     * return: void
     * purpose: Show all records from the database (or a friendly message if none).
     */
    private void handleDisplay() {
        if (!ensureConnected()) return;
        List<Game> all = library.listAll();
        if (all.isEmpty()) {
            println("(no records yet)");
            return;
        }
        println("\nCurrent Library:");
        for (Game game : all) println(game.toString());
    }

    /**
     * method: handleCreate
     * parameters: none
     * return: void
     * purpose: Collect all fields, validate, add to the database, and then show the new list.
     */
    private void handleCreate() {
        if (!ensureConnected()) return;
        println("Add a new game:");

        int id = readUniqueIdForCreate(); // immediate validation
        String name = readNonEmpty("Name");
        String platform = readNonEmpty("Platform");
        Game.Status status = readEnum("Status [UNPLAYED, PLAYING, BEATEN]", Game.Status.class);
        int priority = readIntInRange("Priority (1-5)", 1, 5);
        Game.Ownership own = readEnum("Ownership [PHYSICAL, DIGITAL]", Game.Ownership.class);

        try {
            Game g = new Game(id, name, platform, status, priority, own);
            println(library.add(g));
            handleDisplay();
        } catch (Exception ex) {
            println("❌ " + ex.getMessage());
        }
    }

    /**
     * method: handleRemove
     * parameters: none
     * return: void
     * purpose: Removes a record by id and then refreshes the display.
     */
    private void handleRemove() {
        if (!ensureConnected()) return;
        int id = readPositiveInt("Enter id to remove game");
        println(library.remove(id));
        handleDisplay();
    }

    /**
     * method: handleUpdate
     * parameters: none
     * return: void
     * purpose: Lets the user update any single field safely.
     */
    private void handleUpdate() {
        if (!ensureConnected()) return;

        Integer id = readIntInRangeOrCancelOnce("Enter id to update", 1, Integer.MAX_VALUE);
        if (id == null) {
            println("Update cancelled.");
            return;
        }

        if (library.findById(id).isEmpty()) {
            println("⚠️ No record with id " + id + ". Returning to menu.");
            return;
        }

        println("Fields: name, platform, status, priority, ownership");
        String field = readNonEmpty("Which field?");

        String value;
        if (field.equalsIgnoreCase("status")) {
            value = readEnum("New status [UNPLAYED, PLAYING, BEATEN]", Game.Status.class).name();
        } else if (field.equalsIgnoreCase("ownership")) {
            value = readEnum("New ownership [PHYSICAL, DIGITAL]", Game.Ownership.class).name();
        } else if (field.equalsIgnoreCase("priority")) {
            value = String.valueOf(readIntInRange("New priority (1-5)", 1, 5));
        } else if (field.equalsIgnoreCase("name") || field.equalsIgnoreCase("platform")) {
            value = readNonEmpty("New value");
        } else {
            println("❌ Unknown field.");
            return;
        }

        println(library.updateField(id, field, value));
        handleDisplay();
    }

    /**
     * method: handleCustomFeature
     * parameters: none
     * return: void
     * purpose: Ask for Number and display the CheckPoint report calculated from DB information
     */
    private void handleCustomFeature() {
        if (!ensureConnected()) return;
        int topNumber = readIntInRange("How many top priority games do you want to show?", 1, 10);
        println(library.backlogReport(topNumber));
    }

    // Helpers

    /**
     * method: ensureConnected
     * parameters: none
     * return: boolean
     * purpose: Guards all operations until a DB connection is chosen by the user.
     */
    private boolean ensureConnected() {
        if (library == null) {
            println("⚠️  Not connected. Choose option 1 to connect to a database first.");
            return false;
        }
        return true;
    }

    /**
     * method: readNonEmpty
     * parameters: String
     * return: String
     * purpose: Keep asking until a non-blank string is entered.
     */
    private String readNonEmpty(String prompt) {
        while (true) {
            print(prompt + ": ");
            String s = in.nextLine();
            if (s != null && !s.trim().isEmpty()) return s.trim();
            println("Please enter a value.");
        }
    }

    /**
     * method: readPositiveInt
     * parameters: String
     * return: int
     * purpose: Read a positive integer with validation.
     */
    private int readPositiveInt(String prompt) {
        return readIntInRange(prompt, 1, Integer.MAX_VALUE);
    }

    /**
     * method: readIntInRange
     * parameters: String, min, max
     * return: int
     * purpose: Very extra numeric input in [min, max].
     */
    private int readIntInRange(String prompt, int min, int max) {
        while (true) {
            print(prompt + ": ");
            String s = in.nextLine();
            if (s == null || s.trim().isEmpty()) continue;
            try {
                int n = Integer.parseInt(s.trim());
                if (n < min || n > max) {
                    println("Enter a number between " + min + " and " + max + ".");
                } else {
                    return n;
                }
            } catch (Exception e) {
                println("Please enter a valid number.");
            }
        }
    }

    /**
     * method: readIntInRangeOrCancelOnce
     * parameters: enterIdToUpdate, min, max
     * return: Integer (null if cancelled)
     * purpose: Allows a single cancel in the update flow.
     */
    private Integer readIntInRangeOrCancelOnce(String enterIdToUpdate, int min, int max) {
        while (true) {
            print(enterIdToUpdate + " (or C to cancel): ");
            String s = in.nextLine();
            if (s != null && s.trim().equalsIgnoreCase("C")) return null;
            try {
                int n = Integer.parseInt(s.trim());
                if (n < min || n > max) {
                    println("Enter a number between " + min + " and " + max + ".");
                } else {
                    return n;
                }
            } catch (Exception e) {
                println("Please enter a valid number.");
            }
        }
    }

    /**
     * method: readUniqueIdForCreate
     * parameters: none
     * return: int
     * purpose: Ensures the id is > 0 and not already in use in the current DB.
     */
    private int readUniqueIdForCreate() {
        while (true) {
            int id = readPositiveInt("Id (>0)");
            if (library.findById(id).isPresent()) {
                println("❌ That id already exists. Please choose a different id.");
            } else {
                return id;
            }
        }
    }

    /**
     * method: readEnum
     * parameters: prompt, enumType
     * return: E
     * purpose: Reads an enum by name, case-insensitive, with validation.
     */
    private <E extends Enum<E>> E readEnum(String prompt, Class<E> enumType) {
        while (true) {
            print(prompt + ": ");
            String s = in.nextLine();
            try {
                return Enum.valueOf(enumType, s.trim().toUpperCase(Locale.ROOT));
            } catch (Exception e) {
                println("Please enter one of the listed options.");
            }
        }
    }

    // Tiny wrappers to keep code tidy
    private void println(String s) { System.out.println(s); }
    private void print(String s)   { System.out.print(s); }
} // END APP
