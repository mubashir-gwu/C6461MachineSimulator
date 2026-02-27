import encoder.Encoder;
import fileutil.FileReader;
import fileutil.FileWriter;
import instruction.Instruction;
import ui.UserInterface;

import javax.swing.*;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new UserInterface().setVisible(true));

        // System.out.println("========== C6461 Assembler ==========");
        // System.out.println();
        //
        // final String filename = "test_program.asm";
        // final String programName = FileWriter.getBaseFilename(filename);
        //
        // List<String> inputProgramLines = FileReader.readFile(filename);
        //
        // List<Instruction> encodedInstructions = Encoder.encode(inputProgramLines);
        //
        // FileWriter.writeListingFile(encodedInstructions, programName);
        // FileWriter.writeLoadFile(encodedInstructions, programName);
    }
}