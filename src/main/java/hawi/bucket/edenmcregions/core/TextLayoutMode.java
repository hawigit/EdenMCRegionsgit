package hawi.bucket.edenmcregions.core;

public enum TextLayoutMode {
    FULL_NAME("Full name"), TRUNCATE("Truncate"), WRAP("Wrap");
    private final String label;
    TextLayoutMode(String label) { this.label = label; }
    public String label() { return label; }
    public TextLayoutMode next() { return values()[(ordinal() + 1) % values().length]; }
}
