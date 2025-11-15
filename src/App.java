/**
 * Stephanie Ortiz
 * CEN 3024 - Software Development 1
 * November 9th, 2025
 * CheckPointSwing.java
 * ---------------------------------
 * Everything that puts CheckPoint Together
 */


import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * CheckPoint (Phase 4: Database) CLI.
 *
 * <p>This tiny driver lets a user point at an SQLite file and manage their game backlog
 * with a simple menu: connect, list, add, remove, update, and a quick stats report.
 * No hardcoded paths—the user picks the DB at runtime.</p>
 *
 * <p><b>Role in the overall system:</b> front CLI that delegates all data work to
 * {@link DbLibrary}. This class does input/validation and prints results.</p>
 *
 * <p><b>Usage:</b></p>
 * <pre>{@code
 * // Run from the command line
 * java App
 * }</pre>
 */
public class App {
    private final Scanner in = new Scanner(System.in);

    // NOTE: Now a DB-backed library (not in-memory).
    // It starts as null until the user supplies a database path.
    private DbLibrary library = null;

    /**
     * Entry point for the CLI application.
     *
     * @param args command-line arguments (unused)
     */
    public static void main(String[] args) {
        new App().run();
    }

    /**
     * Main loop with the menu. Stays running until the user chooses Exit.
     * Simple and predictable on purpose.
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

    /**
     * Prints the main menu. Kept separate so it’s easy to tweak.
     */
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

    // === Menu Handlers ===

    /**
     * Ask the user for an SQLite file path and connect to it (no hardcoding).
     * If the file doesn’t exist, {@link DbLibrary} will create the table as needed.
     * Gives clear feedback if the folder path looks wrong.
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
     * Show all records from the database (or a friendly message if there aren’t any).
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
     * Collect user input, validate, add a new record, then refresh the list.
     * Keeps the flow tight so adding a game feels quick.
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
     * Remove a record by id and then refresh the display.
     * Quick way to clean up mistakes or finished items.
     */
    private void handleRemove() {
        if (!ensureConnected()) return;
        int id = readPositiveInt("Enter id to remove game");
        println(library.remove(id));
        handleDisplay();
    }

    /**
     * Let the user update a single field safely with validation.
     * Keeps guardrails (known fields, enums, and range checks).
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
     * Ask for a number and print a small “Top Number” report from the DB.
     * Great for a quick backlog snapshot.
     */
    private void handleCustomFeature() {
        if (!ensureConnected()) return;
        int topNumber = readIntInRange("How many top priority games do you want to show?", 1, 10);
        println(library.backlogReport(topNumber));
    }

    // == Helpers ==

    /**
     * Guard: stop and remind the user to connect to a DB first.
     *
     * @return {@code true} if a DB is connected; {@code false} otherwise
     */
    private boolean ensureConnected() {
        if (library == null) {
            println("⚠️  Not connected. Choose option 1 to connect to a database first.");
            return false;
        }
        return true;
    }

    /**
     * Keep asking until the user enters a non-blank string.
     *
     * @param prompt what to print before reading
     * @return trimmed, non-empty input
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
     * Read a positive integer with simple validation.
     *
     * @param prompt what to print before reading
     * @return integer &gt; 0
     */
    private int readPositiveInt(String prompt) {
        return readIntInRange(prompt, 1, Integer.MAX_VALUE);
    }

    /**
     * Read an integer in the inclusive range {@code [min, max]}.
     *
     * @param prompt label shown to the user
     * @param min    minimum allowed value
     * @param max    maximum allowed value
     * @return a validated integer inside the range
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
     * Read an integer in range with a one-time escape hatch.
     *
     * @param enterIdToUpdate prompt to show (includes the hint to press {@code C} to cancel)
     * @param min             minimum allowed value
     * @param max             maximum allowed value
     * @return the chosen integer, or {@code null} if the user typed {@code C}
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
     * Ensure the new id is unique (> 0 and not already in the DB).
     *
     * @return a valid id ready for insert
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
     * Read an enum value by name (case-insensitive) with friendly retries.
     *
     * @param <E>       enum type
     * @param prompt    text to show before reading
     * @param enumType  {@code Class} of the enum to parse
     * @return a valid enum constant from {@code enumType}
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

    // == Tiny wrappers to keep code tidy ==
    private void println(String s) { System.out.println(s); }
    private void print(String s)   { System.out.print(s); }

} // === END APP ===

