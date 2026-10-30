package hawi.bucket.edenmcregions.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

public final class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public int configVersion = 5;
    public boolean enabled = true;
    public DisplayMode nationDisplay = DisplayMode.TITLE_AND_SCOREBOARD;
    public DisplayMode regionDisplay = DisplayMode.TITLE_AND_SCOREBOARD;
    public DisplayMode stateDisplay = DisplayMode.TITLE_AND_SCOREBOARD;
    public TextLayoutMode nationTextMode = TextLayoutMode.FULL_NAME;
    public TextLayoutMode regionTextMode = TextLayoutMode.FULL_NAME;
    public static final int MIN_WRAP_CHARACTERS = 5, MAX_WRAP_CHARACTERS = 120;
    public int wrapCharacters = 30;
    public int titleSeconds = 4;
    public boolean showCoordinates = false;
    public boolean showInSingleplayer = false;
    public int refreshSeconds = 86400;
    public String nationScoreboardColor = "#E5E7EB";
    public String regionScoreboardColor = "#9CE3C3";
    public String nationTitleColor = "#F1FFF8";
    public String regionTitleColor = "#9CE3C3";
    public boolean nationScoreboardMapColor = false;
    public boolean regionScoreboardMapColor = false;
    public boolean nationTitleMapColor = false;
    public boolean regionTitleMapColor = false;
    public List<String> servers = new ArrayList<>(List.of("play.edenmc.world", "edenmc.world"));

    public static ModConfig load(Path path) throws IOException {
        boolean exists = Files.exists(path);
        JsonObject saved = exists ? JsonParser.parseString(Files.readString(path)).getAsJsonObject() : null;
        ModConfig config = exists ? GSON.fromJson(saved, ModConfig.class) : new ModConfig();
        if (config == null) throw new IOException("Config is empty");
        boolean migrate = exists && (!saved.has("configVersion") || config.configVersion < 5);
        if (migrate) {
            boolean titles = legacyBoolean(saved, "entryNotifications", true);
            boolean scoreboard = legacyBoolean(saved, "scoreboardLocation", true);
            boolean regions = legacyBoolean(saved, "showRegions", true);
            if (!saved.has("nationDisplay")) config.nationDisplay = DisplayMode.from(titles, scoreboard);
            if (!saved.has("regionDisplay")) config.regionDisplay = regions ? DisplayMode.from(titles, scoreboard) : DisplayMode.OFF;
            if (!saved.has("titleSeconds") && saved.has("announcementSeconds")) config.titleSeconds = saved.get("announcementSeconds").getAsInt();
            config.configVersion = 5;
        }
        int requestedRefresh = config.refreshSeconds;
        config.normalize();
        if (!exists || migrate || requestedRefresh != config.refreshSeconds) config.save(path);
        return config;
    }
    private static boolean legacyBoolean(JsonObject saved, String key, boolean fallback) {
        return saved.has(key) && !saved.get(key).isJsonNull() ? saved.get(key).getAsBoolean() : fallback;
    }
    public boolean hasTitles() { return nationDisplay.title() || regionDisplay.title() || stateDisplay.title(); }
    public boolean hasScoreboard() { return nationDisplay.scoreboard() || regionDisplay.scoreboard() || stateDisplay.scoreboard(); }
    public void normalize() {
        if (nationDisplay == null) nationDisplay = DisplayMode.TITLE_AND_SCOREBOARD;
        if (regionDisplay == null) regionDisplay = DisplayMode.TITLE_AND_SCOREBOARD;
        if (stateDisplay == null) stateDisplay = DisplayMode.TITLE_AND_SCOREBOARD;
        if (nationTextMode == null) nationTextMode = TextLayoutMode.FULL_NAME;
        if (regionTextMode == null) regionTextMode = TextLayoutMode.FULL_NAME;
        wrapCharacters = Math.clamp(wrapCharacters, MIN_WRAP_CHARACTERS, MAX_WRAP_CHARACTERS);
        titleSeconds = Math.clamp(titleSeconds, 1, 30);
        refreshSeconds = (TerritoryService.clampRefreshSeconds(refreshSeconds) / 3600) * 3600;
        nationScoreboardColor = RgbColor.normalize(nationScoreboardColor, "#E5E7EB");
        regionScoreboardColor = RgbColor.normalize(regionScoreboardColor, "#9CE3C3");
        nationTitleColor = RgbColor.normalize(nationTitleColor, "#F1FFF8");
        regionTitleColor = RgbColor.normalize(regionTitleColor, "#9CE3C3");
        servers = servers == null ? new ArrayList<>() : new ArrayList<>(servers.stream()
                .filter(Objects::nonNull).map(ModConfig::host).filter(s -> !s.isBlank()).distinct().toList());
    }
    public boolean accepts(String address, boolean singleplayer) {
        return singleplayer ? showInSingleplayer : servers.contains(host(address));
    }
    public static String host(String address) {
        if (address == null) return "";
        String value = address.strip().toLowerCase(Locale.ROOT);
        if (value.startsWith("[")) { int end = value.indexOf(']'); return end >= 0 ? value.substring(0, end + 1) : value; }
        int colon = value.indexOf(':');
        return colon >= 0 && value.indexOf(':', colon + 1) < 0 ? value.substring(0, colon) : value;
    }
    public void save(Path path) throws IOException {
        normalize(); Files.createDirectories(path.getParent());
        Path temp = path.resolveSibling(path.getFileName() + ".tmp");
        Files.writeString(temp, GSON.toJson(this));
        try { Files.move(temp, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
        catch (AtomicMoveNotSupportedException e) { Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING); }
    }
}


