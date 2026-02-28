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

public class CPU {
    private int programCounter = 0;
    private final Memory memory;
    private OutputManager outputManager;
    private RegisterManager registerManager;
    private boolean halted = false;

    public CPU() {
        this.memory = new Memory();
        this.registerManager = new RegisterManager();
    }

    public void setOutputManager(OutputManager outputManager) {
        this.outputManager = outputManager;
    }

    public void setProgramCounter(int programCounter) {
        this.programCounter = programCounter;
        registerManager.loadRegister(Register.PC, programCounter);
    }

    public Memory getMemory() {
        return memory;
    }

    public RegisterManager getRegisterManager() {
        return registerManager;
    }

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

            // Ignore the `DATA` and `HLT` lines.
            if (opcode == 0) {
                continue;
            }

            if (programStartAddress == -1) {
                programStartAddress = location;
            }
        }

        return programStartAddress;
    }

    public void executeAllInstructions() {
        while (!halted) {
            executeNextInstruction();
        }
    }

    public void executeNextInstruction() {
        final int value;

        try {
            value = memory.getMemoryAt(programCounter);
        } catch (IndexOutOfBoundsException e) {
            outputManager.writeError(e.getMessage());
            return;
        }

        final int opcode = value >> 10;
        final OpcodeType opcodeType = OpcodeLookupTable.getOpcodeType(opcode);

        if (opcodeType == null) {
            outputManager.writeError("Invalid mnemonic encountered at address: " + Integer.toOctalString(programCounter));
            return;
        }

        // Update the register values.
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

    public void executeLoadStoreInstruction(int instructionValue) {
        final int opcode = instructionValue >> 10;
        final int r = (instructionValue >> 8) & 0b11;
        final int ix = (instructionValue >> 6) & 0b11;
        final int i = (instructionValue >> 5) & 0b1;
        final int address = instructionValue & 0b11111;

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

        // IX field is the target register for `LDX` and `STX` so no indexing for EA.
        if (!opcodeMnemonic.equals("LDX") && !opcodeMnemonic.equals("STX") && ix > 0) {
            ea += registerManager.getRegisterValue(selectedIndexRegister);
        }

        if (i == 1) {
            // Indirect memory addressing;
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
                registerManager.loadRegister(Register.MBR, registerManager.getRegisterValue(selectedGeneralRegister));
                try {
                    memory.setMemoryAt(ea, registerManager.getRegisterValue(selectedGeneralRegister));
                } catch (IndexOutOfBoundsException e) {
                    outputManager.writeError(e.getMessage());
                    return;
                }
                break;
            case "LDA":
                registerManager.loadRegister(selectedGeneralRegister, ea);
                break;
            case "LDX":
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

    public void executeTransferInstruction(int instructionValue) {
    }

    public void executeArithmeticInstruction(int instructionValue) {
    }

    public void executeMultiplyDivideInstruction(int instructionValue) {
    }

    public void executeLogicalInstruction(int instructionValue) {
    }

    public void executeShiftRotateInstruction(int instructionValue) {
    }

    public void executeIOInstruction(int instructionValue) {
    }

    public void executeMiscInstruction(int instructionValue) {
        final int opcode = instructionValue >> 10;
        final String opcodeMnemonic = OpcodeLookupTable.getMnemonic(opcode);

        if (opcodeMnemonic.equals("HLT")) {
            halted = true;
        }
    }

    public boolean isHalted() {
        return halted;
    }

    public void setHalted(boolean halted) {
        this.halted = halted;
    }
}
