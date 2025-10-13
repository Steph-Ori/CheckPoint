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
import java.util.*;

public class Library {
    private final List<Game> games = new ArrayList<>();

    /**
     * method: listAll
     * parameters: none
     * return: List<Game>
     * purpose: returns an unchangable view of all games in the library.
     */

    public List<Game> listAll() {
        return Collections.unmodifiableList(games);
    }

    /**
     * method: add
     * parameters: game: Game
     * return: String
     * purpose: adds a game if its id is unique (not repeated) and returns a status message.
     */

    public String add(Game game) {
        if (findById(game.getId()).isPresent()) {
            return "❌ A game with that id already exists";
        }
        games.add(game);
        return "✅ Added:\n" + game;
    }

    /**
     * method: remove
     * parameters: id: int
     * return: String
     * purpose: removes a game by id and returns a status message.
     */

    public String remove(int id) {
        Optional<Game> hit = findById(id);
        if (hit.isEmpty()) return "No game record with id " + id + " to remove";
        games.remove(hit.get());
        return "🗑️ Remove id " + id + ".";
    }

    /**
     * method: updateField
     * parameters: id: int, field: String, newValue: String
     * return: String
     * purpose: Updates one field of a game by id and returns a status message.
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
                default -> { return "❌ Unknown field: " + field; }
            }
        } catch (Exception exception) {
            return "❌ Wrong value for " + field + ": " + exception.getMessage();
        }
        return "✅ Updated " + field + ":\n " + game;
    }

    /**
     * method: importFromFile
     * parameters: path: Path
     * return: String
     * purpose: loads games from a UTF-8 text file and returns a summary message.
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
                if (parts.length != 6) { skipped++; continue; }
                try {
                    Game g = new Game(
                            Integer.parseInt(parts[0].trim()),
                            parts[1].trim(),
                            parts[2].trim(),
                            Game.Status.valueOf(parts[3].trim().toUpperCase(Locale.ROOT)),
                            Integer.parseInt(parts[4].trim()),
                            Game.Ownership.valueOf(parts[5].trim().toUpperCase(Locale.ROOT))
                    );
                    if (findById(g.getId()).isPresent()) { skipped++; continue; }
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
     * method: findById
     * parameters: id: int
     * return: Optional<Game>
     * purpose: looks up a game by id and returns it if found.
     */

    public Optional<Game> findById(int id) {
        return games.stream().filter(g -> g.getId() == id).findFirst();
    }

    /**
     * method: scoreFor
     * parameters: game: Game
     * return: int
     * purpose: computes the backlog score for a single game.
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
     * method: backlogReport
     * parameters: topNumber: int
     * return: String
     * purpose: returns a text report with library stats and the top number priority games.
     */

    public String backlogReport(int topNumber) {
        if (games.isEmpty()) return "No games loaded yet.";

         //Building and showing stats of game(s)
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
