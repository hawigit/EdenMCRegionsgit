package hawi.bucket.edenmcregions;

/** Minimal bridge between pre-26 GUI rendering and the 26.x extraction API. */
public interface HudCanvas {
    int width();
    int height();
    void fill(int left, int top, int right, int bottom, int color);
    void text(String text, int x, int y, int color);
    default void component(net.minecraft.network.chat.Component text, int x, int y, int color) { text(text.getString(), x, y, color); }
    void centeredScaledText(String text, int centerX, int y, int color, float scale);
    void centeredScaledComponent(net.minecraft.network.chat.Component text, int centerX, int y, int color, float scale);
}


