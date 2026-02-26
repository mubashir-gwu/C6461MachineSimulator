package fileutil;

import instruction.Instruction;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Predicate;

public class FileWriter {
    public static void writeListingFile(List<Instruction> instructions, String programName) {
        Path path = Path.of("output/" + programName + ".lst");

        try {
            // Create the output directory if it doesn't exist.'
            Files.createDirectories(path.getParent());

            // Ignore the blank lines when writing to the file.
            Files.write(path, instructions.stream().map(Instruction::toListingFileString).filter(Predicate.not(String::isBlank)).toList());

            System.out.println("Listing file written to: " + path.toAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void writeLoadFile(List<Instruction> instructions, String programName) {
        Path path = Path.of("output/" + programName + ".load");

        try {
            // Create the output directory if it doesn't exist.'
            Files.createDirectories(path.getParent());

            // Ignore the blank lines when writing to the file.
            Files.write(path, instructions.stream().map(Instruction::toLoadFileString).filter(Predicate.not(String::isBlank)).toList());

            System.out.println("Load file written to: " + path.toAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String getBaseFilename(String filename) {
        return filename.substring(0, filename.lastIndexOf('.'));
    }
}
