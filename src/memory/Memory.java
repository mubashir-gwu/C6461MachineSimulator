package memory;

import fileutil.FileReader;
import instruction.Instruction;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

public class Memory {
    private final static int MAX_MEMORY_WORDS = 2048;
    private final int[] memory;

    public Memory() {
        memory = new int[MAX_MEMORY_WORDS];
        Arrays.fill(memory, 0);
    }

    public int getMemoryAt(int address) {
        return memory[address];
    }

    public void setMemoryAt(int address, int value) {
        memory[address] = value;
    }

    public void loadProgramToMemory(Path path) {
        List<String> lines = FileReader.readFile(path);

        for (String line : lines) {
            String[] tokens = line.split("\\s+");

            int location = Integer.parseInt(tokens[0], 8);
            int value = Integer.parseInt(tokens[1], 8);

            memory[location] = value;
        }
    }
}
