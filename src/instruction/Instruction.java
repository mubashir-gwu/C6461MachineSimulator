package instruction;

public class Instruction {
    private String label;
    private String mnemonic;
    private String[] operands;
    private String comment;
    private String loc;
    private String octal;

    public Instruction(String label, String mnemonic, String[] operands) {
        this.label = label;
        this.mnemonic = mnemonic;
        this.operands = operands;
        this.comment = "";
        this.loc = "";
        this.octal = "";
    }

    public Instruction(String label, String mnemonic, String[] operands, String comment, String loc, String octal) {
        this.label = label;
        this.mnemonic = mnemonic;
        this.operands = operands;
        this.comment = comment;
        this.loc = loc;
        this.octal = octal;
    }

    public void setOperands(String[] operands) {
        this.operands = operands;
    }

    public void setLoc(String loc) {
        this.loc = loc;
    }

    public void setOctal(String octal) {
        this.octal = octal;
    }

    public String getLabel() {
        return label;
    }

    public String getMnemonic() {
        return mnemonic;
    }

    public String[] getOperands() {
        return operands;
    }

    public String getComment() {
        return comment;
    }

    public String getLoc() {
        return loc;
    }

    public String getOctal() {
        return octal;
    }

    @Override
    public String toString() {
        // return String.format("Instruction(Label: '%s', Mnemonic: %s, Operands: %s)", label, mnemonic, String.join(", ", operands));
        return String.format("Instruction(Label: '%s', Mnemonic: %s, Operands: %s, Loc: %s, Octal: %s, Comment: %s)", label, mnemonic, String.join(", ", operands), loc, octal, comment);
    }

    public String toListingFileString() {
        final String labelToPrint = label.isEmpty() ? "" : label + ":";
        final String commentToPrint = comment.isEmpty() ? "" : ";" + comment;

        return String.format("%6s %6s %-10s %-6s %-20s %s", loc, octal, labelToPrint, mnemonic, String.join(",", operands), commentToPrint);
    }

    public String toLoadFileString() {
        return String.format("%6s %6s", loc, octal);
    }
}