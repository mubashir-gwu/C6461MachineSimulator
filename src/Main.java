import encoder.EncodeInstruction;
import encoder.Encoder;
import instruction.Instruction;
import opcode.InvalidMnemonicException;

public class Main {
    public static void main(String[] args) {
        System.out.println("========== C6461 Assembler ==========");
        System.out.println();

        String[] sampleInstructions = new String[]{
                "LDX 2,0,7",
                "LDR 3,0,10",
                "LDR 2,2,10",
                "LDR 1,2,10,1",
                "LDA 0,0,0",
                "LDX 1,0,8",
                "JZ 0,1,0",
        };


        for (String instructionString : sampleInstructions) {
            Instruction sampleInstruction = getInstruction(instructionString);
            try {
                System.out.println(instructionString);
                System.out.println(EncodeInstruction.encodePrevious(sampleInstruction));
                System.out.println();
            } catch (InvalidMnemonicException e) {
                e.printStackTrace();
            }
        }
    }

    private static Instruction getInstruction(String instructionString) {
        String[] parts = instructionString.split("\\s+");
        String mnemonic = parts[0];
        String[] operands = parts[1].split(",");

        return new Instruction("", mnemonic, operands);
    }
}