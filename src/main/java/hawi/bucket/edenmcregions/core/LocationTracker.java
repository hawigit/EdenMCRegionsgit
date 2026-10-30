package hawi.bucket.edenmcregions.core;

import java.util.Comparator;
import java.util.List;

/** Confirm two samples; only the location types selected for titles affect crossings. */
public final class LocationTracker {
    private record Identity(String nationId, List<String> regionIds, List<TerritoryState> states) {}
    private List<Identity> current, candidate;
    private int mode = -1;
    public boolean update(List<TerritoryIndex.Match> matches) { return update(matches, true, true); }
    public boolean update(List<TerritoryIndex.Match> matches, boolean showRegions) { return update(matches, true, showRegions); }
    public boolean update(List<TerritoryIndex.Match> matches, boolean showNations, boolean showRegions) {
        return update(matches, showNations, showRegions, false);
    }
    public boolean update(List<TerritoryIndex.Match> matches, boolean showNations, boolean showRegions, boolean showStates) {
        int selectedMode = (showNations ? 1 : 0) | (showRegions ? 2 : 0) | (showStates ? 4 : 0);
        if (mode != selectedMode) { reset(); mode = selectedMode; }
        List<Identity> key = matches.stream()
                .filter(m -> showNations || (showRegions && !m.regions().isEmpty()) || (showStates && !m.states().isEmpty()))
                .map(m -> new Identity(m.nationId(), showRegions
                        ? m.regions().stream().map(TerritoryIndex.Region::id).sorted().toList() : List.of(), showStates ? m.states() : List.of()))
                .sorted(Comparator.comparing(Identity::nationId)).toList();
        if (current == null) { current = key; candidate = null; return false; }
        if (key.equals(current)) { candidate = null; return false; }
        if (!key.equals(candidate)) { candidate = key; return false; }
        current = key; candidate = null; return true;
    }
    public void reset() { current = null; candidate = null; mode = -1; }
}
