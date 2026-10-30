package hawi.bucket.edenmcregions.core;

/** Frame-independent fade: a short fade-in, a readable hold, then a fade-out. */
public final class Announcement {
    private String text = "";
    private long started;
    private double seconds;
    public void show(String text, long nowNanos, int durationSeconds) {
        this.text = text; this.started = nowNanos; this.seconds = Math.max(1, durationSeconds);
    }
    public String text() { return text; }
    public void clear() { text = ""; }
    public float alpha(long nowNanos) {
        if (text.isEmpty()) return 0;
        double elapsed = (nowNanos - started) / 1_000_000_000.0;
        if (elapsed < 0 || elapsed >= seconds) return 0;
        double fadeOut = Math.min(1, seconds / 2);
        return (float)Math.max(0, Math.min(1, Math.min(elapsed / .2, (seconds - elapsed) / fadeOut)));
    }
}


