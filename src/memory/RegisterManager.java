package memory;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages the state of all CPU registers in the C6461 machine simulator.
 *
 * <p>Each {@link Register} is mapped to its current integer value. All registers are
 * initialised to zero on construction. The simulator's {@code CPU} and {@code UserInterface}
 * use this class to read and write register values during instruction execution.
 */
public class RegisterManager {
    /** Maps each register to its current value. */
    private final Map<Register, Integer> registerData = new HashMap<>();

    /**
     * Constructs a new RegisterManager with all registers initialised to zero.
     */
    public RegisterManager() {
        // Set all registers to 0 by default.
        for (Register register : Register.values()) {
            registerData.put(register, 0);
        }
    }

    /**
     * Loads (writes) a value into the specified register.
     *
     * @param register the target register to update
     * @param value    the integer value to store in the register
     */
    public void loadRegister(Register register, int value) {
        registerData.put(register, value);
    }

    /**
     * Returns the current value stored in the specified register.
     *
     * @param register the register to read
     * @return the integer value currently held in {@code register}
     */
    public int getRegisterValue(Register register) {
        return registerData.get(register);
    }

    /**
     * Sets or clears an individual bit in the Condition Code (CC) register.
     *
     * <p>Bit indices: 0=OVERFLOW, 1=UNDERFLOW, 2=DIVZERO, 3=EQUALORNOT.
     *
     * @param bitIndex the bit position to modify (0–3)
     * @param value    {@code true} to set the bit to 1, {@code false} to clear it to 0
     */
    public void setConditionCode(int bitIndex, boolean value) {
        int cc = registerData.get(Register.CC);
        if (value) {
            cc |= (1 << bitIndex);
        } else {
            cc &= ~(1 << bitIndex);
        }
        registerData.put(Register.CC, cc & 0xF);
    }
}
