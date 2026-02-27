package memory;

import java.util.HashMap;
import java.util.Map;

public class RegisterManager {
    private static final Map<Register, Integer> registerData = new HashMap<>();

    public static void loadRegister(Register register, int value) {
        registerData.put(register, value);
    }
}
