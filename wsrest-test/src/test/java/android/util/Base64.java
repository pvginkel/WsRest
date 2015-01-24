package android.util;

public class Base64 {
    public static String encodeToString(byte[] bytes, int flags) {
        return org.apache.commons.codec.binary.Base64.encodeBase64String(bytes);
    }
}
