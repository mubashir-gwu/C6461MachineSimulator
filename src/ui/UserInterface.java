package ui;

import cpu.CPU;
import encoder.Encoder;
import encoder.EncoderStringUtil;
import fileutil.FileReader;
import fileutil.FileWriter;
import instruction.Instruction;
import memory.Cache;
import memory.Register;
import outputmanager.OutputManager;
import trace.TraceLogger;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The main graphical console for the C6461 Machine Simulator.
 *
 * <p>The UI is divided into three columns:
 * <ul>
 *   <li><b>Left panel</b> – General Purpose Registers (GPR0–GPR3), Index Registers (IX1–IX3),
 *       and an octal input field with a live binary display for inspecting instruction words.</li>
 *   <li><b>Center panel</b> – Special registers (PC, MAR, MBR, IR) and the main control
 *       buttons (Load, Load+, Store, Store+, Run, Step, Halt, IPL).</li>
 *   <li><b>Right panel</b> – A scrollable messages/errors output area.</li>
 * </ul>
 *
 * <p>All register display fields show values in zero-padded 6-digit octal. Each register
 * row (except IR) has a "Load" button that writes the current octal input value into that
 * register. The IPL button assembles the selected source file, writes the load file, resets
 * the CPU state, loads the program into memory, and initialises the PC.
 */
public class UserInterface extends JFrame {
    /** Writes simulator messages and errors to the output text area in the right panel. */
    private OutputManager outputManager;

    /** Displays the 16-bit binary equivalent of the value typed in the Octal Input field. */
    private JTextField binaryOutputTextField;

    /** The current string value typed in the Octal Input field, used when loading into registers. */
    private String octalInputValue = "";

    /** Maps each register to its corresponding read-only display text field in the UI. */
    private final Map<Register, JTextField> registerTextFieldMap = new HashMap<>();

    /** Path to the assembly source file selected via the Browse button. */
    private Path programFilePath;

    /** The simulated CPU instance; recreated on each IPL to reset state. */
    private CPU cpu;

    /** Run button reference kept as a field so it can be enabled/disabled after halt. */
    private JButton runButton;

    /** Step button reference kept as a field so it can be enabled/disabled after halt. */
    private JButton stepButton;

    /** Halt button reference kept as a field so it can be enabled/disabled after halt. */
    private JButton haltButton;

    /** Checkbox to enable/disable trace logging to file. */
    private JCheckBox traceCheckbox;

    /** Text field where the user types input for the IN instruction (Console Keyboard). */
    private JTextField consoleKeyboardField;

    /** Text area that displays output from the OUT instruction (Console Printer). */
    private JTextArea consolePrinterArea;

    /** Labels displaying each of the 16 cache lines (Line# | Valid | Tag | Data). */
    private JLabel[] cacheLineLabels;

    /** Label showing whether the most recent cache access was a HIT or MISS. */
    private JLabel cacheHitMissLabel;

    /** Label showing the current FIFO replacement pointer position. */
    private JLabel cacheFifoLabel;

    /** Monospace font used for register value display fields. */
    private final Font monospaceFont = new Font(Font.MONOSPACED, Font.PLAIN, 14);

    /** Bold monospace font used for register label and binary output display fields. */
    private final Font monospaceBoldFont = new Font(Font.MONOSPACED, Font.BOLD, 14);

    /**
     * Parses an octal string and stores the resulting integer value into the specified register.
     *
     * @param register   the register to load
     * @param octalValue the octal string representation of the value to load
     */
    private void loadOctalValueIntoRegister(Register register, String octalValue) {
        cpu.getRegisterManager().loadRegister(register, Integer.parseInt(octalValue, 8));
    }

    /**
     * Constructs and lays out the simulator UI window.
     *
     * <p>Initialises the CPU, builds all sub-panels, and assembles the root layout.
     * The window is centred on screen and sized to 1000x700 pixels.
     */
    public UserInterface() {
        cpu = new CPU();

        setTitle("C6461 Machine Simulator");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);     // Exit the application when the window is closed.
        setSize(1300, 700);
        setLocationRelativeTo(null);                        // Center the window on the screen.

        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel title = new JLabel("C6461 Machine Simulator");
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        title.setFont(new Font("Arial", Font.BOLD, 24));

        JPanel executionPanels = new JPanel(new GridLayout(1, 4, 10, 10));

        executionPanels.add(getLeftPanel());
        executionPanels.add(getCenterPanel());
        executionPanels.add(getRightPanel());
        executionPanels.add(getCachePanel());

        root.add(title, BorderLayout.NORTH);
        root.add(Box.createVerticalStrut(10));
        root.add(getFilePickerPanel());
        root.add(Box.createVerticalStrut(10));
        root.add(executionPanels);

        setContentPane(root);

        // Wire console I/O after all UI components are created.
        wireConsoleIO();
    }

    /**
     * Builds the file-picker bar at the top of the window.
     *
     * <p>Contains a label, a read-only path text field, and a Browse button that opens a
     * file chooser starting in the current working directory.
     *
     * @return the constructed file-picker panel
     */
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

        traceCheckbox = new JCheckBox("Enable Trace Logging", true);
        traceCheckbox.setBackground(mainPanel.getBackground());
        traceCheckbox.addActionListener(e -> {
            TraceLogger.getInstance().setEnabled(traceCheckbox.isSelected());
            if (traceCheckbox.isSelected()) {
                outputManager.writeMessage("Trace logging enabled.");
            } else {
                outputManager.writeMessage("Trace logging disabled.");
            }
        });

        JPanel eastPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        eastPanel.setBackground(mainPanel.getBackground());
        eastPanel.add(traceCheckbox);
        eastPanel.add(button);

        mainPanel.add(label, BorderLayout.WEST);
        mainPanel.add(textField, BorderLayout.CENTER);
        mainPanel.add(eastPanel, BorderLayout.EAST);
        mainPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, mainPanel.getPreferredSize().height));
        return mainPanel;
    }

    /**
     * Builds the left panel containing the GPR display, IXR display, octal input, and binary output.
     *
     * @return the constructed left panel
     */
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
        leftPanel.add(Box.createVerticalGlue());            // Fill the remaining space with some blank space.

        return leftPanel;
    }

    /**
     * Builds the center panel containing the special registers and control buttons.
     *
     * @return the constructed center panel
     */
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

    /**
     * Connects the CPU's I/O callbacks to the console keyboard and printer UI components.
     *
     * <p>The console input provider reads and consumes one character at a time from the
     * keyboard text field. The console printer consumer appends characters to the printer area.
     */
    private void wireConsoleIO() {
        // IN (DEVID 0): read and consume one character from the keyboard field.
        // If the field is empty, prompt the user with a dialog.
        cpu.setConsoleInputProvider(() -> {
            String text = consoleKeyboardField.getText();
            if (text == null || text.isEmpty()) {
                // Prompt the user for input via a dialog.
                String input = JOptionPane.showInputDialog(this,
                        "Enter input for Console Keyboard:",
                        "Console Keyboard (IN)", JOptionPane.QUESTION_MESSAGE);
                if (input == null || input.isEmpty()) {
                    return -1; // User cancelled or entered nothing.
                }
                // Place the input into the keyboard field so characters are consumed one at a time.
                // Append CR (13) so programs that read until Enter (e.g., ReadNum) get a terminator.
                input = input + "\r";
                consoleKeyboardField.setText(input);
                text = input;
            }
            int ch = text.charAt(0);
            // Consume the character by removing it from the field.
            consoleKeyboardField.setText(text.substring(1));
            return ch;
        });

        // OUT (DEVID 1): append one character to the printer area.
        cpu.setConsolePrinterConsumer(charVal -> {
            consolePrinterArea.append(String.valueOf((char) (int) charVal));
        });

        // CHK (DEVID 0): keyboard status — 1 if the keyboard field has pending input.
        cpu.setConsoleKeyboardStatusProvider(() -> {
            String text = consoleKeyboardField.getText();
            return (text != null && !text.isEmpty()) ? 1 : 0;
        });
    }

    /**
     * Refreshes all register display fields and button states to match the current CPU state.
     *
     * <p>Called after every instruction execution, IPL, or manual register load to keep
     * the UI consistent with the CPU's internal register values.
     */
    private void syncUIWithCPU() {
        for (Register register : Register.values()) {
            int value = cpu.getRegisterManager().getRegisterValue(register);
            if (register == Register.CC || register == Register.MFR) {
                // Display 4-bit registers as 4-digit binary.
                registerTextFieldMap.get(register).setText(String.format("%4s", Integer.toBinaryString(value & 0xF)).replace(' ', '0'));
            } else {
                registerTextFieldMap.get(register).setText(OutputManager.getPaddedOctalValue(value));
            }
        }

        // Refresh cache display after every instruction.
        refreshCacheDisplay();

        stepButton.setEnabled(!cpu.isHalted());
        runButton.setEnabled(!cpu.isHalted());
        haltButton.setEnabled(!cpu.isHalted());
    }

    /**
     * Builds the panel containing the Load, Load+, Store, Store+, Run, Step, Halt, and IPL buttons.
     *
     * <p>Button behaviour:
     * <ul>
     *   <li><b>Load</b>    – reads the word at the address in MAR and stores it in MBR.</li>
     *   <li><b>Load+</b>   – same as Load, then increments MAR by 1.</li>
     *   <li><b>Store</b>   – writes the value in MBR to the address in MAR.</li>
     *   <li><b>Store+</b>  – same as Store, then increments MAR by 1.</li>
     *   <li><b>Run</b>     – executes all instructions until HLT or Halt.</li>
     *   <li><b>Step</b>    – executes a single instruction and updates the display.</li>
     *   <li><b>Halt</b>    – manually sets the CPU halted flag.</li>
     *   <li><b>IPL</b>     – assembles the selected file, resets the CPU, and loads the program.</li>
     * </ul>
     *
     * @return the constructed buttons panel
     */
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
            final int mbrValue;
            try {
                mbrValue = cpu.getMemory().getMemoryAt(marValue);
            } catch (IndexOutOfBoundsException ex) {
                outputManager.writeError(ex.getMessage());
                return;
            }

            cpu.getRegisterManager().loadRegister(Register.MAR, marValue);
            cpu.getRegisterManager().loadRegister(Register.MBR, mbrValue);

            syncUIWithCPU();

            outputManager.writeMessage("Loaded value " + OutputManager.getPaddedOctalValue(mbrValue) + " from address " + OutputManager.getPaddedOctalValue(marValue));
        });

        JButton loadPlusButton = new JButton("Load+");
        loadPlusButton.addActionListener(e -> {
            final String marText = registerTextFieldMap.get(Register.MAR).getText();
            if (marText.isBlank()) {
                outputManager.writeError("MAR is not set. Please set MAR first.");
                return;
            }

            int marValue = Integer.parseInt(marText, 8);
            int mbrValue;
            try {
                mbrValue = cpu.getMemory().getMemoryAt(marValue);
            } catch (IndexOutOfBoundsException ex) {
                outputManager.writeError(ex.getMessage());
                return;
            }

            cpu.getRegisterManager().loadRegister(Register.MAR, marValue);
            cpu.getRegisterManager().loadRegister(Register.MBR, mbrValue);

            outputManager.writeMessage("Loaded value " + OutputManager.getPaddedOctalValue(mbrValue) + " from address " + OutputManager.getPaddedOctalValue(marValue));

            // Increment the MAR register by 1 and display the value at the new address.
            marValue += 1;
            cpu.getRegisterManager().loadRegister(Register.MAR, marValue);
            // cpu.getRegisterManager().loadRegister(Register.MBR, cpu.getMemory().getMemoryAt(marValue));

            syncUIWithCPU();
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

            cpu.getRegisterManager().loadRegister(Register.MAR, marValue);
            cpu.getRegisterManager().loadRegister(Register.MBR, mbrValue);

            try {
                cpu.getMemory().setMemoryAt(marValue, mbrValue);
            } catch (IndexOutOfBoundsException ex) {
                outputManager.writeError(ex.getMessage());
                return;
            }

            outputManager.writeMessage("Stored value " + OutputManager.getPaddedOctalValue(mbrValue) + " at address " + OutputManager.getPaddedOctalValue(marValue));

            syncUIWithCPU();
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

            cpu.getRegisterManager().loadRegister(Register.MAR, marValue);
            cpu.getRegisterManager().loadRegister(Register.MBR, mbrValue);

            try {
                cpu.getMemory().setMemoryAt(marValue, mbrValue);
            } catch (IndexOutOfBoundsException ex) {
                outputManager.writeError(ex.getMessage());
                return;
            }

            outputManager.writeMessage("Stored value " + OutputManager.getPaddedOctalValue(mbrValue) + " at address " + OutputManager.getPaddedOctalValue(marValue));

            // Increment the MAR register by 1.
            cpu.getRegisterManager().loadRegister(Register.MAR, marValue + 1);

            syncUIWithCPU();
        });

        runButton = new JButton("Run");
        runButton.addActionListener(e -> {
            cpu.executeAllInstructions();
            syncUIWithCPU();
            TraceLogger.getInstance().close();

            outputManager.writeMessage("Execution halted.");
        });

        stepButton = new JButton("Step");
        stepButton.addActionListener(e -> {
            cpu.executeNextInstruction();
            syncUIWithCPU();

            if (cpu.isHalted()) {
                TraceLogger.getInstance().close();
                outputManager.writeMessage("Execution halted.");
            }
        });

        haltButton = new JButton("Halt");
        haltButton.addActionListener(e -> {
            cpu.setHalted(true);
            syncUIWithCPU();
            TraceLogger.getInstance().close();
            outputManager.writeMessage("Execution halted by user.");
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

            // Assemble the source file and write the listing and load output files.
            List<Instruction> instructions = Encoder.encode(programLines);

            final String programName = FileWriter.getBaseFilename(programFilePath);

            FileWriter.writeListingFile(instructions, programName);
            Path loadFilePath = FileWriter.writeLoadFile(instructions, programName);

            // Close any existing trace file and open a new one if trace logging is enabled.
            TraceLogger traceLogger = TraceLogger.getInstance();
            traceLogger.close();
            if (traceCheckbox.isSelected()) {
                traceLogger.setEnabled(true);
                traceLogger.open();
            }

            // Reset the state for each IPL so previous runs do not affect the new program.
            this.cpu = new CPU();
            cpu.setOutputManager(outputManager);
            wireConsoleIO();

            // Clear console I/O areas for the new program run.
            consolePrinterArea.setText("");
            consoleKeyboardField.setText("");

            int programStartAddress = cpu.loadProgramToMemory(loadFilePath);
            this.cpu.setProgramCounter(programStartAddress);

            registerTextFieldMap.get(Register.PC).setText(OutputManager.getPaddedOctalValue(programStartAddress));

            syncUIWithCPU();

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

    /**
     * Builds the right panel containing the messages/errors output text area.
     *
     * <p>Also initialises the {@link OutputManager} instance (which requires the text area
     * to already be constructed) and connects it to the CPU.
     *
     * @return the constructed right panel
     */
    private JPanel getRightPanel() {
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(""),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        rightPanel.setBackground(Color.getHSBColor(0.8f, 0.1f, 0.9f));

        // Messages/Errors output area.
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
        outputPanel.add(new JScrollPane(outputTextArea));

        // Initialize the OutputManager with the `outputTextArea` so that it can write messages to it.
        outputManager = new OutputManager(outputTextArea);
        cpu.setOutputManager(outputManager);

        rightPanel.add(outputPanel);

        // Console Keyboard input panel (for IN instruction, DEVID 0).
        JPanel keyboardPanel = new JPanel();
        keyboardPanel.setLayout(new BoxLayout(keyboardPanel, BoxLayout.Y_AXIS));
        keyboardPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Console Keyboard (IN)"),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));

        consoleKeyboardField = new JTextField(20);
        consoleKeyboardField.setFont(monospaceFont);
        keyboardPanel.add(consoleKeyboardField);
        keyboardPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, keyboardPanel.getPreferredSize().height));

        rightPanel.add(Box.createVerticalStrut(10));
        rightPanel.add(keyboardPanel);

        // Console Printer output panel (for OUT instruction, DEVID 1).
        JPanel printerPanel = new JPanel();
        printerPanel.setLayout(new BoxLayout(printerPanel, BoxLayout.Y_AXIS));
        printerPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Console Printer (OUT)"),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));

        consolePrinterArea = new JTextArea(3, 20);
        consolePrinterArea.setEditable(false);
        consolePrinterArea.setFont(monospaceFont);
        consolePrinterArea.setLineWrap(true);
        consolePrinterArea.setWrapStyleWord(true);
        printerPanel.add(new JScrollPane(consolePrinterArea));

        rightPanel.add(Box.createVerticalStrut(10));
        rightPanel.add(printerPanel);

        return rightPanel;
    }

    /**
     * Builds the cache content panel showing all 16 cache lines, a HIT/MISS indicator,
     * and the FIFO replacement pointer.
     *
     * <p>Each cache line is displayed as a single label row:
     * {@code Line# | Valid | Tag (octal) | Data (octal)}.
     * The most recently accessed line is highlighted with a bold font.
     *
     * @return the constructed cache panel
     */
    private JPanel getCachePanel() {
        JPanel cachePanel = new JPanel();
        cachePanel.setLayout(new BoxLayout(cachePanel, BoxLayout.Y_AXIS));
        cachePanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Cache Content"),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        cachePanel.setBackground(Color.getHSBColor(0.1f, 0.1f, 0.9f));

        // HIT/MISS indicator and FIFO pointer on the same row.
        JPanel statusRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        statusRow.setBackground(cachePanel.getBackground());

        cacheHitMissLabel = new JLabel("--");
        cacheHitMissLabel.setFont(monospaceBoldFont);
        statusRow.add(new JLabel("Last: "));
        statusRow.add(cacheHitMissLabel);

        cacheFifoLabel = new JLabel("FIFO -> 0");
        cacheFifoLabel.setFont(monospaceFont);
        statusRow.add(Box.createHorizontalStrut(10));
        statusRow.add(cacheFifoLabel);

        statusRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        statusRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, statusRow.getPreferredSize().height));
        cachePanel.add(statusRow);
        cachePanel.add(Box.createVerticalStrut(5));

        // Header row.
        JLabel header = new JLabel(" Ln | V |  Tag   |  Data  ");
        header.setFont(monospaceBoldFont);
        header.setAlignmentX(Component.LEFT_ALIGNMENT);
        header.setMaximumSize(new Dimension(Integer.MAX_VALUE, header.getPreferredSize().height));
        cachePanel.add(header);

        // 16 cache line rows.
        cacheLineLabels = new JLabel[Cache.NUM_LINES];
        for (int i = 0; i < Cache.NUM_LINES; i++) {
            JLabel lineLabel = new JLabel(formatCacheLine(i, false, 0, 0, false));
            lineLabel.setFont(monospaceFont);
            lineLabel.setOpaque(true);
            lineLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            lineLabel.setBackground(cachePanel.getBackground());
            cacheLineLabels[i] = lineLabel;
            cachePanel.add(lineLabel);
        }

        cachePanel.add(Box.createVerticalGlue());
        return cachePanel;
    }

    /**
     * Formats a single cache line for display in the cache panel.
     *
     * @param index       the line index (0–15)
     * @param valid       whether the line contains valid data
     * @param tag         the memory address (tag) stored in this line
     * @param data        the 16-bit data word
     * @param highlighted whether this line should be visually highlighted
     * @return the formatted string for this cache line
     */
    private String formatCacheLine(int index, boolean valid, int tag, int data, boolean highlighted) {
        if (!valid) {
            return String.format(" %2d | 0 | ------ | ------", index);
        }
        String tagOctal = OutputManager.getPaddedOctalValue(tag);
        String dataOctal = OutputManager.getPaddedOctalValue(data);
        String prefix = highlighted ? ">" : " ";
        return String.format("%s%2d | 1 | %s | %s", prefix, index, tagOctal, dataOctal);
    }

    /**
     * Refreshes the cache display panel to reflect the current cache state.
     * Called from {@link #syncUIWithCPU()} after every instruction.
     */
    private void refreshCacheDisplay() {
        Cache cache = cpu.getCache();
        Cache.CacheLine[] lines = cache.getLines();
        int lastAccessed = cache.getLastAccessedLine();

        for (int i = 0; i < Cache.NUM_LINES; i++) {
            boolean highlighted = (i == lastAccessed);
            cacheLineLabels[i].setText(formatCacheLine(
                    i, lines[i].isValid(), lines[i].getTag(), lines[i].getData(), highlighted));

            if (highlighted) {
                cacheLineLabels[i].setFont(monospaceBoldFont);
                cacheLineLabels[i].setBackground(new Color(220, 235, 255));
            } else {
                cacheLineLabels[i].setFont(monospaceFont);
                cacheLineLabels[i].setBackground(null);
            }
        }

        // Update HIT/MISS indicator.
        if (lastAccessed >= 0) {
            if (cache.isLastHit()) {
                cacheHitMissLabel.setText("CACHE HIT");
                cacheHitMissLabel.setForeground(new Color(0, 128, 0));
            } else {
                cacheHitMissLabel.setText("CACHE MISS");
                cacheHitMissLabel.setForeground(Color.RED);
            }
        } else {
            cacheHitMissLabel.setText("--");
            cacheHitMissLabel.setForeground(Color.BLACK);
        }

        // Update FIFO pointer display.
        cacheFifoLabel.setText("FIFO -> " + cache.getFifoPointer());
    }

    /**
     * Builds the octal input panel used for manually entering values to load into registers.
     *
     * <p>A document listener keeps {@link #octalInputValue} and the binary output field
     * in sync whenever the user types in this field.
     *
     * @return the constructed octal input panel
     */
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

            /** Updates the cached octal value and refreshes the binary output display. */
            void update() {
                octalInputValue = textField.getText();
                binaryOutputTextField.setText(EncoderStringUtil.getZeroPaddedBinaryString(textField.getText(), 16));
            }
        });


        return mainPanel;
    }

    /**
     * Builds the read-only binary output panel that displays the 16-bit binary equivalent
     * of the value currently typed in the octal input field.
     *
     * @return the constructed binary output panel
     */
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

    /**
     * Builds the General Purpose Register (GPR0–GPR3) display panel.
     *
     * <p>Each register row shows the current value and a Load button to write the
     * octal input value into that register.
     *
     * @return the constructed GPR panel
     */
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

    /**
     * Builds the Index Register (IX1–IX3) display panel.
     *
     * <p>Each register row shows the current value and a Load button to write the
     * octal input value into that register.
     *
     * @return the constructed IXR panel
     */
    private JPanel getIxrPanel() {
        JPanel ixrPanel = new JPanel();
        ixrPanel.setLayout(new BoxLayout(ixrPanel, BoxLayout.Y_AXIS));
        ixrPanel.setBorder(BorderFactory.createTitledBorder("Index Registers"));

        ixrPanel.add(getLabelledTextField(Register.IX1));
        ixrPanel.add(getLabelledTextField(Register.IX2));
        ixrPanel.add(getLabelledTextField(Register.IX3));

        return ixrPanel;
    }

    /**
     * Builds the special registers panel (PC, MAR, MBR, IR).
     *
     * <p>PC, MAR, and MBR have Load buttons. IR is read-only (no Load button) because
     * it is always set automatically by the CPU during instruction fetch.
     *
     * @return the constructed special registers panel
     */
    private JPanel getSpecialRegistersPanel() {
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createTitledBorder("Special Registers/Counters"));

        mainPanel.add(getLabelledTextField(Register.PC));
        mainPanel.add(getLabelledTextField(Register.MAR));
        mainPanel.add(getLabelledTextField(Register.MBR));
        mainPanel.add(getLabelledTextField(Register.IR, false));   // IR is read-only; no Load button.
        mainPanel.add(getLabelledTextField(Register.CC, false));  // CC is read-only; no Load button.
        mainPanel.add(getLabelledTextField(Register.MFR, false)); // MFR is read-only; no Load button.

        return mainPanel;
    }

    /**
     * Convenience overload that creates a labelled register row with a Load button.
     *
     * @param register the register to display
     * @return the constructed register row panel
     */
    private JPanel getLabelledTextField(Register register) {
        return getLabelledTextField(register, true);
    }

    /**
     * Creates a single register display row: a label, a read-only value field, and optionally
     * a Load button that writes the current octal input into the register.
     *
     * @param register   the register to display
     * @param withButton {@code true} to include a Load button; {@code false} for a read-only row
     * @return the constructed register row panel
     */
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

                    // PC and MAR are 12-bit registers, while the rest are 16 bits. So, perform appropriate range check.
                    final int bits;
                    if (register == Register.PC || register == Register.MAR) {
                        bits = 12;
                    } else {
                        bits = 16;
                    }

                    if (octalValue > Math.pow(2, bits) - 1) {
                        outputManager.writeError("Value " + octalValue + " (octal: " + Integer.toOctalString(octalValue) + ")" + " is out of range for the " + bits + "-bit register " + register + ".");
                        return;
                    }

                    loadOctalValueIntoRegister(register, octalInputValue);
                    outputManager.writeMessage("Loaded value " + OutputManager.getPaddedOctalValue(octalValue) + " into register " + register);

                    textField.setText(OutputManager.getPaddedOctalValue(octalValue));

                    // Also update the CPU's internal program counter when the PC register is loaded.
                    if (register == Register.PC) {
                        cpu.setProgramCounter(octalValue);
                    }
                } catch (NumberFormatException ex) {
                    outputManager.writeError("Invalid octal value: " + octalInputValue);
                }
            });
        }

        mainPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, mainPanel.getPreferredSize().height));

        return mainPanel;
    }
}
