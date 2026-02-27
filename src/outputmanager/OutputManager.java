package outputmanager;

import javax.swing.*;

public class OutputManager {
    private final JTextArea printerTextArea;

    public OutputManager(JTextArea printerTextArea) {
        this.printerTextArea = printerTextArea;
    }

    public void writeError(String error) {
        printerTextArea.append(String.format("Error: %s\n", error));
    }

    public void writeMessage(String message) {
        printerTextArea.append(String.format("%s\n", message));
    }

    public static String getPaddedOctalValue(int decimalValue) {
        StringBuilder sb = new StringBuilder(Integer.toOctalString(decimalValue));
        while (sb.length() < 6) {
            sb.insert(0, "0");
        }

        return sb.toString();
    }
}
