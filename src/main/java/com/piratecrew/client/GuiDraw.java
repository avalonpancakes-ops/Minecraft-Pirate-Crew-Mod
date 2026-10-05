package com.piratecrew.client;

import com.piratecrew.PirateCrew;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * Pirate-themed GUI pieces: a ship-plank frame with brass corners around a parchment page, recessed
 * slots, parchment list rows and rope dividers. All textures are the mod's own (textures/gui).
 */
public class GuiDraw {
    public static final int TEXT = 0xFF3B2614;
    public static final int TEXT_LIGHT = 0xFFF4E4C0;
    public static final int GOLD = 0xFFE8B84A;

    public static final ResourceLocation PARCHMENT = PirateCrew.id("textures/gui/parchment.png");
    public static final ResourceLocation WOOD = PirateCrew.id("textures/gui/wood.png");
    public static final ResourceLocation ROPE = PirateCrew.id("textures/gui/rope.png");

    private static final int OUTLINE = 0xFF1A0F08;
    private static final int BRASS_DARK = 0xFF8A6420;
    private static final int BRASS = 0xFFD8A63A;
    private static final int BRASS_LIGHT = 0xFFFFE08A;

    /** Tile a 64x64 texture over a rectangle. */
    public static void tile(GuiGraphics g, ResourceLocation tex, int x, int y, int w, int h, int texSize) {
        for (int ty = 0; ty < h; ty += texSize) {
            int th = Math.min(texSize, h - ty);
            for (int tx = 0; tx < w; tx += texSize) {
                int tw = Math.min(texSize, w - tx);
                g.blit(tex, x + tx, y + ty, 0, 0, tw, th, texSize, texSize);
            }
        }
    }

    public static void panel(GuiGraphics g, int x, int y, int w, int h) {
        // planks
        tile(g, WOOD, x, y, w, h, 64);
        g.fill(x, y, x + w, y + 1, OUTLINE);
        g.fill(x, y + h - 1, x + w, y + h, OUTLINE);
        g.fill(x, y, x + 1, y + h, OUTLINE);
        g.fill(x + w - 1, y, x + w, y + h, OUTLINE);
        g.fill(x + 1, y + 1, x + w - 1, y + 2, 0x30FFFFFF);
        // parchment page
        int px = x + 4, py = y + 4, pw = w - 8, ph = h - 8;
        tile(g, PARCHMENT, px, py, pw, ph, 64);
        g.fill(px - 1, py - 1, px + pw + 1, py, 0xFF2A1A0C);
        g.fill(px - 1, py + ph, px + pw + 1, py + ph + 1, 0xFF6A4A2A);
        g.fill(px - 1, py, px, py + ph, 0xFF2A1A0C);
        g.fill(px + pw, py, px + pw + 1, py + ph, 0xFF6A4A2A);
        // aged edges
        g.fill(px, py, px + pw, py + 2, 0x40603A10);
        g.fill(px, py, px + 2, py + ph, 0x40603A10);
        g.fill(px, py + ph - 2, px + pw, py + ph, 0x30603A10);
        g.fill(px + pw - 2, py, px + pw, py + ph, 0x30603A10);
        // brass corner plates
        corner(g, x + 1, y + 1);
        corner(g, x + w - 4, y + 1);
        corner(g, x + 1, y + h - 4);
        corner(g, x + w - 4, y + h - 4);
    }

    private static void corner(GuiGraphics g, int x, int y) {
        g.fill(x, y, x + 3, y + 3, BRASS);
        g.fill(x, y, x + 3, y + 1, BRASS_LIGHT);
        g.fill(x, y, x + 1, y + 3, BRASS_LIGHT);
        g.fill(x + 2, y + 1, x + 3, y + 3, BRASS_DARK);
        g.fill(x + 1, y + 1, x + 2, y + 2, 0xFF5A3A10);
    }

    /** Recessed box, like the area slots sit in. x,y is the top-left of the 16x16 item area. */
    public static void slot(GuiGraphics g, int x, int y) {
        inset(g, x - 1, y - 1, 18, 18);
    }

    public static void inset(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, 0xFF4A3018);
        g.fill(x + 1, y + 1, x + w, y + h, 0xFFF6E7C4);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xFFA88A5E);
        g.fill(x + 1, y + 1, x + w - 1, y + 2, 0x40000000);
    }

    /** Parchment list rows. */
    public static void row(GuiGraphics g, int x, int y, int w, int h, boolean alt) {
        g.fill(x, y, x + w, y + h, alt ? 0xFFD4BC8C : 0xFFE2CDA0);
        g.fill(x, y + h - 1, x + w, y + h, 0x30402000);
    }

    /** A twisted-rope divider line. */
    public static void rope(GuiGraphics g, int x, int y, int w) {
        for (int i = 0; i < w; i += 16) g.blit(ROPE, x + i, y, 0, 0, Math.min(16, w - i), 8, 16, 8);
    }

    /** A wooden plank button face (used by PirateButton). */
    public static void plank(GuiGraphics g, int x, int y, int w, int h, boolean hovered, boolean active) {
        int base = !active ? 0xFF4A3828 : hovered ? 0xFF94653A : 0xFF7A4F2A;
        int light = !active ? 0xFF5E4A38 : hovered ? 0xFFC08A52 : 0xFF9C6A3C;
        int dark = !active ? 0xFF2E2218 : 0xFF3E2814;
        g.fill(x, y, x + w, y + h, OUTLINE);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, base);
        g.fill(x + 1, y + 1, x + w - 1, y + 2, light);
        g.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, dark);
        // grain
        for (int gx = x + 4; gx < x + w - 4; gx += 9) g.fill(gx, y + h / 2, gx + 4, y + h / 2 + 1, 0x22000000);
        if (hovered && active) {
            g.fill(x, y, x + w, y + 1, BRASS);
            g.fill(x, y + h - 1, x + w, y + h, BRASS_DARK);
            g.fill(x, y, x + 1, y + h, BRASS);
            g.fill(x + w - 1, y, x + w, y + h, BRASS_DARK);
        }
        // brass nail heads
        if (w >= 24) {
            g.fill(x + 2, y + h / 2 - 1, x + 3, y + h / 2, active ? BRASS : 0xFF6A5A40);
            g.fill(x + w - 3, y + h / 2 - 1, x + w - 2, y + h / 2, active ? BRASS : 0xFF6A5A40);
        }
    }
}
