import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

public class LibraryScoreReportTest {

    private Library library;

    @BeforeEach
    void setup() {
        library = new Library();
        library.add(new Game(1, "Hades II", "PC", Game.Status.UNPLAYED, 5, Game.Ownership.DIGITAL));
        library.add(new Game(2, "Spider-Man 2", "PS5", Game.Status.PLAYING, 4, Game.Ownership.PHYSICAL));
        library.add(new Game(3, "Elden Ring", "PC", Game.Status.BEATEN, 2, Game.Ownership.DIGITAL));
    }

    @Test
    void scoreFor_reflectsPriorityAndStatus() {
        int s1 = library.scoreFor(library.findById(1).orElseThrow()); // high priority + UNPLAYED
        int s2 = library.scoreFor(library.findById(2).orElseThrow()); // medium + PLAYING
        int s3 = library.scoreFor(library.findById(3).orElseThrow()); // low + BEATEN
        assertTrue(s1 > s2 && s2 > s3, "scores should order by importance");
    }

    @Test
    void backlogReport_hasCounts_andTopTitles() {
        String report = library.backlogReport(2);
        String lc = report.toLowerCase();
        assertTrue(lc.contains("backlog"));
        assertTrue(lc.contains("total"));
        assertTrue(report.contains("Hades II")); // should appear in top list
    }
} // End Library Score Test
