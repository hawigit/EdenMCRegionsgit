package hawi.bucket.edenmcregions.core;

/** Independent destinations for a type of location information. */
public enum DisplayMode {
    TITLE_AND_SCOREBOARD("Title + Scoreboard", true, true),
    ONLY_SCOREBOARD("Only Scoreboard", false, true),
    ONLY_TITLE("Only Title", true, false),
    OFF("Off", false, false);

    private final String label;
    private final boolean title, scoreboard;
    DisplayMode(String label, boolean title, boolean scoreboard) {
        this.label = label; this.title = title; this.scoreboard = scoreboard;
    }
    public String label() { return label; }
    public boolean title() { return title; }
    public boolean scoreboard() { return scoreboard; }
    public DisplayMode next() { return values()[(ordinal() + 1) % values().length]; }
    public static DisplayMode from(boolean title, boolean scoreboard) {
        for (DisplayMode mode : values()) if (mode.title == title && mode.scoreboard == scoreboard) return mode;
        throw new AssertionError("Unreachable display combination");
    }
}
