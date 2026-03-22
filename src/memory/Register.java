package memory;

/**
 * Enumerates all registers available in the C6461 machine simulator.
 *
 * <p>The machine has four General Purpose Registers (GPRs), three Index Registers (IXRs),
 * a Program Counter (PC), and three special-purpose registers used during memory access
 * (MAR, MBR) and instruction decoding (IR).
 */
public enum Register {
    /** General Purpose Register 0. Used as an accumulator or operand in arithmetic/logic instructions. */
    GPR0,
    /** General Purpose Register 1. */
    GPR1,
    /** General Purpose Register 2. */
    GPR2,
    /** General Purpose Register 3. */
    GPR3,

    /** Index Register 1. Used for indexed addressing: EA = address + IX. */
    IX1,
    /** Index Register 2. Used for indexed addressing: EA = address + IX. */
    IX2,
    /** Index Register 3. Used for indexed addressing: EA = address + IX. */
    IX3,

    /** Program Counter. Holds the address of the next instruction to execute. */
    PC,
    /** Memory Address Register. Holds the effective address for the current memory operation. */
    MAR,
    /** Memory Buffer Register. Holds the data read from or to be written to memory. */
    MBR,
    /** Instruction Register. Holds the full 16-bit word of the currently executing instruction. */
    IR,

    /** Condition Code register. 4 bits: cc(0)=OVERFLOW, cc(1)=UNDERFLOW, cc(2)=DIVZERO, cc(3)=EQUALORNOT. */
    CC,
    /** Machine Fault Register. 4 bits: 0001=illegal reserved addr, 0010=illegal TRAP, 0100=illegal opcode, 1000=addr beyond 2048. */
    MFR,
}
