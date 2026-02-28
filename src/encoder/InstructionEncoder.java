package encoder;

import instruction.Instruction;
import opcode.InvalidMnemonicException;
import opcode.OpcodeLookupTable;

import static encoder.EncoderStringUtil.getOctalString;
import static encoder.EncoderStringUtil.getZeroPaddedBinaryString;

public class InstructionEncoder {
    public static String encodeInstruction(Instruction instruction) throws InvalidMnemonicException {
        if (instruction.getMnemonic().equalsIgnoreCase("DATA")) {
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
            sb.append("0");
        } else {
            // Add the `I` field if it exists.
            sb.append(instruction.getOperands()[3]);
        }

        // Add the `address` field at the end.
        sb.append(getZeroPaddedBinaryString(instruction.getOperands()[operandIndex], 5));

        return sb.toString();
    }


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

    private static String encodeWithShiftRotateFormat(Instruction instruction) throws InvalidMnemonicException {
        StringBuilder sb = new StringBuilder();

        int opcodeValue = OpcodeLookupTable.getOpcodeValue(instruction.getMnemonic());
        sb.append(getZeroPaddedBinaryString(opcodeValue, 6));

        sb.append(getZeroPaddedBinaryString(instruction.getOperands()[0], 2));

        // The `A/L` and `R/L` fields are already a single-length binary string, so they can be appended directly.
        sb.append(instruction.getOperands()[1]);
        sb.append(instruction.getOperands()[2]);

        // The `count` field is preceded by an empty space of 2 bits, so fill it with `00`.
        sb.append("00");

        // The `count` field should be added at the end.
        sb.append(getZeroPaddedBinaryString(instruction.getOperands()[1], 4));

        return sb.toString();
    }

    private static String encodeWithIOFormat(Instruction instruction) throws InvalidMnemonicException {
        StringBuilder sb = new StringBuilder();

        int opcodeValue = OpcodeLookupTable.getOpcodeValue(instruction.getMnemonic());
        sb.append(getZeroPaddedBinaryString(opcodeValue, 6));

        sb.append(getZeroPaddedBinaryString(instruction.getOperands()[0], 2));

        // The `DevID` field is preceded by an empty space of 3 bits, so fill it with `000`.
        sb.append("000");

        // The `DevID` field should be added at the end.
        sb.append(getZeroPaddedBinaryString(instruction.getOperands()[1], 5));

        return sb.toString();
    }

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
