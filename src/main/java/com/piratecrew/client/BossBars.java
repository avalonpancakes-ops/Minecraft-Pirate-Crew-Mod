package com.piratecrew.client;

import com.piratecrew.PirateCrew;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.CustomizeGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Sundered Sea bosses (names starting with ☠) get a pirate boss bar: a rope-lashed plank gauge with
 * brass caps, the health draining in the boss's colour with a glint rolling along it, and the name in
 * gold above. Other mods' and vanilla boss bars are left alone.
 */
@Mod.EventBusSubscriber(modid = PirateCrew.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class BossBars {
    @SubscribeEvent
    public static void bossBar(CustomizeGuiOverlayEvent.BossEventProgress event) {
        var boss = event.getBossEvent();
        Component name = boss.getName();
        if (!name.getString().startsWith("☠")) return;
        BossMusic.seen();
        event.setCanceled(true);
        event.setIncrement(26);
        GuiGraphics g = event.getGuiGraphics();
        Minecraft mc = Minecraft.getInstance();
        int x = event.getX(), y = event.getY() + 4;
        float t = (System.currentTimeMillis() % 100000L) / 50F;
        int[] c = colours(boss.getColor());
        int w = 182;
        // frame
        g.fill(x - 4, y - 2, x + w + 4, y + 8, 0xFF1A0F08);
        GuiDraw.tile(g, GuiDraw.WOOD, x - 3, y - 1, w + 6, 8, 64);
        g.fill(x, y + 1, x + w, y + 5, 0xFF0A0604);
        // health
        int fill = Math.round(w * Mth.clamp(boss.getProgress(), 0, 1));
        for (int i = 0; i < fill; i++) {
            float k = 0.5F + 0.5F * Mth.sin((i - t * 3F) / 11F);
            g.fill(x + i, y + 1, x + i + 1, y + 5, lerp(c[0], c[1], k * 0.55F));
        }
        if (fill > 0) {
            g.fill(x, y + 1, x + fill, y + 2, 0x70FFFFFF);
            g.fill(x + fill - 1, y + 1, x + fill, y + 5, 0xFFFFFFFF);
        }
        // notches every tenth
        for (int n = 1; n < 10; n++) g.fill(x + n * w / 10, y + 1, x + n * w / 10 + 1, y + 5, 0x50000000);
        // brass caps
        cap(g, x - 7, y - 3);
        cap(g, x + w + 1, y - 3);
        // name
        int nw = mc.font.width(name);
        g.drawString(mc.font, name, x + w / 2 - nw / 2, y - 12, 0xFFFFE070, true);
    }

    private static void cap(GuiGraphics g, int x, int y) {
        g.fill(x, y, x + 6, y + 12, 0xFF1A0F08);
        g.fill(x + 1, y + 1, x + 5, y + 11, 0xFFD8A63A);
        g.fill(x + 1, y + 1, x + 5, y + 2, 0xFFFFE08A);
        g.fill(x + 4, y + 2, x + 5, y + 11, 0xFF8A6420);
        g.fill(x + 2, y + 5, x + 4, y + 7, 0xFF5A3A10);
    }

    private static int[] colours(BossEvent.BossBarColor col) {
        return switch (col) {
            case BLUE -> new int[]{0xFF2A8AD0, 0xFF8AD8FF};
            case PURPLE -> new int[]{0xFF8A3AD0, 0xFFE0A8FF};
            case PINK -> new int[]{0xFFC03A80, 0xFFFFA8D0};
            case GREEN -> new int[]{0xFF1AA070, 0xFF8AF0C8};
            case YELLOW -> new int[]{0xFFC8901E, 0xFFFFE070};
            case WHITE -> new int[]{0xFFB0B0B0, 0xFFFFFFFF};
            default -> new int[]{0xFFB01E1E, 0xFFFF7A5A};
        };
    }

    private static int lerp(int a, int b, float t) {
        int ar = a >> 16 & 255, ag = a >> 8 & 255, ab = a & 255, br = b >> 16 & 255, bg = b >> 8 & 255, bb = b & 255;
        return 0xFF000000 | (int) (ar + (br - ar) * t) << 16 | (int) (ag + (bg - ag) * t) << 8 | (int) (ab + (bb - ab) * t);
    }
}
