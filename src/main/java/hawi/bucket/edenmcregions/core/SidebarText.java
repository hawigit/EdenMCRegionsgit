package hawi.bucket.edenmcregions.core;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.ToIntFunction;

/** Font-independent layout; the client supplies Minecraft's pixel measuring/trimming methods. */
public final class SidebarText {
    private SidebarText() {}
    /** Count Unicode characters in the name, excluding the location label. */
    public static List<String> wrapCharacters(String text, int threshold) {
        if (text.isEmpty()) return List.of();
        threshold = Math.clamp(threshold, ModConfig.MIN_WRAP_CHARACTERS, ModConfig.MAX_WRAP_CHARACTERS);
        String prefix = text.startsWith("Nation: ") ? "Nation: " : text.startsWith("Region: ") ? "Region: " : "";
        String remaining = text.substring(prefix.length()).stripLeading();
        List<String> rows = new ArrayList<>();
        while (!remaining.isEmpty()) {
            int end = remaining.offsetByCodePoints(0, Math.min(threshold, remaining.codePointCount(0, remaining.length())));
            String part = remaining.substring(0, end);
            if (end < remaining.length() && remaining.charAt(end) != ' ') {
                int space = part.lastIndexOf(' ');
                if (space > 0) part = part.substring(0, space);
            }
            rows.add((rows.isEmpty() ? prefix : "") + part);
            remaining = remaining.substring(part.length()).stripLeading();
        }
        return rows.isEmpty() ? List.of(text) : List.copyOf(rows);
    }
    public static List<String> format(String text, TextLayoutMode mode, int width, int maxRows, int threshold,
                                      ToIntFunction<String> measure, BiFunction<String, Integer, String> trim) {
        if (mode != TextLayoutMode.WRAP) return format(text, mode, width, maxRows, measure, trim);
        List<String> result = new ArrayList<>();
        for (String row : wrapCharacters(text, threshold)) {
            result.addAll(format(row, mode, width, maxRows - result.size(), measure, trim));
            if (result.size() >= maxRows) break;
        }
        return List.copyOf(result);
    }
    public static List<String> format(String text, TextLayoutMode mode, int width, int maxRows,
                                      ToIntFunction<String> measure, BiFunction<String, Integer, String> trim) {
        if (text.isEmpty() || width <= 0 || maxRows <= 0) return List.of();
        if (mode != TextLayoutMode.WRAP) {
            if (measure.applyAsInt(text) <= width) return List.of(text);
            String ellipsis = trim.apply("...", width);
            return List.of(trim.apply(text, Math.max(0, width - measure.applyAsInt(ellipsis))) + ellipsis);
        }
        List<String> rows = new ArrayList<>();
        String remaining = text;
        while (!remaining.isEmpty() && rows.size() < maxRows) {
            String part = trim.apply(remaining, width);
            if (part.isEmpty()) break;
            if (part.length() < remaining.length() && remaining.charAt(part.length()) != ' ') {
                int space = part.lastIndexOf(' ');
                if (space > 0) part = part.substring(0, space);
            }
            rows.add(part);
            remaining = remaining.substring(part.length()).stripLeading();
        }
        return List.copyOf(rows);
    }
}
