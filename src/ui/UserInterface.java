package ui;

import cpu.CPU;
import encoder.Encoder;
import encoder.EncoderStringUtil;
import fileutil.FileReader;
import fileutil.FileWriter;
import instruction.Instruction;
import memory.Register;
import memory.RegisterManager;
import outputmanager.OutputManager;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserInterface extends JFrame {
    private OutputManager outputManager;
    private JTextField binaryOutputTextField;
    private String octalInputValue = "";
    private final Map<Register, JTextField> registerTextFieldMap = new HashMap<>();
    private Path programFilePath;
    private CPU cpu;

    private final Font monospaceFont = new Font("Consolas", Font.PLAIN, 14);
    private final Font monospaceBoldFont = new Font("Consolas", Font.BOLD, 14);

    private void loadOctalValueIntoRegister(Register register, String octalValue) {
        RegisterManager.loadRegister(register, Integer.parseInt(octalValue, 8));
    }

    public UserInterface() {
        cpu = new CPU();

        setTitle("C6461 Assembler");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);     // Exit the application when the window is closed.
        setSize(1000, 700);
        setLocationRelativeTo(null);                        // Center the window on the screen.

        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel title = new JLabel("C6461 Machine Simulator");
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        title.setFont(new Font("Arial", Font.BOLD, 24));

        JPanel executionPanels = new JPanel(new GridLayout(1, 3, 10, 10));

        executionPanels.add(getLeftPanel());
        executionPanels.add(getCenterPanel());
        executionPanels.add(getRightPanel());

        root.add(title, BorderLayout.NORTH);
        root.add(Box.createVerticalStrut(10));
        root.add(getFilePickerPanel());
        root.add(Box.createVerticalStrut(10));
        root.add(executionPanels);

        setContentPane(root);
    }

    private JPanel getFilePickerPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Load Program"),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        mainPanel.setBackground(Color.getHSBColor(0.65f, 0.1f, 0.9f));

        JLabel label = new JLabel("Program: ");

        JTextField textField = new JTextField(20);
        textField.setEditable(false);
        textField.setFocusable(false);

        JButton button = new JButton("Browse");
        button.addActionListener(e -> {
            // Open a file chooser dialog to select a file in the current directory.
            JFileChooser fileChooser = new JFileChooser(new File("."));

            int returnVal = fileChooser.showOpenDialog(this);

            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                textField.setText(selectedFile.getAbsolutePath());
                programFilePath = selectedFile.toPath();
                outputManager.writeMessage("Loaded program from " + selectedFile.getAbsolutePath());
            }
        });

        mainPanel.add(label, BorderLayout.WEST);
        mainPanel.add(textField, BorderLayout.CENTER);
        mainPanel.add(button, BorderLayout.EAST);
        mainPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, mainPanel.getPreferredSize().height));
        return mainPanel;
    }

    private JPanel getLeftPanel() {
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(""),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        leftPanel.setBackground(Color.getHSBColor(0.2f, 0.1f, 0.9f));

        leftPanel.add(getGprPanel());
        leftPanel.add(Box.createVerticalStrut(10));         // Add a gap of 10 between the panels.
        leftPanel.add(getIxrPanel());
        leftPanel.add(Box.createVerticalStrut(10));         // Add a gap of 10 between the panels.
        leftPanel.add(getOctalInputPanel());
        leftPanel.add(Box.createVerticalStrut(10));         // Add a gap of 10 between the panels.
        leftPanel.add(getBinaryOutputPanel());
        leftPanel.add(Box.createVerticalGlue());                  // Fill the remaining space with some blank space.

        return leftPanel;
    }

    private JPanel getCenterPanel() {
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(""),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        centerPanel.setBackground(Color.getHSBColor(0.5f, 0.1f, 0.9f));

        centerPanel.add(getSpecialRegistersPanel());
        centerPanel.add(Box.createVerticalStrut(10));         // Add a gap of 10 between the panels.
        centerPanel.add(getButtonsPanel());

        return centerPanel;
    }

    private JPanel getButtonsPanel() {
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Load/Store/Run"),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));


        JButton loadButton = new JButton("Load");
        loadButton.addActionListener(e -> {
            final String marText = registerTextFieldMap.get(Register.MAR).getText();
            if (marText.isBlank()) {
                outputManager.writeError("MAR is not set. Please set MAR first.");
                return;
            }

            final int marValue = Integer.parseInt(marText, 8);
            registerTextFieldMap.get(Register.MBR).setText(OutputManager.getPaddedOctalValue(cpu.getMemory().getMemoryAt(marValue)));
            outputManager.writeMessage("Loaded value " + OutputManager.getPaddedOctalValue(cpu.getMemory().getMemoryAt(marValue)) + " from address " + OutputManager.getPaddedOctalValue(marValue));
        });

        JButton loadPlusButton = new JButton("Load+");
        loadPlusButton.addActionListener(e -> {
            final String marText = registerTextFieldMap.get(Register.MAR).getText();
            if (marText.isBlank()) {
                outputManager.writeError("MAR is not set. Please set MAR first.");
                return;
            }

            final int marValue = Integer.parseInt(marText, 8);
            registerTextFieldMap.get(Register.MBR).setText(OutputManager.getPaddedOctalValue(cpu.getMemory().getMemoryAt(marValue)));
            outputManager.writeMessage("Loaded value " + OutputManager.getPaddedOctalValue(cpu.getMemory().getMemoryAt(marValue)) + " from address " + OutputManager.getPaddedOctalValue(marValue));

            // Increment the MAR register by 1 and display the value at the new address.
            registerTextFieldMap.get(Register.MAR).setText(OutputManager.getPaddedOctalValue(marValue + 1));
            registerTextFieldMap.get(Register.MBR).setText(OutputManager.getPaddedOctalValue(cpu.getMemory().getMemoryAt(marValue + 1)));
        });

        JButton storeButton = new JButton("Store");
        storeButton.addActionListener(e -> {
            final String marText = registerTextFieldMap.get(Register.MAR).getText();
            final String mbrText = registerTextFieldMap.get(Register.MBR).getText();

            if (marText.isBlank()) {
                outputManager.writeError("MAR is not set. Please set MAR first.");
                return;
            } else if (mbrText.isBlank()) {
                outputManager.writeError("MBR is not set. Please set MBR first.");
                return;
            }

            final int marValue = Integer.parseInt(registerTextFieldMap.get(Register.MAR).getText(), 8);
            final int mbrValue = Integer.parseInt(registerTextFieldMap.get(Register.MBR).getText(), 8);
            cpu.getMemory().setMemoryAt(marValue, mbrValue);

            outputManager.writeMessage("Stored value " + OutputManager.getPaddedOctalValue(mbrValue) + " at address " + OutputManager.getPaddedOctalValue(marValue));
        });

        JButton storePlusButton = new JButton("Store+");
        storePlusButton.addActionListener(e -> {
            final String marText = registerTextFieldMap.get(Register.MAR).getText();
            final String mbrText = registerTextFieldMap.get(Register.MBR).getText();

            if (marText.isBlank()) {
                outputManager.writeError("MAR is not set. Please set MAR first.");
                return;
            } else if (mbrText.isBlank()) {
                outputManager.writeError("MBR is not set. Please set MBR first.");
                return;
            }

            final int marValue = Integer.parseInt(registerTextFieldMap.get(Register.MAR).getText(), 8);
            final int mbrValue = Integer.parseInt(registerTextFieldMap.get(Register.MBR).getText(), 8);
            cpu.getMemory().setMemoryAt(marValue, mbrValue);

            outputManager.writeMessage("Stored value " + OutputManager.getPaddedOctalValue(mbrValue) + " at address " + OutputManager.getPaddedOctalValue(marValue));

            // Increment the MAR register by 1.
            registerTextFieldMap.get(Register.MAR).setText(OutputManager.getPaddedOctalValue(marValue + 1));
        });

        JButton runButton = new JButton("Run");
        runButton.addActionListener(e -> {
            System.out.println("TODO");
        });

        JButton stepButton = new JButton("Step");
        stepButton.addActionListener(e -> {
            System.out.println("TODO");
        });

        JButton haltButton = new JButton("Halt");
        haltButton.addActionListener(e -> {
            System.out.println("TODO");
        });

        JButton iplButton = new JButton("IPL");
        iplButton.addActionListener(e -> {
            if (programFilePath == null) {
                outputManager.writeError("Select a program first.");
                return;
            }

            List<String> programLines = FileReader.readFile(programFilePath);
            if (programLines == null) {
                outputManager.writeError("Could not read program file.");
                return;
            }

            List<Instruction> instructions = Encoder.encode(programLines);

            final String programName = FileWriter.getBaseFilename(programFilePath);

            FileWriter.writeListingFile(instructions, programName);
            Path loadFilePath = FileWriter.writeLoadFile(instructions, programName);

            int programStartAddress = cpu.loadProgramToMemory(loadFilePath);
            this.cpu.setProgramCounter(programStartAddress);

            registerTextFieldMap.get(Register.PC).setText(OutputManager.getPaddedOctalValue(programStartAddress));

            outputManager.writeMessage("Loaded program " + programName + " into memory.");
        });
        iplButton.setBackground(Color.RED);
        iplButton.setForeground(Color.WHITE);

        JPanel buttonsPanel = new JPanel(new GridLayout(4, 2, 20, 5));
        buttonsPanel.add(loadButton);
        buttonsPanel.add(runButton);

        buttonsPanel.add(loadPlusButton);
        buttonsPanel.add(stepButton);

        buttonsPanel.add(storeButton);
        buttonsPanel.add(haltButton);

        buttonsPanel.add(storePlusButton);
        buttonsPanel.add(iplButton);

        mainPanel.add(buttonsPanel);
        mainPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, mainPanel.getPreferredSize().height));

        return mainPanel;
    }

    private JPanel getRightPanel() {
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(""),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        rightPanel.setBackground(Color.getHSBColor(0.8f, 0.1f, 0.9f));

        JPanel outputPanel = new JPanel();
        outputPanel.setLayout(new BoxLayout(outputPanel, BoxLayout.Y_AXIS));
        outputPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Messages/Errors"),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        JTextArea outputTextArea = new JTextArea(5, 20);
        outputTextArea.setEditable(false);
        outputTextArea.setLineWrap(true);
        outputTextArea.setWrapStyleWord(true);
        outputPanel.add(outputTextArea);

        // Initialize the ErrorManager with the `outputTextArea` so that it can write the errors to it.
        outputManager = new OutputManager(outputTextArea);

        rightPanel.add(outputPanel);

        return rightPanel;
    }

    private JPanel getOctalInputPanel() {
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Octal Input"),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        JTextField textField = new JTextField(10);
        textField.setFont(monospaceFont);
        mainPanel.add(textField, BorderLayout.CENTER);
        mainPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, mainPanel.getPreferredSize().height));

        textField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                update();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                update();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                update();
            }

            void update() {
                octalInputValue = textField.getText();
                binaryOutputTextField.setText(EncoderStringUtil.getZeroPaddedBinaryString(textField.getText(), 16));
            }
        });


        return mainPanel;
    }

    private JPanel getBinaryOutputPanel() {
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Binary equivalent"),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        JTextField textField = new JTextField(10);
        textField.setEditable(false);
        textField.setFocusable(false);
        textField.setBackground(Color.LIGHT_GRAY);
        textField.setFont(monospaceBoldFont);
        mainPanel.add(textField, BorderLayout.CENTER);
        mainPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, mainPanel.getPreferredSize().height));

        binaryOutputTextField = textField;

        return mainPanel;
    }

    private JPanel getGprPanel() {
        JPanel gprPanel = new JPanel();
        gprPanel.setLayout(new BoxLayout(gprPanel, BoxLayout.Y_AXIS));
        gprPanel.setBorder(BorderFactory.createTitledBorder("General Purpose Registers"));

        gprPanel.add(getLabelledTextField(Register.GPR0));
        gprPanel.add(getLabelledTextField(Register.GPR1));
        gprPanel.add(getLabelledTextField(Register.GPR2));
        gprPanel.add(getLabelledTextField(Register.GPR3));

        return gprPanel;
    }

    private JPanel getIxrPanel() {
        JPanel ixrPanel = new JPanel();
        ixrPanel.setLayout(new BoxLayout(ixrPanel, BoxLayout.Y_AXIS));
        ixrPanel.setBorder(BorderFactory.createTitledBorder("Index Registers"));

        ixrPanel.add(getLabelledTextField(Register.IX1));
        ixrPanel.add(getLabelledTextField(Register.IX2));
        ixrPanel.add(getLabelledTextField(Register.IX3));

        return ixrPanel;
    }

    private JPanel getSpecialRegistersPanel() {
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createTitledBorder("Special Registers/Counters"));

        mainPanel.add(getLabelledTextField(Register.PC));
        mainPanel.add(getLabelledTextField(Register.MAR));
        mainPanel.add(getLabelledTextField(Register.MBR));
        mainPanel.add(getLabelledTextField(Register.IR, false));

        return mainPanel;
    }

    private JPanel getLabelledTextField(Register register) {
        return getLabelledTextField(register, true);
    }

    private JPanel getLabelledTextField(Register register, boolean withButton) {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        JLabel label = new JLabel(String.format("%-4s", register));
        label.setFont(monospaceBoldFont);

        JTextField textField = new JTextField(10);
        textField.setEditable(false);
        textField.setFocusable(false);
        textField.setBackground(Color.LIGHT_GRAY);
        textField.setFont(monospaceFont);

        registerTextFieldMap.put(register, textField);

        mainPanel.add(label, BorderLayout.WEST);
        mainPanel.add(textField, BorderLayout.CENTER);

        if (withButton) {
            JButton button = new JButton("Load");
            mainPanel.add(button, BorderLayout.EAST);

            button.addActionListener(e -> {
                if (octalInputValue.isBlank()) {
                    outputManager.writeError("Please enter a value to load into " + register);
                    return;
                }
                try {
                    final int octalValue = Integer.parseInt(octalInputValue, 8);
                    loadOctalValueIntoRegister(register, octalInputValue);
                    outputManager.writeMessage("Loaded value " + OutputManager.getPaddedOctalValue(octalValue) + " into register " + register);

                    textField.setText(OutputManager.getPaddedOctalValue(octalValue));
                } catch (NumberFormatException ex) {
                    outputManager.writeError("Invalid octal value: " + octalInputValue);
                }
            });
        }

        mainPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, mainPanel.getPreferredSize().height));

        return mainPanel;
    }
}
