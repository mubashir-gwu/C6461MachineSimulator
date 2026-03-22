package cpu;

import fileutil.FileReader;
import memory.Memory;
import memory.Register;
import memory.RegisterManager;
import opcode.OpcodeLookupTable;
import opcode.OpcodeType;
import outputmanager.OutputManager;

import trace.TraceLogger;

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
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;

            String[] tokens = trimmed.split("\\s+");
            if (tokens.length < 2) {
                outputManager.writeError("Malformed load file line (missing value): " + line);
                continue;
            }

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

            // Ignore DATA/HLT (opcode 0) and RFS (opcode 015) — neither can be a program entry point.
            if (opcode == 0 || opcode == 015) {
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
        final TraceLogger trace = TraceLogger.getInstance();
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
            trace.logFault(0b0100, "Illegal opcode " + Integer.toOctalString(opcode) + " at address " + Integer.toOctalString(programCounter));
            outputManager.writeError("Invalid mnemonic encountered at address: " + Integer.toOctalString(programCounter));
            return;
        }

        // Log the fetch phase.
        trace.logFetch(programCounter, value);

        // Update the register values to reflect the current fetch.
        registerManager.loadRegister(Register.MAR, programCounter);
        registerManager.loadRegister(Register.MBR, value);
        registerManager.loadRegister(Register.IR, value);

        boolean pcModified = switch (opcodeType) {
            case LOAD_STORE -> executeLoadStoreInstruction(value);
            case TRANSFER -> executeTransferInstruction(value);
            case ARITHMETIC -> executeArithmeticInstruction(value);
            case MULTIPLY_DIVIDE -> executeMultiplyDivideInstruction(value);
            case LOGICAL -> executeLogicalInstruction(value);
            case SHIFT_ROTATE -> executeShiftRotateInstruction(value);
            case IO -> executeIOInstruction(value);
            case MISC -> executeMiscInstruction(value);
        };

        if (!halted && !pcModified) {
            programCounter += 1;
        }

        registerManager.loadRegister(Register.PC, programCounter);

        // Log full register state after each instruction.
        trace.logRegisterState(registerManager);
        trace.incrementStep();
    }

    // ── Helper methods ──────────────────────────────────────────────────────────

    /**
     * Returns the {@link Register} enum constant for a GPR number (0–3).
     */
    private Register getGPR(int r) {
        return switch (r) {
            case 0 -> Register.GPR0;
            case 1 -> Register.GPR1;
            case 2 -> Register.GPR2;
            case 3 -> Register.GPR3;
            default -> null;
        };
    }

    /**
     * Returns the {@link Register} enum constant for an index register number (1–3).
     * Returns {@code null} if ix is 0 (no indexing).
     */
    private Register getIXR(int ix) {
        return switch (ix) {
            case 1 -> Register.IX1;
            case 2 -> Register.IX2;
            case 3 -> Register.IX3;
            default -> null;
        };
    }

    /**
     * Computes the effective address from the Load/Store format fields.
     * Handles index register offset and indirect addressing.
     *
     * @param address the 5-bit address field
     * @param ix      the index register number (0 = no indexing)
     * @param i       the indirect flag (1 = indirect)
     * @param skipIndexing if true, IX is not used for EA (e.g. LDX, STX)
     * @return the computed effective address, or -1 on error
     */
    private int computeEA(int address, int ix, int i, boolean skipIndexing) {
        final TraceLogger trace = TraceLogger.getInstance();
        int ea = address;

        if (!skipIndexing && ix > 0) {
            Register ixr = getIXR(ix);
            ea += registerManager.getRegisterValue(ixr);
        }

        if (i == 1) {
            try {
                int indirectAddr = ea;
                ea = memory.getMemoryAt(ea);
                trace.logMemoryAccess("READ", indirectAddr, ea);
            } catch (IndexOutOfBoundsException e) {
                outputManager.writeError(e.getMessage());
                return -1;
            }
        }

        String eaDesc = (i == 1 ? "indirect" : "direct") +
                (ix > 0 && !skipIndexing ? ", indexed by IX" + ix : ", no indexing");
        trace.logEA(ea, eaDesc);
        return ea;
    }

    /**
     * Interprets a 16-bit unsigned value as a signed 16-bit integer (two's complement).
     */
    private int toSigned16(int value) {
        return (short) (value & 0xFFFF);
    }

    // ── Instruction execution methods ────────────────────────────────────────────

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
     * @return {@code true} if the instruction modified PC directly, {@code false} otherwise
     */
    public boolean executeLoadStoreInstruction(int instructionValue) {
        final TraceLogger trace = TraceLogger.getInstance();
        final int opcode  = instructionValue >> 10;
        final int r       = (instructionValue >> 8) & 0b11;
        final int ix      = (instructionValue >> 6) & 0b11;
        final int i       = (instructionValue >> 5) & 0b1;
        final int address = instructionValue & 0b11111;

        final String mnemonic = OpcodeLookupTable.getMnemonic(opcode);
        trace.logDecode(mnemonic, opcode, r, ix, i, address);

        Register gpr = getGPR(r);
        Register ixr = getIXR(ix);
        boolean skipIndexing = mnemonic.equals("LDX") || mnemonic.equals("STX");

        int ea = computeEA(address, ix, i, skipIndexing);
        if (ea < 0) return false;

        registerManager.loadRegister(Register.MAR, ea);

        int value;
        switch (mnemonic) {
            case "LDR":
                try {
                    value = memory.getMemoryAt(ea);
                } catch (IndexOutOfBoundsException e) {
                    outputManager.writeError(e.getMessage());
                    return false;
                }
                trace.logMemoryAccess("READ", ea, value);
                trace.logExecute("LDR: R" + r + " <- Memory[" + ea + "(oct:" + Integer.toOctalString(ea) + ")] = " + value + "(oct:" + Integer.toOctalString(value) + ")");
                registerManager.loadRegister(Register.MBR, value);
                registerManager.loadRegister(gpr, value);
                break;
            case "STR":
                int strValue = registerManager.getRegisterValue(gpr);
                registerManager.loadRegister(Register.MBR, strValue);
                try {
                    memory.setMemoryAt(ea, strValue);
                } catch (IndexOutOfBoundsException e) {
                    outputManager.writeError(e.getMessage());
                    return false;
                }
                trace.logMemoryAccess("WRITE", ea, strValue);
                trace.logExecute("STR: Memory[" + ea + "(oct:" + Integer.toOctalString(ea) + ")] <- R" + r + " = " + strValue + "(oct:" + Integer.toOctalString(strValue) + ")");
                break;
            case "LDA":
                registerManager.loadRegister(gpr, ea);
                trace.logExecute("LDA: R" + r + " <- EA = " + ea + "(oct:" + Integer.toOctalString(ea) + ")");
                break;
            case "LDX":
                try {
                    value = memory.getMemoryAt(ea);
                } catch (IndexOutOfBoundsException e) {
                    outputManager.writeError(e.getMessage());
                    return false;
                }
                trace.logMemoryAccess("READ", ea, value);
                trace.logExecute("LDX: IX" + ix + " <- Memory[" + ea + "(oct:" + Integer.toOctalString(ea) + ")] = " + value + "(oct:" + Integer.toOctalString(value) + ")");
                registerManager.loadRegister(Register.MBR, value);
                registerManager.loadRegister(ixr, value);
                break;
            case "STX":
                int stxValue = registerManager.getRegisterValue(ixr);
                registerManager.loadRegister(Register.MBR, stxValue);
                try {
                    memory.setMemoryAt(ea, stxValue);
                } catch (IndexOutOfBoundsException e) {
                    outputManager.writeError(e.getMessage());
                    return false;
                }
                trace.logMemoryAccess("WRITE", ea, stxValue);
                trace.logExecute("STX: Memory[" + ea + "(oct:" + Integer.toOctalString(ea) + ")] <- IX" + ix + " = " + stxValue + "(oct:" + Integer.toOctalString(stxValue) + ")");
                break;
        }
        return false;
    }

    /**
     * Executes a control-transfer instruction (JZ, JNE, JCC, JMA, JSR, RFS, SOB, JGE).
     *
     * <p>All transfer instructions use the Load/Store format and always return {@code true}
     * because they manage the PC directly (either jumping or explicitly doing PC+1).
     *
     * @param instructionValue the raw 16-bit instruction word
     * @return {@code true} always — transfer instructions manage PC directly
     */
    public boolean executeTransferInstruction(int instructionValue) {
        final TraceLogger trace = TraceLogger.getInstance();
        final int opcode  = instructionValue >> 10;
        final int r       = (instructionValue >> 8) & 0b11;
        final int ix      = (instructionValue >> 6) & 0b11;
        final int i       = (instructionValue >> 5) & 0b1;
        final int address = instructionValue & 0b11111;

        final String mnemonic = OpcodeLookupTable.getMnemonic(opcode);
        trace.logDecode(mnemonic, opcode, r, ix, i, address);

        // RFS ignores IX and I fields — use address as immediate directly.
        int ea;
        if (mnemonic.equals("RFS")) {
            ea = address; // Immed value, no EA computation
            trace.logEA(ea, "immediate (IX and I ignored)");
        } else {
            ea = computeEA(address, ix, i, false);
            if (ea < 0) return true;
        }

        Register gpr = getGPR(r);
        int regVal = registerManager.getRegisterValue(gpr);
        int signedVal = toSigned16(regVal);

        switch (mnemonic) {
            case "JZ":
                if (signedVal == 0) {
                    programCounter = ea;
                    trace.logExecute("JZ: c(R" + r + ")=0, PC <- EA=" + ea);
                } else {
                    programCounter += 1;
                    trace.logExecute("JZ: c(R" + r + ")=" + regVal + " != 0, PC <- PC+1");
                }
                break;

            case "JNE":
                if (signedVal != 0) {
                    programCounter = ea;
                    trace.logExecute("JNE: c(R" + r + ")=" + regVal + " != 0, PC <- EA=" + ea);
                } else {
                    programCounter += 1;
                    trace.logExecute("JNE: c(R" + r + ")=0, PC <- PC+1");
                }
                break;

            case "JCC": {
                // The R field is used as the CC bit index (0-3) to test.
                int ccBit = r;
                int ccVal = registerManager.getRegisterValue(Register.CC);
                boolean bitSet = ((ccVal >> ccBit) & 1) == 1;
                if (bitSet) {
                    programCounter = ea;
                    trace.logExecute("JCC: CC bit " + ccBit + " is 1, PC <- EA=" + ea);
                } else {
                    programCounter += 1;
                    trace.logExecute("JCC: CC bit " + ccBit + " is 0, PC <- PC+1");
                }
                break;
            }

            case "JMA":
                // Unconditional jump. R field is ignored.
                programCounter = ea;
                trace.logExecute("JMA: PC <- EA=" + ea);
                break;

            case "JSR": {
                // Save return address in R3, jump to EA.
                int returnAddr = programCounter + 1;
                registerManager.loadRegister(Register.GPR3, returnAddr);
                programCounter = ea;
                trace.logExecute("JSR: R3 <- PC+1=" + returnAddr + ", PC <- EA=" + ea);
                break;
            }

            case "RFS":
                // Return from subroutine: R0 <- Immed, PC <- c(R3).
                registerManager.loadRegister(Register.GPR0, address);
                programCounter = registerManager.getRegisterValue(Register.GPR3);
                trace.logExecute("RFS: R0 <- " + address + ", PC <- c(R3)=" + programCounter);
                break;

            case "SOB": {
                // Subtract one and branch: r <- c(r) - 1; if c(r) > 0, PC <- EA.
                int newVal = (regVal - 1) & 0xFFFF;
                registerManager.loadRegister(gpr, newVal);
                int signedNew = toSigned16(newVal);
                if (signedNew > 0) {
                    programCounter = ea;
                    trace.logExecute("SOB: R" + r + " <- " + newVal + ", c(R" + r + ")>0, PC <- EA=" + ea);
                } else {
                    programCounter += 1;
                    trace.logExecute("SOB: R" + r + " <- " + newVal + ", c(R" + r + ")<=0, PC <- PC+1");
                }
                break;
            }

            case "JGE":
                // Jump if c(r) >= 0 (sign bit is 0).
                if (signedVal >= 0) {
                    programCounter = ea;
                    trace.logExecute("JGE: c(R" + r + ")=" + signedVal + " >= 0, PC <- EA=" + ea);
                } else {
                    programCounter += 1;
                    trace.logExecute("JGE: c(R" + r + ")=" + signedVal + " < 0, PC <- PC+1");
                }
                break;
        }

        return true; // All transfer instructions manage PC directly.
    }

    /**
     * Executes an arithmetic instruction (AMR, SMR, AIR, SIR).
     *
     * <p>AMR and SMR use EA computation to access memory operands.
     * AIR and SIR use the address field as an immediate value (IX and I are ignored).
     *
     * @param instructionValue the raw 16-bit instruction word
     * @return {@code true} if the instruction modified PC directly, {@code false} otherwise
     */
    public boolean executeArithmeticInstruction(int instructionValue) {
        final TraceLogger trace = TraceLogger.getInstance();
        final int opcode  = instructionValue >> 10;
        final int r       = (instructionValue >> 8) & 0b11;
        final int ix      = (instructionValue >> 6) & 0b11;
        final int i       = (instructionValue >> 5) & 0b1;
        final int address = instructionValue & 0b11111;

        final String mnemonic = OpcodeLookupTable.getMnemonic(opcode);
        trace.logDecode(mnemonic, opcode, r, ix, i, address);

        Register gpr = getGPR(r);
        int regVal = registerManager.getRegisterValue(gpr);

        switch (mnemonic) {
            case "AMR": {
                // r <- c(r) + c(EA)
                int ea = computeEA(address, ix, i, false);
                if (ea < 0) return false;
                int memVal;
                try {
                    memVal = memory.getMemoryAt(ea);
                } catch (IndexOutOfBoundsException e) {
                    outputManager.writeError(e.getMessage());
                    return false;
                }
                trace.logMemoryAccess("READ", ea, memVal);
                int result = toSigned16(regVal) + toSigned16(memVal);
                if (result > 32767) registerManager.setConditionCode(0, true);   // OVERFLOW
                if (result < -32768) registerManager.setConditionCode(1, true);  // UNDERFLOW
                int masked = result & 0xFFFF;
                registerManager.loadRegister(gpr, masked);
                trace.logExecute("AMR: R" + r + " <- " + regVal + " + " + memVal + " = " + masked);
                break;
            }

            case "SMR": {
                // r <- c(r) - c(EA)
                int ea = computeEA(address, ix, i, false);
                if (ea < 0) return false;
                int memVal;
                try {
                    memVal = memory.getMemoryAt(ea);
                } catch (IndexOutOfBoundsException e) {
                    outputManager.writeError(e.getMessage());
                    return false;
                }
                trace.logMemoryAccess("READ", ea, memVal);
                int result = toSigned16(regVal) - toSigned16(memVal);
                if (result > 32767) registerManager.setConditionCode(0, true);   // OVERFLOW
                if (result < -32768) registerManager.setConditionCode(1, true);  // UNDERFLOW
                int masked = result & 0xFFFF;
                registerManager.loadRegister(gpr, masked);
                trace.logExecute("SMR: R" + r + " <- " + regVal + " - " + memVal + " = " + masked);
                break;
            }

            case "AIR": {
                // r <- c(r) + Immed. IX and I are ignored.
                int immed = address;
                trace.logEA(immed, "immediate (IX and I ignored)");
                if (immed == 0) {
                    trace.logExecute("AIR: Immed=0, no operation");
                    break;
                }
                int result;
                if (regVal == 0) {
                    result = immed;
                } else {
                    result = toSigned16(regVal) + immed;
                    if (result > 32767) registerManager.setConditionCode(0, true);
                    if (result < -32768) registerManager.setConditionCode(1, true);
                }
                int masked = result & 0xFFFF;
                registerManager.loadRegister(gpr, masked);
                trace.logExecute("AIR: R" + r + " <- " + regVal + " + " + immed + " = " + masked);
                break;
            }

            case "SIR": {
                // r <- c(r) - Immed. IX and I are ignored.
                int immed = address;
                trace.logEA(immed, "immediate (IX and I ignored)");
                if (immed == 0) {
                    trace.logExecute("SIR: Immed=0, no operation");
                    break;
                }
                int result;
                if (regVal == 0) {
                    result = (-immed) & 0xFFFF; // -(Immed) in 16-bit two's complement
                } else {
                    result = toSigned16(regVal) - immed;
                    if (result > 32767) registerManager.setConditionCode(0, true);
                    if (result < -32768) registerManager.setConditionCode(1, true);
                }
                int masked = result & 0xFFFF;
                registerManager.loadRegister(gpr, masked);
                trace.logExecute("SIR: R" + r + " <- " + regVal + " - " + immed + " = " + masked);
                break;
            }
        }

        return false;
    }

    /**
     * Executes a multiply or divide instruction (MLT, DVD).
     *
     * <p>Register-to-register format: OPCODE(6)|RX(2)|RY(2)|UNUSED(6).
     * RX and RY must be 0 or 2. Results span two registers (rx and rx+1).
     *
     * @param instructionValue the raw 16-bit instruction word
     * @return {@code true} if the instruction modified PC directly, {@code false} otherwise
     */
    public boolean executeMultiplyDivideInstruction(int instructionValue) {
        final TraceLogger trace = TraceLogger.getInstance();
        final int opcode = instructionValue >> 10;
        final int rx     = (instructionValue >> 8) & 0b11;
        final int ry     = (instructionValue >> 6) & 0b11;

        final String mnemonic = OpcodeLookupTable.getMnemonic(opcode);
        trace.logDecodeRR(mnemonic, opcode, rx, ry);

        // Validate: rx and ry must be 0 or 2.
        if ((rx != 0 && rx != 2) || (ry != 0 && ry != 2)) {
            outputManager.writeError(mnemonic + ": rx and ry must be 0 or 2 (got rx=" + rx + ", ry=" + ry + ")");
            trace.logExecute(mnemonic + ": ILLEGAL — rx=" + rx + ", ry=" + ry + " (must be 0 or 2)");
            return false;
        }

        Register rxReg = getGPR(rx);
        Register rxPlus1Reg = getGPR(rx + 1);
        Register ryReg = getGPR(ry);

        int rxVal = toSigned16(registerManager.getRegisterValue(rxReg));
        int ryVal = toSigned16(registerManager.getRegisterValue(ryReg));

        switch (mnemonic) {
            case "MLT": {
                // rx, rx+1 <- c(rx) * c(ry). 32-bit result.
                int result32 = rxVal * ryVal;
                int high = (result32 >> 16) & 0xFFFF;
                int low = result32 & 0xFFFF;
                registerManager.loadRegister(rxReg, high);
                registerManager.loadRegister(rxPlus1Reg, low);
                // Set OVERFLOW if result doesn't fit in 16 bits.
                if (result32 > 32767 || result32 < -32768) {
                    registerManager.setConditionCode(0, true); // OVERFLOW
                }
                trace.logExecute("MLT: R" + rx + ",R" + (rx + 1) + " <- " + rxVal + " * " + ryVal +
                        " = " + result32 + " (high=" + high + ", low=" + low + ")");
                break;
            }

            case "DVD": {
                // rx <- c(rx)/c(ry), rx+1 <- remainder.
                if (ryVal == 0) {
                    registerManager.setConditionCode(2, true); // DIVZERO
                    trace.logExecute("DVD: division by zero, DIVZERO cc(2) set");
                    outputManager.writeError("DVD: division by zero");
                    break;
                }
                int quotient = rxVal / ryVal;
                int remainder = rxVal % ryVal;
                registerManager.loadRegister(rxReg, quotient & 0xFFFF);
                registerManager.loadRegister(rxPlus1Reg, remainder & 0xFFFF);
                trace.logExecute("DVD: R" + rx + " <- " + rxVal + " / " + ryVal + " = " + quotient +
                        ", R" + (rx + 1) + " <- remainder = " + remainder);
                break;
            }
        }

        return false;
    }

    /**
     * Executes a logical instruction (TRR, AND, ORR, NOT).
     *
     * <p>Register-to-register format: OPCODE(6)|RX(2)|RY(2)|UNUSED(6).
     *
     * @param instructionValue the raw 16-bit instruction word
     * @return {@code true} if the instruction modified PC directly, {@code false} otherwise
     */
    public boolean executeLogicalInstruction(int instructionValue) {
        final TraceLogger trace = TraceLogger.getInstance();
        final int opcode = instructionValue >> 10;
        final int rx     = (instructionValue >> 8) & 0b11;
        final int ry     = (instructionValue >> 6) & 0b11;

        final String mnemonic = OpcodeLookupTable.getMnemonic(opcode);
        trace.logDecodeRR(mnemonic, opcode, rx, ry);

        Register rxReg = getGPR(rx);
        Register ryReg = getGPR(ry);

        int rxVal = registerManager.getRegisterValue(rxReg);
        int ryVal = registerManager.getRegisterValue(ryReg);

        switch (mnemonic) {
            case "TRR":
                // Test equality: if c(rx) == c(ry), set cc(3)=1; else cc(3)=0.
                if (rxVal == ryVal) {
                    registerManager.setConditionCode(3, true);  // EQUALORNOT
                    trace.logExecute("TRR: R" + rx + "=" + rxVal + " == R" + ry + "=" + ryVal + ", cc(3) <- 1");
                } else {
                    registerManager.setConditionCode(3, false);
                    trace.logExecute("TRR: R" + rx + "=" + rxVal + " != R" + ry + "=" + ryVal + ", cc(3) <- 0");
                }
                break;

            case "AND": {
                // c(rx) <- c(rx) AND c(ry)
                int result = (rxVal & ryVal) & 0xFFFF;
                registerManager.loadRegister(rxReg, result);
                trace.logExecute("AND: R" + rx + " <- " + rxVal + " AND " + ryVal + " = " + result);
                break;
            }

            case "ORR": {
                // c(rx) <- c(rx) OR c(ry)
                int result = (rxVal | ryVal) & 0xFFFF;
                registerManager.loadRegister(rxReg, result);
                trace.logExecute("ORR: R" + rx + " <- " + rxVal + " OR " + ryVal + " = " + result);
                break;
            }

            case "NOT": {
                // c(rx) <- NOT c(rx). RY is ignored.
                int result = (~rxVal) & 0xFFFF;
                registerManager.loadRegister(rxReg, result);
                trace.logExecute("NOT: R" + rx + " <- NOT " + rxVal + " = " + result);
                break;
            }
        }

        return false;
    }

    /**
     * Executes a shift or rotate instruction (SRC, RRC).
     *
     * <p>Format: OPCODE(6)|R(2)|A/L(1)|L/R(1)|XX(2)|COUNT(4).
     * <ul>
     *   <li>A/L: 0 = arithmetic, 1 = logical</li>
     *   <li>L/R: 1 = left, 0 = right</li>
     *   <li>COUNT: 0–15 (0 = no shift/rotate)</li>
     * </ul>
     *
     * @param instructionValue the raw 16-bit instruction word
     * @return {@code true} if the instruction modified PC directly, {@code false} otherwise
     */
    public boolean executeShiftRotateInstruction(int instructionValue) {
        final TraceLogger trace = TraceLogger.getInstance();
        final int opcode = instructionValue >> 10;
        final int r      = (instructionValue >> 8) & 0b11;
        final int al     = (instructionValue >> 7) & 1;       // 0=Arithmetic, 1=Logical
        final int lr     = (instructionValue >> 6) & 1;       // 1=Left, 0=Right
        final int count  = instructionValue & 0b1111;

        final String mnemonic = OpcodeLookupTable.getMnemonic(opcode);
        trace.logDecodeSR(mnemonic, opcode, r, al, lr, count);

        if (count == 0) {
            trace.logExecute(mnemonic + ": count=0, no operation");
            return false;
        }

        Register gpr = getGPR(r);
        int val = registerManager.getRegisterValue(gpr) & 0xFFFF;
        int result;

        switch (mnemonic) {
            case "SRC":
                if (al == 0) {
                    // Arithmetic shift — sign bit is NOT shifted.
                    if (lr == 0) {
                        // Arithmetic right shift: sign bit stays, fills with sign bit value.
                        int bitsShiftedOut = val & ((1 << count) - 1);
                        int signed16 = toSigned16(val);
                        result = (signed16 >> count) & 0xFFFF;
                        if (bitsShiftedOut != 0) {
                            registerManager.setConditionCode(1, true); // UNDERFLOW
                        }
                        trace.logExecute("SRC: R" + r + " arithmetic right shift by " + count +
                                ": " + val + " -> " + result);
                    } else {
                        // Arithmetic left shift: sign bit stays in place, shift bits [14:0] left.
                        int signBit = val & 0x8000;
                        int lower15 = val & 0x7FFF;
                        // Check if non-zero bits will be shifted out from bit 14.
                        int bitsShiftedOut = lower15 >> (15 - count);
                        lower15 = (lower15 << count) & 0x7FFF;
                        result = signBit | lower15;
                        if (bitsShiftedOut != 0) {
                            registerManager.setConditionCode(0, true); // OVERFLOW
                        }
                        trace.logExecute("SRC: R" + r + " arithmetic left shift by " + count +
                                ": " + val + " -> " + result);
                    }
                } else {
                    // Logical shift.
                    if (lr == 0) {
                        // Logical right shift: fill with 0 from left.
                        result = (val >> count) & 0xFFFF;
                        trace.logExecute("SRC: R" + r + " logical right shift by " + count +
                                ": " + val + " -> " + result);
                    } else {
                        // Logical left shift: fill with 0 from right.
                        result = (val << count) & 0xFFFF;
                        trace.logExecute("SRC: R" + r + " logical left shift by " + count +
                                ": " + val + " -> " + result);
                    }
                }
                registerManager.loadRegister(gpr, result);
                break;

            case "RRC":
                // Rotate — logical only.
                if (lr == 1) {
                    // Rotate left: bits shifted out the left come back on the right.
                    result = ((val << count) | (val >> (16 - count))) & 0xFFFF;
                    trace.logExecute("RRC: R" + r + " rotate left by " + count +
                            ": " + val + " -> " + result);
                } else {
                    // Rotate right: bits shifted out the right come back on the left.
                    result = ((val >> count) | (val << (16 - count))) & 0xFFFF;
                    trace.logExecute("RRC: R" + r + " rotate right by " + count +
                            ": " + val + " -> " + result);
                }
                registerManager.loadRegister(gpr, result);
                break;
        }

        return false;
    }

    /**
     * Executes an I/O instruction (IN, OUT, CHK).
     * Not yet implemented for Part I.
     *
     * @param instructionValue the raw 16-bit instruction word
     * @return {@code true} if the instruction modified PC directly, {@code false} otherwise
     */
    public boolean executeIOInstruction(int instructionValue) {
        return false;
    }

    /**
     * Executes a miscellaneous instruction (HLT, TRAP).
     *
     * <p>Currently handles only {@code HLT}, which sets the {@link #halted} flag to stop
     * the execution loop.
     *
     * @param instructionValue the raw 16-bit instruction word
     * @return {@code true} if the instruction modified PC directly, {@code false} otherwise
     */
    public boolean executeMiscInstruction(int instructionValue) {
        final TraceLogger trace = TraceLogger.getInstance();
        final int opcode = instructionValue >> 10;
        final String opcodeMnemonic = OpcodeLookupTable.getMnemonic(opcode);

        trace.log("DECODE  | Mnemonic=" + opcodeMnemonic);

        if (opcodeMnemonic.equals("HLT")) {
            halted = true;
            trace.logHalt();
        }
        return false;
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
