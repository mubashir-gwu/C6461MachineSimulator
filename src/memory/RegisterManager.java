package memory;

import java.util.HashMap;
import java.util.Map;

public class RegisterManager {
    private final Map<Register, Integer> registerData = new HashMap<>();

    public RegisterManager() {
        // Set all registers to 0 by default.
        for (Register register : Register.values()) {
            registerData.put(register, 0);
        }
    }

    public void loadRegister(Register register, int value) {
        registerData.put(register, value);
    }

    public int getRegisterValue(Register register) {
        return registerData.get(register);
    }
}
