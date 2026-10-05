package com.piratecrew.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

/** A plank-and-parchment toast with a coloured ribbon and a glint that sweeps across as it lands. */
public class PirateToast implements Toast {
    private static final long SHOW_MS = 5500;
    private final ItemStack icon;
    private final String title, detail;
    private final int color;

    public PirateToast(ItemStack icon, String title, String detail, int color) {
        this.icon = icon;
        this.title = title;
        this.detail = detail;
        this.color = 0xFF000000 | color;
    }

    public static void show(ItemStack icon, String title, String detail, int color) {
        Minecraft.getInstance().getToasts().addToast(new PirateToast(icon, title, detail, color));
    }

    @Override
    public Visibility render(GuiGraphics g, ToastComponent toasts, long time) {
        Font font = toasts.getMinecraft().font;
        int w = width(), h = height();
        GuiDraw.tile(g, GuiDraw.WOOD, 0, 0, w, h, 64);
        g.fill(0, 0, w, 1, 0xFF1A0F08);
        g.fill(0, h - 1, w, h, 0xFF1A0F08);
        g.fill(0, 0, 1, h, 0xFF1A0F08);
        g.fill(w - 1, 0, w, h, 0xFF1A0F08);
        GuiDraw.tile(g, GuiDraw.PARCHMENT, 26, 3, w - 29, h - 6, 64);
        g.fill(25, 3, 26, h - 3, 0xFF2A1A0C);
        // ribbon behind the icon
        g.fill(3, 3, 24, h - 3, 0xFF1C2C3A);
        g.fill(3, h - 6, 24, h - 3, color);
        g.renderItem(icon, 6, 8);
        g.drawString(font, Component.literal(title).withStyle(net.minecraft.ChatFormatting.BOLD), 30, 7, darken(color), false);
        g.drawString(font, font.plainSubstrByWidth(detail, w - 36), 30, 18, GuiDraw.TEXT, false);
        // a glint sweeping across in the first moment
        if (time < 900) {
            int gx = (int) (Mth.lerp(time / 900F, -20, w + 20));
            for (int i = 0; i < 6; i++) {
                int x = gx + i - 3;
                int alpha = 0x18 * (i < 3 ? i + 1 : 6 - i);
                if (x > 1 && x < w - 1) g.fill(x, 1, x + 1, h - 1, alpha << 24 | 0xFFFFFF);
            }
        }
        return time >= SHOW_MS ? Visibility.HIDE : Visibility.SHOW;
    }

    /** Ribbon colours are bright; the headline needs to read on parchment. */
    private static int darken(int c) {
        int r = (c >> 16 & 255) * 3 / 5, gr = (c >> 8 & 255) * 3 / 5, b = (c & 255) * 3 / 5;
        return 0xFF000000 | r << 16 | gr << 8 | b;
    }
}
