/**
 * Stephanie Ortiz
 * CEN 3024 - Software Development 1
 * October 7th, 2025
 * App.java
 * ---------------------------------
 * This class is the main driver for CheckPoint App.
 * It will manage user inputs through the CLI
 * Has all CRUD operations here.
 */

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.nio.file.Files;

public class App {
    private final Scanner in = new Scanner(System.in);
    private final Library library = new Library();

    /**
     * method: autoLoadIfPresent
     * parameters: none
     * return: void
     * purpose: IF a file named 'games.txt' is in the directory, it will import and display.
     */

    private void autoLoadIfPresent() {
        Path p = Path.of("games.txt"); // looks into the directory
        if (Files.exists(p)) {
            println(library.importFromFile(p));
            handleDisplay();
        }
    }

    /**
     * method: main
     * parameters: args: String[]
     * return: void
     * purpose: The start point, creates App and starts the loop.
     */

    public static void main(String[] args) {
        new App().run(); // this is the only static method used
    }

    /**
     * method: run
     * parameters: none
     * return: void
     * purpose: Shows menu, the user choices and app flow.
     */

    public void run() {
        println("\n=== CHECKPOINT (Phase 1: CLI) ===\n");
        autoLoadIfPresent();
        boolean running = true;
        while (running) {
            showMenu();
            int choice = readIntInRange("Choose an option", 0, 6);
            switch (choice) {
                case 1 -> handleImport();
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
                1) Load Game data from text file
                2) Display all game records
                3) Create (add) a new game record
                4) Remove a game record
                5) Update a game record
                6) Custom: Backlog score & stats
                0) Exit
                ---------------------------""");
    }

    //The Menu Handlers.

    /**
     * method: handleImport
     * parameters: none
     * return: void
     * purpose: Asks for a file path, imports games and then prints the library.
     */

    private void handleImport() {
        String pathStr = readNonEmpty("Enter the file path to your game list (example: src/games.txt)");
        String result = library.importFromFile(Path.of(pathStr));
        println(result);
        handleDisplay();
    }

    /**
     * method: handleDisplay
     * parameters: none
     * return: void
     * purpose: This will show all games and will let user know if none exist.
     */

    private void handleDisplay() {
        List<Game> all = library.listAll();
        if (all.isEmpty()) {
            println( "(no records yet)");
            return;
        }
        println("\nCurrentLibrary:");
        for (Game game : all) println(game.toString());
    }

    /**
     * method: handleCreate
     * parameters: none
     * return: void
     * purpose: This is where all information for game is collected.
     * Then makes sure information is correct.
     * Then add to library and shows new library.
     */

    private void handleCreate() {
        println("Add a new game:");

        int id = readUniqueIdForCreate();                   // <-- immediate validation here
        String name = readNonEmpty("Name");
        String platform = readNonEmpty("Platform");
        Game.Status status = readEnum("Status [UNPLAYED, PLAYING, BEATEN]", Game.Status.class);
        int priority = readIntInRange("Priority (1-5)", 1, 5);
        Game.Ownership own = readEnum("Ownership [PHYSICAL, DIGITAL]", Game.Ownership.class);

        try {
            Game g = new Game(id, name, platform, status, priority, own);
            println(library.add(g));   // still double-protects, but user already got fast feedback
            handleDisplay();
        } catch (Exception ex) {
            println("❌ " + ex.getMessage());
        }
    }

    /**
     * method: handleRemove
     * parameters:none
     * return: void
     * purpose: Aks for id, removes the matching game and then shows new library.
     */

    private void handleRemove() {
        int id = readPositiveInt("Enter Id to remove game");
        println(library.remove(id));
        handleDisplay();
    }

    /**
     * method: handleUpdate
     * parameters: none
     * return: void
     * purpose: Lets the user update an existing game and allows for a cancel IF miss clicked.
     */

    private void handleUpdate() {
        // Cancel only here (user may have pressed 5 by accident)
        Integer id = readIntInRangeOrCancelOnce("Enter id to update", 1, Integer.MAX_VALUE);
        if (id == null) {
            println("Update cancelled.");
            return;
        }

        // Id must exist or it will exit

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
     * parameters:none
     * return: void
     * purpose: Asks for number of games and then shows the top number of priority games.
     */

    private void handleCustomFeature() {
        int topNumber = readIntInRange("How many top priority games do you want to show?", 1, 10);
        println(library.backlogReport(topNumber));
    }


    // Input Helpers (made sure for none to be static)

    /**
     * method: readNonEmpty
     * parameters: String
     * return: String
     * purpose: Makes sure this is no blank line and will continue to ask user for valid input until correct.
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
     * purpose: Read a positive number and will keep asking until valid.
     */

    private int readPositiveInt(String prompt) {
        return readIntInRange(prompt, 1, Integer.MAX_VALUE);
    }

    /**
     * method: readIntInRange
     * parameters: String, min/max int
     * return: int
     * purpose: Reads a number between min and max, will keep asking until valid entry.
     */

    private int readIntInRange(String prompt, int min, int max) {
        while (true) {
            print(prompt + ": ");
            String s = in.nextLine();
            if (s == null || s.trim().isEmpty()) {
                // this ignores the extra line and goes straight into the next prompt.
                continue;
            }
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
     * parameters: enterIdToUpdate, String, min, max.
     * return: int
     * purpose: Reads an enum value by name and keeps checking until valid.
     */

    // This only happens if user hits cancel at the start of update
    private Integer readIntInRangeOrCancelOnce(String enterIdToUpdate, int min, int max) {
        while (true) {
            print("Enter id to update" + " (or C to cancel): ");
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

    // For CREATE: Making sure ID is not duplicate from the start.
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

    // Tiny wrappers to keep code tidy (instance, not static)

    /**
     * method: println
     * parameters: String
     * return: void
     * purpose: Wrapper to print a line with a new one.
     */

    private void println(String s) { System.out.println(s); }

    /**
     * method: print
     * parameters: String
     * return: void
     * purpose: Wrapper to print without a new line.
     */

    private void print(String s)   { System.out.print(s); }

} // END APP

