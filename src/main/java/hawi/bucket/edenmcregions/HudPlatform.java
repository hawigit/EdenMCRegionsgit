package hawi.bucket.edenmcregions;

import java.util.function.Consumer;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

/** Small rendering differences are preprocessed by Stonecutter for each target. */
public final class HudPlatform {
    public static net.minecraft.world.scores.DisplaySlot teamSlot(net.minecraft.world.scores.PlayerTeam team) {
        //? if >=26.2 {
        /*return team.getColor().map(net.minecraft.world.scores.TeamColor::displaySlot).orElse(null);
        *///?} else {
        return net.minecraft.world.scores.DisplaySlot.teamColorToSlot(team.getColor());
        //?}
    }
    public static void setScreen(net.minecraft.client.gui.screens.Screen screen) {
        //? if >=26.2 {
        /*Minecraft.getInstance().gui.setScreen(screen);
        *///?} else {
        Minecraft.getInstance().setScreen(screen);
        //?}
    }
    public static boolean screenOpen() {
        //? if >=26.2 {
        /*return Minecraft.getInstance().gui.screen() != null;
        *///?} else {
        return Minecraft.getInstance().screen != null;
        //?}
    }
    public static void feedback(net.minecraft.network.chat.Component message) {
        //? if >=26.2 {
        /*Minecraft.getInstance().gui.hud.getChat().addClientSystemMessage(message);
        *///?} else if >=26.1 {
        Minecraft.getInstance().gui.getChat().addClientSystemMessage(message);
        //?} else {
        /*Minecraft.getInstance().gui.getChat().addMessage(message);
        *///?}
    }
    public static void register(Consumer<HudCanvas> draw, Runnable restore) {
        HudElementRegistry.attachElementBefore(VanillaHudElements.SCOREBOARD,
                Identifier.fromNamespaceAndPath("edenmcregions", "territory"), (graphics, delta) -> draw.accept(canvas(graphics)));
        HudElementRegistry.attachElementAfter(VanillaHudElements.SCOREBOARD,
                Identifier.fromNamespaceAndPath("edenmcregions", "restore"), (graphics, delta) -> restore.run());
    }
    public static HudCanvas canvas(net.minecraft.client.gui.GuiGraphicsExtractor graphics) {
        return new HudCanvas() {
            public int width() { return graphics.guiWidth(); }
            public int height() { return graphics.guiHeight(); }
            public void fill(int l, int t, int r, int b, int color) { graphics.fill(l, t, r, b, color); }
            public void text(String text, int x, int y, int color) { graphics.text(Minecraft.getInstance().font, text, x, y, color, true); }
            public void component(net.minecraft.network.chat.Component text, int x, int y, int color) { graphics.text(Minecraft.getInstance().font, text, x, y, color, true); }
            public void centeredScaledComponent(net.minecraft.network.chat.Component text, int centerX, int y, int color, float scale) {
                graphics.pose().pushMatrix();
                graphics.pose().scale(scale, scale);
                component(text, Math.round(centerX / scale - Minecraft.getInstance().font.width(text) / 2f), Math.round(y / scale), color);
                graphics.pose().popMatrix();
            }
            public void centeredScaledText(String text, int centerX, int y, int color, float scale) {
                graphics.pose().pushMatrix();
                graphics.pose().scale(scale, scale);
                text(text, Math.round(centerX / scale - Minecraft.getInstance().font.width(text) / 2f), Math.round(y / scale), color);
                graphics.pose().popMatrix();
            }
        };
    }
}
