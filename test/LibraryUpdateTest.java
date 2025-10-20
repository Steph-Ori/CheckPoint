import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

public class LibraryUpdateTest {

    private Library library;

    @BeforeEach
    void setup() {
        library = new Library();
        library.add(new Game(10, "Pentiment", "Xbox", Game.Status.UNPLAYED, 3, Game.Ownership.DIGITAL));
    }

    @Test
    void update_name_changesValue() {
        String message = library.updateField(10, "name", "Pentiment (Definitive)");
        assertTrue(message.toLowerCase().contains("update"));
        assertEquals("Pentiment (Definitive)", library.findById(10).orElseThrow().getName());
    }

    @Test
    void update_platform_status_priority_ownership() {
        library.updateField(10, "platform", "PC");
        library.updateField(10, "status", "playing"); // case-insensitive enum
        library.updateField(10, "priority", "5");
        library.updateField(10, "ownership", "PHYSICAL");

        var game = library.findById(10).orElseThrow();
        assertEquals("PC", game.getPlatform());
        assertEquals(Game.Status.PLAYING, game.getStatus());
        assertEquals(5, game.getPriority());
        assertEquals(Game.Ownership.PHYSICAL, game.getOwnership());
    }

    @Test
    void update_unknownField_or_badValues_areRejected() {
        // takes the original value
        var before = library.findById(10).orElseThrow();
        String name = before.getName();
        String plat = before.getPlatform();
        Game.Status status = before.getStatus();
        int prior = before.getPriority();
        Game.Ownership own = before.getOwnership();

        // 1) unknown field name -> should NOT be "Updated" and must not change to updated
        String m1 = library.updateField(10, "madeUp", "x");
        assertFalse(m1.toLowerCase().contains("updated"), "Unexpected success message: " + m1);

        // 2) bad enum -> should NOT be "Updated" and must not change to updated
        String m2 = library.updateField(10, "status", "NOT_A_STATUS");
        assertFalse(m2.toLowerCase().contains("updated"), "Unexpected success message: " + m2);

        // 3) bad priority no number -> should NOT be "Updated" and must not change to updated
        String m3 = library.updateField(10, "priority", "not-a-number");
        assertFalse(m3.toLowerCase().contains("updated"), "Unexpected success message: " + m3);

        // makes sure nothing changed
        var after = library.findById(10).orElseThrow();
        assertEquals(name, after.getName());
        assertEquals(plat, after.getPlatform());
        assertEquals(status, after.getStatus());
        assertEquals(prior, after.getPriority());
        assertEquals(own, after.getOwnership());
    }
    @Test
    void update_missingId_reportsNotFound() {
        String message = library.updateField(999, "name", "X");
        assertTrue(message.toLowerCase().contains("no game"));
    }
} // End Library Test