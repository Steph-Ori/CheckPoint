import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class LibraryOLDAddRemoveTest {
    private Library_OLD libraryOLD;
    @BeforeEach void setUp() { libraryOLD = new Library_OLD(); }

    @Test void addGame_works() {
        Game game = new Game(1,"Hades II","PC", Game.Status.UNPLAYED, 5, Game.Ownership.DIGITAL);
        assertTrue(libraryOLD.add(game).contains("Added"));
    }
    @Test void removeGame_works() {
        libraryOLD.add(new Game(1,"Hades II","PC", Game.Status.UNPLAYED, 5, Game.Ownership.DIGITAL));
        assertTrue(libraryOLD.remove(1).contains("Remove id 1"));
    }
} // End Remove Test
