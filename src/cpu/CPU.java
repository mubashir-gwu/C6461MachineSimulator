package cpu;

import fileutil.FileReader;
import memory.Memory;
import memory.Register;
import memory.RegisterManager;
import opcode.OpcodeLookupTable;
import opcode.OpcodeType;
import outputmanager.OutputManager;

import java.nio.file.Path;
import java.util.List;

/**
 * Simulates the C6461 CPU: fetches, decodes, and executes machine instructions.
 *
 * <p>The CPU operates on a {@link Memory} object and a {@link RegisterManager} that holds
 * the state of all registers. Execution follows the standard fetch-decode-execute cycle:
 * <ol>
 *   <li>Fetch the word at the current program counter (PC) from memory.</li>
 *   <li>Decode the opcode (bits [15:10]) and look up its {@link OpcodeType}.</li>
 *   <li>Dispatch to the appropriate execute method based on the type.</li>
 *   <li>Increment the PC (unless execution was halted by the instruction).</li>
 * </ol>
 *
 * <p>Programs are loaded into memory from a pre-assembled {@code .load} file via
 * {@link #loadProgramToMemory(Path)}.
 */
public class CPU {
    /** The current program counter, pointing to the address of the next instruction to execute. */
    private int programCounter = 0;

    /** The machine's main memory (2048 words). */
    private final Memory memory;

    /** Writes messages and errors to the simulator UI output panel. */
    private OutputManager outputManager;

    /** Holds the current values of all CPU registers (GPRs, index registers, PC, MAR, MBR, IR). */
    private RegisterManager registerManager;

    /** Set to {@code true} when a HLT instruction is encountered or the user clicks Halt. */
    private boolean halted = false;

    /**
     * Constructs a new CPU with freshly initialised memory and registers.
     */
    public CPU() {
        this.memory = new Memory();
        this.registerManager = new RegisterManager();
    }

    /**
     * Sets the output manager used to display messages and errors in the simulator UI.
     *
     * @param outputManager the output manager to use
     */
    public void setOutputManager(OutputManager outputManager) {
        this.outputManager = outputManager;
    }

    /**
     * Sets the program counter to the given address and updates the PC register accordingly.
     *
     * @param programCounter the new program counter value (decimal integer)
     */
    public void setProgramCounter(int programCounter) {
        this.programCounter = programCounter;
        registerManager.loadRegister(Register.PC, programCounter);
    }

    /**
     * Returns the machine's main memory, allowing the UI to perform manual Load/Store operations.
     *
     * @return the {@link Memory} instance used by this CPU
     */
    public Memory getMemory() {
        return memory;
    }

    /**
     * Returns the register manager, allowing the UI to read and write individual register values.
     *
     * @return the {@link RegisterManager} instance used by this CPU
     */
    public RegisterManager getRegisterManager() {
        return registerManager;
    }

    /**
     * Loads a pre-assembled program from a {@code .load} file into memory.
     *
     * <p>Each line of the load file contains an octal address and an octal machine word,
     * separated by whitespace. The method scans the loaded words to identify the first
     * non-data instruction (opcode &ne; 0) and returns its address as the program start.
     *
     * @param path the path to the {@code .load} file
     * @return the address of the first non-data instruction, or {@code -1} on error
     */
    public int loadProgramToMemory(Path path) {
        List<String> lines = FileReader.readFile(path);
        int programStartAddress = -1;

        for (String line : lines) {
            String[] tokens = line.split("\\s+");

            int location = Integer.parseInt(tokens[0], 8);
            int value = Integer.parseInt(tokens[1], 8);

            try {
                memory.setMemoryAt(location, value);
            } catch (IndexOutOfBoundsException e) {
                outputManager.writeError(e.getMessage());
                return -1;
            }

            // Keep only the first 6 bits as that's where the opcode is stored.
            final int opcode = value >> 10;

            // Ignore the `DATA` and `HLT` lines (opcode 0 covers both HLT and raw data words).
            if (opcode == 0) {
                continue;
            }

            if (programStartAddress == -1) {
                programStartAddress = location;
            }
        }

        return programStartAddress;
    }

    /**
     * Runs the program to completion by repeatedly executing instructions until halted.
     *
     * <p>Execution stops when the {@code halted} flag is set (by a HLT instruction or
     * an explicit call to {@link #setHalted(boolean)}).
     */
    public void executeAllInstructions() {
        while (!halted) {
            executeNextInstruction();
        }
    }

    /**
     * Fetches, decodes, and executes the single instruction at the current program counter.
     *
     * <p>After execution the PC is incremented by 1 (unless the CPU was halted by the
     * instruction itself). The MAR, MBR, and IR registers are updated to reflect the
     * current memory access and instruction word.
     */
    public void executeNextInstruction() {
        final int value;

        try {
            value = memory.getMemoryAt(programCounter);
        } catch (IndexOutOfBoundsException e) {
            outputManager.writeError(e.getMessage());
            return;
        }

        // Extract the opcode from bits [15:10] of the instruction word.
        final int opcode = value >> 10;
        final OpcodeType opcodeType = OpcodeLookupTable.getOpcodeType(opcode);

        if (opcodeType == null) {
            outputManager.writeError("Invalid mnemonic encountered at address: " + Integer.toOctalString(programCounter));
            return;
        }

        // Update the register values to reflect the current fetch.
        registerManager.loadRegister(Register.MAR, programCounter);
        registerManager.loadRegister(Register.MBR, value);
        registerManager.loadRegister(Register.IR, value);

        switch (opcodeType) {
            case LOAD_STORE:
                executeLoadStoreInstruction(value);
                break;
            case TRANSFER:
                executeTransferInstruction(value);
                break;
            case ARITHMETIC:
                executeArithmeticInstruction(value);
                break;
            case MULTIPLY_DIVIDE:
                executeMultiplyDivideInstruction(value);
                break;
            case LOGICAL:
                executeLogicalInstruction(value);
                break;
            case SHIFT_ROTATE:
                executeShiftRotateInstruction(value);
                break;
            case IO:
                executeIOInstruction(value);
                break;
            case MISC:
                executeMiscInstruction(value);
                break;
        }

        if (!halted) {
            programCounter += 1;
        }

        registerManager.loadRegister(Register.PC, programCounter);
    }

    /**
     * Executes a load/store instruction (LDR, STR, LDA, LDX, STX).
     *
     * <p>The effective address (EA) is computed from the instruction's address field,
     * optionally offset by an index register, and optionally indirected through memory:
     * <pre>
     *   EA = address + (IX != 0 ? IX_register : 0)
     *   if I == 1: EA = Memory[EA]   // indirect addressing
     * </pre>
     *
     * <p>Instruction word bit fields decoded:
     * <ul>
     *   <li>bits [15:10] – opcode</li>
     *   <li>bits [ 9: 8] – R  (general-purpose register number)</li>
     *   <li>bits [ 7: 6] – IX (index register number; 0 = no indexing)</li>
     *   <li>bit  [    5] – I  (0 = direct, 1 = indirect)</li>
     *   <li>bits [ 4: 0] – address</li>
     * </ul>
     *
     * @param instructionValue the raw 16-bit instruction word
     */
    public void executeLoadStoreInstruction(int instructionValue) {
        final int opcode  = instructionValue >> 10;
        final int r       = (instructionValue >> 8) & 0b11;   // GPR number (bits 9:8)
        final int ix      = (instructionValue >> 6) & 0b11;   // Index register number (bits 7:6)
        final int i       = (instructionValue >> 5) & 0b1;    // Indirect flag (bit 5)
        final int address = instructionValue & 0b11111;        // Address field (bits 4:0)

        final String opcodeMnemonic = OpcodeLookupTable.getMnemonic(opcode);

        Register selectedIndexRegister = switch (ix) {
            case 1 -> Register.IX1;
            case 2 -> Register.IX2;
            case 3 -> Register.IX3;
            default -> null;
        };

        Register selectedGeneralRegister = switch (r) {
            case 0 -> Register.GPR0;
            case 1 -> Register.GPR1;
            case 2 -> Register.GPR2;
            case 3 -> Register.GPR3;
            default -> null;
        };

        int ea = address;

        // IX field is the target register for `LDX` and `STX`, so do not use it for EA calculation.
        if (!opcodeMnemonic.equals("LDX") && !opcodeMnemonic.equals("STX") && ix > 0) {
            ea += registerManager.getRegisterValue(selectedIndexRegister);
        }

        if (i == 1) {
            // Indirect memory addressing: replace EA with the word stored at EA.
            try {
                ea = memory.getMemoryAt(ea);
            } catch (IndexOutOfBoundsException e) {
                outputManager.writeError(e.getMessage());
                return;
            }
        }

        registerManager.loadRegister(Register.MAR, ea);

        int value;
        switch (opcodeMnemonic) {
            case "LDR":
                // Load register R from memory at EA.
                try {
                    value = memory.getMemoryAt(ea);
                } catch (IndexOutOfBoundsException e) {
                    outputManager.writeError(e.getMessage());
                    return;
                }

                registerManager.loadRegister(Register.MBR, value);
                registerManager.loadRegister(selectedGeneralRegister, value);
                break;
            case "STR":
                // Store register R into memory at EA.
                registerManager.loadRegister(Register.MBR, registerManager.getRegisterValue(selectedGeneralRegister));
                try {
                    memory.setMemoryAt(ea, registerManager.getRegisterValue(selectedGeneralRegister));
                } catch (IndexOutOfBoundsException e) {
                    outputManager.writeError(e.getMessage());
                    return;
                }
                break;
            case "LDA":
                // Load the effective address itself into register R (not the value at EA).
                registerManager.loadRegister(selectedGeneralRegister, ea);
                break;
            case "LDX":
                // Load index register IX from memory at EA.
                try {
                    value = memory.getMemoryAt(ea);
                } catch (IndexOutOfBoundsException e) {
                    outputManager.writeError(e.getMessage());
                    return;
                }

                registerManager.loadRegister(Register.MBR, value);
                registerManager.loadRegister(selectedIndexRegister, value);
                break;
            case "STX":
                // Store index register IX into memory at EA.
                registerManager.loadRegister(Register.MBR, registerManager.getRegisterValue(selectedIndexRegister));
                try {
                    memory.setMemoryAt(ea, registerManager.getRegisterValue(selectedIndexRegister));
                } catch (IndexOutOfBoundsException e) {
                    outputManager.writeError(e.getMessage());
                    return;
                }
                break;
        }

        System.out.println("Executed: " + opcodeMnemonic + " " + r + "," + ix + "," + address + "," + i);
    }

    /**
     * Executes a control-transfer instruction (JZ, JNE, JCC, JMA, JSR, RFS, SOB, JGE).
     * Not yet implemented for Part I.
     *
     * @param instructionValue the raw 16-bit instruction word
     */
    public void executeTransferInstruction(int instructionValue) {
    }

    /**
     * Executes an arithmetic instruction (AMR, SMR, AIR, SIR).
     * Not yet implemented for Part I.
     *
     * @param instructionValue the raw 16-bit instruction word
     */
    public void executeArithmeticInstruction(int instructionValue) {
    }

    /**
     * Executes a multiply or divide instruction (MLT, DVD).
     * Not yet implemented for Part I.
     *
     * @param instructionValue the raw 16-bit instruction word
     */
    public void executeMultiplyDivideInstruction(int instructionValue) {
    }

    /**
     * Executes a logical instruction (TRR, AND, ORR, NOT).
     * Not yet implemented for Part I.
     *
     * @param instructionValue the raw 16-bit instruction word
     */
    public void executeLogicalInstruction(int instructionValue) {
    }

    /**
     * Executes a shift or rotate instruction (SRC, RRC).
     * Not yet implemented for Part I.
     *
     * @param instructionValue the raw 16-bit instruction word
     */
    public void executeShiftRotateInstruction(int instructionValue) {
    }

    /**
     * Executes an I/O instruction (IN, OUT, CHK).
     * Not yet implemented for Part I.
     *
     * @param instructionValue the raw 16-bit instruction word
     */
    public void executeIOInstruction(int instructionValue) {
    }

    /**
     * Executes a miscellaneous instruction (HLT, TRAP).
     *
     * <p>Currently handles only {@code HLT}, which sets the {@link #halted} flag to stop
     * the execution loop.
     *
     * @param instructionValue the raw 16-bit instruction word
     */
    public void executeMiscInstruction(int instructionValue) {
        final int opcode = instructionValue >> 10;
        final String opcodeMnemonic = OpcodeLookupTable.getMnemonic(opcode);

        if (opcodeMnemonic.equals("HLT")) {
            halted = true;
        }
    }

    /**
     * Returns whether the CPU is currently in the halted state.
     *
     * @return {@code true} if execution has been halted, {@code false} otherwise
     */
    public boolean isHalted() {
        return halted;
    }

    /**
     * Manually sets the halted state of the CPU.
     *
     * <p>Used by the UI's Halt button to stop execution without executing a HLT instruction.
     *
     * @param halted {@code true} to halt, {@code false} to allow execution to continue
     */
    public void setHalted(boolean halted) {
        this.halted = halted;
    }
}
