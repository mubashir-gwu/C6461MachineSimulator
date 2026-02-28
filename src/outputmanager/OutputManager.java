package outputmanager;

import javax.swing.*;

/**
 * Manages writing messages and errors to the simulator's output text area.
 *
 * <p>The {@code OutputManager} wraps a {@link JTextArea} and provides typed write methods
 * so that all parts of the simulator (CPU, UI button handlers) route output through a
 * single, consistent channel.
 */
public class OutputManager {
    /** The Swing text area that receives all simulator output. */
    private final JTextArea printerTextArea;

    /**
     * Constructs an {@code OutputManager} that writes to the given text area.
     *
     * @param printerTextArea the {@link JTextArea} in the simulator UI to append output to
     */
    public OutputManager(JTextArea printerTextArea) {
        this.printerTextArea = printerTextArea;
    }

    /**
     * Appends an error message to the output area, prefixed with {@code "Error: "}.
     *
     * @param error the error description to display
     */
    public void writeError(String error) {
        printerTextArea.append(String.format("Error: %s\n", error));
    }

    /**
     * Appends an informational message to the output area.
     *
     * @param message the message to display
     */
    public void writeMessage(String message) {
        printerTextArea.append(String.format("%s\n", message));
    }

    /**
     * Converts a decimal integer to a zero-padded 6-digit octal string for display.
     *
     * <p>Example: decimal {@code 5} becomes {@code "000005"}.
     *
     * @param decimalValue the integer value to format
     * @return a 6-digit zero-padded octal string
     */
    public static String getPaddedOctalValue(int decimalValue) {
        StringBuilder sb = new StringBuilder(Integer.toOctalString(decimalValue));
        while (sb.length() < 6) {
            sb.insert(0, "0");
        }

        return sb.toString();
    }
}
