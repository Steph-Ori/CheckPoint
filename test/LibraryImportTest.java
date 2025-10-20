import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

public class LibraryImportTest {

    @TempDir Path temp;

    @Test
    void importFromFile_success_addsGames() throws IOException {
        Path data = temp.resolve("games.txt");
        Files.writeString(data, String.join("\n",
                // Helps with making sure wording format is correct
                "1|Hades II|PC|UNPLAYED|5|DIGITAL",
                "2|Spider-Man 2|PS5|PLAYING|4|PHYSICAL",
                "2|Spider-Man 2 DUP|PS5|PLAYING|4|PHYSICAL",
                "bad|line|oops"
        ));

        Library library = new Library();
        String message = library.importFromFile(data);
        // kept message wording loose to keep options open
        assertTrue(message.toLowerCase().contains("import"));
        assertEquals(2, library.listAll().size(), "should only add unique, well done rows");
    }

    @Test
    void importFromFile_missingFile_reportsNice() {
        Library library = new Library();
        Path missing = temp.resolve("nope.txt");
        String message = library.importFromFile(missing);
        assertTrue(message.toLowerCase().contains("file"), "expect a nice file-not-found message");
        assertEquals(0, library.listAll().size());
    }
} // End Library Import Test

