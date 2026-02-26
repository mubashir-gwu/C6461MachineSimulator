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

        prettyPrintListingFile(instructions);
        System.out.println("================================================================================");
        prettyPrintLoadFile(instructions);
    }

    private static void prettyPrintListingFile(List<Instruction> instructions) {
        for (Instruction instruction : instructions) {
            System.out.println(instruction.toListingFileString());
        }
    }

    private static void prettyPrintLoadFile(List<Instruction> instructions) {
        for (Instruction instruction : instructions) {
            if (instruction.getOctal().isEmpty()) {
                // If the instruction was a `LOC` line, it won't have an address or the octal representation.
                // So, skip it as it will just print a blank line.
                continue;
            }

            System.out.println(instruction.toLoadFileString());
        }
    }


    private static void encodeLines(List<Instruction> instructions) {
        for (Instruction instruction : instructions) {
            if (instruction.getMnemonic().equals("LOC")) {
                continue;
            }

            List<String> flattenedOperands = new ArrayList<>();

            for (String operand : instruction.getOperands()) {
                try {
                    Integer.parseInt(operand);
                    flattenedOperands.add(operand);
                } catch (NumberFormatException e) {
                    if (labelsMap.containsKey(operand)) {
                        flattenedOperands.add(String.valueOf(labelsMap.get(operand)));
                    }
                }
            }

            instruction.setOperands(flattenedOperands.toArray(new String[0]));

            if (instruction.getMnemonic().equalsIgnoreCase("LOC")) {
                continue;
            }

            try {
                instruction.setOctal(InstructionEncoder.encodeInstruction(instruction));
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    private static void buildLabelsMap(List<Instruction> instructions) {
        int counter = 0;

        for (Instruction instruction : instructions) {
            if (instruction.getMnemonic().equals("LOC")) {
                counter = Integer.parseInt(instruction.getOperands()[0]);
                continue;
            }

            StringBuilder sb = new StringBuilder(Integer.toOctalString(counter));
            while (sb.length() < 6) {
                sb.insert(0, "0");
            }
            instruction.setLoc(sb.toString());

            if (!instruction.getLabel().isEmpty()) {
                labelsMap.put(instruction.getLabel(), counter);
            }
            counter++;
        }
    }

    private static List<Instruction> getTokenizedInstructions(String[] lines) {
        final List<Instruction> instructions = new ArrayList<>();

        for (String line : lines) {
            if (line.trim().isEmpty()) {
                continue;
            }

            final String comment = line.contains(";") ? line.substring(line.indexOf(";") + 1).trim() : "";

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
            instructions.add(new Instruction(label, opcode, operands, comment, "", ""));
        }

        return instructions;
    }
}
