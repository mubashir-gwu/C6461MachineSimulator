package encoder;

import instruction.Instruction;
import opcode.InvalidMnemonicException;
import opcode.OpcodeLookupTable;

import static encoder.EncoderStringUtil.getOctalString;
import static encoder.EncoderStringUtil.getZeroPaddedBinaryString;

/**
 * Encodes a single {@link Instruction} into its 6-digit octal machine word.
 *
 * <p>The C6461 architecture uses a fixed 16-bit instruction word. Different instruction
 * categories use different bit-field layouts. This class selects the correct encoding
 * method based on the instruction's {@link opcode.OpcodeType}.
 *
 * <h2>Instruction word formats</h2>
 * <pre>
 * Load/Store / Transfer / Arithmetic (most instructions):
 *   [15:10] opcode  (6 bits)
 *   [ 9: 8] R       general-purpose register number (2 bits)
 *   [ 7: 6] IX      index register number (2 bits)
 *   [    5] I       indirect addressing flag: 0 = direct, 1 = indirect (1 bit)
 *   [ 4: 0] address target address field (5 bits)
 *
 * Register-Register (Multiply/Divide, Logical):
 *   [15:10] opcode  (6 bits)
 *   [ 9: 8] Rx      first source/destination register (2 bits)
 *   [ 7: 6] Ry      second source register (2 bits)
 *   [ 5: 0] unused, set to 000000
 *
 * Shift/Rotate (SRC, RRC):
 *   [15:10] opcode  (6 bits)
 *   [ 9: 8] R       register to shift/rotate (2 bits)
 *   [    7] A/L     0 = arithmetic, 1 = logical (1 bit)
 *   [    6] R/L     0 = right, 1 = left (1 bit)
 *   [ 5: 4] unused, set to 00
 *   [ 3: 0] count   number of bit positions to shift (4 bits)
 *
 * I/O (IN, OUT, CHK):
 *   [15:10] opcode  (6 bits)
 *   [ 9: 8] R       register involved in I/O (2 bits)
 *   [ 7: 5] unused, set to 000
 *   [ 4: 0] DevID   device identifier (5 bits)
 *
 * Miscellaneous:
 *   HLT:  [15:10] opcode | [9:0] 0000000000
 *   TRAP: [15:10] opcode | [9:4] 000000 | [3:0] trap code
 * </pre>
 */
public class InstructionEncoder {

    /**
     * Encodes a single instruction into its 6-digit octal machine word.
     *
     * <p>The {@code DATA} pseudo-instruction is handled as a special case: its operand is
     * treated as a raw decimal value and converted directly to a zero-padded octal string
     * without any bit-field encoding.
     *
     * @param instruction the instruction to encode (must have operands resolved to numeric strings)
     * @return a 6-digit zero-padded octal string representing the encoded machine word
     * @throws InvalidMnemonicException if the instruction's mnemonic is not in the opcode table
     */
    public static String encodeInstruction(Instruction instruction) throws InvalidMnemonicException {
        if (instruction.getMnemonic().equalsIgnoreCase("DATA")) {
            // DATA is a pseudo-instruction that places a raw value directly into memory.
            StringBuilder sb = new StringBuilder(Integer.toOctalString(Integer.parseInt(instruction.getOperands()[0])));
            while (sb.length() < 6) {
                sb.insert(0, "0");
            }

            return sb.toString();
        }

        String binaryString = switch (OpcodeLookupTable.getOpcodeType(instruction.getMnemonic())) {
            case LOAD_STORE, TRANSFER, ARITHMETIC -> encodeWithLoadStoreFormat(instruction);
            case MULTIPLY_DIVIDE, LOGICAL -> encodeWithRegisterRegisterFormat(instruction);
            case SHIFT_ROTATE -> encodeWithShiftRotateFormat(instruction);
            case IO -> encodeWithIOFormat(instruction);
            case MISC -> encodeWithMiscFormat(instruction);
        };

        return getOctalString(binaryString);
    }

    /**
     * Encodes instructions that use the load/store word format.
     *
     * <p>Word layout: {@code [opcode:6][R:2][IX:2][I:1][address:5]}
     *
     * <p>Special cases handled:
     * <ul>
     *   <li>{@code LDX}, {@code STX}, {@code JMA}, {@code JSR} – the R field is unused (set to {@code 00}).</li>
     *   <li>{@code RFS}, {@code AIR}, {@code SIR} – the IX field is unused (set to {@code 00}).</li>
     *   <li>{@code AIR}, {@code SIR} – the I (indirect) field is unused (set to {@code 0}).</li>
     * </ul>
     *
     * @param instruction the instruction to encode
     * @return a 16-character binary string representing the encoded word
     * @throws InvalidMnemonicException if the mnemonic is not in the opcode table
     */
    private static String encodeWithLoadStoreFormat(Instruction instruction) throws InvalidMnemonicException {
        StringBuilder sb = new StringBuilder();

        int opcodeValue = OpcodeLookupTable.getOpcodeValue(instruction.getMnemonic());
        sb.append(getZeroPaddedBinaryString(opcodeValue, 6));

        int operandIndex = 0;

        if (instruction.getMnemonic().equals("LDX")
                || instruction.getMnemonic().equals("STX")
                || instruction.getMnemonic().equals("JMA")
                || instruction.getMnemonic().equals("JSR")) {
            // Handle the special cases separately.
            // `LDX`, `STX`, `JMA`, and `JSR` don't use the `R` field, so set it as `00`.
            sb.append("00");
        } else {
            sb.append(getZeroPaddedBinaryString(instruction.getOperands()[operandIndex++], 2));
        }

        if (instruction.getMnemonic().equals("RFS")
                || instruction.getMnemonic().equals("AIR")
                || instruction.getMnemonic().equals("SIR")) {
            // `RFS`, `AIR`, and `SIR` don't use the `IX` so set it as `00`.
            sb.append("00");
        } else {
            sb.append(getZeroPaddedBinaryString(instruction.getOperands()[operandIndex++], 2));
        }

        if (instruction.getMnemonic().equals("AIR") || instruction.getMnemonic().equals("SIR") || instruction.getOperands().length < 4) {
            // `AIR` and `SIR` don't support indirect addressing; other instructions default to direct (I=0).
            sb.append("0");
        } else {
            // Add the `I` field if it exists.
            sb.append(instruction.getOperands()[3]);
        }

        // Add the `address` field at the end.
        sb.append(getZeroPaddedBinaryString(instruction.getOperands()[operandIndex], 5));

        return sb.toString();
    }


    /**
     * Encodes instructions that use the register-register word format.
     *
     * <p>Word layout: {@code [opcode:6][Rx:2][Ry:2][unused:6]}
     *
     * <p>Special case: {@code NOT} does not use the {@code Ry} field (set to {@code 00}).
     *
     * @param instruction the instruction to encode
     * @return a 16-character binary string representing the encoded word
     * @throws InvalidMnemonicException if the mnemonic is not in the opcode table
     */
    private static String encodeWithRegisterRegisterFormat(Instruction instruction) throws InvalidMnemonicException {
        StringBuilder sb = new StringBuilder();

        int opcodeValue = OpcodeLookupTable.getOpcodeValue(instruction.getMnemonic());
        sb.append(getZeroPaddedBinaryString(opcodeValue, 6));

        // Add the `rx` field.
        sb.append(getZeroPaddedBinaryString(instruction.getOperands()[0], 2));

        if (instruction.getMnemonic().equals("NOT")) {
            // Handle the special cases separately.
            // `NOT` doesn't use the `ry` field, so set it as `00`.
            sb.append("00");
        } else {
            sb.append(getZeroPaddedBinaryString(instruction.getOperands()[1], 2));
        }

        // Register to Register operations have an empty space at the end of 6 bits. So, fill it with `000000`.
        sb.append("000000");

        return sb.toString();
    }

    /**
     * Encodes shift and rotate instructions (SRC, RRC).
     *
     * <p>Word layout: {@code [opcode:6][R:2][A/L:1][R/L:1][unused:2][count:4]}
     *
     * <p>Operand order expected in the instruction: {@code R, count, R/L, A/L}
     *
     * @param instruction the instruction to encode
     * @return a 16-character binary string representing the encoded word
     * @throws InvalidMnemonicException if the mnemonic is not in the opcode table
     */
    private static String encodeWithShiftRotateFormat(Instruction instruction) throws InvalidMnemonicException {
        StringBuilder sb = new StringBuilder();

        int opcodeValue = OpcodeLookupTable.getOpcodeValue(instruction.getMnemonic());
        sb.append(getZeroPaddedBinaryString(opcodeValue, 6));

        // Add the `R` (register) field.
        sb.append(getZeroPaddedBinaryString(instruction.getOperands()[0], 2));

        // The `A/L` and `R/L` fields are already a single-length binary string, so they can be appended directly.
        // Operand[3] = A/L (arithmetic vs logical), operand[2] = R/L (right vs left).
        sb.append(instruction.getOperands()[3]);
        sb.append(instruction.getOperands()[2]);

        // The `count` field is preceded by an empty space of 2 bits, so fill it with `00`.
        sb.append("00");

        // The `count` field should be added at the end.
        sb.append(getZeroPaddedBinaryString(instruction.getOperands()[1], 4));

        return sb.toString();
    }

    /**
     * Encodes I/O instructions (IN, OUT, CHK).
     *
     * <p>Word layout: {@code [opcode:6][R:2][unused:3][DevID:5]}
     *
     * @param instruction the instruction to encode
     * @return a 16-character binary string representing the encoded word
     * @throws InvalidMnemonicException if the mnemonic is not in the opcode table
     */
    private static String encodeWithIOFormat(Instruction instruction) throws InvalidMnemonicException {
        StringBuilder sb = new StringBuilder();

        int opcodeValue = OpcodeLookupTable.getOpcodeValue(instruction.getMnemonic());
        sb.append(getZeroPaddedBinaryString(opcodeValue, 6));

        // Add the `R` (register) field.
        sb.append(getZeroPaddedBinaryString(instruction.getOperands()[0], 2));

        // The `DevID` field is preceded by an empty space of 3 bits, so fill it with `000`.
        sb.append("000");

        // The `DevID` field should be added at the end.
        sb.append(getZeroPaddedBinaryString(instruction.getOperands()[1], 5));

        return sb.toString();
    }

    /**
     * Encodes miscellaneous instructions (HLT, TRAP).
     *
     * <p>Word layout:
     * <ul>
     *   <li>HLT:  {@code [opcode:6][0000000000]}</li>
     *   <li>TRAP: {@code [opcode:6][unused:6][trap_code:4]}</li>
     * </ul>
     *
     * @param instruction the instruction to encode
     * @return a 16-character binary string representing the encoded word
     * @throws InvalidMnemonicException if the mnemonic is not in the opcode table
     */
    private static String encodeWithMiscFormat(Instruction instruction) throws InvalidMnemonicException {
        StringBuilder sb = new StringBuilder();

        int opcodeValue = OpcodeLookupTable.getOpcodeValue(instruction.getMnemonic());
        sb.append(getZeroPaddedBinaryString(opcodeValue, 6));

        if (instruction.getMnemonic().equals("HLT")) {
            // `HLT` doesn't use any operands, so set them as `0000000000`.
            sb.append("0000000000");
        } else if (instruction.getMnemonic().equals("TRAP")) {
            // `TRAP` instruction has an empty space of 6 bits before the Trap Code, so fill it with `000000`.
            sb.append("000000");
            sb.append(getZeroPaddedBinaryString(instruction.getOperands()[0], 4));
        }

        return sb.toString();
    }
}
