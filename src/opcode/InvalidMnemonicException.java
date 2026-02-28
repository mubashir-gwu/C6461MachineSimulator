package opcode;

/**
 * Thrown when an unrecognised instruction mnemonic is encountered during assembly or execution.
 *
 * <p>This exception is raised by {@link OpcodeLookupTable} when a mnemonic string does not
 * match any entry in the opcode table.
 */
public class InvalidMnemonicException extends Exception {
    /**
     * Constructs a new exception for the given unrecognised mnemonic.
     *
     * @param mnemonic the mnemonic string that could not be found in the opcode table
     */
    public InvalidMnemonicException(String mnemonic) {
        super("invalid mnemonic: " + mnemonic);
    }
}
