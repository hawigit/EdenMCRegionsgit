package hawi.bucket.edenmcregions.core;

import java.util.Locale;

/** HSV keeps hue/saturation on the wheel and brightness on a separate slider. */
public final class ColorWheel {
    private double hue, saturation, brightness;
    public ColorWheel(String hex) { setHex(hex); }
    public double hue() { return hue; }
    public double saturation() { return saturation; }
    public double brightness() { return brightness; }
    public void setBrightness(double value) { brightness = Math.clamp(value, 0, 1); }
    public void select(double x, double y, double radius) {
        hue = (Math.atan2(y, x) / (2 * Math.PI) + 1) % 1;
        saturation = Math.clamp(Math.hypot(x, y) / radius, 0, 1);
    }
    public void setHex(String hex) {
        int rgb = RgbColor.argb(hex);
        double r = ((rgb >> 16) & 255) / 255d, g = ((rgb >> 8) & 255) / 255d, b = (rgb & 255) / 255d;
        double max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b)), delta = max - min;
        brightness = max; saturation = max == 0 ? 0 : delta / max;
        if (delta > 0) hue = (((max == r ? (g - b) / delta : max == g ? (b - r) / delta + 2 : (r - g) / delta + 4) / 6) + 1) % 1;
    }
    public String hex() { return String.format(Locale.ROOT, "#%06X", rgb(hue, saturation, brightness) & 0xffffff); }
    public static int rgb(double hue, double saturation, double brightness) {
        double h = ((hue % 1 + 1) % 1) * 6, s = Math.clamp(saturation, 0, 1), v = Math.clamp(brightness, 0, 1);
        double c = v * s, x = c * (1 - Math.abs(h % 2 - 1)), m = v - c;
        double r = 0, g = 0, b = 0;
        switch ((int)h) {
            case 0 -> { r = c; g = x; }
            case 1 -> { r = x; g = c; }
            case 2 -> { g = c; b = x; }
            case 3 -> { g = x; b = c; }
            case 4 -> { r = x; b = c; }
            default -> { r = c; b = x; }
        }
        return 0xff000000 | ((int)Math.round((r + m) * 255) << 16)
                | ((int)Math.round((g + m) * 255) << 8) | (int)Math.round((b + m) * 255);
    }
}
