package hawi.bucket.edenmcregions.core;

import com.google.gson.*;
import java.util.*;

/** Immutable territory data. All geometry is in Minecraft X/Z blocks, never map pixels. */
public final class TerritoryIndex {
    public record Point(double x, double z) {}
    public record Ring(List<Point> points, double minX, double minZ, double maxX, double maxZ) {
        static Ring of(List<Point> points) {
            return new Ring(List.copyOf(points), points.stream().mapToDouble(Point::x).min().orElse(0),
                    points.stream().mapToDouble(Point::z).min().orElse(0),
                    points.stream().mapToDouble(Point::x).max().orElse(0),
                    points.stream().mapToDouble(Point::z).max().orElse(0));
        }
        public boolean contains(double x, double z) {
            if (points.size() < 3 || !Double.isFinite(x) || !Double.isFinite(z)
                    || x < minX || x > maxX || z < minZ || z > maxZ) return false;
            boolean inside = false;
            for (int i = 0, j = points.size() - 1; i < points.size(); j = i++) {
                Point a = points.get(j), b = points.get(i);
                double dx = b.x - a.x, dz = b.z - a.z;
                // Include exterior boundaries consistently. Hole boundaries are excluded below.
                double cross = (x - a.x) * dz - (z - a.z) * dx;
                if (Math.abs(cross) <= 1e-8 * Math.max(1, Math.hypot(dx, dz))
                        && x >= Math.min(a.x, b.x) && x <= Math.max(a.x, b.x)
                        && z >= Math.min(a.z, b.z) && z <= Math.max(a.z, b.z)) return true;
                if ((a.z > z) != (b.z > z) && x < dx * (z - a.z) / dz + a.x) inside = !inside;
            }
            return inside;
        }
    }
    public record Shape(Ring outer, List<Ring> holes) {
        public boolean contains(double x, double z) {
            return outer.contains(x, z) && holes.stream().noneMatch(h -> h.contains(x, z));
        }
    }
    public record Region(String id, String name, boolean outpost, Shape shape, int color, List<TerritoryState> states) {
        public Region { states = states == null ? null : List.copyOf(states); }
        public Region(String id, String name, boolean outpost, Shape shape, int color) { this(id, name, outpost, shape, color, null); }
        public Region(String id, String name, boolean outpost, Shape shape) {
            this(id, name, outpost, shape, 0xff22d3ee);
        }
    }
    public record Nation(String id, String name, int color, Shape shape, List<Region> regions, List<TerritoryState> states) {}
    public static String formatRegion(String nation, Region r) {
        String rName = r.name == null ? "" : r.name.strip();
        String nName = nation == null ? "" : nation.strip();
        boolean redundant = rName.isBlank()
                || rName.equalsIgnoreCase(nName)
                || rName.equalsIgnoreCase("Outpost")
                || rName.equalsIgnoreCase("Capital");
        if (r.outpost) {
            if (redundant) return "Outpost";
            String clean = rName;
            if (clean.toLowerCase(Locale.ROOT).startsWith(nName.toLowerCase(Locale.ROOT))) {
                clean = clean.substring(nName.length()).strip();
                if (clean.startsWith("-") || clean.startsWith(":")) clean = clean.substring(1).strip();
            }
            if (clean.isBlank() || clean.equalsIgnoreCase("Outpost")) return "Outpost";
            return clean.endsWith("(Outpost)") ? clean : clean + " (Outpost)";
        } else {
            return rName.isBlank() ? "Unnamed region" : rName;
        }
    }
    public record Match(String nationId, String nation, int color, List<Region> regions, List<TerritoryState> states) {
        public Match { regions = List.copyOf(regions); states = List.copyOf(states); }
        public Match(String nationId, String nation, int color, List<Region> regions) { this(nationId, nation, color, regions, List.of()); }
        public String label() {
            return label(true);
        }
        public String label(boolean showRegions) {
            return label(true, showRegions);
        }
        public String label(boolean showNations, boolean showRegions) {
            List<String> formatted = (showRegions ? regions : List.<Region>of()).stream()
                    .map(r -> formatRegion(nation, r))
                    .filter(s -> !s.isBlank())
                    .distinct()
                    .toList();
            String regionLabel = String.join(", ", formatted);
            if (!showNations) return regionLabel;
            return regionLabel.isEmpty() ? nation : nation + " / " + regionLabel;
        }
    }
    private final List<Nation> nations;
    private final String cacheJson;
    private TerritoryIndex(List<Nation> nations, String cacheJson) {
        this.nations = List.copyOf(nations);
        this.cacheJson = cacheJson;
    }
    public int nationCount() { return nations.size(); }
    public String cacheJson() { return cacheJson; }

    public List<Match> locate(double x, double z) {
        List<Match> matches = new ArrayList<>();
        for (Nation nation : nations) {
            boolean mainland = nation.shape.contains(x, z);
            List<Region> regions = nation.regions.stream()
                    .filter(r -> (mainland || r.outpost) && r.shape.contains(x, z))
                    .sorted(Comparator.comparing(Region::name).thenComparing(Region::id)).toList();
            if (mainland || regions.stream().anyMatch(Region::outpost)) {
                Set<TerritoryState> active = EnumSet.noneOf(TerritoryState.class);
                if (regions.isEmpty()) active.addAll(nation.states);
                else for (Region region : regions) active.addAll(region.states == null ? nation.states : region.states);
                matches.add(new Match(nation.id, nation.name, nation.color, regions, List.copyOf(active)));
            }
        }
        matches.sort(Comparator.comparing(Match::nation).thenComparing(Match::nationId));
        return List.copyOf(matches);
    }

    /** Reject incomplete/malformed snapshots rather than silently inventing unclaimed land. */
    public static TerritoryIndex parse(String json, String dimension) {
        JsonElement root = JsonParser.parseString(json);
        if (!root.isJsonArray()) throw new IllegalArgumentException("Expected a nations array");
        if (root.getAsJsonArray().size() > 10000) throw new IllegalArgumentException("Too many nations");
        List<Nation> nations = new ArrayList<>();
        JsonArray safeCache = new JsonArray();
        Set<String> ids = new HashSet<>();
        for (JsonElement item : root.getAsJsonArray()) {
            JsonObject obj = item.getAsJsonObject();
            String status = required(obj, "status");
            if (!List.of("approved", "placeholder", "pending", "rejected").contains(status))
                throw new IllegalArgumentException("Unknown claim status");
            if (!"approved".equals(status)) continue;
            if (!dimension.equals(string(obj, "mapType", dimension))) continue;
            String id = required(obj, "id"), name = clean(required(obj, "name"));
            if (!ids.add(id)) throw new IllegalArgumentException("Duplicate nation ID");
            boolean smooth = bool(obj, "smoothBorders", false);
            Shape shape = shape(obj, smooth);
            int color = 0xff70d6a0;
            if (obj.has("color") && obj.get("color").isJsonObject()) {
                String hex = string(obj.getAsJsonObject("color"), "hex", "");
                if (hex.matches("#[0-9a-fA-F]{6}")) color = 0xff000000 | Integer.parseInt(hex.substring(1), 16);
            }
            List<Region> regions = new ArrayList<>();
            JsonArray safeRegions = new JsonArray();
            if (obj.has("regions")) for (JsonElement entry : obj.getAsJsonArray("regions")) {
                JsonObject region = entry.getAsJsonObject();
                regions.add(new Region(required(region, "id"), clean(string(region, "name", "")),
                        bool(region, "isOutpost", false), shape(region, bool(region, "smoothBorders", smooth)),
                        region.has("color") && region.get("color").isJsonPrimitive()
                                ? RgbColor.argb(RgbColor.normalize(region.get("color").getAsString(), "#22D3EE")) : 0xff22d3ee,
                        region.has("zones") && region.get("zones").isJsonArray() ? TerritoryState.parse(region.get("zones")) : null));
                safeRegions.add(keep(region, "id", "name", "polygon", "holes", "smoothBorders", "isOutpost", "color", "zones"));
            }
            nations.add(new Nation(id, name, color, shape, List.copyOf(regions), TerritoryState.parse(obj.get("zones"))));
            JsonObject safe = keep(obj, "id", "name", "status", "mapType", "polygon", "holes", "smoothBorders", "color", "zones");
            safe.add("regions", safeRegions);
            safeCache.add(safe);
        }
        return new TerritoryIndex(nations, safeCache.toString());
    }
    private static JsonObject keep(JsonObject obj, String... keys) {
        JsonObject copy = new JsonObject();
        for (String key : keys) if (obj.has(key)) copy.add(key, obj.get(key));
        return copy;
    }
    private static Shape shape(JsonObject obj, boolean smooth) {
        Ring outer = ring(obj.getAsJsonArray("polygon"), smooth);
        List<Ring> holes = new ArrayList<>();
        if (obj.has("holes") && !obj.get("holes").isJsonNull())
            for (JsonElement hole : obj.getAsJsonArray("holes")) holes.add(ring(hole.getAsJsonArray(), smooth));
        return new Shape(outer, List.copyOf(holes));
    }
    private static Ring ring(JsonArray array, boolean smooth) {
        if (array == null || array.size() > 50000 || (array.size() > 0 && array.size() < 3))
            throw new IllegalArgumentException("Invalid polygon");
        List<Point> points = new ArrayList<>();
        for (JsonElement entry : array) {
            JsonObject p = entry.getAsJsonObject();
            double x = p.get("x").getAsDouble();
            double z = p.get(p.has("z") ? "z" : "y").getAsDouble();
            if (!Double.isFinite(x) || !Double.isFinite(z) || Math.abs(x) > 30000000 || Math.abs(z) > 30000000)
                throw new IllegalArgumentException("Invalid coordinate");
            points.add(new Point(x, z));
        }
        // Chaikin corner cutting, as used by the map. Precompute once, never during HUD drawing.
        if (smooth) for (int pass = 0; pass < 3; pass++) {
            List<Point> next = new ArrayList<>();
            for (int i = 0; i < points.size(); i++) {
                Point a = points.get(i), b = points.get((i + 1) % points.size());
                next.add(new Point(.75 * a.x + .25 * b.x, .75 * a.z + .25 * b.z));
                next.add(new Point(.25 * a.x + .75 * b.x, .25 * a.z + .75 * b.z));
            }
            points = next;
        }
        return Ring.of(points);
    }
    private static String required(JsonObject o, String key) {
        String value = string(o, key, "");
        if (value.isBlank()) throw new IllegalArgumentException("Missing " + key);
        return value;
    }
    private static String string(JsonObject o, String key, String fallback) {
        return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsString() : fallback;
    }
    private static boolean bool(JsonObject o, String key, boolean fallback) {
        return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsBoolean() : fallback;
    }
    private static String clean(String s) {
        String cleaned = s.replaceAll("[\\p{Cntrl}§]", "").strip();
        return cleaned.substring(0, Math.min(160, cleaned.length()));
    }
}


