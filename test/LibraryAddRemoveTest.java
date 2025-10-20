import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class LibraryAddRemoveTest {
    private Library library;
    @BeforeEach void setUp() { library = new Library(); }

    @Test void addGame_works() {
        Game game = new Game(1,"Hades II","PC", Game.Status.UNPLAYED, 5, Game.Ownership.DIGITAL);
        assertTrue(library.add(game).contains("Added"));
    }
    @Test void removeGame_works() {
        library.add(new Game(1,"Hades II","PC", Game.Status.UNPLAYED, 5, Game.Ownership.DIGITAL));
        assertTrue(library.remove(1).contains("Remove id 1"));
    }
} // End Remove Test
