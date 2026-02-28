package opcode;

/**
 * Categorises each instruction in the C6461 instruction set by its encoding format.
 *
 * <p>Instructions that share the same {@code OpcodeType} use the same 16-bit word layout,
 * so the encoder and CPU can branch on this type to apply the correct encoding/decoding logic.
 */
public enum OpcodeType {
    /** Load and store instructions (LDR, STR, LDA, LDX, STX). Use the load/store word format. */
    LOAD_STORE,
    /** Control-transfer instructions (JZ, JNE, JCC, JMA, JSR, RFS, SOB, JGE). Use the load/store word format. */
    TRANSFER,
    /** Arithmetic instructions (AMR, SMR, AIR, SIR). Use the load/store word format. */
    ARITHMETIC,
    /** Register-to-register multiply and divide instructions (MLT, DVD). Use the register-register format. */
    MULTIPLY_DIVIDE,
    /** Logical register-to-register instructions (TRR, AND, ORR, NOT). Use the register-register format. */
    LOGICAL,
    /** Shift and rotate instructions (SRC, RRC). Use the shift/rotate word format. */
    SHIFT_ROTATE,
    /** Input/Output instructions (IN, OUT, CHK). Use the I/O word format. */
    IO,
    /** Miscellaneous instructions (HLT, TRAP). Use the miscellaneous word format. */
    MISC,
}
