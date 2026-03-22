package trace;

import memory.Register;
import memory.RegisterManager;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Singleton trace logger that records every simulator action to a timestamped log file.
 *
 * <p>The trace file is human-readable and self-contained: someone reading only the trace
 * file should be able to understand exactly what the program did, step by step.
 *
 * <p>Each log line is prefixed with a step counter (e.g., {@code [STEP 0042]}) that
 * increments with each instruction executed.
 */
public class TraceLogger {
    private static TraceLogger instance;

    private PrintWriter writer;
    private int stepCounter;
    private boolean enabled;

    private TraceLogger() {
        this.stepCounter = 0;
        this.enabled = false;
    }

    /**
     * Returns the singleton instance of the TraceLogger.
     *
     * @return the TraceLogger instance
     */
    public static TraceLogger getInstance() {
        if (instance == null) {
            instance = new TraceLogger();
        }
        return instance;
    }

    /**
     * Returns whether trace logging is currently enabled.
     *
     * @return {@code true} if logging is enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Enables or disables trace logging.
     *
     * @param enabled {@code true} to enable, {@code false} to disable
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Opens a new trace log file with a timestamp in the filename.
     * Resets the step counter to 0.
     */
    public void open() {
        close(); // Close any previously open file.
        stepCounter = 0;

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        Path logsDir = Paths.get("logs");

        try {
            Files.createDirectories(logsDir);
            String filename = logsDir.resolve("trace_" + timestamp + ".log").toString();
            writer = new PrintWriter(new java.io.FileWriter(filename), true);
            writer.println("=== C6461 Machine Simulator Trace Log ===");
            writer.println("=== Started: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + " ===");
            writer.println();
        } catch (IOException e) {
            System.err.println("TraceLogger: Could not open trace file: " + e.getMessage());
            writer = null;
        }
    }

    /**
     * Writes a general log entry.
     *
     * @param message the message to log
     */
    public void log(String message) {
        if (!enabled || writer == null) return;
        writer.printf("[STEP %04d] %s%n", stepCounter, message);
    }

    /**
     * Logs an instruction fetch event.
     *
     * @param pc          the program counter value
     * @param instruction the raw 16-bit instruction word fetched from memory
     */
    public void logFetch(int pc, int instruction) {
        if (!enabled || writer == null) return;
        writer.printf("[STEP %04d] FETCH   | PC=%s IR=%s%n",
                stepCounter, padOctal(pc), padOctal(instruction));
    }

    /**
     * Logs instruction decode for Load/Store format instructions.
     *
     * @param mnemonic the instruction mnemonic
     * @param opcode   the numeric opcode value
     * @param r        the general register field
     * @param ix       the index register field
     * @param i        the indirect bit
     * @param address  the address field
     */
    public void logDecode(String mnemonic, int opcode, int r, int ix, int i, int address) {
        if (!enabled || writer == null) return;
        writer.printf("[STEP %04d] DECODE  | Mnemonic=%s R=%d IX=%d I=%d ADDR=%d(oct:%s)%n",
                stepCounter, mnemonic, r, ix, i, address, padOctal(address));
    }

    /**
     * Logs instruction decode for Register-Register format instructions (MLT, DVD, TRR, AND, ORR, NOT).
     *
     * @param mnemonic the instruction mnemonic
     * @param opcode   the numeric opcode value
     * @param rx       the first register field
     * @param ry       the second register field
     */
    public void logDecodeRR(String mnemonic, int opcode, int rx, int ry) {
        if (!enabled || writer == null) return;
        writer.printf("[STEP %04d] DECODE  | Mnemonic=%s RX=%d RY=%d%n",
                stepCounter, mnemonic, rx, ry);
    }

    /**
     * Logs instruction decode for Shift/Rotate format instructions.
     *
     * @param mnemonic the instruction mnemonic
     * @param opcode   the numeric opcode value
     * @param r        the register field
     * @param al       arithmetic/logical flag (0=arithmetic, 1=logical)
     * @param lr       left/right flag (1=left, 0=right)
     * @param count    the shift/rotate count
     */
    public void logDecodeSR(String mnemonic, int opcode, int r, int al, int lr, int count) {
        if (!enabled || writer == null) return;
        writer.printf("[STEP %04d] DECODE  | Mnemonic=%s R=%d A/L=%d L/R=%d COUNT=%d%n",
                stepCounter, mnemonic, r, al, lr, count);
    }

    /**
     * Logs instruction decode for I/O format instructions.
     *
     * @param mnemonic the instruction mnemonic
     * @param opcode   the numeric opcode value
     * @param r        the register field
     * @param devid    the device ID
     */
    public void logDecodeIO(String mnemonic, int opcode, int r, int devid) {
        if (!enabled || writer == null) return;
        writer.printf("[STEP %04d] DECODE  | Mnemonic=%s R=%d DEVID=%d%n",
                stepCounter, mnemonic, r, devid);
    }

    /**
     * Logs effective address computation.
     *
     * @param ea          the computed effective address
     * @param description how the EA was derived (e.g., "direct, no indexing" or "indexed by IX1")
     */
    public void logEA(int ea, String description) {
        if (!enabled || writer == null) return;
        writer.printf("[STEP %04d] EA      | EA=%d(oct:%s) (%s)%n", stepCounter, ea, padOctal(ea), description);
    }

    /**
     * Logs what an instruction did during execution.
     *
     * @param description a human-readable description of the instruction's effect
     */
    public void logExecute(String description) {
        if (!enabled || writer == null) return;
        writer.printf("[STEP %04d] EXECUTE | %s%n", stepCounter, description);
    }

    /**
     * Logs the full state of all registers after an instruction executes.
     *
     * @param regs the register manager holding current register values
     */
    public void logRegisterState(RegisterManager regs) {
        if (!enabled || writer == null) return;
        writer.printf("[STEP %04d] REGS    | R0=%d(oct:%s) R1=%d(oct:%s) R2=%d(oct:%s) R3=%d(oct:%s)%n",
                stepCounter,
                regs.getRegisterValue(Register.GPR0), padOctal(regs.getRegisterValue(Register.GPR0)),
                regs.getRegisterValue(Register.GPR1), padOctal(regs.getRegisterValue(Register.GPR1)),
                regs.getRegisterValue(Register.GPR2), padOctal(regs.getRegisterValue(Register.GPR2)),
                regs.getRegisterValue(Register.GPR3), padOctal(regs.getRegisterValue(Register.GPR3)));
        writer.printf("                    | IX1=%d(oct:%s) IX2=%d(oct:%s) IX3=%d(oct:%s)%n",
                regs.getRegisterValue(Register.IX1), padOctal(regs.getRegisterValue(Register.IX1)),
                regs.getRegisterValue(Register.IX2), padOctal(regs.getRegisterValue(Register.IX2)),
                regs.getRegisterValue(Register.IX3), padOctal(regs.getRegisterValue(Register.IX3)));
        writer.printf("                    | PC=%d(oct:%s) MAR=%d(oct:%s) MBR=%d(oct:%s) IR=oct:%s%n",
                regs.getRegisterValue(Register.PC), padOctal(regs.getRegisterValue(Register.PC)),
                regs.getRegisterValue(Register.MAR), padOctal(regs.getRegisterValue(Register.MAR)),
                regs.getRegisterValue(Register.MBR), padOctal(regs.getRegisterValue(Register.MBR)),
                padOctal(regs.getRegisterValue(Register.IR)));
    }

    /**
     * Logs a memory access (read or write).
     *
     * @param type    "READ" or "WRITE"
     * @param address the memory address accessed
     * @param value   the value read or written
     */
    public void logMemoryAccess(String type, int address, int value) {
        if (!enabled || writer == null) return;
        writer.printf("[STEP %04d] MEM     | %-5s addr=%s value=%s%n",
                stepCounter, type, padOctal(address), padOctal(value));
    }

    /**
     * Logs a cache event (hit or miss).
     *
     * @param type    "HIT" or "MISS"
     * @param address the memory address involved
     */
    public void logCacheEvent(String type, int address) {
        if (!enabled || writer == null) return;
        writer.printf("[STEP %04d] CACHE   | %s addr=%s%n",
                stepCounter, type, padOctal(address));
    }

    /**
     * Logs an I/O operation.
     *
     * @param direction "IN" or "OUT"
     * @param devid     the device ID
     * @param value     the value transferred
     */
    public void logIO(String direction, int devid, int value) {
        if (!enabled || writer == null) return;
        String deviceName = switch (devid) {
            case 0 -> "keyboard";
            case 1 -> "printer";
            case 2 -> "card reader";
            default -> "unknown";
        };
        writer.printf("[STEP %04d] I/O     | %-3s devid=%d (%s) value=%d(oct:%s)%n",
                stepCounter, direction, devid, deviceName, value, padOctal(value));
    }

    /**
     * Logs a machine fault event.
     *
     * @param mfrCode     the machine fault register code (binary)
     * @param description a human-readable description of the fault
     */
    public void logFault(int mfrCode, String description) {
        if (!enabled || writer == null) return;
        writer.printf("[STEP %04d] FAULT   | MFR=%s %s%n",
                stepCounter, String.format("%4s", Integer.toBinaryString(mfrCode)).replace(' ', '0'), description);
    }

    /**
     * Logs a halt event.
     */
    public void logHalt() {
        if (!enabled || writer == null) return;
        writer.printf("[STEP %04d] HALT    | Machine halted%n", stepCounter);
    }

    /**
     * Increments the step counter. Called after each instruction completes.
     */
    public void incrementStep() {
        stepCounter++;
    }

    /**
     * Flushes and closes the trace log file.
     */
    public void close() {
        if (writer != null) {
            writer.println();
            writer.println("=== Trace ended ===");
            writer.flush();
            writer.close();
            writer = null;
        }
    }

    /**
     * Returns a zero-padded 6-digit octal string for the given value.
     */
    private String padOctal(int value) {
        StringBuilder sb = new StringBuilder(Integer.toOctalString(value));
        while (sb.length() < 6) {
            sb.insert(0, "0");
        }
        return sb.toString();
    }
}
