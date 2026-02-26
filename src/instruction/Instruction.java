package instruction;

public record Instruction(String label, String mnemonic, String[] operands) {
    @Override
    public String toString() {
        return String.format("Instruction(Label: '%s', Mnemonic: %s, Operands: %s)", label, mnemonic, String.join(", ", operands));
    }
}
