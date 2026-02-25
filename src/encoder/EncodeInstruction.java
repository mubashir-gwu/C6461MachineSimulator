package encoder;

import instruction.Instruction;
import opcode.InvalidMnemonicException;
import opcode.OpcodeLookupTable;

public class EncodeInstruction {
    public static String encode(Instruction instruction) throws InvalidMnemonicException {
        return switch (OpcodeLookupTable.getOpcodeType(instruction.mnemonic())) {
            case LOAD_STORE, TRANSFER, ARITHMETIC -> encodeWithLoadStoreFormat(instruction);
            case MULTIPLY_DIVIDE, LOGICAL -> encodeWithRegisterRegisterFormat(instruction);
            case SHIFT_ROTATE -> encodeWithShiftRotateFormat(instruction);
            case IO -> encodeWithIOFormat(instruction);
            case MISC -> encodeWithMiscFormat(instruction);
        };
    }

    private static String encodeWithLoadStoreFormat(Instruction instruction) {
        // Handle the special cases separately.
        if (instruction.mnemonic().equals("LDX") || instruction.mnemonic().equals("STX")) {
            return "";
        } else if (instruction.mnemonic().equals("JMA") || instruction.mnemonic().equals("JSR")) {
            return "";
        } else if (instruction.mnemonic().equals("RFS")) {
            return "";
        } else if (instruction.mnemonic().equals("AIR") || instruction.mnemonic().equals("SIR")) {
            return "";
        }

        return "";
    }

    private static String encodeWithRegisterRegisterFormat(Instruction instruction) {
        // Handle the special case separately.
        if (instruction.mnemonic().equals("NOT")) {
            return "";
        }

        return "";
    }

    private static String encodeWithShiftRotateFormat(Instruction instruction) {
        return "";
    }

    private static String encodeWithIOFormat(Instruction instruction) {
        return "";
    }

    private static String encodeWithMiscFormat(Instruction instruction) {
        return "";
    }
}
