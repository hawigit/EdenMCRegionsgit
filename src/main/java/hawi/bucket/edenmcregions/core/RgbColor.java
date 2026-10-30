package hawi.bucket.edenmcregions.core;

import java.util.Locale;

/** Six RGB digits only; opacity remains controlled by title fading. */
public final class RgbColor {
    private RgbColor() {}
    public static boolean valid(String value) { return value != null && value.strip().matches("#?[0-9a-fA-F]{6}"); }
    public static String normalize(String value, String fallback) {
        if (!valid(value)) return fallback;
        String hex = value.strip();
        return (hex.startsWith("#") ? hex : "#" + hex).toUpperCase(Locale.ROOT);
    }
    public static int argb(String value) { return 0xff000000 | Integer.parseInt(normalize(value, "#FFFFFF").substring(1), 16); }
}
