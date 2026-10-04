package com.piratecrew.client;

import net.minecraft.client.gui.GuiGraphics;

/** Draws vanilla-looking panels and slots without needing a texture. */
public class GuiDraw {
    public static final int TEXT = 0xFF404040;

    public static void panel(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x + 1, y, x + w - 1, y + h, 0xFF000000);
        g.fill(x, y + 1, x + w, y + h - 1, 0xFF000000);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xFFFFFFFF);
        g.fill(x + 2, y + 2, x + w - 1, y + h - 1, 0xFF555555);
        g.fill(x + 2, y + 2, x + w - 2, y + h - 2, 0xFFC6C6C6);
    }

    /** Recessed box, like the area slots sit in. x,y is the top-left of the 16x16 item area. */
    public static void slot(GuiGraphics g, int x, int y) {
        inset(g, x - 1, y - 1, 18, 18);
    }

    public static void inset(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, 0xFF373737);
        g.fill(x + 1, y + 1, x + w, y + h, 0xFFFFFFFF);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xFF8B8B8B);
    }

    /** A dark parchment-ish strip for list rows. */
    public static void row(GuiGraphics g, int x, int y, int w, int h, boolean alt) {
        g.fill(x, y, x + w, y + h, alt ? 0xFFB4A68C : 0xFFC2B59C);
    }
}
