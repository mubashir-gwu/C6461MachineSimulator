package encoder;

import instruction.Instruction;

import java.util.*;

/**
 * Two-pass assembler that converts a list of assembly source lines into encoded
 * {@link Instruction} objects ready for loading into the simulator.
 *
 * <h2>Pass 1 – Label resolution ({@link #buildLabelsMap})</h2>
 * <p>Each instruction is assigned its memory address (the {@code loc} field) based on
 * {@code LOC} directives and sequential address increments. Any symbolic labels found are
 * recorded in the {@link #labelsMap} so they can be substituted in pass 2.
 *
 * <h2>Pass 2 – Encoding ({@link #encodeLines})</h2>
 * <p>Label operands are replaced with their resolved numeric addresses, and each instruction
 * is handed to {@link InstructionEncoder} to produce the 6-digit octal machine word.
 */
public class Encoder {
    /**
     * Maps each symbolic label to the numeric memory address it was defined at.
     * Populated during pass 1 and consumed during pass 2.
     */
    private final static Map<String, Integer> labelsMap = new HashMap<>();

    /**
     * Assembles a list of source lines into a list of encoded {@link Instruction} objects.
     *
     * <p>The method is stateless between calls: the labels map is cleared at the start of
     * each invocation so the encoder can safely be reused across multiple IPL cycles.
     *
     * @param lines the raw source lines read from the assembly file
     * @return a list of {@link Instruction} objects with {@code loc} and {@code octal} fields populated
     */
    public static List<Instruction> encode(List<String> lines) {
        // Clear the contents from any previous encoding.
        labelsMap.clear();

        final List<Instruction> instructions = getTokenizedInstructions(lines);
        buildLabelsMap(instructions);
        encodeLines(instructions);

        return instructions;
    }

    /**
     * Pass 2: substitutes label operands with their resolved addresses and encodes each instruction.
     *
     * <p>{@code LOC} pseudo-instructions are skipped here because they are address directives,
     * not machine instructions to be encoded.
     *
     * @param instructions the instruction list produced by pass 1
     */
    private static void encodeLines(List<Instruction> instructions) {
        for (Instruction instruction : instructions) {
            if (instruction.getMnemonic().equals("LOC")) {
                continue;
            }

            // Replace any label-name operands with their resolved numeric address strings.
            List<String> flattenedOperands = new ArrayList<>();

            for (String operand : instruction.getOperands()) {
                try {
                    Integer.parseInt(operand);
                    flattenedOperands.add(operand);
                } catch (NumberFormatException e) {
                    // Operand is not a number, so treat it as a label reference.
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

    /**
     * Pass 1: assigns memory addresses to each instruction and records label definitions.
     *
     * <p>The address counter starts at 0. A {@code LOC n} directive sets the counter to {@code n};
     * all other instructions increment the counter by 1 after being assigned their address.
     *
     * @param instructions the tokenised instruction list from {@link #getTokenizedInstructions}
     */
    private static void buildLabelsMap(List<Instruction> instructions) {
        int counter = 0;

        for (Instruction instruction : instructions) {
            if (instruction.getMnemonic().equals("LOC")) {
                // LOC sets the current assembly address counter to the given operand value.
                counter = Integer.parseInt(instruction.getOperands()[0]);
                continue;
            }

            // Build the 6-digit zero-padded octal address string for this instruction.
            StringBuilder sb = new StringBuilder(Integer.toOctalString(counter));
            while (sb.length() < 6) {
                sb.insert(0, "0");
            }
            instruction.setLoc(sb.toString());

            // If the instruction has a label, record its address for pass 2 substitution.
            if (!instruction.getLabel().isEmpty()) {
                labelsMap.put(instruction.getLabel(), counter);
            }
            counter++;
        }
    }

    /**
     * Tokenises each source line into an {@link Instruction} object.
     *
     * <p>Lines that are blank or consist entirely of a comment are skipped. Inline comments
     * (text following {@code ;}) are stripped from the instruction tokens and stored separately.
     * The parser handles two token layouts:
     * <ul>
     *   <li>With label:    {@code LABEL: MNEMONIC [operands]}</li>
     *   <li>Without label: {@code MNEMONIC [operands]}</li>
     * </ul>
     *
     * @param lines the raw source lines to tokenise
     * @return a list of partially-constructed {@link Instruction} objects (no {@code loc} or {@code octal} yet)
     */
    private static List<Instruction> getTokenizedInstructions(List<String> lines) {
        final List<Instruction> instructions = new ArrayList<>();

        for (String line : lines) {
            if (line.trim().isEmpty()) {
                continue;
            }

            // Extract and strip the inline comment (everything after ';').
            final String comment = line.contains(";") ? line.substring(line.indexOf(";") + 1).trim() : "";

            // Remove comment from the lines.
            line = line.contains(";") ? line.substring(0, line.indexOf(";")) : line;
            line = line.trim();

            // Skip the comment-only lines (nothing left after stripping the comment).
            if (line.isEmpty()) {
                continue;
            }

            final String[] tokens = line.split("\\s+");
            final String label;
            final String opcode;
            final String[] operands;
            if (tokens[0].endsWith(":")) {
                // First token is a label (ends with ':'), mnemonic follows at index 1.
                label = tokens[0].endsWith(":") ? tokens[0].substring(0, tokens[0].length() - 1) : "";
                opcode = tokens[1];
                operands = tokens.length == 3 ? tokens[2].split(",") : new String[0];
            } else {
                // No label; mnemonic is at index 0.
                label = "";
                opcode = tokens[0];
                operands = tokens.length == 2 ? tokens[1].split(",") : new String[0];
            }
            instructions.add(new Instruction(label, opcode, operands, comment, "", ""));
        }

        return instructions;
    }
}
