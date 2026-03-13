package pl.co.common.util;

public final class StringUtils {

    private StringUtils() {
    }

    public static boolean hasText(String value) {
        return org.springframework.util.StringUtils.hasText(value);
    }

    public static String normalize(String value) {
        if (!hasText(value)) {
            return null;
        }
        return value.trim();
    }

    public static String[] tokenizeToStringArray(String value, String delimiters) {
        return org.springframework.util.StringUtils.tokenizeToStringArray(value, delimiters);
    }

    public static String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
