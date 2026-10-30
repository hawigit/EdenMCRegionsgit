package hawi.bucket.edenmcregions;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import hawi.bucket.edenmcregions.core.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;

public final class EdenMCRegionsClient implements ClientModInitializer {
    private static final Logger LOG = LoggerFactory.getLogger("EdenMCRegions");
    private final Path configPath = FabricLoader.getInstance().getConfigDir().resolve("eden-territories.json");
    private final LocationTracker tracker = new LocationTracker();
    private final Announcement announcement = new Announcement();
    private TitleText titleText = new TitleText(List.of());
    private List<TerritoryState> titleStates = List.of();
    private ModConfig config;
    private TerritoryService service;
    private String context = "";
    private int ticks;
    private boolean settingsRequested;
    private DisplayMode lastNationDisplay, lastRegionDisplay, lastStateDisplay;
    private net.minecraft.world.scores.DisplaySlot hiddenSlot;
    private net.minecraft.world.scores.Objective hiddenObjective;
    private List<Line> lines = List.of();
    private record Line(String text, int color, TextLayoutMode mode) {
        private Line(String text, int color) { this(text, color, TextLayoutMode.WRAP); }
    }

    @Override public void onInitializeClient() {
        config = new ModConfig();
        loadConfig();
        service = new TerritoryService(FabricLoader.getInstance().getConfigDir().resolve("eden-territories-cache"));
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> service.close());
        HudPlatform.register(this::render, this::restore);
        registerCommands();
        LOG.info("EdenMCRegions initialized; automatic requests are limited to once per {} seconds per dimension", config.refreshSeconds);
    }
    private void restore() {
        Minecraft client = Minecraft.getInstance();
        if (hiddenSlot != null && hiddenObjective != null && client.level != null) {
            client.level.getScoreboard().setDisplayObjective(hiddenSlot, hiddenObjective);
            hiddenSlot = null;
            hiddenObjective = null;
        }
    }
    public ModConfig config() { return config; }
    private boolean loadConfig() {
        try { config = ModConfig.load(configPath); return true; }
        catch (Exception e) { LOG.warn("Cannot load {}. Keeping current/default settings.", configPath, e); return false; }
    }
    public boolean savePreferences() {
        syncDisplayModes();
        try { config.save(configPath); return true; }
        catch (Exception e) { LOG.warn("Cannot save configuration", e); return false; }
    }

    private void syncDisplayModes() {
        if (config.nationDisplay != lastNationDisplay || config.regionDisplay != lastRegionDisplay || config.stateDisplay != lastStateDisplay) {
            tracker.reset(); announcement.clear(); lines = List.of();
            lastNationDisplay = config.nationDisplay; lastRegionDisplay = config.regionDisplay;
            lastStateDisplay = config.stateDisplay;
        }
    }
    private String dimension(Minecraft client) {
        if (client.level == null) return "";
        if (client.level.dimension().equals(Level.OVERWORLD)) return "overworld";
        if (client.level.dimension().equals(Level.NETHER)) return "nether";
        return "unsupported";
    }
    private String server(Minecraft client) { return client.getCurrentServer() == null ? "" : client.getCurrentServer().ip; }
    private void tick(Minecraft client) {
        // Opening on the following tick avoids the chat screen closing over the settings screen.
        if (settingsRequested) { settingsRequested = false; HudPlatform.setScreen(new SettingsScreen(this)); }
        syncDisplayModes();
        if (++ticks % 4 != 0) return;
        if (!config.enabled || (!config.hasScoreboard() && !config.hasTitles())
                || client.player == null || client.level == null
                || !config.accepts(server(client), client.hasSingleplayerServer())) {
            lines = List.of(); context = ""; tracker.reset(); announcement.clear(); return;
        }
        String dim = dimension(client), nextContext = server(client) + "/" + dim;
        if (!nextContext.equals(context)) { context = nextContext; tracker.reset(); announcement.clear(); }
        List<Line> next = new ArrayList<>();
        if (dim.equals("unsupported")) {
            next.add(new Line("No map for this dimension", 0xffaeb8c3));
            lines = List.copyOf(next); return;
        }
        service.refresh(dim, config.refreshSeconds);
        TerritoryService.State state = service.state(dim);
        if (state.index() == null) {
            next.add(new Line(state.loading() ? "Loading map..." : "Map unavailable", 0xffffcc77));
        } else {
            List<TerritoryIndex.Match> matches = state.index().locate(client.player.getX(), client.player.getZ());
            if (matches.isEmpty() && (config.nationDisplay.scoreboard() || config.regionDisplay.scoreboard())) next.add(new Line(config.nationDisplay.scoreboard() ? "Nation: Unclaimed" : "Region: Unclaimed", RgbColor.argb(config.nationDisplay.scoreboard() ? config.nationScoreboardColor : config.regionScoreboardColor), config.nationDisplay.scoreboard() ? config.nationTextMode : config.regionTextMode));
            else {
                if (matches.size() > 1) next.add(new Line("Overlapping claims", 0xffffcc77));
                for (TerritoryIndex.Match match : matches.stream().limit(4).toList()) {
                    if (config.nationDisplay.scoreboard()) next.add(new Line("Nation: " + match.nation(), config.nationScoreboardMapColor ? match.color() : RgbColor.argb(config.nationScoreboardColor), config.nationTextMode));
                    List<String> regions = match.regions().stream()
                            .map(r -> TerritoryIndex.formatRegion(match.nation(), r))
                            .filter(s -> !s.isBlank())
                            .distinct()
                            .toList();
                    if (config.regionDisplay.scoreboard() && !regions.isEmpty()) {
                        if (config.regionScoreboardMapColor) {
                            java.util.Set<String> seen = new java.util.HashSet<>();
                            for (var region : match.regions()) {
                                String name = TerritoryIndex.formatRegion(match.nation(), region);
                                if (!name.isBlank() && seen.add(name)) next.add(new Line("Region: " + name, region.color(), config.regionTextMode));
                            }
                        } else next.add(new Line("Region: " + String.join(", ", regions), RgbColor.argb(config.regionScoreboardColor), config.regionTextMode));
                    }
                    if (config.stateDisplay.scoreboard()) for (TerritoryState stateBadge : match.states())
                        next.add(new Line(stateBadge.label(), stateBadge.color()));
                }
                if (matches.size() > 4) next.add(new Line("More: /regions status", 0xffaeb8c3));
            }
            if (tracker.update(matches, config.nationDisplay.title(), config.regionDisplay.title(), config.stateDisplay.title()) && config.hasTitles()) {
                titleText = TitleText.create(matches, config.nationDisplay.title(), config.regionDisplay.title());
                titleStates = config.stateDisplay.title() ? matches.stream().flatMap(m -> m.states().stream()).distinct().sorted().toList() : List.of();
                announcement.show(titleText.text() + titleStates.stream().map(TerritoryState::label).reduce("", String::concat), System.nanoTime(), config.titleSeconds);
            }
            if (state.stale(Instant.now(), config.refreshSeconds)) next.add(new Line("Using cached borders", 0xffffcc77));
        }
        if (config.showCoordinates) next.add(new Line("X " + (int)Math.floor(client.player.getX()) + "  Z " + (int)Math.floor(client.player.getZ()), 0xffaeb8c3));
        lines = List.copyOf(next);
    }
    private void render(HudCanvas canvas) {
        Minecraft client = Minecraft.getInstance();
        // The registry attachment inherits vanilla's F1 HUD visibility condition.
        if (!config.enabled || client.player == null || HudPlatform.screenOpen()) return;
        if (config.hasScoreboard() && !lines.isEmpty()) renderSidebar(canvas);
        if (config.hasTitles()) renderTitle(canvas);
    }
    private void renderSidebar(HudCanvas canvas) {
        Minecraft client = Minecraft.getInstance();
        SidebarLayout layout = SidebarLayout.measure(canvas.height());
        int fullNameWidth = lines.stream().filter(l -> l.mode == TextLayoutMode.FULL_NAME)
                .mapToInt(l -> client.font.width(l.text)).max().orElse(0);
        int normalWidth = layout.present() ? Math.max(100, layout.width()) : 180;
        int wrapWidth = lines.stream().filter(l -> l.mode == TextLayoutMode.WRAP)
                .flatMap(l -> SidebarText.wrapCharacters(l.text, config.wrapCharacters).stream())
                .mapToInt(client.font::width).max().orElse(normalWidth);
        int targetWidth = Math.max(normalWidth, Math.max(fullNameWidth, wrapWidth));
        int width = Math.max(20, Math.min(canvas.width() - 10, targetWidth));
        int left = canvas.width() - width - 3;
        int headerHeight = 11;

        int serverRowsCount = layout.present() ? layout.rows().size() : 0;
        int top = layout.present() ? layout.headerTop() : (canvas.height() / 2 - ((lines.size() + 1) * 9 + headerHeight) / 2);
        int room = (canvas.height() - 6 - top - headerHeight - serverRowsCount * 9) / 9;
        if (room < 1) return; // Never overlap server rows to force our footer onto a tiny screen.

        List<Line> wrapped = new ArrayList<>();
        for (Line line : lines) {
            int lineWidth = line.mode == TextLayoutMode.TRUNCATE ? Math.min(width, normalWidth) : width;
            for (String part : SidebarText.format(line.text, line.mode, lineWidth, Math.max(1, room + 1 - wrapped.size()), config.wrapCharacters,
                    client.font::width, (text, available) -> client.font.plainSubstrByWidth(text, available)))
                wrapped.add(new Line(part, line.color));
            if (wrapped.size() > room) break;
        }
        boolean overflow = wrapped.size() > room;
        List<Line> shown = new ArrayList<>(wrapped.subList(0, Math.min(room, wrapped.size())));
        if (overflow) shown.set(shown.size() - 1, new Line(client.font.plainSubstrByWidth("... /regions status", width), 0xffaeb8c3));

        if (layout.present()) {
            hiddenSlot = layout.slot();
            hiddenObjective = layout.objective();
            client.level.getScoreboard().setDisplayObjective(hiddenSlot, null);

            // 1. Draw Server Header centered across the unified width:
            canvas.fill(left - 2, layout.headerTop(), canvas.width() - 1, layout.headerBottom(), client.options.getBackgroundColor(.4f));
            canvas.component(layout.title(), left + (width - layout.titleWidth()) / 2, layout.headerTop() + 1, 0xffffffff);

            // 2. Draw Server Rows anchored to the LEFT:
            canvas.fill(left - 2, layout.headerBottom(), canvas.width() - 1, layout.bottom(), client.options.getBackgroundColor(.3f));
            for (int j = 0; j < layout.rows().size(); j++) {
                var row = layout.rows().get(j);
                int rowY = layout.bottom() - (layout.rows().size() - j) * 9;
                canvas.component(row.name(), left, rowY, 0xffffffff);
                if (row.scoreWidth() > 0) {
                    canvas.component(row.score(), canvas.width() - 3 - row.scoreWidth(), rowY, 0xffff5555);
                }
            }
        } else {
            canvas.fill(left - 2, top, canvas.width() - 1, top + 11, client.options.getBackgroundColor(.4f));
            String title = client.font.plainSubstrByWidth("Location", width);
            canvas.text(title, left + (width - client.font.width(title)) / 2, top + 1, 0xff9ce3c3);
        }

        // 3. Draw Location Rows anchored to the LEFT:
        int bodyTop = layout.present() ? layout.bottom() : (top + headerHeight);
        canvas.fill(left - 2, bodyTop, canvas.width() - 1, bodyTop + shown.size() * 9, client.options.getBackgroundColor(.3f));
        for (int i = 0; i < shown.size(); i++) {
            canvas.text(shown.get(i).text, left, bodyTop + i * 9, shown.get(i).color);
        }
    }
    private void renderTitle(HudCanvas canvas) {
        float alpha = announcement.alpha(System.nanoTime());
        int opacity = Math.round(alpha * 255);
        if (opacity < 4) return;
        var font = Minecraft.getInstance().font;
        float scale = 1.5f;
        int maxWidth = Math.max(20, (int)((canvas.width() - 32) / scale));
        var label = net.minecraft.network.chat.Component.empty();
        boolean truncated = font.width(titleText.text()) > maxWidth;
        int remaining = truncated ? maxWidth - font.width("...") : maxWidth;
        for (var span : titleText.spans()) {
            String part = font.plainSubstrByWidth(span.text(), remaining);
            int rgb = switch (span.kind()) {
                case NATION -> config.nationTitleMapColor && span.mapColor() != null ? span.mapColor() : RgbColor.argb(config.nationTitleColor);
                case REGION -> config.regionTitleMapColor && span.mapColor() != null ? span.mapColor() : RgbColor.argb(config.regionTitleColor);
                case SEPARATOR -> 0xffaeb8c3;
            };
            if (!part.isEmpty()) label.append(Component.literal(part).withStyle(style -> style.withColor(rgb & 0xffffff)));
            remaining -= font.width(part);
            if (part.length() < span.text().length()) break;
        }
        if (truncated) label.append(Component.literal("...").withStyle(style -> style.withColor(0xaeb8c3)));
        canvas.centeredScaledText("ENTERING", canvas.width() / 2, 18, (opacity << 24) | 0xa5c9b9, 1f);
        canvas.centeredScaledComponent(label, canvas.width() / 2, 31, (opacity << 24) | 0xffffff, scale);
        if (!titleStates.isEmpty()) {
            var badges = Component.empty();
            for (TerritoryState state : titleStates) {
                if (!badges.getString().isEmpty()) badges.append(" ");
                badges.append(Component.literal(state.label()).withStyle(style -> style.withColor(state.color() & 0xffffff)));
            }
            float badgeScale = Math.min(1f, (canvas.width() - 32f) / Math.max(1, font.width(badges)));
            canvas.centeredScaledComponent(badges, canvas.width() / 2, titleText.text().isEmpty() ? 31 : 50,
                    (opacity << 24) | 0xffffff, badgeScale);
        }
    }
    private int requestSettings() { settingsRequested = true; return 1; }
    private void registerCommands() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, access) -> dispatcher.register(literal("regions")
                .then(literal("settings").executes(c -> requestSettings()))
                .then(literal("status").executes(c -> status()))
                .then(literal("reload").executes(c -> {
                    boolean loaded = loadConfig();
                    service.refresh(dimension(Minecraft.getInstance()), config.refreshSeconds);
                    feedback(loaded ? "Settings reloaded. The configured update interval still applies (minimum 6 hours)." : "Config invalid; kept current settings.");
                    return loaded ? 1 : 0;
                }))
        ));
    }
    private static LiteralArgumentBuilder<FabricClientCommandSource> literal(String name) { return LiteralArgumentBuilder.literal(name); }
    private int status() {
        Minecraft client = Minecraft.getInstance();
        TerritoryService.State state = service.state(dimension(client));
        String location = "Unavailable";
        if (state.index() != null && client.player != null) {
            List<TerritoryIndex.Match> matches = state.index().locate(client.player.getX(), client.player.getZ());
            location = matches.isEmpty() ? "Unclaimed" : String.join(" + ", matches.stream().map(TerritoryIndex.Match::label).toList());
        }
        feedback("Current region: " + location);
        feedback("Last updated: " + (state.index() == null ? "Never" : state.updated().toString()));
        return 1;
    }
    private void feedback(String message) {
        if (Minecraft.getInstance().player != null) HudPlatform.feedback(Component.literal("[EdenMCRegions] " + message));
    }
}






