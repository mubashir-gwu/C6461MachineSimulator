package memory;

import java.util.Arrays;

/**
 * Represents the main memory of the C6461 machine simulator.
 *
 * <p>Memory is modelled as a fixed-size array of 16-bit words addressed by integer indices.
 * The machine supports {@value #MAX_MEMORY_WORDS} words (addresses 0–2047, octal 0–03777),
 * matching the 11-bit address space of the architecture.
 */
public class Memory {
    /** Total number of addressable memory words (2^11 = 2048). */
    private final static int MAX_MEMORY_WORDS = 2048;

    /** The backing array storing one integer word per memory address. */
    private final int[] memory;

    /**
     * Constructs a new Memory instance with all words initialised to zero.
     */
    public Memory() {
        memory = new int[MAX_MEMORY_WORDS];
        Arrays.fill(memory, 0);
    }

    /**
     * Returns the word stored at the given memory address.
     *
     * @param address the memory address to read (must be in range [0, {@value #MAX_MEMORY_WORDS}))
     * @return the integer word stored at {@code address}
     * @throws IndexOutOfBoundsException if {@code address} is negative or &ge; {@value #MAX_MEMORY_WORDS}
     */
    public int getMemoryAt(int address) throws IndexOutOfBoundsException {
        if (address < 0 || address >= MAX_MEMORY_WORDS) {
            throw new IndexOutOfBoundsException("address out of bounds: " + address + " for memory of size " + MAX_MEMORY_WORDS);
        }

        return memory[address];
    }

    /**
     * Writes a word to the given memory address.
     *
     * @param address the memory address to write (must be in range [0, {@value #MAX_MEMORY_WORDS}))
     * @param value   the integer word to store at {@code address}
     * @throws IndexOutOfBoundsException if {@code address} is negative or &ge; {@value #MAX_MEMORY_WORDS}
     */
    public void setMemoryAt(int address, int value) throws IndexOutOfBoundsException {
        if (address < 0 || address >= MAX_MEMORY_WORDS) {
            throw new IndexOutOfBoundsException("address out of bounds: " + address + " for memory of size " + MAX_MEMORY_WORDS);
        }

        memory[address] = value;
    }
}
