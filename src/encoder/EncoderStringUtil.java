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
//        StringBuilder binaryStringBuilder = new StringBuilder(binaryString);
//        while (binaryStringBuilder.length() < 18) {
//            binaryStringBuilder.insert(0, "0");
//        }
//
//        System.out.println(binaryStringBuilder.toString());

//        StringBuilder sb = new StringBuilder();
//        for (int i = 0; i < binaryStringBuilder.length(); i += 3) {
//            sb.append(Integer.toOctalString(Integer.parseInt(binaryStringBuilder.substring(i, i + 3), 2)));
//        }

        StringBuilder sb = new StringBuilder(Integer.toOctalString(Integer.parseInt(binaryString, 2)));
        while (sb.length() < 6) {
            sb.insert(0, "0");
        }

        return sb.toString();
    }
}
