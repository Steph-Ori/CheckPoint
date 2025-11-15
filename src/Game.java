/**
 * Stephanie Ortiz
 * CEN 3024 - Software Development 1
 * November 9th, 2025
 * Game.java
 */

/**
 * Simple domain model for a game record.
 *
 * <p>Fields are validated in setters so the object can’t exist in a bad state.
 * Used by both the CLI and DB layers.</p>
 *
 * <p><b>Role in the system:</b> a small, safe data holder with guardrails.
 * Keep it boring and predictable (stable).</p>
 */
public class Game {

    /** High-level progress for a title. */
    public enum Status { UNPLAYED, PLAYING, BEATEN }

    /** Where a user owns games. */
    public enum Ownership { PHYSICAL, DIGITAL }

    private int id;
    private String name;
    private String platform;
    private Status status;
    private int priority;          // 1 (low) .. 5 (high)
    private Ownership ownership;

    /**
     * Build a fully-initialized {@code Game}. All fields are validated via setters.
     *
     * @param id        unique id (&gt; 0)
     * @param name      non-blank title
     * @param platform  non-blank platform label (e.g., PC, PS5, Switch)
     * @param status    current progress (not null)
     * @param priority  1–5 (1=low urgency, 5=high)
     * @param ownership PHYSICAL or DIGITAL (not null)
     * @throws IllegalArgumentException if any value fails validation
     */
    public Game(int id, String name, String platform, Status status, int priority, Ownership ownership) {
        setId(id);
        setName(name);
        setPlatform(platform);
        setStatus(status);
        setPriority(priority);
        setOwnership(ownership);
    }

    // == Setters with validation ==

    /**
     * Set the unique identifier.
     * @param id value &gt; 0
     * @throws IllegalArgumentException if {@code id <= 0}
     */
    public void setId(int id) {
        if (id <= 0) throw new IllegalArgumentException("id MUST be > 0");
        this.id = id;
    }

    /**
     * Set the display name.
     * @param name non-blank
     * @throws IllegalArgumentException if blank/null
     */
    public void setName(String name) {
        if (name == null || name.trim().isEmpty()) throw new IllegalArgumentException("name required");
        this.name = name;
    }

    /**
     * Set the platform label.
     * @param platform non-blank (e.g., PC, PS5)
     * @throws IllegalArgumentException if blank/null
     */
    public void setPlatform(String platform) {
        if (platform == null || platform.trim().isEmpty()) throw new IllegalArgumentException("platform required");
        this.platform = platform;
    }

    /**
     * Set the progress status.
     * @param status not null
     * @throws IllegalArgumentException if null
     */
    public void setStatus(Status status) {
        if (status == null) throw new IllegalArgumentException("status required");
        this.status = status;
    }

    /**
     * Set the priority (1–5).
     * @param priority integer in [1,5]
     * @throws IllegalArgumentException if out of range
     */
    public void setPriority(int priority) {
        if (priority < 1 || priority > 5) throw new IllegalArgumentException("priority MUST be 1-5");
        this.priority = priority;
    }

    /**
     * Set the ownership type.
     * @param ownership not null
     * @throws IllegalArgumentException if null
     */
    public void setOwnership(Ownership ownership) {
        if (ownership == null) throw new IllegalArgumentException("ownership required");
        this.ownership = ownership;
    }

    // == Getters ==

    /**
     * Returns the unique id for this game.
     *
     * @return unique id greater than 0
     */
    public int getId() { return id; }

    /** @return game title (non-blank) */
    public String getName() { return name; }

    /** @return platform label (non-blank) */
    public String getPlatform() { return platform; }

    /** @return current status */
    public Status getStatus() { return status; }

    /** @return priority in [1,5] */
    public int getPriority() { return priority; }

    /** @return ownership type */
    public Ownership getOwnership() { return ownership; }

    /**
     * One-line summary used in CLI lists and debug logs.
     * @return formatted row with key fields
     */
    @Override
    public String toString() {
        return String.format("#%d | %s | %s | %s | P%d | %s",
                id, name, platform, status, priority, ownership);
    }

} // === END GAME ===