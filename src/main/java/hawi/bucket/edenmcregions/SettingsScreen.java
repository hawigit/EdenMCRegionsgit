package hawi.bucket.edenmcregions;

import hawi.bucket.edenmcregions.core.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.screens.Screen;
//? if >=1.21.9
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Client settings with Stonecutter conditions for rendering and mouse APIs. */
public final class SettingsScreen extends Screen {
    protected final EdenMCRegionsClient mod;
    private int left, top, panelWidth;
    private String saveError = "";
    private boolean colorsPage;
    private boolean textPage;
    private ColorSetting selectedColor;
    private ColorWheel wheel;
    private EditBox pickerHex;
    private AbstractSliderButton brightnessSlider;
    private boolean syncingPicker, draggingWheel;
    private Button duration, refresh, refreshMinus, refreshPlus, wrapLimit, wrapMinus, wrapPlus;
    private final List<ColorRow> colors = new ArrayList<>();
    private record ColorSetting(String name, Supplier<String> value, Consumer<String> setter,
                                Supplier<Boolean> map, Consumer<Boolean> setMap, String fallback) {}
    private record ColorRow(ColorSetting setting, int y) {}
    public SettingsScreen(EdenMCRegionsClient mod) {
        super(Component.literal("EdenMCRegions")); this.mod = mod;
    }
    @Override protected void init() {
        panelWidth = Math.min(340, width - 24);
        left = (width - panelWidth) / 2;
        top = Math.max(4, (height - (colorsPage ? 232 : 218)) / 2);
        colors.clear();
        draggingWheel = false;
        if (selectedColor != null) { initPicker(); return; }
        if (colorsPage) { initColors(); return; }
        if (textPage) { initTextSettings(); return; }
        addRenderableWidget(Button.builder(toggle("Mod enabled", mod.config().enabled), b -> {
            mod.config().enabled = !mod.config().enabled;
            b.setMessage(toggle("Mod enabled", mod.config().enabled)); save();
        }).bounds(left + 16, top + 33, panelWidth - 32, 20).build());
        addRenderableWidget(Button.builder(modeLabel("Show Nations", mod.config().nationDisplay), b -> {
            mod.config().nationDisplay = mod.config().nationDisplay.next();
            b.setMessage(modeLabel("Show Nations", mod.config().nationDisplay)); save();
        }).bounds(left + 16, top + 57, panelWidth - 32, 20).build());
        addRenderableWidget(Button.builder(modeLabel("Show Regions", mod.config().regionDisplay), b -> {
            mod.config().regionDisplay = mod.config().regionDisplay.next();
            b.setMessage(modeLabel("Show Regions", mod.config().regionDisplay)); save();
        }).bounds(left + 16, top + 82, panelWidth - 32, 20).build());
        addRenderableWidget(Button.builder(Component.literal("-"), b -> changeDuration(-1))
                .bounds(left + 16, top + 107, 24, 20).build());
        duration = addRenderableWidget(Button.builder(durationLabel(), b -> {}).bounds(left + 44, top + 107, panelWidth - 88, 20).build());
        duration.active = false;
        addRenderableWidget(Button.builder(Component.literal("+"), b -> changeDuration(1))
                .bounds(left + panelWidth - 40, top + 107, 24, 20).build());
        refreshMinus = addRenderableWidget(Button.builder(Component.literal("-1h"), b -> changeRefresh(-1))
                .bounds(left + 16, top + 132, 32, 20).build());
        refresh = addRenderableWidget(Button.builder(refreshLabel(), b -> {}).bounds(left + 52, top + 132, panelWidth - 104, 20).build());
        refresh.active = false;
        refreshPlus = addRenderableWidget(Button.builder(Component.literal("+1h"), b -> changeRefresh(1))
                .bounds(left + panelWidth - 48, top + 132, 32, 20).build());
        updateRefreshButtons();
        int half = (panelWidth - 36) / 2;
        addRenderableWidget(Button.builder(toggle("Coordinates", mod.config().showCoordinates), b -> {
            mod.config().showCoordinates = !mod.config().showCoordinates;
            b.setMessage(toggle("Coordinates", mod.config().showCoordinates)); save();
        }).bounds(left + 16, top + 157, half, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Colors..."), b -> changePage(true))
                .bounds(left + 20 + half, top + 157, half, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Text & states..."), b -> {
            textPage = true; clearWidgets(); init();
        }).bounds(left + 16, top + 185, half, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
                .bounds(left + 20 + half, top + 185, half, 20).build());
    }
    private void initTextSettings() {
        addRenderableWidget(Button.builder(textModeLabel("Nation", mod.config().nationTextMode), b -> {
            mod.config().nationTextMode = mod.config().nationTextMode.next();
            b.setMessage(textModeLabel("Nation", mod.config().nationTextMode)); save();
        }).bounds(left + 16, top + 57, panelWidth - 32, 20).build());
        addRenderableWidget(Button.builder(textModeLabel("Region", mod.config().regionTextMode), b -> {
            mod.config().regionTextMode = mod.config().regionTextMode.next();
            b.setMessage(textModeLabel("Region", mod.config().regionTextMode)); save();
        }).bounds(left + 16, top + 82, panelWidth - 32, 20).build());
        addRenderableWidget(Button.builder(modeLabel("Show states", mod.config().stateDisplay), b -> {
            mod.config().stateDisplay = mod.config().stateDisplay.next();
            b.setMessage(modeLabel("Show states", mod.config().stateDisplay)); save();
        }).bounds(left + 16, top + 107, panelWidth - 32, 20).build());
        wrapMinus = addRenderableWidget(Button.builder(Component.literal("-1"), b -> changeWrapLimit(-1))
                .bounds(left + 16, top + 132, 28, 20).build());
        wrapLimit = addRenderableWidget(Button.builder(wrapLimitLabel(), b -> {})
                .bounds(left + 48, top + 132, panelWidth - 96, 20).build());
        wrapLimit.active = false;
        wrapPlus = addRenderableWidget(Button.builder(Component.literal("+1"), b -> changeWrapLimit(1))
                .bounds(left + panelWidth - 44, top + 132, 28, 20).build());
        updateWrapButtons();
        addRenderableWidget(Button.builder(Component.literal("Back"), b -> {
            textPage = false; clearWidgets(); init();
        }).bounds(left + 16, top + 185, panelWidth - 32, 20).build());
    }
    private Component textModeLabel(String name, TextLayoutMode mode) { return Component.literal(name + " text: " + mode.label()); }
    private Component wrapLimitLabel() { return Component.literal("Wrap after: " + mod.config().wrapCharacters + " characters"); }
    private void changeWrapLimit(int delta) {
        mod.config().wrapCharacters = Math.clamp(mod.config().wrapCharacters + delta, ModConfig.MIN_WRAP_CHARACTERS, ModConfig.MAX_WRAP_CHARACTERS);
        wrapLimit.setMessage(wrapLimitLabel()); updateWrapButtons(); save();
    }
    private void updateWrapButtons() {
        wrapMinus.active = mod.config().wrapCharacters > ModConfig.MIN_WRAP_CHARACTERS;
        wrapPlus.active = mod.config().wrapCharacters < ModConfig.MAX_WRAP_CHARACTERS;
    }
    private void initColors() {
        addColor(new ColorSetting("Nation scoreboard", () -> mod.config().nationScoreboardColor, s -> mod.config().nationScoreboardColor = s,
                () -> mod.config().nationScoreboardMapColor, b -> mod.config().nationScoreboardMapColor = b, "#E5E7EB"), 43);
        addColor(new ColorSetting("Region scoreboard", () -> mod.config().regionScoreboardColor, s -> mod.config().regionScoreboardColor = s,
                () -> mod.config().regionScoreboardMapColor, b -> mod.config().regionScoreboardMapColor = b, "#9CE3C3"), 79);
        addColor(new ColorSetting("Nation title", () -> mod.config().nationTitleColor, s -> mod.config().nationTitleColor = s,
                () -> mod.config().nationTitleMapColor, b -> mod.config().nationTitleMapColor = b, "#F1FFF8"), 115);
        addColor(new ColorSetting("Region title", () -> mod.config().regionTitleColor, s -> mod.config().regionTitleColor = s,
                () -> mod.config().regionTitleMapColor, b -> mod.config().regionTitleMapColor = b, "#9CE3C3"), 151);
        addRenderableWidget(Button.builder(Component.literal("Back"), b -> changePage(false))
                .bounds(left + 16, top + 208, panelWidth - 32, 20).build());
    }
    private void addColor(ColorSetting setting, int y) {
        colors.add(new ColorRow(setting, top + y));
        int fieldWidth = Math.max(50, panelWidth - 250);
        EditBox input = new EditBox(Minecraft.getInstance().font, left + 16, top + y + 12, fieldWidth, 20, Component.literal(setting.name() + " color"));
        input.setMaxLength(7);
        input.setValue(setting.value().get());
        input.setEditable(!setting.map().get()); input.active = !setting.map().get();
        input.setResponder(text -> {
            if (setting.map().get()) return;
            boolean valid = RgbColor.valid(text);
            input.setTextColor(valid ? 0xffe5e7eb : 0xffff7777);
            if (valid) { setting.setter().accept(RgbColor.normalize(text, setting.fallback())); save(); }
        });
        addRenderableWidget(input);
        Button pick = addRenderableWidget(Button.builder(Component.literal("Pick..."), b -> openPicker(setting))
                .bounds(left + 20 + fieldWidth, top + y + 12, 54, 20).build());
        pick.active = !setting.map().get();
        addRenderableWidget(Button.builder(toggle("Map color", setting.map().get()), b -> {
            setting.setMap().accept(!setting.map().get()); save(); clearWidgets(); init();
        }).bounds(left + 78 + fieldWidth, top + y + 12, 100, 20).build());
        Button reset = addRenderableWidget(Button.builder(Component.literal("Reset"), b -> input.setValue(setting.fallback()))
                .bounds(left + panelWidth - 68, top + y + 12, 52, 20).build());
        reset.active = !setting.map().get();
    }
    private void changePage(boolean colorsPage) { save(); this.colorsPage = colorsPage; selectedColor = null; clearWidgets(); init(); }
    private void openPicker(ColorSetting setting) {
        if (setting.map().get()) return;
        selectedColor = setting; wheel = new ColorWheel(setting.value().get()); clearWidgets(); init();
    }
    private void initPicker() {
        int side = left + panelWidth / 2 + 8, sideWidth = panelWidth / 2 - 24;
        brightnessSlider = addRenderableWidget(new AbstractSliderButton(side, top + 98, sideWidth, 20,
                Component.empty(), wheel.brightness()) {
            @Override protected void updateMessage() { setMessage(Component.literal("Brightness: " + Math.round(value * 100) + "%")); }
            @Override protected void applyValue() { wheel.setBrightness(value); applyWheel(); }
        });
        brightnessSlider.setMessage(Component.literal("Brightness: " + Math.round(wheel.brightness() * 100) + "%"));
        pickerHex = new EditBox(Minecraft.getInstance().font, side, top + 136, sideWidth, 20, Component.literal(selectedColor.name() + " color"));
        pickerHex.setMaxLength(7); pickerHex.setValue(wheel.hex());
        pickerHex.setResponder(text -> {
            if (syncingPicker || selectedColor.map().get()) return;
            boolean valid = RgbColor.valid(text);
            pickerHex.setTextColor(valid ? 0xffe5e7eb : 0xffff7777);
            if (valid) {
                selectedColor.setter().accept(RgbColor.normalize(text, selectedColor.fallback())); save();
                wheel.setHex(text);
                // Rebuild the slider with the brightness represented by the entered RGB value.
                removeWidget(brightnessSlider);
                brightnessSlider = addRenderableWidget(new AbstractSliderButton(side, top + 98, sideWidth, 20,
                        Component.literal("Brightness: " + Math.round(wheel.brightness() * 100) + "%"), wheel.brightness()) {
                    @Override protected void updateMessage() { setMessage(Component.literal("Brightness: " + Math.round(value * 100) + "%")); }
                    @Override protected void applyValue() { wheel.setBrightness(value); applyWheel(); }
                });
            }
        });
        addRenderableWidget(pickerHex);
        addRenderableWidget(Button.builder(Component.literal("Reset"), b -> pickerHex.setValue(selectedColor.fallback()))
                .bounds(side, top + 164, sideWidth, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Back"), b -> changePage(true))
                .bounds(left + 16, top + 208, panelWidth - 32, 20).build());
    }
    private int wheelX() { return left + panelWidth / 4 + 4; }
    private int wheelY() { return top + 117; }
    private int wheelRadius() { return Math.min(58, panelWidth / 4 - 12); }
    private void applyWheel() {
        if (selectedColor == null || selectedColor.map().get()) return;
        String hex = wheel.hex(); selectedColor.setter().accept(hex);
        syncingPicker = true;
        try { pickerHex.setValue(hex); pickerHex.setTextColor(0xffe5e7eb); }
        finally { syncingPicker = false; }
    }
    protected boolean pickerClick(double x, double y, int button) {
        if (selectedColor == null || selectedColor.map().get() || button != 0
                || Math.hypot(x - wheelX(), y - wheelY()) > wheelRadius()) return false;
        draggingWheel = true;
        setFocused(null);
        wheel.select(x - wheelX(), y - wheelY(), wheelRadius()); applyWheel();
        return true;
    }
    protected boolean pickerDrag(double x, double y, int button) {
        if (!draggingWheel || selectedColor == null || button != 0) return false;
        wheel.select(x - wheelX(), y - wheelY(), wheelRadius()); applyWheel(); return true;
    }
    protected boolean pickerRelease(int button) {
        if (button != 0) return false;
        boolean handled = draggingWheel; draggingWheel = false;
        if (selectedColor != null) save();
        return handled;
    }
    private void drawPicker(HudCanvas canvas) {
        canvas.centeredScaledText(selectedColor.name(), width / 2, top + 32, 0xffaeb8c3, 1f);
        int radius = wheelRadius(), cx = wheelX(), cy = wheelY();
        for (int y = -radius; y <= radius; y += 2) for (int x = -radius; x <= radius; x += 2) {
            double distance = Math.hypot(x, y);
            if (distance <= radius) canvas.fill(cx + x, cy + y, cx + x + 2, cy + y + 2,
                    ColorWheel.rgb(Math.atan2(y, x) / (2 * Math.PI), distance / radius, wheel.brightness()));
        }
        int px = cx + (int)Math.round(Math.cos(wheel.hue() * 2 * Math.PI) * wheel.saturation() * radius);
        int py = cy + (int)Math.round(Math.sin(wheel.hue() * 2 * Math.PI) * wheel.saturation() * radius);
        canvas.fill(px - 3, py - 3, px + 4, py + 4, 0xff000000);
        canvas.fill(px - 2, py - 2, px + 3, py + 3, 0xffffffff);
        canvas.fill(px - 1, py - 1, px + 2, py + 2, RgbColor.argb(wheel.hex()));
        int side = left + panelWidth / 2 + 8;
        canvas.fill(side, top + 52, left + panelWidth - 16, top + 84, RgbColor.argb(wheel.hex()));
        canvas.text("Brightness", side, top + 87, 0xffe5e7eb);
        canvas.text("Hex color (optional)", side, top + 125, 0xffaeb8c3);
    }
    private Component toggle(String name, boolean enabled) { return Component.literal(name + ": " + (enabled ? "ON" : "OFF")); }
    private Component modeLabel(String name, DisplayMode mode) { return Component.literal(name + ": " + mode.label()); }
    private Component durationLabel() { return Component.literal("Title: " + mod.config().titleSeconds + " seconds"); }
    private Component refreshLabel() { return Component.literal("Border updates: every " + mod.config().refreshSeconds / 3600 + " hours"); }
    private void changeDuration(int delta) {
        mod.config().titleSeconds = Math.clamp(mod.config().titleSeconds + delta, 1, 30);
        duration.setMessage(durationLabel()); save();
    }
    private void changeRefresh(int deltaHours) {
        mod.config().refreshSeconds = TerritoryService.clampRefreshSeconds(mod.config().refreshSeconds + deltaHours * 3600);
        refresh.setMessage(refreshLabel()); updateRefreshButtons(); save();
    }
    private void updateRefreshButtons() {
        refreshMinus.active = mod.config().refreshSeconds > TerritoryService.MIN_REFRESH_SECONDS;
        refreshPlus.active = mod.config().refreshSeconds < TerritoryService.MAX_REFRESH_SECONDS;
    }
    private void save() { saveError = mod.savePreferences() ? "" : "Could not save settings. Check latest.log."; }
    protected void drawPanel(HudCanvas canvas) {
        // Screen already draws Minecraft's vanilla menu background before this content.
        canvas.centeredScaledText("EdenMCRegions", width / 2, top + 15, 0xffffffff, 1f);
        if (textPage) canvas.centeredScaledText("Display options", width / 2, top + 32, 0xffffffff, 1f);
        if (selectedColor != null) {
            drawPicker(canvas);
        } else if (colorsPage) {
            for (ColorRow row : colors) {
                String label = row.setting().name() + (row.setting().map().get() ? " (from map)" : "");
                canvas.text(label, left + 16, row.y(), row.setting().map().get() ? 0xffe5e7eb : RgbColor.argb(row.setting().value().get()));
            }
        }
        String info = saveError;
        info = Minecraft.getInstance().font.plainSubstrByWidth(info, panelWidth - 20);
        canvas.centeredScaledText(info, width / 2, top + (selectedColor != null ? 43 : colorsPage ? 194 : 208), saveError.isEmpty() ? 0xffaeb8c3 : 0xffff9988, 1f);
    }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void onClose() { save(); HudPlatform.setScreen(null); }

    //? if >=26.1 {
    @Override public void extractRenderState(net.minecraft.client.gui.GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        drawPanel(HudPlatform.canvas(graphics));
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }
    //?} else {
    /*@Override public void render(net.minecraft.client.gui.GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        drawPanel(HudPlatform.canvas(graphics));
        super.render(graphics, mouseX, mouseY, delta);
    }
    *///?}

    //? if >=1.21.9 {
    @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        return pickerClick(event.x(), event.y(), event.button()) || super.mouseClicked(event, doubleClick);
    }
    @Override public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        return pickerDrag(event.x(), event.y(), event.button()) || super.mouseDragged(event, dx, dy);
    }
    @Override public boolean mouseReleased(MouseButtonEvent event) {
        return pickerRelease(event.button()) || super.mouseReleased(event);
    }
    //?} else {
    /*@Override public boolean mouseClicked(double x, double y, int button) {
        return pickerClick(x, y, button) || super.mouseClicked(x, y, button);
    }
    @Override public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        return pickerDrag(x, y, button) || super.mouseDragged(x, y, button, dx, dy);
    }
    @Override public boolean mouseReleased(double x, double y, int button) {
        return pickerRelease(button) || super.mouseReleased(x, y, button);
    }
    *///?}
}
