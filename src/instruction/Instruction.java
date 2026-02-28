package instruction;

/**
 * Represents a single assembly language instruction after it has been parsed from source.
 *
 * <p>An {@code Instruction} holds all fields produced by the two-pass assembler:
 * <ul>
 *   <li>{@code label}  – optional symbolic label defined at this line (e.g. {@code LOOP:})</li>
 *   <li>{@code mnemonic} – the operation name (e.g. {@code LDR}, {@code STR}, {@code HLT})</li>
 *   <li>{@code operands} – the comma-separated operand tokens (e.g. {@code ["0","1","5"]})</li>
 *   <li>{@code comment} – the inline comment text following the {@code ;} delimiter</li>
 *   <li>{@code loc}    – the 6-digit octal memory address assigned during pass 1</li>
 *   <li>{@code octal}  – the 6-digit octal machine word produced during pass 2 encoding</li>
 * </ul>
 */
public class Instruction {
    /** Optional label defined at this instruction (empty string if none). */
    private String label;
    /** The instruction mnemonic (e.g. {@code LDR}, {@code DATA}, {@code LOC}). */
    private String mnemonic;
    /** Parsed operand tokens. Interpretation depends on the instruction format. */
    private String[] operands;
    /** Inline comment text (without the leading {@code ;}). Empty string if none. */
    private String comment;
    /** The 6-digit zero-padded octal address of this instruction in memory. */
    private String loc;
    /** The 6-digit zero-padded octal encoding of the assembled machine word. */
    private String octal;

    /**
     * Constructs an instruction with only the core assembly fields; comment, loc and octal
     * are initialised to empty strings.
     *
     * @param label    the optional label (empty string if none)
     * @param mnemonic the instruction mnemonic
     * @param operands the operand tokens
     */
    public Instruction(String label, String mnemonic, String[] operands) {
        this.label = label;
        this.mnemonic = mnemonic;
        this.operands = operands;
        this.comment = "";
        this.loc = "";
        this.octal = "";
    }

    /**
     * Constructs a fully-specified instruction with all fields.
     *
     * @param label    the optional label (empty string if none)
     * @param mnemonic the instruction mnemonic
     * @param operands the operand tokens
     * @param comment  the inline comment text (empty string if none)
     * @param loc      the 6-digit octal memory address (empty string if not yet assigned)
     * @param octal    the 6-digit octal machine word (empty string if not yet encoded)
     */
    public Instruction(String label, String mnemonic, String[] operands, String comment, String loc, String octal) {
        this.label = label;
        this.mnemonic = mnemonic;
        this.operands = operands;
        this.comment = comment;
        this.loc = loc;
        this.octal = octal;
    }

    /**
     * Replaces the operand tokens. Used during pass 2 to substitute label names with
     * their resolved numeric addresses.
     *
     * @param operands the updated operand tokens
     */
    public void setOperands(String[] operands) {
        this.operands = operands;
    }

    /**
     * Sets the 6-digit octal memory address assigned to this instruction.
     *
     * @param loc zero-padded 6-digit octal address string
     */
    public void setLoc(String loc) {
        this.loc = loc;
    }

    /**
     * Sets the 6-digit octal machine word produced by encoding this instruction.
     *
     * @param octal zero-padded 6-digit octal encoding string
     */
    public void setOctal(String octal) {
        this.octal = octal;
    }

    /** @return the label defined at this instruction, or an empty string if none */
    public String getLabel() {
        return label;
    }

    /** @return the instruction mnemonic */
    public String getMnemonic() {
        return mnemonic;
    }

    /** @return the operand tokens for this instruction */
    public String[] getOperands() {
        return operands;
    }

    /** @return the inline comment text, or an empty string if none */
    public String getComment() {
        return comment;
    }

    /** @return the 6-digit octal memory address, or an empty string if not yet assigned */
    public String getLoc() {
        return loc;
    }

    /** @return the 6-digit octal machine word, or an empty string if not yet encoded */
    public String getOctal() {
        return octal;
    }

    /**
     * Returns a debug-friendly string representation showing all fields.
     *
     * @return formatted string with label, mnemonic, operands, loc, octal, and comment
     */
    @Override
    public String toString() {
        return String.format("Instruction(Label: '%s', Mnemonic: %s, Operands: %s, Loc: %s, Octal: %s, Comment: %s)", label, mnemonic, String.join(", ", operands), loc, octal, comment);
    }

    /**
     * Formats this instruction as a line in a human-readable assembler listing file.
     *
     * <p>Columns: {@code loc  octal  label  mnemonic  operands  ;comment}
     *
     * @return a formatted listing-file line for this instruction
     */
    public String toListingFileString() {
        final String labelToPrint = label.isEmpty() ? "" : label + ":";
        final String commentToPrint = comment.isEmpty() ? "" : ";" + comment;

        return String.format("%6s %6s %-10s %-6s %-20s %s", loc, octal, labelToPrint, mnemonic, String.join(",", operands), commentToPrint);
    }

    /**
     * Formats this instruction as a line in the load file consumed by the simulator.
     *
     * <p>Columns: {@code loc  octal} (space-separated, suitable for direct memory loading).
     *
     * @return a formatted load-file line for this instruction
     */
    public String toLoadFileString() {
        return String.format("%6s %6s", loc, octal);
    }
}
