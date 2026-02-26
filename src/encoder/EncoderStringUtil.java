package encoder;

public class EncoderStringUtil {
    public static String getZeroPaddedBinaryString(int value, int length) {
        String binaryString = Integer.toBinaryString(value);
        StringBuilder sb = new StringBuilder(binaryString);

        while (sb.length() < length) {
            sb.insert(0, "0");
        }

        return sb.toString();
    }

    public static String getZeroPaddedBinaryString(String value, int length) {
        String binaryString = Integer.toBinaryString(Integer.parseInt(value));
        StringBuilder sb = new StringBuilder(binaryString);

        while (sb.length() < length) {
            sb.insert(0, "0");
        }

        return sb.toString();
    }

    public static String getOctalString(String binaryString) {
        StringBuilder sb = new StringBuilder(Integer.toOctalString(Integer.parseInt(binaryString, 2)));
        while (sb.length() < 6) {
            sb.insert(0, "0");
        }

        return sb.toString();
    }
}
