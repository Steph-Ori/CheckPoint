/**
 * Stephanie Ortiz
 * CEN 3024 - Software Development 1
 * October 7th, 2025
 * Library.java
 * ---------------------------------
 * The library (collection) of all games
 * Has crud here and the import of data from files
 * Holds the user interface.
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Legacy in-memory library for the original CheckPoint phases.
 *
 * <p>This class keeps games in a simple {@link List} in memory and exposes
 * basic operations such as list, add, remove, update, import, and reporting.
 * It mirrors the behavior of {@link DbLibrary} closely enough so that the
 * earlier JUnit tests still compile and run, even after the app moved to
 * a database-backed implementation.</p>
 *
 * <p><b>Role in the overall system:</b> test-only, in-memory version of the
 * game library. The main GUI and CLI use {@link DbLibrary} instead.</p>
 */
public class Library_OLD {

    private final List<Game> games = new ArrayList<>();

    // Kept for historical reasons; not used by the in-memory methods.
    private final DbLibrary library = new DbLibrary("G:/checkpoint.db");

    /**
     * Returns an unmodifiable view of all games currently loaded in memory.
     *
     * @return unmodifiable list of games
     */
    public List<Game> listAll() {
        return Collections.unmodifiableList(games);
    }

    /**
     * Adds a game if its id is unique and returns a status message.
     *
     * @param game game to add
     * @return status message describing the result
     */
    public String add(Game game) {
        if (findById(game.getId()).isPresent()) {
            return "❌ A game with that id already exists";
        }
        games.add(game);
        return "✅ Added:\n" + game;
    }

    /**
     * Removes a game by id and returns a status message.
     *
     * @param id id of the game to remove
     * @return status message describing the result
     */
    public String remove(int id) {
        Optional<Game> hit = findById(id);
        if (hit.isEmpty()) return "No game record with id " + id + " to remove";
        games.remove(hit.get());
        return "🗑️ Remove id " + id + ".";
    }

    /**
     * Updates one field of a game by id and returns a status message.
     *
     * @param id       id of the game to update
     * @param field    field name to update (name, platform, status, priority, ownership)
     * @param newValue new value for the given field
     * @return status message describing the result
     */
    public String updateField(int id, String field, String newValue) {
        Optional<Game> hit = findById(id);
        if (hit.isEmpty()) return "⚠️ No game record with id " + id + " to update";
        Game game = hit.get();
        try {
            switch (field.toLowerCase(Locale.ROOT)) {
                case "name" -> game.setName(newValue);
                case "platform" -> game.setPlatform(newValue);
                case "status" -> game.setStatus(Game.Status.valueOf(newValue.toUpperCase(Locale.ROOT)));
                case "priority" -> game.setPriority(Integer.parseInt(newValue));
                case "ownership" -> game.setOwnership(Game.Ownership.valueOf(newValue.toUpperCase(Locale.ROOT)));
                default -> {
                    return "❌ Unknown field: " + field;
                }
            }
        } catch (Exception exception) {
            return "❌ Wrong value for " + field + ": " + exception.getMessage();
        }
        return "✅ Updated " + field + ":\n " + game;
    }

    /**
     * Loads games from a UTF-8 text file and returns a summary message.
     * Existing games are left in place; imported games are appended.
     *
     * @param path path to the text file to import
     * @return summary message with added/skipped counts
     */
    public String importFromFile(Path path) {
        if (path == null) return "❌ Path is needed.";
        if (!Files.exists(path)) return "❌ File not found: " + path;

        int added = 0, skipped = 0, lineNo = 0;
        try (BufferedReader bufferedReader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                lineNo++;
                if (line.trim().isEmpty() || line.trim().startsWith("#")) continue;
                // Format: id|name|platform|status|priority|ownership
                String[] parts = line.split("\\|");
                if (parts.length != 6) {
                    skipped++;
                    continue;
                }
                try {
                    Game g = new Game(
                            Integer.parseInt(parts[0].trim()),
                            parts[1].trim(),
                            parts[2].trim(),
                            Game.Status.valueOf(parts[3].trim().toUpperCase(Locale.ROOT)),
                            Integer.parseInt(parts[4].trim()),
                            Game.Ownership.valueOf(parts[5].trim().toUpperCase(Locale.ROOT))
                    );
                    if (findById(g.getId()).isPresent()) {
                        skipped++;
                        continue;
                    }
                    games.add(g);
                    added++;
                } catch (Exception exception) {
                    skipped++;
                }
            }
        } catch (IOException exception) {
            return " Error reading file: " + exception.getMessage();
        }
        return String.format("📥 Import complete. Added: %d, Skipped: %d, Total now: %d",
                added, skipped, games.size());
    }

    /**
     * Looks up a game by its id.
     *
     * @param id id to search for
     * @return an {@code Optional} containing the game if found, or empty otherwise
     */
    public Optional<Game> findById(int id) {
        return games.stream().filter(g -> g.getId() == id).findFirst();
    }

    /**
     * Computes a backlog score for a single game.
     *
     * <p>Status contributes a weight (UNPLAYED &gt; PLAYING &gt; BEATEN) and is
     * combined with priority to give a simple "tackle this next" score.</p>
     *
     * @param game game to score
     * @return numeric score where higher means more urgent
     */
    public int scoreFor(Game game) {
        int statusWeight = switch (game.getStatus()) {
            case UNPLAYED -> 3;
            case PLAYING -> 1;
            case BEATEN -> 0;
        };
        return (game.getPriority() * 2) + statusWeight;
    }

    /**
     * Builds a text report with overall stats and the top Number games to tackle next.
     *
     * @param topNumber how many top-scoring games to include
     * @return multi-line text report with backlog stats and top games
     */
    public String backlogReport(int topNumber) {
        if (games.isEmpty()) return "No games loaded yet.";

        // Building and showing stats of game(s)
        long unplayed = games.stream().filter(g -> g.getStatus() == Game.Status.UNPLAYED).count();
        long playing = games.stream().filter(g -> g.getStatus() == Game.Status.PLAYING).count();
        long beaten = games.stream().filter(g -> g.getStatus() == Game.Status.BEATEN).count();

        // Sort by score number
        List<Game> sorted = new ArrayList<>(games);
        sorted.sort((a, b) -> Integer.compare(scoreFor(b), scoreFor(a)));

        StringBuilder sb = new StringBuilder();
        sb.append("📊 Backlog Health\n")
                .append("Total: ").append(games.size())
                .append(" | Unplayed: ").append(unplayed)
                .append(" | Playing: ").append(playing)
                .append(" | Beaten: ").append(beaten).append("\n\n")
                .append("🔥 Top ").append(Math.min(topNumber, sorted.size())).append(" To Tackle Next:\n");
        for (int i = 0; i < Math.min(topNumber, sorted.size()); i++) {
            Game g = sorted.get(i);
            sb.append(String.format("%d) [%d] %s (score=%d)\n", i + 1, g.getId(), g.getName(), scoreFor(g)));
        }
        return sb.toString();
    }

} // END LIBRARY
