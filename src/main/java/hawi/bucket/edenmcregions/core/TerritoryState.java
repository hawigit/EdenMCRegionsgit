package hawi.bucket.edenmcregions.core;

import com.google.gson.JsonElement;
import java.util.*;

/** Community-declared map states; absent data never implies Safe. */
public enum TerritoryState {
    KOS("kos", "[KOS]", 0xffef4444), POS("pos", "[POS]", 0xffc084fc),
    CLOSED("closed", "[Closed Border]", 0xfff59e0b), SAFE("peaceful", "[Safe]", 0xff22c55e);
    private final String key, label;
    private final int color;
    TerritoryState(String key, String label, int color) { this.key = key; this.label = label; this.color = color; }
    public String label() { return label; }
    public int color() { return color; }
    public static List<TerritoryState> parse(JsonElement data) {
        if (data == null || !data.isJsonArray()) return List.of();
        Set<String> keys = new HashSet<>();
        for (JsonElement entry : data.getAsJsonArray()) if (entry.isJsonPrimitive() && entry.getAsJsonPrimitive().isString()) keys.add(entry.getAsString());
        return Arrays.stream(values()).filter(s -> keys.contains(s.key)).toList();
    }
}
