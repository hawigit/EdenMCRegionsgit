package hawi.bucket.edenmcregions.core;

import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.FileTime;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;

/** Network and cache work stays on a daemon worker, independent of the game/render thread. */
public final class TerritoryService implements AutoCloseable {
    public static final String API = "https://core.edenmc.lol/api/nations?map=";
    public static final int MAX_BYTES = 8 * 1024 * 1024;
    public static final int MIN_REFRESH_SECONDS = 6 * 3600;
    public static final int MAX_REFRESH_SECONDS = 24 * 3600;
    public static int clampRefreshSeconds(int seconds) { return Math.clamp(seconds, MIN_REFRESH_SECONDS, MAX_REFRESH_SECONDS); }
    public record State(TerritoryIndex index, Instant updated, boolean loading, String error) {
        public boolean stale(Instant now, int refreshSeconds) {
            return index != null && (!error.isEmpty() || !now.isBefore(updated.plusSeconds(clampRefreshSeconds(refreshSeconds))));
        }
    }
    @FunctionalInterface public interface Transport { String fetch(String dimension) throws Exception; }
    private final Path cacheDir;
    private final Transport transport;
    private final Clock clock;
    private final ExecutorService worker = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "EdenMCRegions-map"); t.setDaemon(true); return t;
    });
    private final Map<String, State> states = new ConcurrentHashMap<>();
    private final Map<String, Instant> attempts = new HashMap<>();

    public TerritoryService(Path cacheDir) { this(cacheDir, httpTransport(), Clock.systemUTC()); }
    public TerritoryService(Path cacheDir, Transport transport, Clock clock) {
        this.cacheDir = cacheDir; this.transport = transport; this.clock = clock;
        for (String dimension : List.of("overworld", "nether")) {
            State state = new State(null, Instant.EPOCH, false, "");
            Path path = cacheDir.resolve(dimension + ".json");
            try {
                if (Files.isRegularFile(path) && Files.size(path) <= MAX_BYTES) {
                    state = new State(TerritoryIndex.parse(Files.readString(path), dimension),
                            Files.getLastModifiedTime(path).toInstant(), false, "");
                }
            } catch (Exception ignored) { state = new State(null, Instant.EPOCH, false, "Cache could not be read"); }
            states.put(dimension, state);
            try {
                Path lastRequest = cacheDir.resolve(dimension + ".last-request");
                if (Files.isRegularFile(lastRequest)) attempts.put(dimension, Instant.parse(Files.readString(lastRequest).strip()));
            } catch (Exception ignored) {
                // A damaged schedule is treated conservatively without discarding usable borders.
                attempts.put(dimension, clock.instant());
            }
        }
    }
    public State state(String dimension) {
        return states.getOrDefault(dimension, new State(null, Instant.EPOCH, false, "No map for this dimension"));
    }
    public synchronized CompletableFuture<Void> refresh(String dimension, int seconds) {
        if (!states.containsKey(dimension)) return CompletableFuture.completedFuture(null);
        State old = state(dimension);
        Instant now = clock.instant(), lastAttempt = attempts.getOrDefault(dimension, Instant.EPOCH);
        if (old.loading) return CompletableFuture.completedFuture(null);
        int interval = clampRefreshSeconds(seconds);
        // Both successful cache age and attempted requests survive restarts. Reload has no bypass.
        if (old.index != null && now.isBefore(old.updated.plusSeconds(interval)))
            return CompletableFuture.completedFuture(null);
        if (now.isBefore(lastAttempt.plusSeconds(interval))) return CompletableFuture.completedFuture(null);
        attempts.put(dimension, now);
        states.put(dimension, new State(old.index, old.updated, true, old.error));
        return CompletableFuture.runAsync(() -> {
            try {
                // Persist BEFORE contacting the server, including when that request later fails.
                writeAtomic(cacheDir.resolve(dimension + ".last-request"), now.toString());
                String response = transport.fetch(dimension);
                if (response.length() > MAX_BYTES) throw new IOException("Map response is too large");
                TerritoryIndex index = TerritoryIndex.parse(response, dimension);
                String cacheError = "";
                try { save(dimension, index.cacheJson()); }
                catch (IOException e) { cacheError = "Map loaded; disk cache could not be saved"; }
                states.put(dimension, new State(index, clock.instant(), false, cacheError));
            } catch (Exception e) {
                if (e instanceof InterruptedException) Thread.currentThread().interrupt();
                states.put(dimension, new State(old.index, old.updated, false,
                        "Map refresh failed (" + e.getClass().getSimpleName() + ")"));
            }
        }, worker);
    }
    public synchronized Instant nextRefresh(String dimension, int seconds) {
        long interval = clampRefreshSeconds(seconds);
        Instant due = attempts.getOrDefault(dimension, Instant.EPOCH).plusSeconds(interval);
        State state = state(dimension);
        if (state.index != null && state.updated.plusSeconds(interval).isAfter(due)) due = state.updated.plusSeconds(interval);
        return due;
    }
    private void save(String dimension, String json) throws IOException {
        writeAtomic(cacheDir.resolve(dimension + ".json"), json);
    }
    private void writeAtomic(Path target, String contents) throws IOException {
        Files.createDirectories(cacheDir);
        Path temp = target.resolveSibling(target.getFileName() + ".tmp");
        Files.writeString(temp, contents, StandardCharsets.UTF_8);
        Files.setLastModifiedTime(temp, FileTime.from(clock.instant()));
        try { Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
        catch (AtomicMoveNotSupportedException e) { Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING); }
    }
    private static Transport httpTransport() {
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        return dimension -> {
            HttpRequest request = HttpRequest.newBuilder(URI.create(API + dimension))
                    .timeout(Duration.ofSeconds(20)).header("Accept", "application/json")
                    .header("User-Agent", "EdenMCRegions/1.4.0 (Minecraft client map HUD)").GET().build();
            // Bound the full body, including a stalled stream after response headers arrive.
            CompletableFuture<HttpResponse<byte[]>> pending = client.sendAsync(request, info -> new LimitedBodySubscriber(MAX_BYTES));
            HttpResponse<byte[]> response;
            try { response = pending.get(20, TimeUnit.SECONDS); }
            catch (Exception e) { pending.cancel(true); throw e; }
            if (response.statusCode() != 200) throw new IOException("HTTP " + response.statusCode());
            return new String(response.body(), StandardCharsets.UTF_8);
        };
    }
    @Override public void close() { worker.shutdownNow(); }
}






