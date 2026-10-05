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

    public static void update(PactSyncPacket p) {
        pact = SoulPact.byId(p.pact);
        ready = p.ready;
        duration = Math.max(1, p.duration);
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
    }

    private static int lerp(int a, int b, float t) {
        int ar = a >> 16 & 255, ag = a >> 8 & 255, ab = a & 255, br = b >> 16 & 255, bg = b >> 8 & 255, bb = b & 255;
        return 0xFF000000 | (int) (ar + (br - ar) * t) << 16 | (int) (ag + (bg - ag) * t) << 8 | (int) (ab + (bb - ab) * t);
    }
}
