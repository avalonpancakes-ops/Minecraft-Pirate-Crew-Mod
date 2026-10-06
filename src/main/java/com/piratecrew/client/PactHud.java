package com.piratecrew.client;

import com.piratecrew.network.PactSyncPacket;
import com.piratecrew.pact.SoulPact;
import com.piratecrew.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.gui.overlay.ForgeGui;

/**
 * The Soul Pact badge beside the hotbar: the pact's scroll in a brass frame. While the power recharges
 * a shadow drains away from top to bottom with the seconds left; when it's ready the frame glows; in
 * water it goes dark blue (the sea drains every pact).
 */
public class PactHud {
    private static SoulPact pact;
    private static long ready, duration = 1;
    private static int points;
    private static long techReady, techDuration = 1, ultUntil, ultReady;

    public static void update(PactSyncPacket p) {
        pact = SoulPact.byId(p.pact);
        ready = p.ready;
        duration = Math.max(1, p.duration);
        points = p.points;
        techReady = p.techReady;
        techDuration = Math.max(1, p.techDuration);
        ultUntil = p.ultUntil;
        ultReady = p.ultReady;
    }

    public static void render(ForgeGui gui, GuiGraphics g, float partial, int sw, int sh) {
        Minecraft mc = Minecraft.getInstance();
        if (pact == null || mc.player == null || mc.options.hideGui || mc.player.isSpectator()) return;
        int x = sw / 2 + 91 + 6, y = sh - 24;
        if (mc.player.getMainArm() == HumanoidArm.LEFT && !mc.player.getOffhandItem().isEmpty()) x += 29;
        float t = mc.player.tickCount + partial;
        long now = mc.level == null ? 0 : mc.level.getGameTime();
        long left = Math.max(0, ready - now);
        boolean drained = mc.player.isInWater();
        int frame;
        if (drained) frame = 0xFF2A4A7A;
        else if (left > 0) frame = 0xFF6A5030;
        else frame = lerp(0xFFC8901E, 0xFFFFF0A0, 0.5F + 0.5F * Mth.sin(t * 0.25F));
        boolean inUlt = ultUntil > now;
        if (inUlt && !drained) frame = lerp(0xFF000000 | pact.seal, 0xFFFFFFFF, 0.35F + 0.35F * Mth.sin(t * 0.5F));
        g.fill(x, y, x + 24, y + 24, 0xFF1A0F08);
        g.fill(x + 1, y + 1, x + 23, y + 23, frame);
        g.fill(x + 2, y + 2, x + 22, y + 22, 0xE0101820);
        g.renderItem(new ItemStack(ModItems.SOUL_PACTS.get(pact).get()), x + 4, y + 4);
        if (drained) {
            g.fill(x + 2, y + 2, x + 22, y + 22, 0x903060C0);
            g.drawString(mc.font, "≈", x + 9, y + 8, 0xFFB8E0FF, true);
        } else if (left > 0) {
            int h = (int) Math.ceil(20 * (left / (double) duration));
            g.pose().pushPose();
            g.pose().translate(0, 0, 200);
            g.fill(x + 2, y + 2, x + 22, y + 2 + h, 0xA0000000);
            String s = String.valueOf((left + 19) / 20);
            g.drawString(mc.font, s, x + 12 - mc.font.width(s) / 2, y + 8, 0xFFFFFFFF, true);
            g.pose().popPose();
        } else {
            // ready: the key to press, small, in the corner
            String key = ClientSetup.PACT_POWER.getTranslatedKeyMessage().getString();
            if (key.length() <= 2) {
                g.pose().pushPose();
                g.pose().translate(0, 0, 200);
                g.drawString(mc.font, key, x + 23 - mc.font.width(key), y + 16, 0xFFFFE070, true);
                g.pose().popPose();
            }
        }
        extras(mc, g, x, y, now, t);
    }

    /** Mastery pips and progress above the badge; technique and ultimate slots beside it. */
    private static void extras(Minecraft mc, GuiGraphics g, int x, int y, long now, float t) {
        int rank = com.piratecrew.pact.PactMastery.rank(points);
        int seal = 0xFF000000 | pact.seal;
        // five rank pips
        for (int i = 0; i < 5; i++) {
            int px = x + 1 + i * 5, py = y - 5;
            boolean lit = i < rank;
            g.fill(px, py, px + 3, py + 3, 0xFF1A0F08);
            g.fill(px + 1, py, px + 2, py + 3, lit ? seal : 0xFF4A3A2A);
            g.fill(px, py + 1, px + 3, py + 2, lit ? seal : 0xFF4A3A2A);
        }
        // progress toward the next rank
        if (rank < 5) {
            int from = com.piratecrew.pact.PactMastery.RANK_AT[rank - 1], to = com.piratecrew.pact.PactMastery.RANK_AT[rank];
            float f = Mth.clamp((points - from) / (float) (to - from), 0, 1);
            g.fill(x + 1, y - 1, x + 23, y, 0xFF1A0F08);
            g.fill(x + 1, y - 1, x + 1 + Math.round(22 * f), y, lerp(seal, 0xFFFFFFFF, 0.3F));
        }
        // ultimate timer: a bar draining along the top of the badge
        if (ultUntil > now) {
            float f = (ultUntil - now) / (float) com.piratecrew.pact.PactMastery.ULT_TICKS;
            g.fill(x + 1, y - 1, x + 23, y, 0xFF1A0F08);
            g.fill(x + 1, y - 1, x + 1 + Math.round(22 * f), y, lerp(seal, 0xFFFFFFFF, 0.5F + 0.5F * Mth.sin(t * 0.6F)));
        }
        int sx = x + 26;
        if (rank >= com.piratecrew.pact.PactMastery.TECHNIQUE_RANK) {
            slot(mc, g, sx, y, now, techReady, techDuration, "T", ClientSetup.PACT_TECHNIQUE, seal, t, false);
        }
        if (rank >= com.piratecrew.pact.PactMastery.ULTIMATE_RANK) {
            slot(mc, g, sx, y + 13, now, ultReady, com.piratecrew.pact.PactMastery.ULT_COOLDOWN, "\u2605", ClientSetup.PACT_ULTIMATE, seal, t, ultUntil > now);
        }
    }

    private static void slot(Minecraft mc, GuiGraphics g, int x, int y, long now, long readyAt, long span, String glyph,
                             net.minecraft.client.KeyMapping key, int seal, float t, boolean active) {
        long left = Math.max(0, readyAt - now);
        int frame = active ? lerp(seal, 0xFFFFFFFF, 0.5F + 0.5F * Mth.sin(t * 0.6F))
                : left > 0 ? 0xFF6A5030 : lerp(0xFFC8901E, 0xFFFFF0A0, 0.5F + 0.5F * Mth.sin(t * 0.25F + 1.0F));
        g.fill(x, y, x + 11, y + 11, 0xFF1A0F08);
        g.fill(x + 1, y + 1, x + 10, y + 10, frame);
        g.fill(x + 2, y + 2, x + 9, y + 9, 0xE0101820);
        g.pose().pushPose();
        g.pose().translate(0, 0, 200);
        if (left > 0 && !active) {
            int h = (int) Math.ceil(7 * Math.min(1.0, left / (double) span));
            g.fill(x + 2, y + 2, x + 9, y + 2 + h, 0xA0000000);
            long secs = (left + 19) / 20;
            String s = secs >= 60 ? (secs / 60) + "m" : String.valueOf(secs);
            g.pose().scale(0.5F, 0.5F, 1F);
            g.drawString(mc.font, s, (x + 6) * 2 - mc.font.width(s) / 2, (y + 4) * 2, 0xFFFFFFFF, true);
        } else {
            String k = key.getTranslatedKeyMessage().getString();
            String label = k.length() == 1 ? k : glyph;
            g.pose().scale(0.5F, 0.5F, 1F);
            g.drawString(mc.font, label, (x + 6) * 2 - mc.font.width(label) / 2, (y + 3) * 2 + 1, active ? 0xFFFFFFFF : 0xFFFFE070, true);
        }
        g.pose().popPose();
    }

    private static int lerp(int a, int b, float t) {
        int ar = a >> 16 & 255, ag = a >> 8 & 255, ab = a & 255, br = b >> 16 & 255, bg = b >> 8 & 255, bb = b & 255;
        return 0xFF000000 | (int) (ar + (br - ar) * t) << 16 | (int) (ag + (bg - ag) * t) << 8 | (int) (ab + (bb - ab) * t);
    }
}
