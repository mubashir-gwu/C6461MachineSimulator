package fileutil;

import instruction.Instruction;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Predicate;

/**
 * Utility class for writing assembler output files.
 *
 * <p>After the assembler encodes a program, two output files are written to the
 * {@code output/} directory:
 * <ul>
 *   <li><b>.lst</b> (listing file) – human-readable table showing address, machine word,
 *       label, mnemonic, operands and comment for each instruction.</li>
 *   <li><b>.load</b> (load file) – machine-readable file consumed by the simulator to
 *       load program words directly into memory (address and octal word per line).</li>
 * </ul>
 */
public class FileWriter {

    /**
     * Writes a human-readable assembler listing file for the given program.
     *
     * <p>Each instruction is formatted via {@link Instruction#toListingFileString()}.
     * Blank lines (e.g. from {@code LOC} directives) are filtered out before writing.
     * The output file is created at {@code output/<programName>.lst}.
     *
     * @param instructions the fully-assembled instruction list
     * @param programName  the base name of the program (without extension)
     * @return the {@link Path} of the written listing file
     */
    public static Path writeListingFile(List<Instruction> instructions, String programName) {
        Path path = Path.of("output/" + programName + ".lst");

        try {
            // Create the output directory if it doesn't exist.
            Files.createDirectories(path.getParent());

            // Ignore the blank lines when writing to the file.
            Files.write(path, instructions.stream().map(Instruction::toListingFileString).filter(Predicate.not(String::isBlank)).toList());

            System.out.println("Listing file written to: " + path.toAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }

        return path;
    }

    /**
     * Writes the machine-readable load file for the given program.
     *
     * <p>Each instruction is formatted via {@link Instruction#toLoadFileString()}.
     * Blank lines are filtered out before writing.
     * The output file is created at {@code output/<programName>.load}.
     *
     * @param instructions the fully-assembled instruction list
     * @param programName  the base name of the program (without extension)
     * @return the {@link Path} of the written load file
     */
    public static Path writeLoadFile(List<Instruction> instructions, String programName) {
        Path path = Path.of("output/" + programName + ".load");

        try {
            // Create the output directory if it doesn't exist.
            Files.createDirectories(path.getParent());

            // Ignore the blank lines when writing to the file.
            Files.write(path, instructions.stream().map(Instruction::toLoadFileString).filter(Predicate.not(String::isBlank)).toList());

            System.out.println("Load file written to: " + path.toAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }

        return path;
    }

    /**
     * Extracts the base filename (without extension) from a file path.
     *
     * <p>Example: {@code getBaseFilename(Path.of("programs/test.asm"))} returns {@code "test"}.
     *
     * @param path the file path whose base name is required
     * @return the filename without its extension
     */
    public static String getBaseFilename(Path path) {
        String filename = path.getFileName().toString();
        return filename.substring(0, filename.lastIndexOf('.'));
    }
}
