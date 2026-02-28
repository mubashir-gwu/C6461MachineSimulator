package opcode;

import java.util.HashMap;
import java.util.Map;

/**
 * Static lookup table that maps instruction mnemonics to their numeric opcode values and types.
 *
 * <p>The table is populated once in a static initialiser and is used by both the assembler
 * (to encode instructions) and the CPU (to decode and dispatch instructions at runtime).
 *
 * <p>All numeric opcode values are stored as octal integer literals to match the octal
 * representation used throughout the C6461 architecture documentation.
 */
public class OpcodeLookupTable {
    /** Internal map from mnemonic string to its {@link Opcode} descriptor. */
    private static final Map<String, Opcode> opcodeMap = new HashMap<>();

    static {
        // Load/Store instructions.
        opcodeMap.put("LDR", new Opcode(001, OpcodeType.LOAD_STORE));
        opcodeMap.put("STR", new Opcode(002, OpcodeType.LOAD_STORE));
        opcodeMap.put("LDA", new Opcode(003, OpcodeType.LOAD_STORE));
        opcodeMap.put("LDX", new Opcode(041, OpcodeType.LOAD_STORE));
        opcodeMap.put("STX", new Opcode(042, OpcodeType.LOAD_STORE));

        // Transfer instructions.
        opcodeMap.put("JZ",  new Opcode(010, OpcodeType.TRANSFER));
        opcodeMap.put("JNE", new Opcode(011, OpcodeType.TRANSFER));
        opcodeMap.put("JCC", new Opcode(012, OpcodeType.TRANSFER));
        opcodeMap.put("JMA", new Opcode(013, OpcodeType.TRANSFER));
        opcodeMap.put("JSR", new Opcode(014, OpcodeType.TRANSFER));
        opcodeMap.put("RFS", new Opcode(015, OpcodeType.TRANSFER));
        opcodeMap.put("SOB", new Opcode(016, OpcodeType.TRANSFER));
        opcodeMap.put("JGE", new Opcode(017, OpcodeType.TRANSFER));

        // Arithmetic instructions.
        opcodeMap.put("AMR", new Opcode(004, OpcodeType.ARITHMETIC));
        opcodeMap.put("SMR", new Opcode(005, OpcodeType.ARITHMETIC));
        opcodeMap.put("AIR", new Opcode(006, OpcodeType.ARITHMETIC));
        opcodeMap.put("SIR", new Opcode(007, OpcodeType.ARITHMETIC));

        // Multiply/Divide operations.
        opcodeMap.put("MLT", new Opcode(070, OpcodeType.MULTIPLY_DIVIDE));
        opcodeMap.put("DVD", new Opcode(071, OpcodeType.MULTIPLY_DIVIDE));

        // Logical operations.
        opcodeMap.put("TRR", new Opcode(072, OpcodeType.LOGICAL));
        opcodeMap.put("AND", new Opcode(073, OpcodeType.LOGICAL));
        opcodeMap.put("ORR", new Opcode(074, OpcodeType.LOGICAL));
        opcodeMap.put("NOT", new Opcode(075, OpcodeType.LOGICAL));

        // Shift/Rotate operations.
        opcodeMap.put("SRC", new Opcode(031, OpcodeType.SHIFT_ROTATE));
        opcodeMap.put("RRC", new Opcode(032, OpcodeType.SHIFT_ROTATE));

        // I/O operations.
        opcodeMap.put("IN",  new Opcode(061, OpcodeType.IO));
        opcodeMap.put("OUT", new Opcode(062, OpcodeType.IO));
        opcodeMap.put("CHK", new Opcode(063, OpcodeType.IO));

        // Miscellaneous instructions.
        opcodeMap.put("HLT",  new Opcode(000, OpcodeType.MISC));
        opcodeMap.put("TRAP", new Opcode(030, OpcodeType.MISC));
    }

    /**
     * Returns the numeric opcode value for the given mnemonic.
     *
     * @param mnemonic the instruction mnemonic to look up (e.g. {@code "LDR"})
     * @return the 6-bit opcode value as a Java int (in octal notation)
     * @throws InvalidMnemonicException if {@code mnemonic} is not in the table
     */
    public static int getOpcodeValue(String mnemonic) throws InvalidMnemonicException {
        if (!opcodeMap.containsKey(mnemonic)) {
            throw new InvalidMnemonicException(mnemonic);
        }
        return opcodeMap.get(mnemonic).opcodeValue();
    }

    /**
     * Returns the {@link OpcodeType} for the given mnemonic string.
     *
     * @param mnemonic the instruction mnemonic to look up (e.g. {@code "LDR"})
     * @return the {@link OpcodeType} that determines the instruction's encoding format
     * @throws InvalidMnemonicException if {@code mnemonic} is not in the table
     */
    public static OpcodeType getOpcodeType(String mnemonic) throws InvalidMnemonicException {
        if (!opcodeMap.containsKey(mnemonic)) {
            throw new InvalidMnemonicException(mnemonic);
        }
        return opcodeMap.get(mnemonic).opcodeType();
    }

    /**
     * Returns the {@link OpcodeType} for a numeric opcode value decoded from a machine word.
     *
     * <p>Used by the CPU at runtime to classify an instruction after extracting its opcode bits.
     *
     * @param opcodeValue the 6-bit numeric opcode extracted from a 16-bit instruction word
     * @return the corresponding {@link OpcodeType}, or {@code null} if the value is unrecognised
     */
    public static OpcodeType getOpcodeType(int opcodeValue) {
        for (Opcode opcode : opcodeMap.values()) {
            if (opcode.opcodeValue() == opcodeValue) {
                return opcode.opcodeType();
            }
        }

        return null;
    }

    /**
     * Returns the mnemonic string for a numeric opcode value decoded from a machine word.
     *
     * <p>Used by the CPU at runtime to look up the mnemonic for logging and dispatch purposes.
     *
     * @param opcodeValue the 6-bit numeric opcode extracted from a 16-bit instruction word
     * @return the mnemonic string (e.g. {@code "LDR"}), or an empty string if unrecognised
     */
    public static String getMnemonic(int opcodeValue) {
        for (Map.Entry<String, Opcode> entry : opcodeMap.entrySet()) {
            if (entry.getValue().opcodeValue() == opcodeValue) {
                return entry.getKey();
            }
        }

        return "";
    }
}
