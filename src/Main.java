import ui.UserInterface;

import javax.swing.*;

/**
 * Application entry point for the C6461 Machine Simulator.
 *
 * <p>Launches the Swing-based {@link UserInterface} on the Event Dispatch Thread (EDT)
 * as required by the Swing threading model.
 */
public class Main {
    /**
     * Starts the simulator by creating and displaying the main UI window.
     *
     * @param args command-line arguments (not used)
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new UserInterface().setVisible(true));
    }
}
