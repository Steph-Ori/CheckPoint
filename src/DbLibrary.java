/**
 * Stephanie Ortiz
 * CEN 3024 - Software Development 1
 * November 9th, 2025
 * DbLibrary.java
 */

import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;

/**
 * SQLite-backed library for game records.
 *
 * <p>Same API as the in-memory {@code Library}, but data lives in an SQLite DB.
 * This class owns the JDBC connection string, ensures the table exists,
 * and keeps an in-memory snapshot synced with the DB for fast reads.</p>
 *
 * <p><b>Role in the system:</b> data access layer for the CLI/Swing apps.
 * All persistence work (insert/update/delete/query) flows through here.</p>
 *
 * <p><b>Usage:</b> construct with a file path and call the CRUD methods.
 * The constructor will create the table if needed and load rows.</p>
 */
public class DbLibrary {

    private final List<Game> games = new ArrayList<>();
    private final String url; // Example: jdbc:sqlite:G:/checkpoint.db

    /**
     * Build a DB-backed library for the given SQLite file.
     * Ensures schema exists and loads current rows into memory.
     *
     * @param sqliteFilePath path to the SQLite database file (created if missing)
     */
    public DbLibrary(String sqliteFilePath) {
        this.url = "jdbc:sqlite:" + sqliteFilePath;
        ensureTable();
        reloadFromDb();
    }

    // == Public API (same as Library) ==

    /**
     * Returns all games as an unmodifiable view.
     *
     * @return immutable list of games currently loaded
     */
    public List<Game> listAll() {
        return Collections.unmodifiableList(games);
    }

    /**
     * Add a game if its id is unique; persists to DB and updates memory.
     *
     * @param game fully-populated {@link Game} to insert
     * @return status string describing the result (success or error message)
     */
    public String add(Game game) {
        if (findById(game.getId()).isPresent()) return "❌ A game with that id already exists";
        final String sql =
                "INSERT INTO games(id,name,platform,status,priority,ownership) VALUES(?,?,?,?,?,?)";
        try (Connection c = DriverManager.getConnection(url);
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, game.getId());
            ps.setString(2, game.getName());
            ps.setString(3, game.getPlatform());
            ps.setString(4, game.getStatus().name());
            ps.setInt(5, game.getPriority());
            ps.setString(6, game.getOwnership().name());
            ps.executeUpdate();
            games.add(game);
            return "✅ Added:\n" + game;
        } catch (SQLException e) {
            return "❌ DB error adding game: " + e.getMessage();
        }
    }

    /**
     * Remove a game by id; deletes from DB and in-memory list.
     *
     * @param id unique identifier of the game to remove
     * @return status string describing the result
     */
    public String remove(int id) {
        Optional<Game> hit = findById(id);
        if (hit.isEmpty()) return "No game record with id " + id + " to remove";
        try (Connection c = DriverManager.getConnection(url);
             PreparedStatement ps = c.prepareStatement("DELETE FROM games WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
            games.remove(hit.get());
            return "🗑️ Remove id " + id + ".";
        } catch (SQLException e) {
            return "❌ DB error removing: " + e.getMessage();
        }
    }

    /**
     * Update a single field on a game and persist only that column.
     * Valid fields: {@code name, platform, status, priority, ownership}.
     *
     * @param id       game id to update
     * @param field    column name to change
     * @param newValue new value (parsed/validated per field)
     * @return status string describing the result
     */
    public String updateField(int id, String field, String newValue) {
        Optional<Game> hitOpt = findById(id);
        if (hitOpt.isEmpty()) return "⚠️ No game record with id " + id + " to update";
        Game g = hitOpt.get();

        // 1) Validate & update in-memory Game rules
        try {
            switch (field.toLowerCase(Locale.ROOT)) {
                case "name"      -> g.setName(newValue);
                case "platform"  -> g.setPlatform(newValue);
                case "status"    -> g.setStatus(toStatus(newValue));
                case "priority"  -> g.setPriority(Integer.parseInt(newValue));
                case "ownership" -> g.setOwnership(toOwnership(newValue));
                default -> { return "❌ Unknown field: " + field; }
            }
        } catch (Exception ex) {
            return "❌ Wrong value for " + field + ": " + ex.getMessage();
        }

        // 2) Save just that column
        String col = switch (field.toLowerCase(Locale.ROOT)) {
            case "name" -> "name";
            case "platform" -> "platform";
            case "status" -> "status";
            case "priority" -> "priority";
            case "ownership" -> "ownership";
            default -> null;
        };
        if (col == null) return "❌ Unknown field: " + field;

        String sql = "UPDATE games SET " + col + "=? WHERE id=?";
        try (Connection c = DriverManager.getConnection(url);
             PreparedStatement ps = c.prepareStatement(sql)) {
            switch (col) {
                case "priority"  -> ps.setInt(1, g.getPriority());
                case "status"    -> ps.setString(1, g.getStatus().name());
                case "ownership" -> ps.setString(1, g.getOwnership().name());
                case "name"      -> ps.setString(1, g.getName());
                default          -> ps.setString(1, g.getPlatform());
            }
            ps.setInt(2, id);
            ps.executeUpdate();
            return "✅ Updated " + field + ":\n " + g;
        } catch (SQLException e) {
            return "❌ DB error updating: " + e.getMessage();
        }
    }

    /**
     * Bulk-import from a UTF-8 text file with pipe-separated columns:
     * {@code id|name|platform|status|priority|ownership}. Comments start with {@code #}.
     * Skips invalid/duplicate rows, batches inserts, and commits once.
     *
     * @param path path to the source text file
     * @return import summary with counts
     */
    public String importFromFile(Path path) {
        if (path == null) return "❌ Path is needed.";
        if (!Files.exists(path)) return "❌ File not found: " + path;

        int added = 0, skipped = 0;
        final String sql =
                "INSERT INTO games(id,name,platform,status,priority,ownership) VALUES(?,?,?,?,?,?)";

        try (BufferedReader br = Files.newBufferedReader(path, StandardCharsets.UTF_8);
             Connection c = DriverManager.getConnection(url)) {
            c.setAutoCommit(false);
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                String line;
                while ((line = br.readLine()) != null) {
                    if (line.trim().isEmpty() || line.trim().startsWith("#")) continue;
                    String[] p = line.split("\\|");
                    if (p.length != 6) { skipped++; continue; }
                    try {
                        Game g = new Game(
                                Integer.parseInt(p[0].trim()),
                                p[1].trim(),
                                p[2].trim(),
                                toStatus(p[3].trim()),
                                Integer.parseInt(p[4].trim()),
                                toOwnership(p[5].trim())
                        );
                        if (findById(g.getId()).isPresent()) { skipped++; continue; }

                        ps.setInt(1, g.getId());
                        ps.setString(2, g.getName());
                        ps.setString(3, g.getPlatform());
                        ps.setString(4, g.getStatus().name());
                        ps.setInt(5, g.getPriority());
                        ps.setString(6, g.getOwnership().name());
                        ps.addBatch();

                        games.add(g);
                        added++;
                    } catch (Exception ex) {
                        skipped++;
                    }
                }
                ps.executeBatch();
                c.commit();
            } catch (Exception inner) {
                c.rollback();
                throw inner;
            } finally {
                c.setAutoCommit(true);
            }
        } catch (Exception e) {
            return " Error importing to DB: " + e.getMessage();
        }
        return String.format("📥 Import complete. Added: %d, Skipped: %d, Total now: %d",
                added, skipped, games.size());
    }

    /**
     * Find a game by id from the in-memory snapshot.
     *
     * @param id game id
     * @return {@link Optional} containing the game if found
     */
    public Optional<Game> findById(int id) {
        return games.stream().filter(g -> g.getId() == id).findFirst();
    }

    /**
     * Compute the backlog score for a game.
     * Higher means “tackle sooner.” Priority is weighted x2; status adds a small bias.
     *
     * @param game target game
     * @return score used for ranking
     */
    public int scoreFor(Game game) {
        int statusWeight = switch (game.getStatus()) {
            case UNPLAYED -> 3;
            case PLAYING  -> 1;
            case BEATEN   -> 0;
        };
        return (game.getPriority() * 2) + statusWeight;
    }

    /**
     * Build a quick backlog health summary plus the top Number items to focus on.
     *
     * @param topNumber how many items to list (capped by total size)
     * @return formatted report
     */
    public String backlogReport(int topNumber) {
        if (games.isEmpty()) return "No games loaded yet.";

        long unplayed = games.stream().filter(g -> g.getStatus() == Game.Status.UNPLAYED).count();
        long playing  = games.stream().filter(g -> g.getStatus() == Game.Status.PLAYING).count();
        long beaten   = games.stream().filter(g -> g.getStatus() == Game.Status.BEATEN).count();

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

    // == Helpers ==

    /**
     * Create the {@code games} table if missing.
     * Uses CHECK constraints to guard enum-like values and priority range.
     */
    private void ensureTable() {
        String ddl = """
            CREATE TABLE IF NOT EXISTS games(
              id        INTEGER PRIMARY KEY,
              name      TEXT NOT NULL,
              platform  TEXT NOT NULL,
              status    TEXT NOT NULL CHECK (status IN ('UNPLAYED','PLAYING','BEATEN')),
              priority  INTEGER NOT NULL CHECK (priority BETWEEN 1 AND 5),
              ownership TEXT NOT NULL CHECK (ownership IN ('PHYSICAL','DIGITAL'))
            );
            """;
        try (Connection c = DriverManager.getConnection(url);
             Statement st = c.createStatement()) {
            st.executeUpdate(ddl);
        } catch (SQLException e) {
            System.out.println("⚠️ ensureTable: " + e.getMessage());
        }
    }

    /**
     * Refresh the in-memory list from the DB. Normalizes strings to enums safely.
     */
    private void reloadFromDb() {
        games.clear();
        final String sql =
                "SELECT id,name,platform,status,priority,ownership FROM games ORDER BY id";
        try (Connection c = DriverManager.getConnection(url);
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Game g = new Game(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("platform"),
                        toStatus(rs.getString("status")),
                        rs.getInt("priority"),
                        toOwnership(rs.getString("ownership"))
                );
                games.add(g);
            }
        } catch (SQLException e) {
            System.out.println("⚠️ reloadFromDb: " + e.getMessage());
        }
    }

    /**
     * Parse a status string into a supported enum (defaults to UNPLAYED for unknowns).
     *
     * @param s input string
     * @return normalized {@link Game.Status}
     */
    private static Game.Status toStatus(String s) {
        if (s == null) return Game.Status.UNPLAYED;
        return switch (s.trim().toUpperCase(Locale.ROOT)) {
            case "PLAYING" -> Game.Status.PLAYING;
            case "BEATEN"  -> Game.Status.BEATEN;
            default        -> Game.Status.UNPLAYED; // e.g., ABANDONED/unknown -> UNPLAYED
        };
    }

    /**
     * Parse an ownership string into a supported enum (defaults to DIGITAL for unknowns).
     *
     * @param s input string
     * @return normalized {@link Game.Ownership}
     */
    private static Game.Ownership toOwnership(String s) {
        if (s == null) return Game.Ownership.DIGITAL;
        return switch (s.trim().toUpperCase(Locale.ROOT)) {
            case "PHYSICAL" -> Game.Ownership.PHYSICAL;
            default         -> Game.Ownership.DIGITAL; // e.g., Game Pass/PS Plus/unknown -> DIGITAL
        };
    }

} // === END DBLIBRARY ===
