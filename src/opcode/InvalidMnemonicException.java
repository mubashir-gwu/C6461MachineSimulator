package opcode;

public class InvalidMnemonicException extends Exception {
    public InvalidMnemonicException(String mnemonic) {
        super("invalid mnemonic: " + mnemonic);
    }
}
