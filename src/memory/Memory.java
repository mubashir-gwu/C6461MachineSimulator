package memory;

import java.util.Arrays;

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
}
