package encoder;

/**
 * Utility methods for converting numeric values to zero-padded binary and octal strings.
 *
 * <p>These helpers are used by the instruction encoder to build up the bit fields of a
 * 16-bit machine word and to produce the final octal representation written to the load file.
 */
public class EncoderStringUtil {

    /**
     * Converts an integer to a zero-padded binary string of the specified length.
     *
     * <p>Example: {@code getZeroPaddedBinaryString(5, 6)} returns {@code "000101"}.
     *
     * @param value  the integer value to convert
     * @param length the minimum number of binary digits in the result (padded with leading zeros)
     * @return a binary string of at least {@code length} characters
     */
    public static String getZeroPaddedBinaryString(int value, int length) {
        String binaryString = Integer.toBinaryString(value);
        StringBuilder sb = new StringBuilder(binaryString);

        while (sb.length() < length) {
            sb.insert(0, "0");
        }

        return sb.toString();
    }

    /**
     * Parses a decimal string and converts it to a zero-padded binary string of the specified length.
     *
     * <p>If {@code value} is not a valid integer string, an empty string is returned.
     *
     * @param value  a decimal integer string (e.g. {@code "5"})
     * @param length the minimum number of binary digits in the result
     * @return a binary string of at least {@code length} characters, or {@code ""} on parse failure
     */
    public static String getZeroPaddedBinaryString(String value, int length) {
        int intValue;
        try  {
                intValue = Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return "";
        }

        return getZeroPaddedBinaryString(intValue, length);
    }

    /**
     * Converts a binary string to a zero-padded 6-digit octal string.
     *
     * <p>Example: {@code getOctalString("000001000000")} returns {@code "000100"}.
     *
     * @param binaryString a string of binary digits (e.g. the 16-bit encoded instruction)
     * @return a 6-digit zero-padded octal string representing the same value
     */
    public static String getOctalString(String binaryString) {
        StringBuilder sb = new StringBuilder(Integer.toOctalString(Integer.parseInt(binaryString, 2)));
        while (sb.length() < 6) {
            sb.insert(0, "0");
        }

        return sb.toString();
    }
}
