package cpu;

import fileutil.FileReader;
import memory.Memory;

import java.nio.file.Path;
import java.util.List;

public class CPU {
    private int programCounter = 0;
    private final Memory memory;

    public CPU() {
        this.memory = new Memory();
    }

    public void setProgramCounter(int programCounter) {
        this.programCounter = programCounter;
    }

    public Memory getMemory() {
        return memory;
    }

    public int loadProgramToMemory(Path path) {
        List<String> lines = FileReader.readFile(path);
        int programStartAddress = -1;

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            String[] tokens = line.split("\\s+");

            int location = Integer.parseInt(tokens[0], 8);
            int value = Integer.parseInt(tokens[1], 8);

            memory.setMemoryAt(location, value);

            // Keep only the first 6 bits as that's where the opcode is stored.
            int opcode = value & 0770000;

            // Shift the value by 10 bits to get the numeric value of the opcode.
            opcode >>= 10;

            // Ignore the `DATA` and `HLT` lines.
            if (opcode == 0) {
                continue;
            }

            if (programStartAddress == -1) {
                programStartAddress = location;
            }
        }

        return programStartAddress;
    }
}
