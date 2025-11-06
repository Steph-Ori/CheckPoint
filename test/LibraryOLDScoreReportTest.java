import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

public class LibraryOLDScoreReportTest {

    private Library_OLD libraryOLD;

    @BeforeEach
    void setup() {
        libraryOLD = new Library_OLD();
        libraryOLD.add(new Game(1, "Hades II", "PC", Game.Status.UNPLAYED, 5, Game.Ownership.DIGITAL));
        libraryOLD.add(new Game(2, "Spider-Man 2", "PS5", Game.Status.PLAYING, 4, Game.Ownership.PHYSICAL));
        libraryOLD.add(new Game(3, "Elden Ring", "PC", Game.Status.BEATEN, 2, Game.Ownership.DIGITAL));
    }

    @Test
    void scoreFor_reflectsPriorityAndStatus() {
        int s1 = libraryOLD.scoreFor(libraryOLD.findById(1).orElseThrow()); // high priority + UNPLAYED
        int s2 = libraryOLD.scoreFor(libraryOLD.findById(2).orElseThrow()); // medium + PLAYING
        int s3 = libraryOLD.scoreFor(libraryOLD.findById(3).orElseThrow()); // low + BEATEN
        assertTrue(s1 > s2 && s2 > s3, "scores should order by importance");
    }

    @Test
    void backlogReport_hasCounts_andTopTitles() {
        String report = libraryOLD.backlogReport(2);
        String lc = report.toLowerCase();
        assertTrue(lc.contains("backlog"));
        assertTrue(lc.contains("total"));
        assertTrue(report.contains("Hades II")); // should appear in top list
    }
} // End Library Score Test
