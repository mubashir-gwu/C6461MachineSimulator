package opcode;

/**
 * Immutable data record that associates a numeric opcode value with its instruction type.
 *
 * @param opcodeValue the 6-bit numeric opcode as an octal integer (e.g. {@code 001} for LDR)
 * @param opcodeType  the category of the instruction, which determines its 16-bit word format
 */
public record Opcode(int opcodeValue, OpcodeType opcodeType) {}
