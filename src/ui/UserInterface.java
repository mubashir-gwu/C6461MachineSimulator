package ui;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

public class UserInterface extends JFrame {
    public UserInterface() {
        setTitle("C6461 Assembler");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);     // Exit the application when the window is closed.
        setSize(1000, 600);
        setLocationRelativeTo(null);                        // Center the window on the screen.

        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel title = new JLabel("C6461 Machine Simulator");
        title.setFont(new Font("Arial", Font.BOLD, 24));
        root.add(title, BorderLayout.NORTH);

        JPanel panels = new JPanel(new GridLayout(1, 3, 10, 10));

        panels.add(getLeftPanel());
        panels.add(getCenterPanel());
        panels.add(getRightPanel());
        root.add(panels, BorderLayout.CENTER);

        setContentPane(root);
    }

    private JPanel getLeftPanel() {
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Left Panel"),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        leftPanel.add(getGprPanel());
        leftPanel.add(Box.createVerticalStrut(10));         // Add a gap of 10 between the panels.
        leftPanel.add(getIxrPanel());
        leftPanel.add(Box.createVerticalGlue());                  // Fill the remaining space with blank space.

        return leftPanel;
    }

    private JPanel getCenterPanel() {
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Center Panel"),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        centerPanel.add(getSpecialRegistersPanel());

        return centerPanel;
    }

    private JPanel getRightPanel() {
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Right Panel"),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));


        JPanel cacheContentPanel = new JPanel();
        cacheContentPanel.setLayout(new BoxLayout(cacheContentPanel, BoxLayout.Y_AXIS));
        cacheContentPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Cache Content"),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        JTextArea cacheContentTextArea = new JTextArea(5, 20);
        cacheContentTextArea.setEditable(false);
        cacheContentTextArea.setLineWrap(true);
        cacheContentTextArea.setWrapStyleWord(true);
        cacheContentPanel.add(cacheContentTextArea);

        JPanel printerPanel = new JPanel();
        printerPanel.setLayout(new BoxLayout(printerPanel, BoxLayout.Y_AXIS));
        printerPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Printer"),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        JTextArea printerTextArea = new JTextArea(5, 20);
        printerTextArea.setEditable(false);
        printerTextArea.setLineWrap(true);
        printerTextArea.setWrapStyleWord(true);
        printerPanel.add(printerTextArea);

        rightPanel.add(cacheContentPanel);
        rightPanel.add(Box.createVerticalStrut(10));
        rightPanel.add(printerPanel);

        return rightPanel;
    }

    private JPanel getGprPanel() {
        JPanel gprPanel = new JPanel();
        gprPanel.setLayout(new BoxLayout(gprPanel, BoxLayout.Y_AXIS));
        gprPanel.setBorder(BorderFactory.createTitledBorder("General Purpose Registers"));

        gprPanel.add(getLabelledTextField("R0"));
        gprPanel.add(getLabelledTextField("R1"));
        gprPanel.add(getLabelledTextField("R2"));
        gprPanel.add(getLabelledTextField("R3"));

        return gprPanel;
    }

    private JPanel getIxrPanel() {
        JPanel ixrPanel = new JPanel();
        ixrPanel.setLayout(new BoxLayout(ixrPanel, BoxLayout.Y_AXIS));
        ixrPanel.setBorder(BorderFactory.createTitledBorder("Index Registers"));

        ixrPanel.add(getLabelledTextField("IX1"));
        ixrPanel.add(getLabelledTextField("IX2"));
        ixrPanel.add(getLabelledTextField("IX3"));

        return ixrPanel;
    }

    private JPanel getSpecialRegistersPanel() {
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createTitledBorder("Special Registers/Counters"));

        mainPanel.add(getLabelledTextField("PC"));
        mainPanel.add(getLabelledTextField("MAR"));
        mainPanel.add(getLabelledTextField("MBR"));
        mainPanel.add(getLabelledTextField("IR", false));

        return mainPanel;
    }

    private static JPanel getLabelledTextField(String labelTex) {
        return getLabelledTextField(labelTex, true);
    }

    private static JPanel getLabelledTextField(String labelText, boolean withButton) {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        JLabel label = new JLabel(labelText);
        JTextField textField = new JTextField(10);

        mainPanel.add(label, BorderLayout.WEST);
        mainPanel.add(textField, BorderLayout.CENTER);

        if (withButton) {
            JButton button = new JButton();
            mainPanel.add(button, BorderLayout.EAST);
        }

        mainPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, mainPanel.getPreferredSize().height));

        return mainPanel;
    }
}
