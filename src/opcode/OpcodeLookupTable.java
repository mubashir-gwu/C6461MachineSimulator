package opcode;

import java.util.HashMap;
import java.util.Map;

public class OpcodeLookupTable {
    private static final Map<String, Integer> opcodeMap = new HashMap<>();

    static {
        // Load/Store instructions.
        opcodeMap.put("LDR", 001);
        opcodeMap.put("STR", 002);
        opcodeMap.put("LDA", 003);
        opcodeMap.put("LDX", 041);
        opcodeMap.put("STX", 042);

        // Transfer instructions.
        opcodeMap.put("JZ", 010);
        opcodeMap.put("JNE", 011);
        opcodeMap.put("JCC", 012);
        opcodeMap.put("JMA", 013);
        opcodeMap.put("JSR", 014);
        opcodeMap.put("RFS", 015);
        opcodeMap.put("SOB", 016);
        opcodeMap.put("JGE", 017);

        // Arithmetic instructions.
        opcodeMap.put("AMR", 004);
        opcodeMap.put("SMR", 005);
        opcodeMap.put("AIR", 006);
        opcodeMap.put("SIR", 007);

        // Multiply/Divide operations.
        opcodeMap.put("MLT", 070);
        opcodeMap.put("DVD", 071);

        // Logical operations.
        opcodeMap.put("TRR", 072);
        opcodeMap.put("AND", 073);
        opcodeMap.put("ORR", 074);
        opcodeMap.put("NOT", 075);

        // Shift/Rotate operations.
        opcodeMap.put("SRC", 031);
        opcodeMap.put("RRC", 032);

        // I/O operations.
        opcodeMap.put("IN", 061);
        opcodeMap.put("OUT", 062);
        opcodeMap.put("CHK", 063);

        // Miscellaneous instructions.
        opcodeMap.put("HLT", 000);
        opcodeMap.put("TRAP", 030);
    }

    public static int getOpcode(String mnemonic) throws InvalidMnemonicException {
        if (!opcodeMap.containsKey(mnemonic)) {
            throw new InvalidMnemonicException(mnemonic);
        }
        return opcodeMap.get(mnemonic);
    }

    public static void printOpcodeMap() {
        for (Map.Entry<String, Integer> entry : opcodeMap.entrySet()) {
            System.out.println(entry.getKey() + ": " + entry.getValue());
        }
    }
}
