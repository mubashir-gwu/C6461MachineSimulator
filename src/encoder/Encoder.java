package encoder;

import instruction.Instruction;

import java.util.*;

public class Encoder {
    private final static Map<String, Integer> labelsMap = new HashMap<>();

    public static void encode(String[] lines) {
        // Clear the contents from any previous encoding.
        labelsMap.clear();

        final List<Instruction> instructions = getTokenizedInstructions(lines);

        System.out.println("Tokenized Lines:");
        for (Instruction instruction : instructions) {
            System.out.println(instruction);
        }

        System.out.println("================================================================================");

        buildLabelsMap(instructions);
        System.out.println("Labels Map:");
        labelsMap.forEach((key, value) -> System.out.println(key + " -> " + value));

        System.out.println("================================================================================");

        encodeLines(instructions);
    }

    private static void encodeLines(List<Instruction> instructions) {
        int counter = 0;

        for (Instruction instruction : instructions) {
            if (instruction.mnemonic().equals("LOC")) {
                counter = Integer.parseInt(instruction.operands()[0]);
                continue;
            }

            List<String> flattenedOperands = new ArrayList<>();

            for (String operand : instruction.operands()) {
                try {
                    Integer.parseInt(operand);
                    flattenedOperands.add(operand);
                } catch (NumberFormatException e) {
                    if (labelsMap.containsKey(operand)) {
                        flattenedOperands.add(String.valueOf(labelsMap.get(operand)));
                    }
                }
            }

            instruction = new Instruction(instruction.label(), instruction.mnemonic(), flattenedOperands.toArray(new String[0]));

            if (instruction.mnemonic().equalsIgnoreCase("LOC")) {
                continue;
            }

            try {
                final String encoded = InstructionEncoder.encodeInstruction(instruction);

                StringBuilder sb = new StringBuilder(Integer.toOctalString(counter++));
                while (sb.length() < 6) {
                    sb.insert(0, "0");
                }

                System.out.println(sb + " " + encoded);
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    private static void buildLabelsMap(List<Instruction> instructions) {
        int counter = 0;

        for (Instruction instruction : instructions) {
            if (instruction.mnemonic().equals("LOC")) {
                counter = Integer.parseInt(instruction.operands()[0]);
                continue;
            }

            if (!instruction.label().isEmpty()) {
                labelsMap.put(instruction.label(), counter);
            }
            counter++;
        }
    }

    private static List<Instruction> getTokenizedInstructions(String[] lines) {
        // TODO: Skip blank lines.

        final List<Instruction> instructions = new ArrayList<>();

        for (String line : lines) {
            // Remove comment from the lines.
            line = line.contains(";") ? line.substring(0, line.indexOf(";")) : line;
            line = line.trim();

            final String[] tokens = line.split("\\s+");
            final String label;
            final String opcode;
            final String[] operands;
            if (tokens[0].endsWith(":")) {
                label = tokens[0].endsWith(":") ? tokens[0].substring(0, tokens[0].length() - 1) : "";
                opcode = tokens[1];
                operands = tokens.length == 3 ? tokens[2].split(",") : new String[0];
            } else {
                label = "";
                opcode = tokens[0];
                operands = tokens.length == 2 ? tokens[1].split(",") : new String[0];
            }
            instructions.add(new Instruction(label, opcode, operands));
        }

        return instructions;
    }
}
