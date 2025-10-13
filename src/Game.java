/**
 * Stephanie Ortiz
 * CEN 3024 - Software Development 1
 * October 7th, 2025
 * Game.java
 * ---------------------------------
 * Defines everything for what makes Game
 * Will store all game data (id, name, platform, status, priority(backlog score) ownership)
 * Validations while also making sure no bad or unfinished data is entered by user
 */

/**
 * method: readIntInRangeOrCancelOnce
 * parameters: enterIdToUpdate, String, min, max.
 * return: int
 * purpose: Reads an enum value by name and keeps checking until valid.
 */


public class Game {
    public enum Status { UNPLAYED, PLAYING , BEATEN}
    public enum Ownership {PHYSICAL, DIGITAL}

    private int id;
    private String name;
    private String platform;
    private Status status;
    private int priority;
    private Ownership ownership;

    /**
     * method: Game
     * parameters: id: int, name: String, platform: String, status: Status, priority: int, ownership: Ownership
     * return: none
     * purpose: Builds game and makes sure all fields are setters.
     */

    public Game(int id, String name, String platform, Status status, int priority, Ownership ownership) {
        setId(id);
        setName(name);
        setPlatform(platform);
        setStatus(status);
        setPriority(priority);
        setOwnership(ownership);
    }

    // Setters with Validations

    public void setId(int id) {
        if (id <= 0) throw new IllegalArgumentException("id MUST be > 0");
        this.id = id;
    }

    public void setName(String name) {
        if (name == null || name.trim().isEmpty()) throw new IllegalArgumentException("name required");
        this.name = name;
    }

    public void setPlatform(String platform) {
        if (platform == null || platform.trim().isEmpty()) throw new IllegalArgumentException("platform required");
        this.platform = platform;
    }

    public void setStatus(Status status) {
        if (status == null) throw new IllegalArgumentException("status required");
        this.status = status;
    }

    public void setPriority(int priorty) {
        if (priorty < 1 || priorty > 5) throw new IllegalArgumentException("priority MUST be 1-5");
        this.priority = priorty;
    }

    public void setOwnership(Ownership ownership) {
        if (ownership == null) throw new IllegalArgumentException("ownership required");
        this.ownership = ownership;
    }

    // Getters

    public int getId() { return id; }
    public String getName() { return name;}
    public String getPlatform() { return platform; }
    public Status getStatus() { return status; }
    public int getPriority() { return priority; }
    public Ownership getOwnership() { return ownership; }

    /**
     * method: toString
     * parameters: none
     * return: String
     * purpose: shows a single line of formatted information for games.
     */

    @Override public String toString() {
        return String.format("#%d | %s | %s | %s | P%d | %s" ,
                id, name, platform, status, priority, ownership);
    }
} // END GAME
