package encoder;

public record TokenizedLine(String label, String opcode, String[] operands) {
    @Override
    public String toString() {
        return String.format("TokenizedLine(Label: '%s', Opcode: %s, Operands: %s)", label, opcode, String.join(", ", operands));
    }
}
