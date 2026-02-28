package memory;

import java.util.Arrays;

public class Memory {
    private final static int MAX_MEMORY_WORDS = 2048;
    private final int[] memory;

    public Memory() {
        memory = new int[MAX_MEMORY_WORDS];
        Arrays.fill(memory, 0);
    }

    public int getMemoryAt(int address) throws IndexOutOfBoundsException {
        if (address < 0 || address >= MAX_MEMORY_WORDS) {
            throw new IndexOutOfBoundsException("address out of bounds: " + address + " for memory of size " + MAX_MEMORY_WORDS);
        }

        return memory[address];
    }

    public void setMemoryAt(int address, int value) throws IndexOutOfBoundsException {
        if (address < 0 || address >= MAX_MEMORY_WORDS) {
            throw new IndexOutOfBoundsException("address out of bounds: " + address + " for memory of size " + MAX_MEMORY_WORDS);
        }

        memory[address] = value;
    }
}
