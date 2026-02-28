package fileutil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Utility class for reading text files line by line.
 *
 * <p>Used by the simulator to read both assembly source files (for the assembler pass)
 * and pre-assembled load files (for loading program words into memory).
 */
public class FileReader {

    /**
     * Reads all lines from the file at the given path.
     *
     * @param path the path to the file to read
     * @return a list of lines from the file, or {@code null} if an I/O error occurs
     */
    public static List<String> readFile(Path path) {
        List<String> lines = null;
        try {
            lines = Files.readAllLines(path);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return lines;
    }
}
