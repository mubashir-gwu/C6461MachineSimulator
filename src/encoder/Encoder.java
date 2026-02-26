package encoder;

import java.util.*;

public class Encoder {
    private final static Map<String, Integer> labelsMap = new HashMap<>();

    public static void encode(String[] lines) {
        // Clear the contents from any previous encoding.
        labelsMap.clear();

        final List<TokenizedLine> tokenizedLines = getTokenizedLines(lines);

        System.out.println("Tokenized Lines:");
        for (TokenizedLine tokenizedLine : tokenizedLines) {
            System.out.println(tokenizedLine);
        }

        System.out.println("================================================================================");

        buildLabelsMap(tokenizedLines);
        System.out.println("Labels Map:");
        labelsMap.forEach((key, value) -> System.out.println(key + " -> " + value));
    }

    private static void buildLabelsMap(List<TokenizedLine> tokenizedLines) {
        int counter = 0;

        for (TokenizedLine tokenizedLine : tokenizedLines) {
            if (tokenizedLine.opcode().equals("LOC")) {
                counter = Integer.parseInt(tokenizedLine.operands()[0]);
                continue;
            }

            if (!tokenizedLine.label().isEmpty()) {
                labelsMap.put(tokenizedLine.label(), counter);
            }
            counter++;
        }
    }

    private static List<TokenizedLine> getTokenizedLines(String[] lines) {
        // TODO: Skip blank lines.

        final List<TokenizedLine> tokenizedLines = new ArrayList<>();

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
            tokenizedLines.add(new TokenizedLine(label, opcode, operands));
        }

        return tokenizedLines;
    }
}
