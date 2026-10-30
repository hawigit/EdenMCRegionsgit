package hawi.bucket.edenmcregions.core;

import java.util.ArrayList;
import java.util.List;

/** Preserve the type of each span so nation/region colors can differ in one title. */
public record TitleText(List<Span> spans) {
    public enum Kind { NATION, REGION, SEPARATOR }
    public record Span(String text, Kind kind, Integer mapColor) {
        public Span(String text, Kind kind) { this(text, kind, null); }
    }
    public TitleText { spans = List.copyOf(spans); }
    public String text() { return String.join("", spans.stream().map(Span::text).toList()); }
    public static TitleText create(List<TerritoryIndex.Match> matches, boolean nations, boolean regions) {
        List<Span> result = new ArrayList<>();
        for (var match : matches) {
            List<Span> location = new ArrayList<>();
            if (nations) location.add(new Span(match.nation(), Kind.NATION, match.color()));
            List<TerritoryIndex.Region> shown = regions ? match.regions() : List.of();
            if (!shown.isEmpty()) {
                if (!location.isEmpty()) location.add(new Span(" / ", Kind.SEPARATOR));
                java.util.Set<String> seen = new java.util.HashSet<>();
                for (var region : shown) {
                    String name = TerritoryIndex.formatRegion(match.nation(), region);
                    if (name.isBlank() || !seen.add(name)) continue;
                    if (seen.size() > 1) location.add(new Span(", ", Kind.SEPARATOR));
                    location.add(new Span(name, Kind.REGION, region.color()));
                }
            }
            if (!location.isEmpty()) {
                if (!result.isEmpty()) result.add(new Span(" + ", Kind.SEPARATOR));
                result.addAll(location);
            }
        }
        if (result.isEmpty() && (nations || regions)) result.add(new Span(matches.isEmpty() ? "Unclaimed territory" : "Unnamed region",
                nations ? Kind.NATION : Kind.REGION));
        return new TitleText(result);
    }
}
