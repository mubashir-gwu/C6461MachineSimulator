package memory;

/**
 * Simulates a fully associative, unified cache sitting between the CPU and main memory.
 *
 * <p>The cache holds 16 lines, each storing a single 16-bit word (since memory is
 * word-addressable). Replacement uses a simple FIFO (First-In, First-Out) policy:
 * a circular pointer tracks which line to evict next.
 *
 * <p>Write policy is <b>write-through, no write-allocate</b>: on a store, the word is
 * always written to main memory; if the address is already cached, the cached copy is
 * updated too, but a store to an uncached address does not bring the line into the cache.
 *
 * <p>Usage from the CPU:
 * <ul>
 *   <li>{@link #lookup(int)} — check for a hit before reading main memory.</li>
 *   <li>{@link #insert(int, int)} — on a miss, insert the word just read from memory.</li>
 *   <li>{@link #writeThrough(int, int)} — on a store, update the cached copy if present.</li>
 * </ul>
 */
public class Cache {

    /** Number of cache lines (fully associative, so every line can hold any address). */
    public static final int NUM_LINES = 16;

    // ── Inner class ────────────────────────────────────────────────────────────

    /**
     * A single cache line holding one word.
     */
    public static class CacheLine {
        /** Whether this line contains valid data. */
        private boolean valid;

        /** The memory address whose data is stored in this line (the "tag"). */
        private int tag;

        /** The 16-bit data word cached from memory. */
        private int data;

        /** Constructs an empty (invalid) cache line. */
        public CacheLine() {
            this.valid = false;
            this.tag = -1;
            this.data = 0;
        }

        public boolean isValid() { return valid; }
        public int getTag()      { return tag; }
        public int getData()     { return data; }
    }

    // ── Fields ─────────────────────────────────────────────────────────────────

    /** The 16 cache lines. */
    private final CacheLine[] lines;

    /** FIFO pointer — index of the next line to be replaced on a miss. */
    private int fifoPointer;

    /** Index of the cache line that was accessed most recently (-1 if none). */
    private int lastAccessedLine;

    /** Whether the most recent access was a hit ({@code true}) or miss ({@code false}). */
    private boolean lastHit;

    // ── Constructor ────────────────────────────────────────────────────────────

    /**
     * Constructs a new cache with all lines initially invalid.
     */
    public Cache() {
        lines = new CacheLine[NUM_LINES];
        for (int i = 0; i < NUM_LINES; i++) {
            lines[i] = new CacheLine();
        }
        fifoPointer = 0;
        lastAccessedLine = -1;
        lastHit = false;
    }

    // ── Core operations ────────────────────────────────────────────────────────

    /**
     * Searches the cache for a word at the given memory address.
     *
     * <p>If the address is found (cache hit), updates {@link #lastAccessedLine} and
     * {@link #lastHit} and returns the cached data word. If not found (cache miss),
     * returns {@code -1} and sets {@link #lastHit} to {@code false}.
     *
     * @param address the memory address to look up
     * @return the cached data word on a hit, or {@code -1} on a miss
     */
    public int lookup(int address) {
        for (int i = 0; i < NUM_LINES; i++) {
            if (lines[i].valid && lines[i].tag == address) {
                lastAccessedLine = i;
                lastHit = true;
                return lines[i].data;
            }
        }
        lastHit = false;
        return -1;
    }

    /**
     * Inserts a word into the cache using FIFO replacement.
     *
     * <p>The word is placed at the line indicated by {@link #fifoPointer}, which then
     * advances to the next position (wrapping around after line 15).
     *
     * @param address the memory address (used as the tag)
     * @param data    the 16-bit data word to cache
     * @return the cache line index where the word was placed
     */
    public int insert(int address, int data) {
        int lineIndex = fifoPointer;
        lines[lineIndex].valid = true;
        lines[lineIndex].tag = address;
        lines[lineIndex].data = data;

        lastAccessedLine = lineIndex;

        // Advance FIFO pointer (circular).
        fifoPointer = (fifoPointer + 1) % NUM_LINES;

        return lineIndex;
    }

    /**
     * Updates the cached copy of a word on a store (write-through).
     *
     * <p>If the address is present in the cache, its data is updated in place and the
     * method returns {@code true}. If the address is not cached, no insertion occurs
     * (no write-allocate) and the method returns {@code false}.
     *
     * @param address the memory address being written
     * @param data    the new 16-bit data word
     * @return {@code true} if the address was found and updated, {@code false} otherwise
     */
    public boolean writeThrough(int address, int data) {
        for (int i = 0; i < NUM_LINES; i++) {
            if (lines[i].valid && lines[i].tag == address) {
                lines[i].data = data;
                lastAccessedLine = i;
                lastHit = true;
                return true;
            }
        }
        lastHit = false;
        return false;
    }

    // ── Accessors for UI display ───────────────────────────────────────────────

    /**
     * Returns the array of all cache lines (for UI display).
     *
     * @return the cache lines array (length {@value #NUM_LINES})
     */
    public CacheLine[] getLines() {
        return lines;
    }

    /**
     * Returns the index of the cache line most recently accessed.
     *
     * @return the last accessed line index, or {@code -1} if no access has occurred
     */
    public int getLastAccessedLine() {
        return lastAccessedLine;
    }

    /**
     * Returns whether the most recent cache access was a hit.
     *
     * @return {@code true} for a hit, {@code false} for a miss
     */
    public boolean isLastHit() {
        return lastHit;
    }

    /**
     * Returns the current FIFO pointer position (the next line to be replaced).
     *
     * @return the FIFO pointer index (0–15)
     */
    public int getFifoPointer() {
        return fifoPointer;
    }
}
